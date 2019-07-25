package org.etri.ado;

import org.etri.ado.device.emulator.LandmarkEmulator;
import org.etri.ado.device.emulator.RobotLocalizer;
import org.etri.ado.schedule.ConsoleInRouteBuilder;
import org.etri.ado.schedule.LandmarkScheduler;
import org.etri.ado.schedule.SpeakerScheduler;
import org.etri.ado.task.ActionCommander;
import org.etri.ado.task.LocationUpdater;
import org.javatuples.Pair;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.camel.Camel;
import akka.camel.CamelExtension;

public class BlueLandmarkMain {

	public static void main(String[] args) throws Exception {
		
		Config conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2555)
				.withFallback(ConfigFactory.load("blue"));
				
		AgentSystem system = AgentSystem.create("ADO-Demo", conf);		
		Camel camel = CamelExtension.get(system.getActorSystem());
		ActorRef scheduler = system.actorOf(LandmarkScheduler.prop(system));
		camel.context().addRoutes(new ConsoleInRouteBuilder(scheduler));
				
		system.actorOf(ADOActor.prop(system), "ado");		
		ActorRef locationUpdater = system.actorOf(LocationUpdater.props(system));		
		system.actorOf(RobotLocalizer.props(locationUpdater));
		
		ActorRef landmark = system.actorOf(LandmarkEmulator.prop(Pair.with(1.5f, 1.9f)));
		system.actorOf(ActionCommander.props(landmark));
	}

}
