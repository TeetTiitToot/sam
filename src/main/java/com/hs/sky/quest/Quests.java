package com.hs.sky.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
/**
 * Helper class for Quests
 * @author hs
 *
 */
public final class Quests {
	private Quests(){
		// Helper class.
	}
	
	
	
	public static List<Quests.QuestList> loadQuestsByPriorityDesc(JSONObject config){
		List<Quest> quests = loadQuests(config);
		Map<Long, QuestList> map = new HashMap<>();
		for(Quest q : quests){
			QuestList ql;
			if (map.containsKey(q.getPriority())){
				ql = map.get(q.getPriority());
			} else {
				ql = new QuestList(q.getPriority());
				map.put(q.getPriority(), ql);
			}
			ql.add(q);
		}
		ArrayList<QuestList> questLists = new ArrayList<>(map.values());
		questLists.sort(QUEST_LIST_PRIORITY_COMP);
		return questLists;
	}
	
	public static List<Quest> loadQuests(JSONObject config) {
		JSONArray quests = (JSONArray)config.get("Quests");
		List<Quest> newQuests = new ArrayList<>();
		for(Object q : quests){
			newQuests.add(readQuest((JSONObject)q));
		}
		return newQuests;
	}

	private static Quest readQuest(JSONObject q) {
		JSONObject quest = q; 
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
		return newQuest;
	}
	
	public static final Comparator<Quest> QUEST_PRIORITY_COMP = Comparator.comparing(Quest::getPriority).reversed();
	private static final Comparator<QuestList> QUEST_LIST_PRIORITY_COMP = Comparator.comparing(QuestList::getPriority).reversed();
	
	public static class QuestList extends ArrayList<Quest>{
		/**
		 * Generated serial ID: not sure it works this way?
		 */
		private static final long serialVersionUID = -9020469412442845501L;
		final long priority;
		public long getPriority(){
			return priority;
		}
		
		public QuestList(long priority){
			this.priority = priority;
		}
		
	}

}
