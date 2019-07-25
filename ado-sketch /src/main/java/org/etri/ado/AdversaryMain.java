package org.etri.ado;

import org.etri.ado.device.emulator.ListenerEmulator;
import org.etri.ado.device.emulator.RobotLocalizer;
import org.etri.ado.device.emulator.RobotSpeedometer;
import org.etri.ado.schedule.AdversaryScheduler;
import org.etri.ado.task.ActionCommander;
import org.etri.ado.task.LocationUpdater;
import org.etri.ado.task.VelocityUpdater;
import org.javatuples.Pair;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;

public class AdversaryMain {

	public static void main(String[] args) throws Exception {
		Config conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2552)
				.withFallback(ConfigFactory.load("agent1"));
		
		AgentSystem system = AgentSystem.create("ADO-Demo", conf);
		system.actorOf(ADOActor.prop(system), "ado");
		
		ActorRef locationUpdater = system.actorOf(LocationUpdater.props(system));
		ActorRef velocityUpdater = system.actorOf(VelocityUpdater.props(system));
		
		system.actorOf(RobotLocalizer.props(locationUpdater));
		system.actorOf(RobotSpeedometer.props(velocityUpdater));
		
		ActorRef robot = system.actorOf(ListenerEmulator.prop(Pair.with(0.1f, -0.3f)));
		system.actorOf(ActionCommander.props(robot));
		
		system.actorOf(AdversaryScheduler.prop(system), "scheduler");		
	}
}
