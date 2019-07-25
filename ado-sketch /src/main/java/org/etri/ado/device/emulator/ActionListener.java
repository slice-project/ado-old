package org.etri.ado.device.emulator;

import org.etri.ado.device.AbstractSensor;
import org.etri.ado.task.ActionUpdater.AddAction;

import akka.actor.ActorRef;
import akka.actor.Props;

public class ActionListener extends AbstractSensor<AddAction> {	

	public static Props props(ActorRef task) {
		return Props.create(ActionListener.class, AddAction.class, task);
	}	

	public ActionListener(Class<AddAction> eventType, ActorRef task) {
		super(eventType, task);
	}

	@Override
	protected void execute(AddAction command) {
		getTask().forward(command, getContext());
	}

	@Override
	protected void executeAny(Object msg) {
		
	}

}
