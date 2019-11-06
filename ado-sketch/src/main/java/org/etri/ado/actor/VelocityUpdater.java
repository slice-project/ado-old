package org.etri.ado.actor;

import org.etri.ado.AgentSystem;
import org.etri.ado.agent.tuplespace.Put;
import org.javatuples.KeyValue;
import org.javatuples.Pair;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class VelocityUpdater extends AbstractActor {

	public static class AddVelocity {
		public final Pair<Float, Float> velocity;

		public AddVelocity(Pair<Float, Float> vel) {
			velocity = vel;
		}
	}

	public static Props props(AgentSystem system) {
		return Props.create(VelocityUpdater.class, system.getTupleSpace());
	}

	private final ActorRef m_tupleSpace;
	private final String m_key = "velocity";

	public VelocityUpdater(ActorRef tupleSpace) {
		m_tupleSpace = tupleSpace;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(AddVelocity.class, this::receiveAddLocation).build();
	}

	private void receiveAddLocation(AddVelocity add) {
		KeyValue<String,Pair<Float,Float>> tuple = KeyValue.with(m_key, add.velocity);
		Put<Pair<Float,Float>> putCmd = new Put<Pair<Float, Float>>(tuple);
		m_tupleSpace.tell(putCmd, self());
	}
}