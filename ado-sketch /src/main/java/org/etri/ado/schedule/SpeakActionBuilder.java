package org.etri.ado.schedule;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.etri.ado.gateway.openai.OpenAI.Action;

import akka.camel.CamelMessage;

@SuppressWarnings("deprecation")
public class SpeakActionBuilder implements Function<CamelMessage, Action> {

	private CommandLineParser m_parser = new DefaultParser();
	private Options m_options = new Options();
	private HelpFormatter m_help = new HelpFormatter();	
	
	public SpeakActionBuilder() {
		m_options.addOption(Option.builder().longOpt("target").argName("string").desc("target landmark ( red | green | blue )").hasArg().build());
	}
	
	@Override
	public Action apply(CamelMessage msg) {
		
		String cmdline = (String) msg.body();
		String[] splited = StringUtils.split(cmdline);
		
		CommandLine line = null;
		try {
			line = m_parser.parse(m_options, splited);
		} 
		catch ( ParseException e ) {
			m_help.printHelp("speak", m_options);
			return null;
		}
		
		List<String> argList = line.getArgList();
		if ( argList.isEmpty() ) {
			m_help.printHelp("speak", m_options);
			return null;
		}
		
		String cmd = line.getArgList().get(0);		
		if ( !line.hasOption("target") ) {
			m_help.printHelp("speak", m_options);
			return null;
		}
		
		String target = line.getOptionValue("target");
		if ( target.equals("red") ) {
			return Action.newBuilder().setCapability("Speak").addActions(0.65f).addActions(0.15f).addActions(0.15f).build();
		}
		else if ( target.equals("green") ) {
			return Action.newBuilder().setCapability("Speak").addActions(0.15f).addActions(0.65f).addActions(0.15f).build();
		}
		else if ( target.equals("blue") ) {
			return Action.newBuilder().setCapability("Speak").addActions(0.15f).addActions(0.15f).addActions(0.65f).build();
		}
		else {
			return null;
		}
	}

}
