package org.etri.ado.task;

import org.etri.ado.AgentSystem;
import org.etri.ado.agent.TupleSpace;
import org.javatuples.KeyValue;
import org.javatuples.Triplet;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ActionUpdater extends AbstractActor {

	public static class AddAction {
		public final Triplet<Float,Float,Float> action;

		public AddAction(Triplet<Float,Float,Float> action) {
			this.action = action;
		}
	}

	public static Props props(AgentSystem system) {
		return Props.create(ActionUpdater.class, system.getTupleSpace(), system.getAgentId());
	}

	private final ActorRef m_tupleSpace;
	private final String m_key;

	public ActionUpdater(ActorRef tupleSpace, String agentId) {
		m_tupleSpace = tupleSpace;
		m_key = agentId + "-action";
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(AddAction.class, this::receiveAddAction).build();
	}

	private void receiveAddAction(AddAction add) {
		KeyValue<String,Triplet<Float,Float,Float>> tuple = KeyValue.with(m_key, add.action);
		TupleSpace.Put<Triplet<Float,Float,Float>> putCmd = new TupleSpace.Put<Triplet<Float,Float,Float>>(tuple);
		m_tupleSpace.tell(putCmd, self());
	}
}