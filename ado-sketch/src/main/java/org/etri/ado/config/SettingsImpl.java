package org.etri.ado.config;

import java.util.List;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;

import akka.actor.Extension;

public class SettingsImpl implements Extension {

  public final String AGENT_ID;
  public final String AGENT_ROLE;
  public final String[] CAPABILITIES;

  public SettingsImpl(Config config) {
    AGENT_ID = config.getString("agent.id");
    AGENT_ROLE = config.getString("agent.role");
    ConfigList capas = config.getList("agent.capabilities");
    List<Object> capaList = capas.unwrapped();
    CAPABILITIES = capaList.toArray(new String[capaList.size()]);    
  }
}

