package org.topbraid.mauiserver;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

public class JsonUtil {

	public static boolean isObject(JsonValue value) {
		return value != null && value.getValueType() == ValueType.OBJECT;
	}

	public static boolean hasValue(JsonValue json, String key, ValueType valueType) {
		return hasValue(json, key, valueType, false);
	}
	
	public static boolean hasValue(JsonValue json, String key, ValueType valueType, boolean mustBeNonEmpty) {
		if (json == null || json.getValueType() != ValueType.OBJECT) return false;
		JsonObject o = json.asJsonObject();
		if (key == null || !o.containsKey(key)) return false;
		JsonValue value = o.get(key);
		if (value == null || value.getValueType() != valueType) return false;
		if (mustBeNonEmpty && valueType == ValueType.STRING && "".equals(((JsonString) value).getString())) return false;
		if (mustBeNonEmpty && valueType == ValueType.ARRAY && ((JsonArray) value).size() == 0) return false;
		return true;
	}
	
	public static JsonValue getValue(JsonValue json, String key) {
		if (json == null || json.getValueType() != ValueType.OBJECT) return null;
		JsonObject o = json.asJsonObject();
		if (key == null || !o.containsKey(key)) return null;
		return o.get(key);
	}
	
	public static JsonValue getValue(JsonValue json, String key, ValueType valueType) {
		JsonValue result = getValue(json, key);
		if (result == null || result.getValueType() != valueType) return null;
		return result;
	}
	
	public static String getString(JsonValue json, String key) {
		return asString(getValue(json, key));
	}
	
	public static int getInt(JsonValue json, String key) {
		return getInt(json, key, 0);
	}
	
	public static int getInt(JsonValue json, String key, int defaultValue) {
		JsonValue value = getValue(json, key, ValueType.NUMBER);
		if (value == null) return defaultValue;
		return ((JsonNumber) value).intValue();
	}

	public static double getDouble(JsonValue json, String key) {
		return getDouble(json, key, 0.0);
	}
	
	public static double getDouble(JsonValue json, String key, double defaultValue) {
		JsonValue value = getValue(json, key, ValueType.NUMBER);
		if (value == null) return defaultValue;
		return ((JsonNumber) value).doubleValue();
	}
	
	public static JsonObject asObject(JsonValue json) {
		if (!isObject(json)) {
			return null;
		}
		return json.asJsonObject();
	}
	
	public static String asString(JsonValue value) {
		if (value == null) return null;
		if (value.getValueType() == ValueType.ARRAY || value.getValueType() == ValueType.OBJECT || value.getValueType() == ValueType.NULL) return null;
		if (value.getValueType() == ValueType.STRING) return ((JsonString) value).getString();
		return value.toString();
	}

	/**
	 * Returns an object builder that allows adding null values.
	 * Adding a null value has the same effect as calling
	 * <code>remove(key)</code>.
	 */
	public static JsonObjectBuilder createObjectBuilderThatIgnoresNulls() {
		return ignoreNulls(Json.createObjectBuilder());
	}
	
	/**
	 * Returns an object builder that wraps another object builder
	 * but allows adding null values. Adding a null value has the
	 * same effect as calling <code>remove(key)</code>.
	 */
	public static JsonObjectBuilder ignoreNulls(JsonObjectBuilder wrapped) {
		return new JsonObjectBuilder() {

			@Override
			public JsonObjectBuilder add(String name, JsonValue value) {
				if (value == null || value.getValueType() == ValueType.NULL) {
					wrapped.remove(name);
				} else {
					wrapped.add(name, value);
				}
				return this;
			}

			@Override
			public JsonObjectBuilder add(String name, String value) {
				if (value == null) {
					wrapped.remove(name);
				} else {
					wrapped.add(name, value);
				}
				return this;
			}

			@Override
			public JsonObjectBuilder add(String name, BigInteger value) {
				if (value == null) {
					wrapped.remove(name);
				} else {
					wrapped.add(name, value);
				}
				return this;
			}

			@Override
			public JsonObjectBuilder add(String name, BigDecimal value) {
				if (value == null) {
					wrapped.remove(name);
				} else {
					wrapped.add(name, value);
				}
				return this;
			}

			@Override
			public JsonObjectBuilder add(String name, int value) {
				wrapped.add(name, value);
				return this;
			}

			@Override
			public JsonObjectBuilder add(String name, long value) {
				wrapped.add(name, value);
				return this;
			}

			@Override
			public JsonObjectBuilder add(String name, double value) {
				wrapped.add(name, value);
				return this;
			}

			@Override
			public JsonObjectBuilder add(String name, boolean value) {
				wrapped.add(name, value);
				return this;
			}

			@Override
			public JsonObjectBuilder addNull(String name) {
				wrapped.addNull(name);
				return this;
			}

			@Override
			public JsonObjectBuilder add(String name, JsonObjectBuilder builder) {
				if (builder == null) {
					wrapped.remove(name);
				} else {
					wrapped.add(name, builder);
				}
				return this;
			}

			@Override
			public JsonObjectBuilder add(String name, JsonArrayBuilder builder) {
				if (builder == null) {
					wrapped.remove(name);
				} else {
					wrapped.add(name, builder);
				}
				return this;
			}

			@Override
			public JsonObjectBuilder addAll(JsonObjectBuilder builder) {
				if (builder != null) {
					JsonObject o = builder.build();
					for (String key: o.keySet()) {
						JsonValue value = o.get(key);
						if (value.getValueType() == ValueType.NULL) continue;
						add(key, value);
					}
				}
				return this;
			}

			@Override
			public JsonObject build() {
				return wrapped.build();
			}
		};
	}
}
