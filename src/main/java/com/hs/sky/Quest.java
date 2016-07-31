package com.hs.sky;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Quest{
	private String name;
	private long priority;
	private List<String> adepts;
	private long successRate;
	
	
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


	public static List<Quest> loadQuests(JSONObject config) {
		JSONArray quests = (JSONArray)config.get("Quests");
		List<Quest> newQuests = new ArrayList<>();
		for(Object q : quests){
			JSONObject quest =(JSONObject) q; 
			Quest newQuest = new Quest();
			newQuest.name = (String)quest.get("Name");
			newQuest.priority = (long)quest.getOrDefault("Priority", 0l);
			JSONArray adepts = (JSONArray)quest.getOrDefault("Adepts", null);
			if(adepts == null || adepts.isEmpty()){
				newQuest.adepts = Collections.emptyList();
			} else {
				newQuest.adepts = new ArrayList<>();
				for(Object adept : adepts){
					newQuest.adepts.add((String) adept);
				}
			}
			newQuest.successRate = (long)quest.getOrDefault("SuccessRate", 100l);
			newQuests.add(newQuest);
		}
		return newQuests;
	}
	
	public static final Comparator<Quest> PRIORITY_COMP = Comparator.comparing(Quest::getPriority).reversed();
}