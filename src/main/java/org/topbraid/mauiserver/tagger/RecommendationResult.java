package org.topbraid.mauiserver.tagger;

import java.util.ArrayList;
import java.util.List;

import com.entopix.maui.util.Topic;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RecommendationResult {
	private List<String> recommendations;
	private double[] probabilities;
	
	public RecommendationResult(List<Topic> mauiTopics) {
		recommendations = new ArrayList<String>(mauiTopics.size());
		probabilities = new double[mauiTopics.size()];
		for (int i = 0; i < mauiTopics.size(); i++) {
			Topic t = mauiTopics.get(i);
			recommendations.add(t.getTitle());
			probabilities[i] = t.getProbability();
		}
	}
	
	public List<String> getRecommendations() {
		return recommendations;
	}
	
	public double getProbability(String recommendation) {
		int i = recommendations.indexOf(recommendation);
		if (i == -1) return 0;
		return probabilities[i];
	}
	
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append('[');
		for (int i = 0; i < recommendations.size(); i++) {
			if (i > 0) s.append(", ");
			s.append('"');
			s.append(recommendations.get(i));
			s.append("\"(");
			s.append(probabilities[i]);
			s.append(')');
		}
		s.append(']');
		return s.toString();
	}
	
	public void toJSON(ObjectNode root) {
		ArrayNode results = root.arrayNode();
		for (int i = 0; i < recommendations.size(); i++) {
			ObjectNode result = root.objectNode();
			result.put("label", recommendations.get(i));
			result.put("probability", probabilities[i]);
			results.add(result);
		}
		root.set("topics", results);
	}
}
