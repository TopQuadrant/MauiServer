package org.topbraid.mauiserver.framework;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.mauiserver.MauiServerException;

/**
 * Parses an {@link InputStream} in JSON Lines (.jsonl) format and delivers an
 * iterator over the parsed {@link JsonValue}s. The input
 * is <code>\n</code>-separated, with each line being a JSON node. 
 */
public class JsonLinesParser implements Iterator<JsonValue> {
	private static final Logger log = LoggerFactory.getLogger(JsonLinesParser.class);
	
	private final InputStream in;
	private boolean skipBadJsonLines = false;
	private int skippedLinesCount = 0;
	private boolean done = false;
	private JsonValue next = null;
	private int line = 0;
	
	public JsonLinesParser(InputStream in) {
		this.in = new BufferedInputStream(in);
	}

	/**
	 * If set to <code>true</code>, lines with JSON parse errors will be skipped
	 * rather than causing an exception. Defaults to <code>false</code>. 
	 */
	public void setSkipBadJsonLines(boolean flag) {
		this.skipBadJsonLines = flag;
	}

	public int getLineNumber() {
		return line;
	}

	public int getSkippedBadLinesCount() {
		return skippedLinesCount;
	}
	
	@Override
	public boolean hasNext() {
		while (next == null && !done) {
			next = readValue();
		}
		return next != null;
	}
	
	@Override
	public JsonValue next() {
		if (!hasNext()) throw new NoSuchElementException();
		JsonValue result = next;
		next = null;
		return result;
	}
	
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	private String readLine() throws IOException {
		List<Byte> buf = new ArrayList<Byte>();
		while (true) {
			int b = in.read();
			if (b == -1) {
				done = true;
			}
			if (b == '\n' || b == -1) {
				byte[] bytes = new byte[buf.size()];
				for (int i = 0; i < bytes.length; i++) {
					bytes[i] = buf.get(i);
				}
				line++;
				return new String(bytes, "utf-8");
			}
			buf.add((byte) b);
		}
	}
	
	private JsonValue readValue() {
		try {
			String nextLine = readLine();
			if ("".equals(nextLine)) {
				if (done) return null;
			}
			return Json.createReader(new StringReader(nextLine)).readValue();
		} catch (JsonParsingException ex) {
			if (skipBadJsonLines) {
				log.warn("Skipping line " + line + ": JSON parse error: " + ex.getMessage());
				skippedLinesCount++;
				return null;
			}
			throw new MauiServerException("JSON Lines parse error on line " + line + ": " + ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new MauiServerException(ex);
		}
	}
}
