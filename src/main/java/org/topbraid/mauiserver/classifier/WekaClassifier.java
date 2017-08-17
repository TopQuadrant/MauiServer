package org.topbraid.mauiserver.classifier;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

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
	
	// For nominal attributes: from path strings to TTL of Jena Nodes by index
	private Map<String,List<String>> path2Nodes = new HashMap<>();
	
	
	public WekaClassifier(JsonNode jsonNode) throws Exception {
		String instancesString = jsonNode.get("instances").textValue();
		instances = new Instances(new StringReader(instancesString));
		classAttributeIndex = jsonNode.get("classIndex").intValue();
		instances.setClassIndex(classAttributeIndex);
		DecisionTable classifier = new DecisionTable();
		classifier.buildClassifier(instances);

		JsonNode pathsArray = jsonNode.get("paths");
		for(int i = 0; i < pathsArray.size(); i++) {
			Attribute attribute = instances.attribute(i);
			attribute2Path.put(attribute, pathsArray.get(i).textValue());
		}

		JsonNode nodesObject = jsonNode.get("nodes");
		Iterator<String> it = nodesObject.fieldNames();
		while(it.hasNext()) {
			String fieldName = it.next();
			JsonNode array = nodesObject.get(fieldName);
			List<String> nodes = new LinkedList<>();
			array.forEach((x) -> nodes.add(x.textValue()));
			path2Nodes.put(fieldName, nodes);
		}
	}
	
	
	/**
	 * 
	 * @param jsonNode  
	 * @return the TTL serialization of an RDF node
	 * @throws Exception
	 */
	public String classifyInstance(JsonNode jsonNode) throws Exception {
		
		// TODO
		
		Instance instance = new Instance(instances.numAttributes());
		instances.add(instance);
		instance = instances.lastInstance();
		
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
}
