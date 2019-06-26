package org.etri.ado.agent;

import org.etri.ado.config.Settings;
import org.etri.ado.config.SettingsImpl;

import akka.actor.AbstractActor;

public abstract class AbstractAgent extends AbstractActor implements Agent {
	
	private final SettingsImpl m_settings;
	protected final AgentInfo m_info;
	
	public AbstractAgent() {
		m_settings = Settings.SettingsProvider.get(getContext().system());
		m_info = new AgentInfo(m_settings.AGENT_ID, getSelf(), m_settings.CAPABILITIES);
	}
	
	public AgentInfo getAgentInfo() {
		return m_info;
	}
}
