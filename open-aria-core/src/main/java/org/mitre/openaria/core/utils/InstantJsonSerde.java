package org.mitre.openaria.core.utils;

import java.lang.reflect.Type;
import java.time.Instant;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * The {@code InstantJsonSerde} class holds a serializer and deserializer for reading/writing {@link
 * Instant} objects as JSON in a slightly nicer form. If no Instant {@code TypeAdapter}s are
 * provided, the default
 * representation will look like: <pre>
 *     "myInstant":{"seconds":1617023277,"nanos":195000000}
 * </pre>
 * The serializer provided here will create the following representation: <pre>
 *     "myInstant":{"epochMilli":1617025242521,"utc":"2021-03-29T13:40:42.521Z"}
 * </pre>
 * The {@link com.google.gson.Gson} object would be constructed as follows: <pre>
 *     private static final Gson GSON = new GsonBuilder()
 *         .registerTypeAdapter(Instant.class, new InstantJsonSerializer())
 *         .registerTypeAdapter(Instant.class, new InstantJsonDeserializer())
 *         .create();
 * </pre>
 */
public final class InstantJsonSerde {

    private InstantJsonSerde() {
        throw new UnsupportedOperationException();
    }


    /**
     * This serializer will create the following JSON representation: <pre>
     *     "myInstant":{"epochMilli":1617025242521,"utc":"2021-03-29T13:40:42.521Z"}
     * </pre>
     */
    public static class InstantJsonSerializer implements JsonSerializer<Instant> {

        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("epochMilli", src.toEpochMilli());
            json.addProperty("utc", src.toString());
            return json;
        }
    }

    /**
     * This deserializer reads from the following JSON representation: <pre>
     *     "myInstant":{"epochMilli":1617025242521,"utc":"2021-03-29T13:40:42.521Z"}
     * </pre>
     */
    public static class InstantJsonDeserializer implements JsonDeserializer<Instant> {

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Instant.ofEpochMilli(json.getAsJsonObject().get("epochMilli").getAsLong());
        }
    }
}
