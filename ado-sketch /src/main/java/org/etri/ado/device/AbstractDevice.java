package org.etri.ado.device;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;

public abstract class AbstractDevice<T> extends AbstractActor {
	
	protected final Class<T> typeParameterClass;
	private final ActorRef m_task;
	
	public AbstractDevice(Class<T> typeParamClass, ActorRef task) {
		this.typeParameterClass = typeParamClass;
		m_task = task;
	}

	protected abstract void execute(T command);
	protected abstract void executeAny(Object msg);
	
	
	protected ActorRef getTask() {
		return m_task;
	}	
	
	@Override
	public Receive createReceive() {
		return receiveBuilder().match(typeParameterClass, this::execute).matchAny(this::executeAny).build();
	}
	
}
