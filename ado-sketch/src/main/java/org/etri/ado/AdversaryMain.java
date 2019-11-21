package org.etri.ado;

import org.etri.ado.actor.ActionCommander;
import org.etri.ado.actor.LocationUpdater;
import org.etri.ado.actor.VelocityUpdater;
import org.etri.ado.device.emulator.AdversaryEmulator;
import org.etri.ado.device.emulator.RobotLocalizer;
import org.etri.ado.device.emulator.RobotSpeedometer;
import org.etri.ado.device.ros.ROSAgentDevice;
import org.etri.ado.schedule.AdversaryScheduler;
import org.javatuples.Pair;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;

public class AdversaryMain {

	public static void main(String[] args) throws Exception {
		Config conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2554)
				.withFallback(ConfigFactory.load("agent3"));
		
		AgentSystem system = AgentSystem.create("ADO-Demo", conf);
		system.actorOf(ADOActor.prop(system), "ado");
	
		ActorRef locationUpdater = system.actorOf(LocationUpdater.props(system));
		ActorRef velocityUpdater = system.actorOf(VelocityUpdater.props(system));
		
		system.actorOf(RobotLocalizer.props(locationUpdater));
		system.actorOf(RobotSpeedometer.props(velocityUpdater));	
		
		ActorRef robot = null;
		long interval = 1000;
		if ( args.length > 0 ) {
			robot = system.actorOf(AdversaryEmulator.prop(Pair.with(1f, 1f)));
		}
		else {
			robot = system.actorOf(ROSAgentDevice.prop("192.168.0.185"));
		}
	
		system.actorOf(ActionCommander.props(robot));		
		system.actorOf(AdversaryScheduler.prop(system, interval), "scheduler");
	}
}
