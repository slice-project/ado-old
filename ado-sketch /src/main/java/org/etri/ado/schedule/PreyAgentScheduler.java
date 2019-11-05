package org.etri.ado.schedule;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.etri.ado.AgentSystem;
import org.etri.ado.agent.tuplespace.Get;
import org.etri.ado.gateway.openai.OpenAI.Action;
import org.javatuples.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;

import akka.actor.AbstractActor;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import scala.concurrent.duration.FiniteDuration;

public class PreyAgentScheduler extends AbstractActor implements KeyListener {

	private static class Tick {	}	
	
	private final LoggingAdapter m_log = Logging.getLogger(getContext().system(), this);	
	
	public static Props prop(AgentSystem system) {
		return Props.create(PreyAgentScheduler.class, system);
	}

	private static final Tick s_tick = new Tick();
	private final AgentSystem m_system;
	private ComputationGraph m_model;
	private Cancellable m_task;
		
	public PreyAgentScheduler(AgentSystem system) {
		m_system = system;
	}
	
	@Override
	public void preStart() {
		try {
//			String simpleMlp = new ClassPathResource("agent0_mlp.h5").getFile().getPath();
//			m_model = KerasModelImport.importKerasModelAndWeights(simpleMlp);
//			
//			m_task = getContext().system().scheduler().schedule(FiniteDuration.Zero(), 
//					FiniteDuration.create(100, TimeUnit.MILLISECONDS), 
//					getSelf(), 
//					s_tick, 
//					getContext().getDispatcher(), 
//					getSelf());					
		}
		catch ( Throwable e ) {
			m_log.error(e, e.getMessage());
		}
	}
	
	@Override
	public void postStop() {
		m_task.cancel();
	}	
	
	@Override
	public Receive createReceive() {
		return receiveBuilder().match(Tick.class, this::receiveTick).build();
	}
	
	private void receiveTick(Tick tick) {
		float[][] obs = new float[1][10];
		
		Optional<Pair<Float, Float>> agent_vel = getObservation("agent0-velocity");
		if ( !agent_vel.isPresent() ) return;
		obs[0][0] = agent_vel.get().getValue0();
		obs[0][1] = agent_vel.get().getValue1();
		
		Optional<Pair<Float, Float>> agent_loc = getObservation("agent0-loc");
		if ( !agent_loc.isPresent() ) return;
		
		obs[0][2] = agent_loc.get().getValue0();
		obs[0][3] = agent_loc.get().getValue1();
		
		Optional<Pair<Float, Float>> entity_loc = getObservation("agent2-loc");
		if ( !entity_loc.isPresent() ) return;
		
		obs[0][4] = entity_loc.get().getValue0() - obs[0][2];
		obs[0][5] = entity_loc.get().getValue1() - obs[0][3];
		
		Optional<Pair<Float, Float>> other_loc = getObservation("agent1-loc");
		if ( !other_loc.isPresent() ) return;
		
		obs[0][6] = other_loc.get().getValue0() - obs[0][2];
		obs[0][7] = other_loc.get().getValue1() - obs[0][3];
		
		Optional<Pair<Float, Float>> other_vel = getObservation("agent1-velocity");
		if ( !other_vel.isPresent() ) return;
		
		obs[0][8] = other_vel.get().getValue0();
		obs[0][9] = other_vel.get().getValue1();
		
		INDArray input = Nd4j.create(obs);
		INDArray[] output = m_model.output(input);
		
		float deltaX = output[0].getFloat(1) - output[0].getFloat(2);
		deltaX *= 4.0f;
		float deltaY = output[0].getFloat(3) - output[0].getFloat(4);
		deltaY *= 4.0f;
		
		Action action = Action.newBuilder().setCapability("MoveDeltaXY").addActions(deltaX).addActions(deltaY).build();
		getContext().system().eventStream().publish(action);
	}
	
	@SuppressWarnings("unchecked")
	private Optional<Pair<Float, Float>> getObservation(String obsId) {
		
		CompletionStage<Object> stage = Patterns.ask(m_system.getTupleSpace(), new Get(obsId), Duration.ofSeconds(1000));
		Optional<Pair<Float, Float>> pair = null;
		try {
			pair = (Optional<Pair<Float, Float>>)stage.toCompletableFuture().get();
		}
		catch ( Throwable e ) {
			e.printStackTrace();
		}
		
		return pair;
	}

	@Override
	public void keyTyped(KeyEvent e) {
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		
	}
}
