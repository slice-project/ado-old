package org.etri.ado.task;

import org.etri.ado.gateway.openai.OpenAI.Action;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ActionCommander extends AbstractActor {

	public static class MoveDeltaXY {
		public final Pair<Float, Float> delta;

		public MoveDeltaXY(Pair<Float, Float> delta) {
			this.delta = delta;
		}
	}
	
	public static class MoveToXY {
		public final Pair<Float, Float> loc;

		public MoveToXY(Pair<Float, Float> loc) {
			this.loc = loc;
		}
	}
	
	public static class Speak {
		public final Triplet<Float, Float, Float> target;
		
		public Speak(Triplet<Float, Float, Float> target) {
			this.target = target;
		}
	}

	public static Props props(ActorRef device) {
		return Props.create(ActionCommander.class, device);
	}
	
	private static final String MoveDeltaXY = "MoveDeltaXY";
	private static final String MoveToXY = "MoveToXY";
	private static final String Speak = "Speak";

	private final ActorRef m_device;

	public ActionCommander(ActorRef device) {
		m_device = device;
	}

	@Override
	public void preStart() {
		getContext().system().getEventStream().subscribe(getContext().getSelf(), Action.class);
	}		
	
	@Override
	public Receive createReceive() {
		return receiveBuilder().match(Action.class, this::receiveAction).build();
	}

	private void receiveAction(Action action) {
		String capability = action.getCapability();
		Pair<Float,Float> pair = Pair.with(action.getActions(0), action.getActions(1));
		if ( capability.equals(MoveDeltaXY) ) {			
			m_device.tell(new MoveDeltaXY(pair), getSelf());
		}
		else if ( capability.equals(MoveToXY) ) {
			m_device.tell(new MoveToXY(pair), getSelf());
		}
		else if ( capability.equals(Speak) ) {
			Triplet<Float,Float,Float> triplet = Triplet.with(action.getActions(0), action.getActions(1), action.getActions(2));
			m_device.tell(new Speak(triplet), getSelf());
		}
	}

}