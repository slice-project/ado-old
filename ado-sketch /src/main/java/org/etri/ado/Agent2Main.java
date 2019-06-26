package org.etri.ado;

import org.etri.ado.agent.AgentRegistry;
import org.etri.ado.agent.TupleSpace;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class Agent2Main {

	public static void main(String[] args) throws Exception {
		
		Config conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2553)
				.withFallback(ConfigFactory.load());
		ActorSystem system = ActorSystem.create("ClusterSystem", conf);
		
		ActorRef space = system.actorOf(TupleSpace.props(system));	
		ActorRef registry = system.actorOf(AgentRegistry.props());
		
		Thread.sleep(3 * 1000);		
	}

}
