package org.topbraid.mauiserver.classifier;

import java.io.File;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestWekaClassifier {

	@Test
	public void testSimple() throws Exception {
		File file = new File("src/test/resources/simple.json.txt");
		JsonNode json = new ObjectMapper().readTree(file);
		new WekaClassifier(json);
	}
}
