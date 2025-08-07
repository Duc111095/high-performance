package com.ducnh.highperformance.concurrent;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.ducnh.highperformance.collections.ArrayUtil;

public class DynamicCompositeAgent implements Agent{
	public enum Status {
		INIT,
		ACTIVE,
		CLOSE
	}
	
	private static final Agent[] EMPTY_AGENTS = new Agent[0];
	
	private int agentIndex = 0;
	private volatile Status status = Status.INIT;
	private Agent[] agents;
	private final String roleName;
	private final AtomicReference<Agent> addAgent = new AtomicReference<>();
	private final AtomicReference<Agent> removeAgent = new AtomicReference<>();
	
	public DynamicCompositeAgent(final String roleName, final List<? extends Agent> agents) {
		this.roleName = roleName;
		this.agents = new Agent[agents.size()];
		
		int i = 0;
		for (final Agent agent : agents) {
			Objects.requireNonNull(agent, "agents cannot be null");
			this.agents[i++] = agent;
		}
	}
	
	public Status status() {
		return status;
	} 
	
	public DynamicCompositeAgent(final String roleName, final Agent... agents) {
		this.roleName = roleName;
		this.agents = agents;
		
		int i = 0;
		for (final Agent agent : agents) {
			Objects.requireNonNull(agent, "agents cannot be null");
			this.agents[i++] = agent;
		}
	}
	
	public void onStart() {
		for (final Agent agent : agents) {
			agent.onStart();
		}
		
		status = Status.ACTIVE;
	}
	
	public int doWork() throws Exception {
		int workCount = 0;
		
		final Agent agentToAdd = addAgent.get();
		if (null != agentToAdd) {
			add(agentToAdd);
		}
		
		final Agent agentToRemove = removeAgent.get();
		if (null != agentToRemove) {
			remove(agentToRemove);
		}
		
		final Agent[] agents = this.agents;
		while (agentIndex < agents.length) {
			final Agent agent = agents[agentIndex++];
			workCount += agent.doWork();
		}
		
		agentIndex = 0;
		
		return workCount;
	}
	
	public void onClose() {
		status = Status.CLOSE;
		
		RuntimeException ce = null;
		for (final Agent agent : agents) {
			try {
				agent.onClose();
			} catch (final Exception ex) {
				if (ce == null) {
					ce = new RuntimeException(getClass().getName() + ": underlying agent error on close");
				}
				ce.addSuppressed(ex);
			}
		}
		
		agents = EMPTY_AGENTS;
		if (null != ce) {
			throw ce;
		}
	}
	
	public String roleName() {
		return roleName;
	}
	
	public boolean tryAdd(final Agent agent) {
		Objects.requireNonNull(agent, "agent cannot be null");
		
		if (Status.ACTIVE != status) {
			throw new IllegalStateException("add called when not active");
		}
		
		return addAgent.compareAndSet(null, agent);
	}
	
	public boolean hasAddAgentCompleted() {
		if (Status.ACTIVE != status) {
			throw new IllegalStateException("agent is not active");
		}
		
		return null == addAgent.get();
	}
	
	public boolean tryRemove(final Agent agent) {
		Objects.requireNonNull(agent, "agent cannot be null");
		
		if (Status.ACTIVE != status) {
			throw new IllegalStateException("remove called when not active");
		}
		
		return removeAgent.compareAndSet(null, agent);
	}
	
	public boolean hasRemoveAgentCompleted() {
		if (Status.ACTIVE != status) {
			throw new IllegalStateException("agent is not active");
		}
		
		return null == removeAgent.get();
	}
	
	private void add(final Agent agent) {
		addAgent.lazySet(null);
		
		try {
			agent.onStart();
		} catch (final Exception ex) {
			try {
				agent.onClose();
			} catch (final Exception ce) {
				ex.addSuppressed(ce);
			}
			
			throw ex;
		}
		
		agents = ArrayUtil.add(agents, agent);
	}
	
	private void remove(final Agent agent) {
		removeAgent.lazySet(null);
		
		final Agent[] newAgents = ArrayUtil.remove(agents, agent);
		
		try {
			if (newAgents != agents) {
				agent.onClose();
			}
		}
		finally {
			agents = newAgents;
		}
	}
} 
