package org.etri.ado;

import org.etri.ado.actor.ActionCommander;
import org.etri.ado.actor.LocationUpdater;
import org.etri.ado.device.emulator.LandmarkEmulator;
import org.etri.ado.device.emulator.RobotLocalizer;
import org.etri.ado.schedule.ConsoleInRouteBuilder;
import org.etri.ado.schedule.LandmarkScheduler;
import org.javatuples.Pair;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.camel.Camel;
import akka.camel.CamelExtension;

public class RedLandmarkMain {

	public static void main(String[] args) throws Exception {
		
		Config conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2553)
				.withFallback(ConfigFactory.load("red"));
				
		
		AgentSystem system = AgentSystem.create("ADO-Demo", conf);		
		Camel camel = CamelExtension.get(system.getActorSystem());
		ActorRef scheduler = system.actorOf(LandmarkScheduler.prop(system));
		camel.context().addRoutes(new ConsoleInRouteBuilder(scheduler));
				
		system.actorOf(ADOActor.prop(system), "ado");		
		ActorRef locationUpdater = system.actorOf(LocationUpdater.props(system));		
		system.actorOf(RobotLocalizer.props(locationUpdater));
		
		ActorRef landmark = system.actorOf(LandmarkEmulator.prop(Pair.with(-0.5f, 0.7f)));
		system.actorOf(ActionCommander.props(landmark));
	}

}
