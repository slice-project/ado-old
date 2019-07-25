package org.etri.ado;

import org.etri.ado.device.emulator.LandmarkEmulator;
import org.etri.ado.device.emulator.RobotLocalizer;
import org.etri.ado.task.LocationUpdater;
import org.javatuples.Pair;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;

public class LandmarkMain {

	public static void main(String[] args) throws Exception {
		
		Config conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2553)
				.withFallback(ConfigFactory.load("red"));
				
		AgentSystem system = AgentSystem.create("ADO-Demo", conf);
		system.actorOf(ADOActor.prop(system), "ado");
		
		ActorRef locationUpdater = system.actorOf(LocationUpdater.props(system));
		
		system.actorOf(RobotLocalizer.props(locationUpdater));
		ActorRef landmark = system.actorOf(LandmarkEmulator.prop(Pair.with(0f, 0f)));
	
	}

}
