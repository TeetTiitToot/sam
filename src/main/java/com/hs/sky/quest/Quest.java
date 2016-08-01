package com.hs.sky.quest;

import java.util.List;

/**
 * Object that represents a quest for the adepts.
 * @author hs
 *
 */
public class Quest{
	String name;
	long priority;
	List<String> adepts;
	long successRate;
	
	
	public String getName() {
		return name;
	}

	public long getPriority() {
		return priority;
	}

	public List<String> getAdepts() {
		return adepts;
	}

	public long getSuccessRate() {
		return successRate;
	}

	@Override
	public String toString() {
		return "Quest [name=" + name + ", priority=" + priority + ", adepts=" + adepts + ", successRate=" + successRate
				+ "]";
	}
}