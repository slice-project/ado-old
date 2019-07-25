package org.etri.ado.agent;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.Serializable;
import java.util.Optional;

import org.javatuples.KeyValue;
import org.javatuples.Tuple;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.ddata.DistributedData;
import akka.cluster.ddata.Key;
import akka.cluster.ddata.LWWMap;
import akka.cluster.ddata.LWWMapKey;
import akka.cluster.ddata.Replicator;
import akka.cluster.ddata.Replicator.GetFailure;
import akka.cluster.ddata.Replicator.GetResponse;
import akka.cluster.ddata.Replicator.GetSuccess;
import akka.cluster.ddata.Replicator.NotFound;
import akka.cluster.ddata.Replicator.ReadConsistency;
import akka.cluster.ddata.Replicator.ReadMajority;
import akka.cluster.ddata.Replicator.Update;
import akka.cluster.ddata.Replicator.UpdateFailure;
import akka.cluster.ddata.Replicator.UpdateSuccess;
import akka.cluster.ddata.Replicator.UpdateTimeout;
import akka.cluster.ddata.Replicator.WriteAll;
import akka.cluster.ddata.Replicator.WriteConsistency;
import akka.cluster.ddata.Replicator.WriteMajority;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.cluster.ddata.SelfUniqueAddress;
import scala.Option;
import scala.concurrent.duration.Duration;

@SuppressWarnings("unchecked")
public class TupleSpace <T extends Tuple> extends AbstractActor {
	
	private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

	// #read-write-majority
	private final WriteConsistency writeMajority = new WriteMajority(Duration.create(3, SECONDS));
	private final WriteAll writeAll = new WriteAll(Duration.create(3, SECONDS));
	private final static ReadConsistency readMajority = new ReadMajority(Duration.create(3, SECONDS));
	// #read-write-majority

	public static class Put<T>  implements Serializable {
		private static final long serialVersionUID = -816719379737729692L;
		
		public final KeyValue<String,T> tuple;

		public Put(KeyValue<String,T> tuple) {
			this.tuple = tuple;
		}
		
		private static class Context<T> {
			private final KeyValue<String,T> tuple;
			
			private Context(KeyValue<String, T> tuple) {
				this.tuple = tuple;
			}
			
		}
	}
	
	public static class Get implements Serializable {
		private static final long serialVersionUID = 5538286467938886952L;
		
		public final String key;

		public Get(String key) {
			this.key = key;
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

	public static class Remove implements Serializable {
		private static final long serialVersionUID = -3048484489517982772L;
		
		public final String key;

		public Remove(String key) {
			this.key = key;
		}
	}

	public static <T> Props props(ActorSystem system) {
		Key<LWWMap<String,T>> key = LWWMapKey.create("tuplespace-" + system.name());
		return Props.create(TupleSpace.class, key);
	}

	private final ActorRef m_replicator = DistributedData.get(context().system()).replicator();
	private final SelfUniqueAddress m_node = DistributedData.get(getContext().getSystem()).selfUniqueAddress();
	private final Key<LWWMap<String,T>> m_dataKey;
	
	public TupleSpace(Key<LWWMap<String,T>> dataKey) {
		m_dataKey = dataKey;
	}

	@Override
	public Receive createReceive() {
		return matchGet().orElse(matchPut()).orElse(matchRemove()).orElse(matchOther());
	}

	// #get-cart
	private Receive matchGet() {
		return receiveBuilder().match(Get.class, s -> receiveGet(s))
				.match(GetSuccess.class, this::isResponseToGet,
						g -> receiveGetSuccess((GetSuccess<LWWMap<String,T>>) g))
				.match(NotFound.class, this::isResponseToGet,
						n -> receiveNotFound((NotFound<LWWMap<String,T>>) n))
				.match(GetFailure.class, this::isResponseToGet,
						f -> receiveGetFailure((GetFailure<LWWMap<String,T>>) f))
				.build();
	}

	private void receiveGet(Get g) {
		Optional<Object> ctx = Optional.of(new Get.Context(sender(), g.key));
		m_replicator.tell(new Replicator.Get<>(m_dataKey, readMajority, ctx), self());
	}

	private boolean isResponseToGet(GetResponse<?> response) {
		return response.key().equals(m_dataKey) && (response.getRequest().orElse(null) instanceof Get.Context);
	}

	private void receiveGetSuccess(GetSuccess<LWWMap<String,T>> g) {
		Get.Context ctx = (Get.Context) g.getRequest().get();
		Option<T> option = g.dataValue().get(ctx.key);
		Optional<T> optional = Optional.<T> ofNullable(option.getOrElse(() -> null));
		
		ctx.sender.tell(optional, self());
	}

	private void receiveNotFound(NotFound<LWWMap<String,T>> n) {
		Get.Context ctx = (Get.Context) n.getRequest().get();
		ctx.sender.tell(Optional.empty(), self());
	}

	private void receiveGetFailure(GetFailure<LWWMap<String,T>> f) {
		// ReadMajority failure, try again with local read
		Optional<Object> ctx = f.getRequest();
		m_replicator.tell(new Replicator.Get<>(m_dataKey, Replicator.readLocal(), ctx), self());
	}

	private Receive matchPut() {
		return receiveBuilder().match(Put.class, this::receivePut).build();
	}

	private void receivePut(Put<T> p) {
		Optional<Object> ctx = Optional.of(new Put.Context<T>(p.tuple));
		Update<LWWMap<String,T>> update = new Update<>(m_dataKey, LWWMap.create(), writeAll, ctx,
				space -> updateTuple(space, p.tuple));
		m_replicator.tell(update, self());
	}

	private LWWMap<String,T> updateTuple(LWWMap<String,T> space, KeyValue<String,T> tuple) {
		T value = tuple.getValue();
		return space.put(m_node, tuple.getKey(), value);
	}

	private Receive matchRemove() {
		return receiveBuilder().match(Remove.class, this::receiveRemove)
				.match(GetSuccess.class, this::isResponseToRemove,
						g -> receiveRemoveSuccess((GetSuccess<LWWMap<String,T>>) g))
				.match(GetFailure.class, this::isResponseToRemove,
						f -> receiveRemoveGetFailure((GetFailure<LWWMap<String,T>>) f))
				.match(NotFound.class, this::isResponseToRemove, n -> {
					/* nothing to remove */})
				.build();
	}

	private void receiveRemove(Remove rm) {
		// Try to fetch latest from a majority of nodes first, since ORMap
		// remove must have seen the item to be able to remove it.
		Optional<Object> ctx = Optional.of(rm);
		m_replicator.tell(new Replicator.Get<>(m_dataKey, readMajority, ctx), self());
	}

	private void receiveRemoveSuccess(GetSuccess<LWWMap<String,T>> g) {
		Remove rm = (Remove) g.getRequest().get();
		removeTuple(rm.key);
	}

	private void receiveRemoveGetFailure(GetFailure<LWWMap<String,T>> f) {
		// ReadMajority failed, fall back to best effort local value
		Remove rm = (Remove) f.getRequest().get();
		removeTuple(rm.key);
	}

	private void removeTuple(String key) {
		Update<LWWMap<String,T>> update = new Update<>(m_dataKey, LWWMap.create(), writeMajority,
				space -> space.remove(m_node, key));
		m_replicator.tell(update, self());
	}

	private boolean isResponseToRemove(GetResponse<?> response) {
		return response.key().equals(m_dataKey) && (response.getRequest().orElse(null) instanceof Remove);
	}

	private Receive matchOther() {
		return receiveBuilder().match(UpdateSuccess.class, u -> {
			 Put.Context<Tuple> ctx = (org.etri.ado.agent.TupleSpace.Put.Context<Tuple>) u.getRequest().get();
			 logger.info("updated - " + ctx.tuple.getKey() + " : " + ctx.tuple.getValue());
		}).match(UpdateTimeout.class, t -> {
			// will eventually be replicated
		}).match(UpdateFailure.class, f -> {
			throw new IllegalStateException("Unexpected failure: " + f);
		}).build();
	}

}