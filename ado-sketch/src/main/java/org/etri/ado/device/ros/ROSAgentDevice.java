package org.etri.ado.device.ros;

import org.etri.ado.actor.ActionCommander.MoveDeltaXY;
import org.etri.ado.actor.LocationUpdater.AddLocation;
import org.etri.ado.actor.VelocityUpdater.AddVelocity;
import org.javatuples.Pair;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import ros.RosBridge;
import ros.RosListenDelegate;
import ros.SubscriptionRequestMsg;
import ros.msgs.geometry_msgs.Vector3;
import ros.tools.MessageUnpacker;

public class ROSAgentDevice extends AbstractActor {
		
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private static final float SCALE = 1.0f / 6.0f ;
	private static final float BORDER = 2.0f;
	
	private final RosBridge m_ros = new RosBridge();
	private final String m_host;
	private float m_xPos;
	private float m_yPos;
	
	public static Props prop(String host) {
		return Props.create(ROSAgentDevice.class, host);
	}
	
	public ROSAgentDevice(String host) {
		m_host = host;
	}
	
	@Override
	public void preStart() {

		StringBuilder builder = new StringBuilder();
		builder.append("ws://");
		builder.append(m_host);
		builder.append(":");
		builder.append(9090);
			
		m_ros.connect(builder.toString(), true);
		log.info("connection established to ({}:{}]", m_host, 9090);
		
		m_ros.subscribe(SubscriptionRequestMsg.generate("/waffle/current_loc")
				.setType("geometry_msgs/Vector3")
				.setThrottleRate(1)
				.setQueueLength(1),
			new RosListenDelegate() {
				@Override
				public void receive(JsonNode data, String stringRep) {
					MessageUnpacker<Vector3> unpacker = new MessageUnpacker<Vector3>(Vector3.class);
					Vector3 msg = unpacker.unpackRosMessage(data);
					m_xPos = (float)msg.x;
					m_yPos = (float)msg.y;
					getContext().system().eventStream().publish(new AddLocation(Pair.with(m_xPos, m_yPos)));
					log.debug("publish[AddLocation({}, {}]", msg.x, msg.y);				
				}
			}
		);
		
		m_ros.subscribe(SubscriptionRequestMsg.generate("/waffle/velocity")
				.setType("geometry_msgs/Vector3")
				.setThrottleRate(1)
				.setQueueLength(1),
			new RosListenDelegate() {
				@Override
				public void receive(JsonNode data, String stringRep) {
					MessageUnpacker<Vector3> unpacker = new MessageUnpacker<Vector3>(Vector3.class);
					Vector3 msg = unpacker.unpackRosMessage(data);
					getContext().system().eventStream().publish(new AddVelocity(Pair.with((float)msg.x, (float)msg.y)));
					log.debug("publish[AddVelocity({}, {}]", msg.x, msg.y);						
				}
			}
		);		
	}
	
	@Override
	public void postStop() {

	}	

	@Override
	public Receive createReceive() {
		ReceiveBuilder builder = ReceiveBuilder.create();
		builder.match(MoveDeltaXY.class, this::receiveActionForce);
		builder.matchAny(this::unhandled);
		
		return builder.build();
	}
	
	private void receiveActionForce(MoveDeltaXY force) {
		
		float deltaX = force.delta.getValue0() * SCALE;
		float deltaY = force.delta.getValue1() * SCALE;
		log.info("received[MoveDeltaXY({}, {}]", deltaX, deltaY);
		
		if ( Math.abs(m_xPos + deltaX) > BORDER ) {
			deltaX =  -deltaX;
		}
		
		if ( Math.abs(m_yPos + deltaY) > BORDER ) {
			deltaY = -deltaY;
		}
		
		Vector3 delta = new Vector3(deltaX, deltaY, 0);
		m_ros.publish("/waffle/move_delta",  "geometry_msgs/Vector3", delta);
		log.info("MOVE : ({},{}) => ({},{})", m_xPos, m_yPos, m_xPos + deltaX, m_yPos + deltaY);
	}
}