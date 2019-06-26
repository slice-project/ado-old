package org.etri.ado.agent;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.collect.Sets;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.ddata.DistributedData;
import akka.cluster.ddata.Key;
import akka.cluster.ddata.LWWMap;
import akka.cluster.ddata.LWWMapKey;
import akka.cluster.ddata.ORMultiMap;
import akka.cluster.ddata.ORMultiMapKey;
import akka.cluster.ddata.Replicator;
import akka.cluster.ddata.Replicator.GetFailure;
import akka.cluster.ddata.Replicator.GetResponse;
import akka.cluster.ddata.Replicator.GetSuccess;
import akka.cluster.ddata.Replicator.NotFound;
import akka.cluster.ddata.Replicator.ReadAll;
import akka.cluster.ddata.Replicator.ReadConsistency;
import akka.cluster.ddata.Replicator.Update;
import akka.cluster.ddata.Replicator.UpdateFailure;
import akka.cluster.ddata.Replicator.UpdateSuccess;
import akka.cluster.ddata.Replicator.UpdateTimeout;
import akka.cluster.ddata.Replicator.WriteAll;
import akka.cluster.ddata.Replicator.WriteConsistency;
import akka.cluster.ddata.SelfUniqueAddress;
import scala.Option;
import scala.concurrent.duration.Duration;

@SuppressWarnings("unchecked")
public class AgentRegistry extends AbstractActor {

	private final static WriteConsistency writeMajority = new WriteAll(Duration.create(3, SECONDS));
	private final static ReadConsistency readMajority = new ReadAll(Duration.create(3, SECONDS));
	// #read-write-majority

	public static class Put {
		public final AgentInfo agent;
		
		public Put(AgentInfo agent) {
			this.agent = agent;
		}
	}
	
	public static class GetAll implements Serializable {
		private static final long serialVersionUID = 8236922294327736690L;
		
		private static class Context {
			private final ActorRef sender;
			
			private Context(ActorRef sender) {
				this.sender = sender;
			}
		}		
	}
	
	public static class GetById implements Serializable {
		private static final long serialVersionUID = 2968893148086681132L;
		public final String id;
		
		public GetById(String id) {
			this.id = id;
		}
		
		private static class Context {
			private final ActorRef sender;
			private final String key;
			
			private Context(ActorRef sender, String key) {
				this.sender = sender;
				this.key = key;
			}			
		}
	}
	
	public static class GetByCapabilities implements Serializable {
		private static final long serialVersionUID = -2137076549589260672L;
		public final String[] capabilities;
		
		public GetByCapabilities(String[] capabilities) {
			this.capabilities = capabilities;
		}
		private static class Context {
			private final ActorRef sender;
			private final String[] capabilities;
			
			private Context(ActorRef sender, String[] capabilities) {
				this.sender = sender;
				this.capabilities = capabilities;
			}			
		}		
	}

	public static Props props() {
		return Props.create(AgentRegistry.class);
	}

	private final ActorRef m_replicator = DistributedData.get(context().system()).replicator();
	private final SelfUniqueAddress m_node = DistributedData.get(getContext().getSystem()).selfUniqueAddress();
	
	private final Key<LWWMap<String,AgentInfo>> m_idToAgentKey;
	private final Key<ORMultiMap<String,AgentInfo>> m_capaToAgentKey;

	public AgentRegistry() {
		this.m_idToAgentKey = LWWMapKey.create("id-to-agent-mapping");
		this.m_capaToAgentKey = ORMultiMapKey.create("capa-to-agent-mapping");
	}

	@Override
	public Receive createReceive() {
		return matchGetAll().orElse(matchGetById()).orElse(matchGetByCapabilities()).orElse(matchPut()).orElse(matchOther());
	}

	private Receive matchPut() {		
		return receiveBuilder().match(Put.class, this::receivePut).build();
	}
	
	private void receivePut(Put cmd) {
		Update<LWWMap<String,AgentInfo>> update1 = new Update<>(m_idToAgentKey, LWWMap.create(), writeMajority,
				map -> updateIdToAgents(map, cmd.agent));
		m_replicator.tell(update1, self());
		
		Update<ORMultiMap<String,AgentInfo>> update2 = new Update<>(m_capaToAgentKey, ORMultiMap.create(), writeMajority,
				map -> updateCapaToAgents(map, cmd.agent));
		m_replicator.tell(update2, self());
	}
	
	private LWWMap<String,AgentInfo> updateIdToAgents(LWWMap<String,AgentInfo> map, AgentInfo info) {
		return map.put(m_node, info.id, info);
	}	
	
	private ORMultiMap<String,AgentInfo> updateCapaToAgents(ORMultiMap<String,AgentInfo> map, AgentInfo info) {
		SortedSet<String> capabilities = new TreeSet<>(Arrays.asList(info.capabilities));

		for ( int i = 1; i <= capabilities.size(); ++i ) {
			Set<Set<String>> combinations = Sets.combinations(capabilities, i);			
			Iterator<Set<String>> iter = combinations.iterator();
			
			while ( iter.hasNext() ) {
				Set<String> combination = iter.next();
				map = map.addBinding(m_node, combination.toString(), info);
			}			
		}
		
		return map;
	}
	
	private Receive matchGetAll() {
		return receiveBuilder().match(GetAll.class, s -> receiveGetAll(s))
				.match(GetSuccess.class, this::isResponseToGetAll,
						g -> receiveGetAllSuccess((GetSuccess<LWWMap<String,AgentInfo>>) g))
				.match(NotFound.class, this::isResponseToGetAll,
						n -> receiveByAllNotFound((NotFound<LWWMap<String,AgentInfo>>) n))
				.match(GetFailure.class, this::isResponseToGetById,
						f -> receiveGetAllFailure((GetFailure<LWWMap<String,AgentInfo>>) f))
				.build();
	}
	
	private void receiveGetAll(GetAll g) {
		Optional<Object> ctx = Optional.of(new GetAll.Context(sender()));
		m_replicator.tell(new Replicator.Get<>(m_idToAgentKey, readMajority, ctx), self());
	}

	private boolean isResponseToGetAll(GetResponse<?> response) {
		return response.key().equals(m_idToAgentKey) && (response.getRequest().orElse(null) instanceof GetAll.Context);
	}

	private void receiveGetAllSuccess(GetSuccess<LWWMap<String,AgentInfo>> g) {
		GetAll.Context ctx = (GetAll.Context) g.getRequest().get();
		Map<String, AgentInfo> entries = g.dataValue().getEntries();
				
		ctx.sender.tell(entries.values().toArray(new AgentInfo[entries.size()]), self());
	}

	private void receiveByAllNotFound(NotFound<LWWMap<String,AgentInfo>> n) {
		GetAll.Context ctx = (GetAll.Context) n.getRequest().get();
		ctx.sender.tell(Optional.empty(), self());
	}

	private void receiveGetAllFailure(GetFailure<LWWMap<String,AgentInfo>> f) {
		// ReadMajority failure, try again with local read
		Optional<Object> ctx = Optional.of(sender());
		m_replicator.tell(new Replicator.Get<>(m_idToAgentKey, Replicator.readLocal(), ctx), self());
	}		
	
	private Receive matchGetById() {
		return receiveBuilder().match(GetById.class, s -> receiveGetById(s))
				.match(GetSuccess.class, this::isResponseToGetById,
						g -> receiveGetByIdSuccess((GetSuccess<LWWMap<String,AgentInfo>>) g))
				.match(NotFound.class, this::isResponseToGetById,
						n -> receiveByIdNotFound((NotFound<LWWMap<String,AgentInfo>>) n))
				.match(GetFailure.class, this::isResponseToGetById,
						f -> receiveGetByIdFailure((GetFailure<LWWMap<String,AgentInfo>>) f))
				.build();
	}
	
	private void receiveGetById(GetById g) {
		Optional<Object> ctx = Optional.of(new GetById.Context(sender(), g.id));
		m_replicator.tell(new Replicator.Get<>(m_idToAgentKey, readMajority, ctx), self());
	}

	private boolean isResponseToGetById(GetResponse<?> response) {
		return response.key().equals(m_idToAgentKey) && (response.getRequest().orElse(null) instanceof GetById.Context);
	}

	private void receiveGetByIdSuccess(GetSuccess<LWWMap<String,AgentInfo>> g) {
		GetById.Context ctx = (GetById.Context) g.getRequest().get();
		Option<AgentInfo> option = g.dataValue().get(ctx.key);
		Optional<AgentInfo> optional = Optional.<AgentInfo> ofNullable(option.getOrElse(() -> null));
				
		ctx.sender.tell(optional, self());
	}

	private void receiveByIdNotFound(NotFound<LWWMap<String,AgentInfo>> n) {
		GetById.Context ctx = (GetById.Context) n.getRequest().get();
		ctx.sender.tell(Optional.empty(), self());
	}

	private void receiveGetByIdFailure(GetFailure<LWWMap<String,AgentInfo>> f) {
		// ReadMajority failure, try again with local read
		Optional<Object> ctx = Optional.of(sender());
		m_replicator.tell(new Replicator.Get<>(m_idToAgentKey, Replicator.readLocal(), ctx), self());
	}	
	
	private Receive matchGetByCapabilities() {
		return receiveBuilder().match(GetByCapabilities.class, s -> receiveGetByCapabilities(s))
				.match(GetSuccess.class, this::isResponseToGetByCapabilities,
						g -> receiveGetByCapabilitiesSuccess((GetSuccess<ORMultiMap<String,AgentInfo>>) g))
				.match(NotFound.class, this::isResponseToGetById,
						n -> receiveByCapabilitiesNotFound((NotFound<ORMultiMap<String,AgentInfo>>) n))
				.match(GetFailure.class, this::isResponseToGetById,
						f -> receiveGetByCapabilitiesFailure((GetFailure<ORMultiMap<String,AgentInfo>>) f))
				.build();
	}	
	
	private void receiveGetByCapabilities(GetByCapabilities g) {
		Optional<Object> ctx = Optional.of(new GetByCapabilities.Context(sender(), g.capabilities));
		m_replicator.tell(new Replicator.Get<>(m_capaToAgentKey, readMajority, ctx), self());
	}

	private boolean isResponseToGetByCapabilities(GetResponse<?> response) {
		return response.key().equals(m_capaToAgentKey) && (response.getRequest().orElse(null) instanceof GetByCapabilities.Context);
	}	
	
	private void receiveGetByCapabilitiesSuccess(GetSuccess<ORMultiMap<String,AgentInfo>> g) {
		GetByCapabilities.Context ctx = (GetByCapabilities.Context) g.getRequest().get();
		SortedSet<String> capabilities = new TreeSet<>(Arrays.asList(ctx.capabilities));
		
		Option<scala.collection.immutable.Set<AgentInfo>> option = g.dataValue().get(capabilities.toString());
		Optional<scala.collection.immutable.Set<AgentInfo>> optional = 
				Optional.<scala.collection.immutable.Set<AgentInfo>> ofNullable(option.getOrElse(() -> null));
				
		ctx.sender.tell(optional, self());
	}

	private void receiveByCapabilitiesNotFound(NotFound<ORMultiMap<String,AgentInfo>> n) {
		GetById.Context ctx = (GetById.Context) n.getRequest().get();
		ctx.sender.tell(Optional.empty(), self());
	}

	private void receiveGetByCapabilitiesFailure(GetFailure<ORMultiMap<String,AgentInfo>> f) {
		// ReadMajority failure, try again with local read
		Optional<Object> ctx = Optional.of(sender());
		m_replicator.tell(new Replicator.Get<>(m_capaToAgentKey, Replicator.readLocal(), ctx), self());
	}		

	private Receive matchOther() {
		return receiveBuilder().match(UpdateSuccess.class, u -> {
			System.out.println("Update Success => " + u);
		}).match(UpdateTimeout.class, t -> {
			// will eventually be replicated
		}).match(UpdateFailure.class, f -> {
			throw new IllegalStateException("Unexpected failure: " + f);
		}).build();
	}
}