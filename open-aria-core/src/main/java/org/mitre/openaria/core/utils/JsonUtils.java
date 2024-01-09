package org.mitre.openaria.core.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonUtils {

    /** Manually removes a top level field from a raw JSON String, the result is unformatted Json. */
    public static String removeTopLevelField(String property, String rawJson) {

        JsonElement element = JsonParser.parseString(rawJson);
        JsonObject jo = element.getAsJsonObject();
        jo.remove(property);
        return jo.toString(); //produces unformatted "single line JSON"
    }

    /** Applies a formatter to json. */
    public static String reformatJson(Gson formatter, String rawJson) {
        JsonElement element = JsonParser.parseString(rawJson);
        return formatter.toJson(element);
    }
}
