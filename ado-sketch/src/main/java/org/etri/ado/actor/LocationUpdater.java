package org.etri.ado.actor;

import org.etri.ado.AgentSystem;
import org.etri.ado.agent.tuplespace.Put;
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

	public static Props props(AgentSystem system) {
		return Props.create(LocationUpdater.class, system.getTupleSpace());
	}

	private final ActorRef m_tupleSpace;
	private final String m_key = "loc";

	public LocationUpdater(ActorRef tupleSpace) {
		m_tupleSpace = tupleSpace;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(AddLocation.class, this::receiveAddLocation).build();
	}
	
	private void receiveAddLocation(AddLocation add) {
		KeyValue<String,Pair<Float,Float>> tuple = KeyValue.with(m_key, add.location);
		Put<Pair<Float,Float>> putCmd = new Put<Pair<Float, Float>>(tuple);
		m_tupleSpace.tell(putCmd, self());
	}
}