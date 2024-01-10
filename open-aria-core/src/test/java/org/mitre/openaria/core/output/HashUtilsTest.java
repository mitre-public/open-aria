package org.mitre.openaria.core.output;

import static java.lang.Math.PI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mitre.openaria.core.output.HashUtils.*;


import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class HashUtilsTest {

    @Test
    public void removeWhiteSpaceFromJson_noOp() {
        String input = "{\"a\":123,\"b\":456}";
        String output = "{\"a\":123,\"b\":456}";

        assertThat(
            removeWhiteSpaceFromJson(input),
            is(output)
        );
    }

    @Test
    public void removeWhiteSpaceFromJson_removesNewLines() {
        String input = "{\n\"a\":123,\n\"b\":456\n}";
        String output = "{\"a\":123,\"b\":456}";

        assertThat(
            removeWhiteSpaceFromJson(input),
            is(output)
        );
    }

    @Test
    public void removeWhiteSpaceFromJson_removesNewLinesAndLeadingSpace() {
        String input = "{\n  \"a\":123,\n  \"b\":456\n}";
        String output = "{\"a\":123,\"b\":456}";

        assertThat(
            removeWhiteSpaceFromJson(input),
            is(output)
        );
    }

    @Test
    public void removeWhiteSpaceFromJson_removesNewLinesAndLeadingSpaceAndMiddleSpace() {
        String input = "{\n  \"a\": 123,\n  \"b\": 456\n}";
        String output = "{\"a\":123,\"b\":456}";

        assertThat(
            removeWhiteSpaceFromJson(input),
            is(output)
        );
    }

    @Test
    public void hashesAreTheSame() {

        String input1 = "{\"a\":123,\"b\":456}";
        String input2 = "{\n\"a\":123,\n\"b\":456\n}";
        String input3 = "{\n  \"a\":123,\n  \"b\":456\n}";
        String input4 = "{\n  \"a\": 123,\n  \"b\": 456\n}";

        String hash1 = HashUtils.hashForJson(input1);
        String hash2 = HashUtils.hashForJson(input2);
        String hash3 = HashUtils.hashForJson(input3);
        String hash4 = HashUtils.hashForJson(input4);

        assertThat(hash1.equals(hash2), is(true));
        assertThat(hash2.equals(hash3), is(true));
        assertThat(hash4.equals(hash4), is(true));
    }

    @Test
    public void addHash_retainsSingleLineFormatting() {

        String input1 = "{\"a\":123,\"b\":456}";
        String hash = HashUtils.hashForJson(input1);

        String correctOutput = "{\"" + HASH_FIELD_NAME + "\": \"" + hash + "\",\"a\":123,\"b\":456}";

        assertThat(HashUtils.addHash(input1), is(correctOutput));
    }

    @Test
    public void addHash_retainsMultiLineFormatting_ex1() {

        String input = "{\n\"a\":123,\n\"b\":456\n}"; //multi-line, no indenting
        String hash = HashUtils.hashForJson(input);

        String correctOutput = "{\n\"" + HASH_FIELD_NAME + "\": \"" + hash + "\",\n\"a\":123,\n\"b\":456\n}";

        assertThat(HashUtils.addHash(input), is(correctOutput));
    }

    @Test
    public void addHash_retainsMultiLineFormatting_ex2() {

        String input = "{\n  \"a\":123,\n  \"b\":456\n}"; //multi-line, with indenting
        String hash = HashUtils.hashForJson(input);

        String correctOutput = "{\n  \"" + HASH_FIELD_NAME + "\": \"" + hash + "\",\n  \"a\":123,\n  \"b\":456\n}";

        assertThat(HashUtils.addHash(input), is(correctOutput));
    }

    /*
     * SampleData and NestedData are designed to be written to JSON and "waste" verical space in
     * arrays. We want to show how this vertical space can be removed using
     * "removeArrayWhiteSpace(json)"
     */
    private static class SampleData {

        String str;
        double[] data;
        NestedData inner;

        SampleData() {
            this.str = "hello";
            this.data = new double[]{1.0 / 3.0, PI};
            this.inner = new NestedData(new float[]{(float) (1.0 / 3.0), (float) PI}, new double[]{1.0 / 3.0, PI});
        }
    }

    private static class NestedData {

        float[] floats;
        double[] doubles;

        NestedData(float[] floats, double[] doubles) {
            this.floats = floats;
            this.doubles = doubles;
        }
    }

    @Test
    public void testRemovingArrayWhiteSpace() {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        //Create a sample POJO with some array data
        String json = gson.toJson(new SampleData());

        //Now remove the white space in the array data...
        String jsonWithLessWastedVerticalSpace = removeArrayWhiteSpace(json);

        //Look!  The Hashes are the same for both...
        assertThat(hashForJson(json), is(hashForJson(jsonWithLessWastedVerticalSpace)));

        String[] linesPre = json.split("\n");
        String[] linesPost = jsonWithLessWastedVerticalSpace.split("\n");

        //BUT!  The two copies of the raw json have different number of lines
        assertThat(linesPre.length, is(17));
        assertThat(linesPost.length, is(8));
    }

    @Test
    public void hashForStringArrayAcceptsNull() {
        String[] arrayWithNullValue = new String[]{"a", "b", null};

        assertDoesNotThrow(() -> hashForStringArray(arrayWithNullValue));
    }

    @Test
    public void hashForStringArrayDoesNotIgnoreNull() {
        String[] array1 = new String[]{"a", "b"};
        String[] array2 = new String[]{"a", "b", null};
        String[] array3 = new String[]{"a", null, "b"};

        //rehashing produces same output twice
        assertThat(hashForStringArray(array1), is(hashForStringArray(array1)));
        assertThat(hashForStringArray(array2), is(hashForStringArray(array2)));
        assertThat(hashForStringArray(array3), is(hashForStringArray(array3)));

        //all 3 hashes are different
        assertThat(hashForStringArray(array1), is(not(hashForStringArray(array2))));
        assertThat(hashForStringArray(array1), is(not(hashForStringArray(array3))));
        assertThat(hashForStringArray(array2), is(not(hashForStringArray(array3))));
    }

}
