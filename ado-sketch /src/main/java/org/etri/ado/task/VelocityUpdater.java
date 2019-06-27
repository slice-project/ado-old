package org.etri.ado.task;

import org.etri.ado.AgentSystem;
import org.etri.ado.agent.TupleSpace;
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

	public static Props props(AgentSystem system, String agentId) {
		return Props.create(VelocityUpdater.class, system.getTupleSpace(), agentId);
	}

	private final ActorRef m_tupleSpace;
	private final String m_key;

	public VelocityUpdater(ActorRef tupleSpace, String agentId) {
		m_tupleSpace = tupleSpace;
		m_key = agentId + "-velocity";
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(AddVelocity.class, this::receiveAddLocation).build();
	}

	private void receiveAddLocation(AddVelocity add) {
		KeyValue<String,Pair<Float,Float>> tuple = KeyValue.with(m_key, add.velocity);
		TupleSpace.Put<Pair<Float,Float>> putCmd = new TupleSpace.Put<Pair<Float, Float>>(tuple);
		m_tupleSpace.tell(putCmd, self());
	}
}