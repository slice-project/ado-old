package org.etri.ado;

import org.etri.ado.agent.AgentRegistry;
import org.etri.ado.agent.TupleSpace;

import akka.actor.AbstractActor;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.cluster.client.ClusterClientReceptionist;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import akka.japi.pf.ReceiveBuilder;

public class ADOActor extends AbstractActor {

	public static Props prop(AgentSystem system) {
		return ClusterSingletonManager.props(Props.create(ADOActor.class,system), PoisonPill.getInstance(),
				ClusterSingletonManagerSettings.create(system.getActorSystem()));
	}
	
	private final AgentSystem m_system;
		
	public ADOActor(AgentSystem system) {
		m_system = system;
		ClusterClientReceptionist.get(getContext().system()).registerService(getSelf());
	}
	
	@Override
	public Receive createReceive() {
		ReceiveBuilder builder = ReceiveBuilder.create();
		builder.match(AgentRegistry.GetAll.class, this::receiveGetAll);
		builder.match(AgentRegistry.GetById.class, this::receiveGetById);
		builder.match(AgentRegistry.GetByCapabilities.class, this::receiveGetByCapabilities);
		builder.match(TupleSpace.Get.class, this::receiveGetTuple);
		builder.matchAny(this::unhandled);
		
		return builder.build();		
	}
	
	private void receiveGetAll(AgentRegistry.GetAll cmd) {
		m_system.getAgentRegistry().forward(cmd, getContext());
	}	

	private void receiveGetById(AgentRegistry.GetById cmd) {
		m_system.getAgentRegistry().forward(cmd, getContext());
	}
		
	private void receiveGetByCapabilities(AgentRegistry.GetByCapabilities cmd) {
		m_system.getAgentRegistry().forward(cmd, getContext());
	}
	
	private void receiveGetTuple(TupleSpace.Get cmd) {
		m_system.getTupleSpace().forward(cmd, getContext());
	}
}
