package org.topbraid.mauiserver.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class JSONFileStore<T> extends FileStore<T> {
	protected static final ObjectMapper mapper = new ObjectMapper();

	public JSONFileStore(String taggerId, File jsonFile) {
		super(taggerId, jsonFile);
	}

	public T read() throws IOException {
		try {
			JsonNode json = mapper.readTree(new FileInputStream(getFile()));
			if (!json.isObject()) {
				return throwException("Expected JSON object on");
			}
			return decode((ObjectNode) json);
		} catch (JsonProcessingException ex) {
			return throwException("JSON processing error on", ex);
		}
	}

	protected abstract T decode(ObjectNode json);

	@Override
	protected void write(T storable) throws IOException {
		try {
			JsonNode json = encode(storable);
			if (json == null) return;
			mapper.writeValue(getFile(), json);
		} catch (JsonMappingException ex) {
			throwException("JSON processing error on", ex);
		} catch (JsonGenerationException ex) {
			throwException("JSON processing error on", ex);
		}
	}
	
	protected abstract JsonNode encode(T storable);
}
