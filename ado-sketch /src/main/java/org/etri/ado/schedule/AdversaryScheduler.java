package org.etri.ado.schedule;

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

public class AdversaryScheduler extends AbstractActor {

	private static class Tick {	}	
	
	private final LoggingAdapter m_log = Logging.getLogger(getContext().system(), this);	
	
	public static Props prop(AgentSystem system) {
		return Props.create(AdversaryScheduler.class, system);
	}

	private static final Tick s_tick = new Tick();
	private final AgentSystem m_system;
	private ComputationGraph m_model;
	private Cancellable m_task;
		
	public AdversaryScheduler(AgentSystem system) {
		m_system = system;		
	}
	
	@Override
	public void preStart() {
		try {
			String simpleMlp = new ClassPathResource("agent1_mlp.h5").getFile().getPath();
			m_model = KerasModelImport.importKerasModelAndWeights(simpleMlp);
			
			m_task = getContext().system().scheduler().schedule(FiniteDuration.Zero(), 
					FiniteDuration.create(100, TimeUnit.MILLISECONDS), 
					getSelf(), 
					s_tick, 
					getContext().getDispatcher(), 
					getSelf());					
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
		float[][] obs = new float[1][8];
		
		Pair<Float, Float> agent_vel = getObservation("agent1-velocity");
		if ( agent_vel == null ) return;
		
		obs[0][0] = agent_vel.getValue0();
		obs[0][1] = agent_vel.getValue1();
		
		Pair<Float, Float> agent_loc = getObservation("agent1-loc");
		if ( agent_loc == null ) return;
		
		obs[0][2] = agent_loc.getValue0();
		obs[0][3] = agent_loc.getValue1();
		
		Pair<Float, Float> entity_loc = getObservation("agent2-loc");
		if ( entity_loc == null ) return;
		
		obs[0][4] = entity_loc.getValue0() - obs[0][2];
		obs[0][5] = entity_loc.getValue1() - obs[0][3];
		
		Pair<Float, Float> other_loc = getObservation("agent0-loc");
		if ( other_loc == null ) return;
		
		obs[0][6] = other_loc.getValue0() - obs[0][2];
		obs[0][7] = other_loc.getValue1() - obs[0][3];
		
		INDArray input = Nd4j.create(obs);
		INDArray[] output = m_model.output(input);
		
		float deltaX = output[0].getFloat(1) - output[0].getFloat(2);
		deltaX *= 3.0f;
		float deltaY = output[0].getFloat(3) - output[0].getFloat(4);
		deltaY *= 3.0f;
		
		Action action = Action.newBuilder().setCapability("MoveDeltaXY").addActions(deltaX).addActions(deltaY).build();
		getContext().system().eventStream().publish(action);
	}
	
	@SuppressWarnings("unchecked")
	private Pair<Float, Float> getObservation(String obsId) {
		
		CompletionStage<Object> stage = Patterns.ask(m_system.getTupleSpace(), new Get(obsId), Duration.ofSeconds(1000));
		Optional<Pair<Float, Float>> pair = null;
		try {
			pair = (Optional<Pair<Float, Float>>)stage.toCompletableFuture().get();
		}
		catch ( Throwable e ) {
			e.printStackTrace();
		}
		
		return pair.isPresent() ? pair.get() : null;
	}	
}
