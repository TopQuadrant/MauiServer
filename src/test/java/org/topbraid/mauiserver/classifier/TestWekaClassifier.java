package org.topbraid.mauiserver.classifier;

import java.io.File;
import java.io.FileInputStream;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;

public class TestWekaClassifier {

	@Test
	public void testSimple() throws Exception {
		File file = new File("src/test/resources/simple.json.txt");
		JsonObject json = Json.createReader(new FileInputStream(file)).readObject();
		new WekaClassifier(json);
	}
}
