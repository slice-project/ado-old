package org.etri.ado.device.emulator;

import org.etri.ado.actor.LocationUpdater.AddLocation;
import org.etri.ado.device.AbstractSensor;

import akka.actor.ActorRef;
import akka.actor.Props;

public class RobotLocalizer extends AbstractSensor<AddLocation> {	

	public static Props props(ActorRef task) {
		return Props.create(RobotLocalizer.class, AddLocation.class, task);
	}	

	public RobotLocalizer(Class<AddLocation> eventType, ActorRef task) {
		super(eventType, task);
	}

	@Override
	protected void execute(AddLocation command) {
		getTask().forward(command, getContext());
	}

	@Override
	protected void executeAny(Object msg) {
		
	}

}
