package org.etri.ado;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;

import org.etri.ado.actor.ActionCommander.MoveDeltaXY;
import org.etri.ado.actor.LocationUpdater;
import org.etri.ado.actor.VelocityUpdater;
import org.etri.ado.device.emulator.AdversaryEmulator;
import org.etri.ado.device.emulator.RobotLocalizer;
import org.etri.ado.device.emulator.RobotSpeedometer;
import org.etri.ado.device.ros.ROSAgentDevice;
import org.javatuples.Pair;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;

public class PreyAgentMain implements KeyListener {	
	
	private static final float delta = 10f;
	private final ActorRef m_robot;	
	
	public PreyAgentMain(ActorRef robot) {
		m_robot = robot;
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();				
		switch ( keyCode ) {
			case KeyEvent.VK_UP:
				m_robot.tell(new MoveDeltaXY(Pair.with(0f, delta)), ActorRef.noSender());
				System.out.println("Move delat to up!");
				break;
			case KeyEvent.VK_DOWN:
				m_robot.tell(new MoveDeltaXY(Pair.with(0f, -delta)), ActorRef.noSender());
				System.out.println("Move delat to down!");
				break;
			case KeyEvent.VK_LEFT:
				m_robot.tell(new MoveDeltaXY(Pair.with(-delta, 0f)), ActorRef.noSender());
				System.out.println("Move delat to left!");
				break;
			case KeyEvent.VK_RIGHT:
				m_robot.tell(new MoveDeltaXY(Pair.with(delta, 0f)), ActorRef.noSender());
				System.out.println("Move delat to right!");
				break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {			
	}	

	public static void main(String[] args) throws Exception {
		
		Config conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2554)
				.withFallback(ConfigFactory.load("agent3"));
		
		AgentSystem system = AgentSystem.create("ADO-Demo", conf);
		system.actorOf(ADOActor.prop(system), "ado");
	
		ActorRef locationUpdater = system.actorOf(LocationUpdater.props(system));
		ActorRef velocityUpdater = system.actorOf(VelocityUpdater.props(system));
			
		
		system.actorOf(RobotLocalizer.props(locationUpdater));
		system.actorOf(RobotSpeedometer.props(velocityUpdater));	
		
		ActorRef robot = null;
		if ( args.length > 0 ) {
			robot = system.actorOf(AdversaryEmulator.prop(Pair.with(1f, 1f)));
		}
		else {
			robot = system.actorOf(ROSAgentDevice.prop("192.168.0.185"));
		}
		
		JFrame f = new JFrame();
		f.setSize(300,200);
		f.setLayout(null);
		f.setVisible(true);
		f.addKeyListener(new PreyAgentMain(robot));		
	}
}
