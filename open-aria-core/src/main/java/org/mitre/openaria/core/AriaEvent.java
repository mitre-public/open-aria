package org.mitre.openaria.core;

import static org.mitre.openaria.core.output.HashUtils.hashForJson;

import org.mitre.caasd.commons.HasPosition;
import org.mitre.caasd.commons.HasTime;
import org.mitre.caasd.commons.out.JsonWritable;

import com.google.gson.GsonBuilder;

public interface AriaEvent<T> extends JsonWritable, HasPosition, HasTime {

    T event();

    double score();

    default String uuid() {

        //get the JSON string that does not contain a hash within it
        String asJsonWithoutHash = new GsonBuilder().create().toJson(this);

        //Now, produce a hash for this JSON text AFTER normalizing for whitespace
        return hashForJson(asJsonWithoutHash);
    }
}
