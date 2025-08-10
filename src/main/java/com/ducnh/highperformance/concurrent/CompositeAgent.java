package com.ducnh.highperformance.concurrent;

import java.util.List;
import java.util.Objects;

public class CompositeAgent implements Agent{
	private final Agent[] agents;
	private final String roleName;
	private int agentIndex = 0;
	
	public CompositeAgent(final List<? extends Agent> agents) {
		this(agents.toArray(new Agent[0]));
	}
	
	public CompositeAgent(final Agent... agents) {
		if (agents.length == 0) {
			throw new IllegalArgumentException("requires at least one sub-agent");
		}
		
		this.agents = new Agent[agents.length];
		
		final StringBuilder sb = new StringBuilder(agents.length * 16);
		sb.append('[');
		int i = 0;
		for (final Agent agent : agents) {
			Objects.requireNonNull(agent, "agent cannot be null");
			sb.append(agent.roleName()).append(',');
			this.agents[i++] = agent;
		}
		
		sb.setCharAt(sb.length() -1, ']');
		this.roleName = sb.toString();
	}
	
	public void onStart() {
		RuntimeException ce = null;
		for (final Agent agent : agents) {
			try {
				agent.onStart();
			} catch (final Exception ex) {
				if (ce == null) {
					ce = new RuntimeException(getClass().getName() + ": underlying agent error on start");
				}
				ce.addSuppressed(ce);
			}
		}
		
		if (null != ce) {
			throw ce;
		}
	}
	
	public int doWork() throws Exception {
		int workCount = 0;
		
		final Agent[] agents = this.agents;
		while (agentIndex < agents.length) {
			final Agent agent = agents[agentIndex++];
			workCount += agent.doWork();
		}
		
		agentIndex = 0;
		return workCount;
	}
	
	public void onClose() {
		RuntimeException ce = null;
		for (final Agent agent : agents) {
			try {
				agent.onClose();
			} catch (Exception ex) {
				if (ce == null) {
					ce = new RuntimeException(getClass().getName() + ": underlying agent error on close");
				}
				ce.addSuppressed(ex);
			}
		}
		
		if (null != ce) {
			throw ce;
		}
	}

	@Override
	public String roleName() {
		return this.roleName;
	}
}
