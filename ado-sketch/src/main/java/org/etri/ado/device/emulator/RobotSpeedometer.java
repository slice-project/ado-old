package org.etri.ado.device.emulator;

import org.etri.ado.actor.VelocityUpdater.AddVelocity;
import org.etri.ado.device.AbstractSensor;

import akka.actor.ActorRef;
import akka.actor.Props;

public class RobotSpeedometer extends AbstractSensor<AddVelocity> {	

	public static Props props(ActorRef task) {
		return Props.create(RobotSpeedometer.class, AddVelocity.class, task);
	}	

	public RobotSpeedometer(Class<AddVelocity> eventType, ActorRef task) {
		super(eventType, task);
	}

	@Override
	protected void execute(AddVelocity command) {
		getTask().forward(command, getContext());
	}

	@Override
	protected void executeAny(Object msg) {
		
	}

}
