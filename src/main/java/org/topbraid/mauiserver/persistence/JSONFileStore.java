package org.topbraid.mauiserver.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonGenerationException;
import javax.json.stream.JsonParsingException;

public abstract class JSONFileStore<T> extends FileStore<T> {

	public JSONFileStore(String taggerId, File jsonFile) {
		super(taggerId, jsonFile);
	}

	public T read() throws IOException {
		try {
			JsonStructure json = Json.createReader(new FileInputStream(getFile())).read();
			if (!ValueType.OBJECT.equals(json.getValueType())) {
				return throwException("Expected JSON object on");
			}
			return decode(json.asJsonObject());
		} catch (JsonParsingException ex) {
			return throwException("JSON parsing error on", ex);
		}
	}

	protected abstract T decode(JsonObject json);

	@Override
	protected void write(T storable) throws IOException {
		try {
			JsonStructure json = encode(storable);
			if (json == null) return;
			Json.createWriter(new FileOutputStream(getFile())).write(json);
		} catch (JsonGenerationException ex) {
			throwException("JSON processing error on", ex);
		}
	}
	
	protected abstract JsonStructure encode(T storable);
}
