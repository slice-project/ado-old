package org.etri.ado;

import org.etri.ado.actor.ActionCommander;
import org.etri.ado.actor.ActionUpdater;
import org.etri.ado.actor.LocationUpdater;
import org.etri.ado.device.emulator.ActionListener;
import org.etri.ado.device.emulator.RobotLocalizer;
import org.etri.ado.device.emulator.SpeakerEmulator;
import org.etri.ado.schedule.ConsoleInRouteBuilder;
import org.etri.ado.schedule.SpeakerScheduler;
import org.javatuples.Pair;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.camel.Camel;
import akka.camel.CamelExtension;

public class SpeakerMain {	

	public static void main(String[] args) throws Exception {
		
		Config conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2551)
				.withFallback(ConfigFactory.load("speaker"));
		
		AgentSystem system = AgentSystem.create("ADO-Demo", conf);
		Camel camel = CamelExtension.get(system.getActorSystem());
		ActorRef scheduler = system.actorOf(SpeakerScheduler.prop(system));
		camel.context().addRoutes(new ConsoleInRouteBuilder(scheduler));		
		
		system.actorOf(ADOActor.prop(system), "ado");	
		ActorRef locationUpdater = system.actorOf(LocationUpdater.props(system));		
		system.actorOf(RobotLocalizer.props(locationUpdater));
		
		ActorRef actionUpdater = system.actorOf(ActionUpdater.props(system));
		system.actorOf(ActionListener.props(actionUpdater));
		
		ActorRef robot = system.actorOf(SpeakerEmulator.prop(Pair.with(1.3f, 1.3f)));
		system.actorOf(ActionCommander.props(robot));		
	}
}
