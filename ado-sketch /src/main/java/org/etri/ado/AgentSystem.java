package org.etri.ado;

import org.etri.ado.agent.AgentActor;
import org.etri.ado.agent.AgentRegistry;
import org.etri.ado.agent.TupleSpace;

import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class AgentSystem {
	
	private final ActorSystem m_system;
	private final ActorRef m_registry;
	private final ActorRef m_tuples;
	private final ActorRef m_agent;
		
	public static AgentSystem create(String name, Config config) {
		ActorSystem system = ActorSystem.create(name, config);
		return new AgentSystem(system);
	}
	
	private AgentSystem(ActorSystem system) {
		m_system = system;
		
		m_registry = system.actorOf(AgentRegistry.props());
		m_tuples = system.actorOf(TupleSpace.props(system));
		m_agent = system.actorOf(AgentActor.props(this));
	}
	
	public ActorRef getAgent() {
		return m_agent;
	}
	
	public ActorRef getAgentRegistry() {
		return m_registry;
	}
	
	public ActorRef getTupleSpace() {
		return m_tuples;
	}
	
	public ActorSystem getActorSystem() {
		return m_system;
	}
	
	public ActorRef actorOf(Props props) {
		return m_system.actorOf(props);
	}
	
	public ActorRef actorOf(Props props, String name) {
		return m_system.actorOf(props, name);
	}
}
