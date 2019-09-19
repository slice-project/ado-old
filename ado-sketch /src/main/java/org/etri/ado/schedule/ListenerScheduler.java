package org.etri.ado.schedule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.etri.ado.AgentSystem;
import org.etri.ado.agent.tuplespace.Get;
import org.etri.ado.agent.tuplespace.SubscribeTo;
import org.etri.ado.gateway.openai.OpenAI.Action;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.javatuples.Tuple;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;

import akka.actor.AbstractActor;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.camel.CamelMessage;
import akka.cluster.ddata.LWWMap;
import akka.cluster.ddata.Replicator.Changed;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import scala.concurrent.duration.FiniteDuration;

public class ListenerScheduler extends AbstractActor {

	private static class Tick {	}	
	
	private final LoggingAdapter m_log = Logging.getLogger(getContext().system(), this);	
	
	public static Props prop(AgentSystem system) {
		return Props.create(ListenerScheduler.class, system);
	}

	private static final Tick s_tick = new Tick();
	private final AgentSystem m_system;
	private MultiLayerNetwork m_model;
	private Cancellable m_task;
	
	private List<Function<CamelMessage,Action>> m_handlers = new ArrayList<Function<CamelMessage,Action>>();
		
	public ListenerScheduler(AgentSystem system) {
		m_system = system;
	}
	
	@Override
	public void preStart() {
		try {
			String simpleMlp = new ClassPathResource("listener_mlp.h5").getFile().getPath();
			m_model = KerasModelImport.importKerasSequentialModelAndWeights(simpleMlp);
			
			m_task = getContext().system().scheduler().schedule(FiniteDuration.Zero(), 
					FiniteDuration.create(100, TimeUnit.MILLISECONDS), 
					getSelf(), 
					s_tick, 
					getContext().getDispatcher(), 
					getSelf());	
			
			m_handlers.add(new MoveToXYActionBuilder());
			m_system.getTupleSpace().tell(new SubscribeTo("agent0"), getSelf());
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
		ReceiveBuilder builder = ReceiveBuilder.create();
		builder.match(Changed.class, this::receiveChanged);
		builder.match(Tick.class, this::receiveTick);
		builder.match(CamelMessage.class, this::receiveCommand);
		builder.matchAny(this::unhandled);
		
		return builder.build();
	}
	
	private void receiveChanged(Changed<LWWMap<String,Tuple>> g) {
		System.out.println(g.key());
		System.out.println(g.dataValue());
	}
	
	private void receiveCommand( CamelMessage msg) {
		m_handlers.stream().forEach(handler -> {
			Action action = handler.apply(msg);
			if (action == null ) return;
			getContext().system().eventStream().publish(action);	
		});		
	}
	
	private void receiveTick(Tick tick) {
		float[][] obs = new float[1][11];
		
		Pair<Float, Float> this_vel = getPair("agent1-velocity");
		if ( this_vel == null ) return;
		
		Pair<Float, Float> this_loc = getPair("agent1-loc");
		if ( this_loc == null ) return;

		Pair<Float, Float> red_loc = getPair("red-loc");
		if ( red_loc == null ) return;			
		
		Pair<Float, Float> green_loc = getPair("green-loc");
		if ( green_loc == null ) return;
		
		Pair<Float, Float> blue_loc = getPair("blue-loc");
		if ( blue_loc == null ) return;
		
		Triplet<Float,Float,Float> agent_act = getTriplet("agent0-action");
		if ( agent_act == null ) return;
		
		obs[0][0] = this_vel.getValue0();
		obs[0][1] = this_vel.getValue1();
		obs[0][2] = red_loc.getValue0() - this_loc.getValue0();
		obs[0][3] = red_loc.getValue1() - this_loc.getValue1();		
		obs[0][4] = green_loc.getValue0() - this_loc.getValue0();
		obs[0][5] = green_loc.getValue1() - this_loc.getValue1();
		obs[0][6] = blue_loc.getValue0() - this_loc.getValue0();
		obs[0][7] = blue_loc.getValue1() - this_loc.getValue1();
		obs[0][8] = agent_act.getValue0();
		obs[0][9] = agent_act.getValue1();
		obs[0][10] = agent_act.getValue2();
		
		INDArray input = Nd4j.create(obs);
		INDArray output = m_model.output(input);
		
		float deltaX = output.getFloat(1) - output.getFloat(2);
		deltaX *= 3.0f;
		float deltaY = output.getFloat(3) - output.getFloat(4);
		deltaY *= 3.0f;
		
		Action action = Action.newBuilder().setCapability("MoveDeltaXY").addActions(deltaX).addActions(deltaY).build();
		getContext().system().eventStream().publish(action);
	}
	
	@SuppressWarnings("unchecked")
	private Pair<Float, Float> getPair(String obsId) {
		
		CompletionStage<Object> stage = Patterns.ask(m_system.getTupleSpace(), new Get(obsId), Duration.ofSeconds(1));
		Optional<Pair<Float, Float>> pair = null;
		try {
			pair = (Optional<Pair<Float, Float>>)stage.toCompletableFuture().get();
		}
		catch ( Throwable e ) {
//			e.printStackTrace();
			return null;
		}
		
		return pair.isPresent() ? pair.get() : null;
	}		
	
	@SuppressWarnings("unchecked")
	private Triplet<Float,Float,Float> getTriplet(String obsId) {
		
		CompletionStage<Object> stage = Patterns.ask(m_system.getTupleSpace(), new Get(obsId), Duration.ofSeconds(1));
		Optional<Triplet<Float,Float,Float>> triplet = null;
		try {
			triplet = (Optional<Triplet<Float,Float,Float>>)stage.toCompletableFuture().get();
		}
		catch ( Throwable e ) {
//			e.printStackTrace();
			return null;
		}
		
		return triplet.isPresent() ? triplet.get() : null;
	}	
}
