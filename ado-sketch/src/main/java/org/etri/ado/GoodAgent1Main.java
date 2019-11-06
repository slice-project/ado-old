package org.etri.ado;

import org.etri.ado.actor.ActionCommander;
import org.etri.ado.actor.LocationUpdater;
import org.etri.ado.actor.VelocityUpdater;
import org.etri.ado.device.emulator.GoodAgentEmulator;
import org.etri.ado.device.emulator.RobotLocalizer;
import org.etri.ado.device.emulator.RobotSpeedometer;
import org.etri.ado.schedule.GoodAgent1Scheduler;
import org.javatuples.Pair;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;

public class GoodAgent1Main {	

	public static void main(String[] args) throws Exception {
		Config conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2552)
				.withFallback(ConfigFactory.load("good1"));
		
		AgentSystem system = AgentSystem.create("ADO-Demo", conf);
		system.actorOf(ADOActor.prop(system), "ado");
	
		ActorRef locationUpdater = system.actorOf(LocationUpdater.props(system));
		ActorRef velocityUpdater = system.actorOf(VelocityUpdater.props(system));
		
		system.actorOf(RobotLocalizer.props(locationUpdater));
		system.actorOf(RobotSpeedometer.props(velocityUpdater));	
		
		ActorRef robot = system.actorOf(GoodAgentEmulator.prop(Pair.with(0.3f, 0.7f)));
		system.actorOf(ActionCommander.props(robot));
		
		system.actorOf(GoodAgent1Scheduler.prop(system), "scheduler");
	}
}