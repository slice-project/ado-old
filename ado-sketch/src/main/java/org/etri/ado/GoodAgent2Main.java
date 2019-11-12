package org.etri.ado;

import org.etri.ado.actor.ActionCommander;
import org.etri.ado.actor.LocationUpdater;
import org.etri.ado.actor.VelocityUpdater;
import org.etri.ado.device.emulator.GoodAgentEmulator;
import org.etri.ado.device.emulator.RobotLocalizer;
import org.etri.ado.device.emulator.RobotSpeedometer;
import org.etri.ado.device.ros.ROSAgentDevice;
import org.etri.ado.schedule.GoodAgent2Scheduler;
import org.javatuples.Pair;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;

public class GoodAgent2Main {	

	public static void main(String[] args) throws Exception {
		Config conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2553)
				.withFallback(ConfigFactory.load("good2"));
		
		AgentSystem system = AgentSystem.create("ADO-Demo", conf);
		system.actorOf(ADOActor.prop(system), "ado");
	
		ActorRef locationUpdater = system.actorOf(LocationUpdater.props(system));
		ActorRef velocityUpdater = system.actorOf(VelocityUpdater.props(system));
		
		system.actorOf(RobotLocalizer.props(locationUpdater));
		system.actorOf(RobotSpeedometer.props(velocityUpdater));	
		
		ActorRef robot = system.actorOf(ROSAgentDevice.prop("192.168.0.198"));
		system.actorOf(ActionCommander.props(robot));
		
		system.actorOf(GoodAgent2Scheduler.prop(system), "scheduler");			
	}
}
