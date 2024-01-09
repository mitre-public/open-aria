/*
 * NOTICE:
 * This is the copyright work of The MITRE Corporation, and was produced for the
 * U. S. Government under Contract Number DTFAWA-10-C-00080, and is subject to
 * Federal Aviation Administration Acquisition Management System Clause 3.5-13,
 * Rights In Data-General, Alt. III and Alt. IV (Oct. 1996).
 *
 * No other use other than that granted to the U. S. Government, or to those
 * acting on behalf of the U. S. Government, under that Clause is authorized
 * without the express written permission of The MITRE Corporation. For further
 * information, please contact The MITRE Corporation, Contracts Management
 * Office, 7515 Colshire Drive, McLean, VA  22102-7539, (703) 983-6000.
 *
 * Copyright 2021 The MITRE Corporation. All Rights Reserved.
 */

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
