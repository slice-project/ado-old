package org.etri.ado.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.etri.ado.AgentSystem;
import org.etri.ado.gateway.openai.OpenAI.Action;

import akka.actor.AbstractActor;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.camel.CamelMessage;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

public class LandmarkScheduler extends AbstractActor {

	private final LoggingAdapter m_log = Logging.getLogger(getContext().system(), this);	
	
	public static Props prop(AgentSystem system) {
		return Props.create(LandmarkScheduler.class, system);
	}

	private final AgentSystem m_system;
	private Cancellable m_task;
	
	private List<Function<CamelMessage,Action>> m_handlers = new ArrayList<Function<CamelMessage,Action>>();
		
	public LandmarkScheduler(AgentSystem system) {
		m_system = system;
	}
	
	@Override
	public void preStart() {
		m_handlers.add(new MoveToXYActionBuilder());
	}
	
	@Override
	public void postStop() {
		m_task.cancel();
	}	
	
	@Override
	public Receive createReceive() {
		ReceiveBuilder builder = ReceiveBuilder.create();
		builder.match(CamelMessage.class, this::receiveCommand);
		builder.matchAny(this::unhandled);
		
		return builder.build();
	}
	
	private void receiveCommand( CamelMessage msg) {
		m_handlers.stream().forEach(handler -> {
			Action action = handler.apply(msg);
			if (action == null ) return;
			getContext().system().eventStream().publish(action);	
		});		
	}
}
