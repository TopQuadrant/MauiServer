package org.topbraid.mauiserver.tagger;

import static javax.json.Json.createArrayBuilder;
import static org.topbraid.mauiserver.JsonUtil.createObjectBuilderThatIgnoresNulls;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import com.entopix.maui.util.Topic;

public class RecommendationResult {
	private List<String> ids;
	private List<String> titles;
	private List<Double> probabilities;
	
	public RecommendationResult(List<Topic> mauiTopics) {
		this(mauiTopics, 0.0);
	}
	
	public RecommendationResult(List<Topic> mauiTopics, double probabilityThreshold) {
		ids = new ArrayList<String>(mauiTopics.size());
		titles = new ArrayList<String>(mauiTopics.size());
		probabilities = new ArrayList<Double>(mauiTopics.size());
		for (int i = 0; i < mauiTopics.size(); i++) {
			Topic t = mauiTopics.get(i);
			if (t.getProbability() < probabilityThreshold) continue;
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
	
	public List<String> getTitles() {
		return titles;
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
	
	public void toJSON(JsonObjectBuilder root) {
		JsonArrayBuilder results = createArrayBuilder();
		for (int i = 0; i < ids.size(); i++) {
			results.add(
					createObjectBuilderThatIgnoresNulls()
							.add("id", ids.get(i))
							.add("label", titles.get(i))
							.add("probability", probabilities.get(i)));
		}
		root.add("topics", results);
	}
}
