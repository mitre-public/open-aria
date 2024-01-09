package org.mitre.openaria.airborne;

import static java.lang.Math.PI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.lang.reflect.Type;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ExtraDecimalGsonTest {

    float oneThird_f = (float) (1.0 / 3.0);
    float pi_f = (float) PI;
    double oneThird_d = 1.0 / 3.0;

    //The serialized form of this data should NOT waste characters on worthless accuracy
    class SerializeMeWithGson {

        float[] floatyData;
        double[] doublelyData;

        public SerializeMeWithGson(float[] f, double[] d) {
            this.floatyData = f;
            this.doublelyData = d;
        }
    }

    @Test
    public void gsonDoesNotTooManyDecimalFields() {

        SerializeMeWithGson item = new SerializeMeWithGson(new float[]{oneThird_f, pi_f}, new double[]{oneThird_d, PI});

        /* The converter is static to allow reuse. Creating the Gson using reflection is expensive. */
        Gson GSON_CONVERTER_WITH_ALL_DIGITS = new GsonBuilder().create();
        Gson GSON_CONVERTER_WITH_4_DIGITS = makeTruncatingGson();

        String bigJson = GSON_CONVERTER_WITH_ALL_DIGITS.toJson(item);
        String smallJson = GSON_CONVERTER_WITH_4_DIGITS.toJson(item);

        assertThat(bigJson, containsString("floatyData\":[0.33333334,3.1415927]"));
        assertThat(bigJson, containsString("\"doublelyData\":[0.3333333333333333,3.141592653589793]"));

        assertThat(smallJson, containsString("\"floatyData\":[0.3334,3.1416]"));
        assertThat(smallJson, containsString("\"doublelyData\":[0.3334,3.1416]"));
    }

    Gson makeTruncatingGson() {

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Double.class, (JsonSerializer<Double>) (src, typeOfSrc, context) -> {
            DecimalFormat df = new DecimalFormat("#.####");
            df.setRoundingMode(RoundingMode.CEILING);
            return new JsonPrimitive(Double.parseDouble(df.format(src)));
        });
        builder.registerTypeAdapter(Float.class, (JsonSerializer<Float>) (src, typeOfSrc, context) -> {
            DecimalFormat df = new DecimalFormat("#.####");
            df.setRoundingMode(RoundingMode.CEILING);
            return new JsonPrimitive((float) Double.parseDouble(df.format(src)));
        });
        Gson gson = builder.create();

        return gson;
    }

    //The serialized form of this data should maintain all available accuracy.
    class Outer {

        float[] outerFloats;
        double[] outerDoubles;

        Inner inner;

        public Outer(float[] f, double[] d) {
            this.outerFloats = f;
            this.outerDoubles = d;
            this.inner = new Inner(f, d);
        }
    }

    //The serialized form of this data should NOT waste characters on worthless accuracy
    class Inner {

        float[] innerFloats;
        double[] innerDoubles;

        public Inner(float[] f, double[] d) {
            this.innerFloats = f;
            this.innerDoubles = d;
        }
    }

    @Test
    public void outerHasFullAccuracyButInnerIsSmaller() {
        /*
         * Goal: Show how to mix serialize accuracy levels. Simultaneously serialize some numeric
         * data with full accuracy and some numeric data with reduced accuracy.
         *
         * i.e. Generate:
         * {"outerFloats":[0.33333334,3.1415927],"outerDoubles":[0.3333333333333333,3.141592653589793],"inner":{"innerFloats":[0.3334,3.1416],"innerDoubles":[0.3334,3.1416]}}
         */

        Outer item = new Outer(new float[]{oneThird_f, pi_f}, new double[]{oneThird_d, PI});

        Gson NESTED_GSON = makeNestedGson();

        String mixedAccuracyJson = NESTED_GSON.toJson(item);

        //numbers in the "outer object" get high precision
        assertThat(mixedAccuracyJson, containsString("outerFloats\":[0.33333334,3.1415927]"));
        assertThat(mixedAccuracyJson, containsString("\"outerDoubles\":[0.3333333333333333,3.141592653589793]"));

        //numbers in the "inner object" get low precision
        assertThat(mixedAccuracyJson, containsString("\"innerFloats\":[0.3334,3.1416]"));
        assertThat(mixedAccuracyJson, containsString("\"innerDoubles\":[0.3334,3.1416]"));
    }

    Gson makeNestedGson() {

        //outer array objects are printed with full accuracy.
        //inner array objects are printed with reduced accuracy.
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Inner.class, new InnerSerializer())  //reduced precision only applies to this inner object.
            .create();

        return gson;
    }

    class InnerSerializer implements JsonSerializer<Inner> {

        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Float.class, (JsonSerializer<Float>) (src, typeOfSrc, context) -> {
                DecimalFormat df = new DecimalFormat("#.####");
                df.setRoundingMode(RoundingMode.CEILING);
                return new JsonPrimitive((float) Double.parseDouble(df.format(src)));
            })
            .registerTypeAdapter(Double.class, (JsonSerializer<Double>) (src, typeOfSrc, context) -> {
                DecimalFormat df = new DecimalFormat("#.####");
                df.setRoundingMode(RoundingMode.CEILING);
                return new JsonPrimitive(Double.parseDouble(df.format(src)));
            })
            .create();

        @Override
        public JsonElement serialize(Inner t, Type type, JsonSerializationContext jsc) {
            return gson.toJsonTree(t);
        }
    }
}
