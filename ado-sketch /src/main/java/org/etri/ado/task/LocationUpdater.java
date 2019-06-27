package org.etri.ado.task;

import org.etri.ado.AgentSystem;
import org.etri.ado.agent.TupleSpace;
import org.javatuples.KeyValue;
import org.javatuples.Pair;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class LocationUpdater extends AbstractActor {

	public static class AddLocation {
		public final Pair<Float, Float> location;

		public AddLocation(Pair<Float, Float> loc) {
			location = loc;
		}
	}

	public static Props props(AgentSystem system, String agentId) {
		return Props.create(LocationUpdater.class, system.getTupleSpace(), agentId);
	}

	private final ActorRef m_tupleSpace;
	private final String m_key;

	public LocationUpdater(ActorRef tupleSpace, String agentId) {
		m_tupleSpace = tupleSpace;
		m_key = agentId + "-loc";
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(AddLocation.class, this::receiveAddLocation).build();
	}
	
	private void receiveAddLocation(AddLocation add) {
		KeyValue<String,Pair<Float,Float>> tuple = KeyValue.with(m_key, add.location);
		TupleSpace.Put<Pair<Float,Float>> putCmd = new TupleSpace.Put<Pair<Float, Float>>(tuple);
		m_tupleSpace.tell(putCmd, self());
	}
}