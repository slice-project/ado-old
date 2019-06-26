package org.etri.ado.device;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public abstract class AbstractSensor<T> extends AbstractDevice<T> {
	
	protected LoggingAdapter m_logger = Logging.getLogger(getContext().system(), this);
	
	public AbstractSensor(Class<T> typeParamClass, ActorRef task) {
		super(typeParamClass, task);
	}
	
	@Override
	public void preStart() {
		getContext().system().getEventStream().subscribe(getContext().getSelf(), typeParameterClass);
	}
}
