package org.etri.ado.agent;

import org.javatuples.Tuple;

public interface Agent {

	AgentInfo getAgentInfo();
	Tuple getObservation(String id);
}
