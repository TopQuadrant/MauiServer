package org.topbraid.mauiserver.tagger;

import java.util.ArrayList;
import java.util.List;

import com.entopix.maui.util.Topic;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RecommendationResult {
	private List<String> ids;
	private List<String> titles;
	private List<Double> probabilities;
	
	public RecommendationResult(List<Topic> mauiTopics) {
		ids = new ArrayList<String>(mauiTopics.size());
		titles = new ArrayList<String>(mauiTopics.size());
		probabilities = new ArrayList<Double>(mauiTopics.size());
		for (int i = 0; i < mauiTopics.size(); i++) {
			Topic t = mauiTopics.get(i);
			ids.add(t.getId());
			titles.add(t.getTitle());
			probabilities.add(t.getProbability());
		}
	}
	
	public int size() {
		return ids.size();
	}
	
	public List<String> getRecommendations() {
		return ids;
	}
	
	public String getTitle(String recommendation) {
		int i = ids.indexOf(recommendation);
		if (i == -1) return null;
		return titles.get(i);
	}
	
	public double getProbability(String recommendation) {
		int i = ids.indexOf(recommendation);
		if (i == -1) return 0;
		return probabilities.get(i);
	}
	
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append('[');
		for (int i = 0; i < ids.size(); i++) {
			if (i > 0) s.append(", ");
			s.append('"');
			s.append(titles.get(i));
			s.append("\"|\"");
			s.append(ids.get(i));
			s.append("\"(");
			s.append(probabilities.get(i));
			s.append(')');
		}
		s.append(']');
		return s.toString();
	}
	
	public void toJSON(ObjectNode root) {
		ArrayNode results = root.arrayNode();
		for (int i = 0; i < ids.size(); i++) {
			ObjectNode result = root.objectNode();
			result.put("id", ids.get(i));
			result.put("label", titles.get(i));
			result.put("probability", probabilities.get(i));
			results.add(result);
		}
		root.set("topics", results);
	}
}
