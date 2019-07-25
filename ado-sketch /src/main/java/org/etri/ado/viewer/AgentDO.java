package org.etri.ado.viewer;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.etri.ado.agent.AgentInfo;
import org.etri.ado.agent.AgentRegistry;
import org.etri.ado.agent.TupleSpace;
import org.javatuples.Pair;

import akka.actor.ActorRef;
import akka.cluster.client.ClusterClient.SendToAll;
import akka.pattern.Patterns;
import scala.collection.immutable.Set;

public class AgentDO {
	
	private static final String ADO_PATH = "/user/ado/singleton";
	
	private final ActorRef m_clusterClient;
	
	public AgentDO(ActorRef clusterClient) {
		m_clusterClient = clusterClient;
	}
	
	@SuppressWarnings("unchecked")
	public Optional<AgentInfo> getAgentById(String agentId) {
		SendToAll msg = new SendToAll(ADO_PATH, new AgentRegistry.GetById(agentId));
		CompletionStage<Object> getById = Patterns.ask(m_clusterClient, msg, Duration.ofSeconds(1));
		Optional<AgentInfo> agentInfo = Optional.empty();
		try {
			agentInfo = (Optional<AgentInfo>)getById.toCompletableFuture().get();		
		} 
		catch ( Throwable ignored ) { }		
	
		return agentInfo;
	}

	public AgentInfo[] getAgentAll() {
		SendToAll msg = new SendToAll(ADO_PATH, new AgentRegistry.GetAll());
		CompletionStage<Object> getAll = 
				Patterns.ask(m_clusterClient, msg, Duration.ofSeconds(1));
		
		AgentInfo[] agentInfos = new AgentInfo[0];
		try {		
			agentInfos = (AgentInfo[])getAll.toCompletableFuture().get();
		}
		catch ( Throwable ignored ) { }
		
		return agentInfos;
	}	
	
	@SuppressWarnings("unchecked")
	public Optional<Set<AgentInfo>> getAgentOf(String[] capas) {
		SendToAll msg = new SendToAll(ADO_PATH, new AgentRegistry.GetByCapabilities(capas));
		CompletionStage<Object> getByCapa = 
				Patterns.ask(m_clusterClient, msg, Duration.ofSeconds(1));
		
		Optional<Set<AgentInfo>> agentInfos = Optional.empty();
		try {		
			agentInfos = (Optional<Set<AgentInfo>>)getByCapa.toCompletableFuture().get();
		}
		catch ( Throwable ignored ) { }
		
		return agentInfos;
	}

	@SuppressWarnings("unchecked")
	public Optional<Pair<Float, Float>> getObservation(String obsId) {
		
		SendToAll msg = new SendToAll(ADO_PATH, new TupleSpace.Get(obsId));
		CompletionStage<Object> stage = Patterns.ask(m_clusterClient, msg, Duration.ofSeconds(1));
		Optional<Pair<Float, Float>> pair = Optional.empty();
		try {
			pair = (Optional<Pair<Float, Float>>)stage.toCompletableFuture().get();
		}
		catch ( Throwable ignored ) { }
		
		return pair;
	}
}
