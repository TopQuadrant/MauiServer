package org.topbraid.mauiserver.classifier;

import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import weka.classifiers.Classifier;
import weka.classifiers.rules.DecisionTable;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Encapsulates a Weka Classifier together with other background info about how it was
 * created and with what data.
 * 
 * @author Holger Knublauch
 */
public class WekaClassifier {

	// The path string for each Attribute
	private Map<Attribute,String> attribute2Path = new HashMap<>();
	
	private int classAttributeIndex;
	
	private Classifier classifier;

	private Instances instances;
	
	private Map<String,Attribute> path2Attribute = new HashMap<>();
	
	// For nominal attributes: from path strings to TTL of Jena Nodes by index
	private Map<String,List<String>> path2Nodes = new HashMap<>();
	
	
	public WekaClassifier(JsonObject jsonNode) throws Exception {
		String instancesString = jsonNode.getString("instances");
		instances = new Instances(new StringReader(instancesString));
		classAttributeIndex = jsonNode.getInt("classAttributeIndex");
		instances.setClassIndex(classAttributeIndex);
		
		classifier = new DecisionTable();
		classifier.buildClassifier(instances);

		JsonArray pathsArray = jsonNode.getJsonArray("paths");
		for(int i = 0; i < pathsArray.size(); i++) {
			Attribute attribute = instances.attribute(i);
			String path = pathsArray.getString(i);
			attribute2Path.put(attribute, path);
			if(!attribute.name().contains("$")) {
				path2Attribute.put(path, attribute);
			}
		}

		JsonObject nodesObject = jsonNode.getJsonObject("nodes");
		for (String fieldName: nodesObject.keySet()) {
			JsonArray array = nodesObject.getJsonArray(fieldName);
			path2Nodes.put(fieldName, new LinkedList<>(array.getValuesAs(JsonString::getString)));
		}
	}
	
	
	/**
	 * 
	 * @param jsonNode  
	 * @return the TTL serialization of an RDF node
	 * @throws Exception
	 */
	public String classifyInstance(JsonObject jsonNode) throws Exception {
		
		Instance instance = new Instance(instances.numAttributes());
		instances.add(instance);
		instance = instances.lastInstance();
		
		for (String fieldName: jsonNode.keySet()) {
			Attribute attribute = path2Attribute.get(fieldName);
			List<String> nodes = path2Nodes.get(fieldName);
			JsonArray array = jsonNode.getJsonArray(fieldName);
			for(int i = 0; i < array.size(); i++) {
				String str = array.getString(i);
				if(nodes != null) { // Nominal
					int index = nodes.indexOf(str);
					if(index >= 0) {
						if(attribute == null) {
							Attribute attr = getAttribute(fieldName, index);
							if(attr != null) {
								instance.setValue(attr, "" + index);
							}
						}
						else {
							instance.setValue(attribute, "" + index);
						}
					}
				}
				else {
					try {
						double number = Double.parseDouble(str);
						instance.setValue(attribute, number);
					}
					catch(NumberFormatException ex) {
					}
				}
			}
		}
		System.out.println("Classifying Instance: " + instance);
		
		double value = classifier.classifyInstance(instance);
		if(Instance.isMissingValue(value)) {
			return null;
		}
		Attribute attribute = instances.attribute(classAttributeIndex);
		if(attribute.isNumeric()) {
			return "" + value;
		}
		else if(attribute.isNominal()) {
			int valueIndex = (int) value;
			String path = attribute2Path.get(attribute);
			List<String> nodes = path2Nodes.get(path);
			return nodes.get(valueIndex);
		}
		return null;
	}
	
	
	private Attribute getAttribute(String path, int index) {
		String matchName = path + "$" + index;
		for(int i = 0; i < instances.numAttributes(); i++) {
			Attribute attribute = instances.attribute(i);
			if(matchName.equals(attribute.name())) {
				return attribute;
			}
		}
		return null;
	}
}
