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
public class MoveToXYActionBuilder implements Function<CamelMessage, Action> {

	private CommandLineParser m_parser = new DefaultParser();
	private Options m_options = new Options();
	private HelpFormatter m_help = new HelpFormatter();	
	
	public MoveToXYActionBuilder() {
		m_options.addOption(Option.builder().longOpt("x").argName("float").desc("x coodidate").hasArg().build());
		m_options.addOption(Option.builder().longOpt("y").argName("float").desc("y coodidate").hasArg().build());		
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
			m_help.printHelp("goto", m_options);
			return null;
		}
		
		List<String> argList = line.getArgList();
		if ( argList.isEmpty() ) {
			m_help.printHelp("goto", m_options);
			return null;
		}
		
		String cmd = line.getArgList().get(0);		
		if ( !line.hasOption("x") || !line.hasOption("y") ) {
			m_help.printHelp("goto", m_options);
			return null;
		}
		
		float x = Float.parseFloat(line.getOptionValue("x"));
		float y = Float.parseFloat(line.getOptionValue("y"));
		
		
		return  Action.newBuilder().setCapability("MoveToXY").addActions(x).addActions(y).build();
	}

}
