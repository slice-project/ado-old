package org.etri.ado.agent;

import org.etri.ado.AgentSystem;
import org.etri.ado.gateway.openai.OpenAI.Action;
import org.javatuples.Tuple;

import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;

public class AgentActor extends AbstractAgent {
	
	public static Props props(AgentSystem system) {
		return Props.create(AgentActor.class, system);
	}	
	
	private final AgentSystem m_system;

	public AgentActor(AgentSystem system) {		
		m_system = system;
	}
	
	@Override
	public void preStart() {
		m_system.getAgentRegistry().tell(new AgentRegistry.Put(m_info), getSelf());
	}

	@Override
	public Receive createReceive() {
		ReceiveBuilder builder = ReceiveBuilder.create();
		builder.match(Action.class, this::receiveAction);
		builder.matchAny(this::unhandled);
		
		return builder.build();
	}
	
	private void receiveAction(Action action) {
		getContext().system().eventStream().publish(action);
	}

	@Override
	public Tuple getObservation(String id) {
		// TODO Auto-generated method stub
		return null;
	}

}
