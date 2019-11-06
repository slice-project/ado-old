package org.etri.ado;

import org.etri.ado.actor.ActionCommander;
import org.etri.ado.actor.LocationUpdater;
import org.etri.ado.actor.VelocityUpdater;
import org.etri.ado.device.emulator.ListenerEmulator;
import org.etri.ado.device.emulator.RobotLocalizer;
import org.etri.ado.device.emulator.RobotSpeedometer;
import org.etri.ado.schedule.ConsoleInRouteBuilder;
import org.etri.ado.schedule.ListenerScheduler;
import org.javatuples.Pair;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.camel.Camel;
import akka.camel.CamelExtension;

public class ListenerMain {

	public static void main(String[] args) throws Exception {
		
		Config conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2552)
				.withFallback(ConfigFactory.load("listener"));
		
		AgentSystem system = AgentSystem.create("ADO-Demo", conf);		
		Camel camel = CamelExtension.get(system.getActorSystem());
		ActorRef scheduler = system.actorOf(ListenerScheduler.prop(system));
		camel.context().addRoutes(new ConsoleInRouteBuilder(scheduler));		
		

		system.actorOf(ADOActor.prop(system), "ado");		
		ActorRef locationUpdater = system.actorOf(LocationUpdater.props(system));
		ActorRef velocityUpdater = system.actorOf(VelocityUpdater.props(system));
		
		system.actorOf(RobotLocalizer.props(locationUpdater));
		system.actorOf(RobotSpeedometer.props(velocityUpdater));
		
		ActorRef robot = system.actorOf(ListenerEmulator.prop(Pair.with(0f, 0f)));
		system.actorOf(ActionCommander.props(robot));

	}
}

