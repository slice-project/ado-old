package org.etri.ado;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Agent3Main {

	public static void main(String[] args) throws Exception {
		Config conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2554)
				.withFallback(ConfigFactory.load("agent3"));
				
		AgentSystem system = AgentSystem.create("ADO-Demo", conf);
		system.actorOf(ADOActor.prop(system), "ado");
	}

}
