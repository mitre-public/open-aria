package org.mitre.openaria.core.output;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.hashing.Hashers.getHasherFor;

import org.mitre.hashing.Hashables;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

public class HashUtils {

    private static final Gson FLAT_GSON = new Gson();
    private static final JsonParser PARSER = new JsonParser();

    public static final String HASH_FIELD_NAME = "uniqueId";

    /**
     * Compute a unique hash for this JSON String. The hash is computed from a "normalized" version
     * of the input JSON. The normalization ensures simple formatting choices (e.g. single-line or
     * multi-line JSON? Spaces or tabs for indenting? 2 spaces or 4?) do not impact the resulting
     * hash.
     * <p>
     * Importantly, the ORDERING of the fields in the JSON will matter, so "{"a": 123, "b": 456}"
     * will generate a different hash from "{"b": 456, "a": 123}"
     *
     * @return A hash computed from this Json String, the form of the incoming Json flattened to a
     *     single line.
     */
    public static String hashForJson(String json) {

        String regularizedJson = removeWhiteSpaceFromJson(json);

        String hash = Hashables.computeHashFor(regularizedJson);
        return hash;
    }

    /** @return An equivalent form of the incoming Json flattened to a single line. */
    static String removeWhiteSpaceFromJson(String json) {

        //reparse the JSON to ensure that all whitespace formatting is uniform
        String flattend = FLAT_GSON.toJson(PARSER.parse(json));

        return flattend;
    }

    /**
     * Computes a unique hash for this JSON String (see hashForJson(String)) and then inserts the
     * hash into the JSON object under the field name "uniqueId". The newly inserted field is always
     * the 1st field in the new JSON string and it should copy the formatting of the JSON.
     *
     * @return An copy of the input with a new row like: "uniqueId": "38c39bafe36332da80002d6c45afd98c"
     */
    public static String addHash(String json) {
        String hash = hashForJson(json);
        return addHash(json, hash);
    }

    /**
     * Insert a pre-computed hash into ean existing json object
     *
     * @return An copy of the input with a new row like: "uniqueId": "38c39bafe36332da80002d6c45afd98c"
     */
    public static String addHash(String json, String hash) {
        int newLineIndex = json.indexOf("\n");

        return (newLineIndex == -1)
            ? addHashToSingleLineJson(json, hash)
            : addHashToMultiLineJson(json, hash);
    }

    private static String addHashToSingleLineJson(String json, String hash) {
        checkArgument(json.indexOf('{') == 0);

        StringBuilder sb = new StringBuilder(json);
        sb.insert(1, hashJsonEntry(hash));
        return sb.toString();
    }

    private static String addHashToMultiLineJson(String json, String hash) {
        checkArgument(json.indexOf('{') == 0);

        //copy the formatting of the 1st field in the json, maybe it uses tabs, spaces, etc..
        String whitespace = json.substring(1, json.indexOf("\""));

        StringBuilder sb = new StringBuilder(json);

        sb.insert(1, whitespace + hashJsonEntry(hash));

        return sb.toString();
    }

    //write: --> "uniqueId": "38c39bafe36332da80002d6c45afd98c",
    private static String hashJsonEntry(String hash) {
        return "\"" + HASH_FIELD_NAME + "\": \"" + hash + "\",";
    }

    /**
     * Use a {@code AutoHasher<String[]>} to compute a unique hash for an Array of Strings.
     *
     * @return A hash computed from this Json String, the form of the incoming Json flattened to a
     *     single line.
     */
    public static String hashForStringArray(String[] stringData) {
        checkNotNull(stringData);

        return getHasherFor(String[].class).hash(stringData);
    }

    /**
     * Can be used to prevent a pretty printed JSON String from wasting too much vertical space when
     * listing an array of numbers.
     */
    public static String removeArrayWhiteSpace(String json) {

        String VERTICAL_ARRAY = ": [\n";  //appears at the beginning of an array that is printed one entry per line

        StringBuilder workingCopy = new StringBuilder(json);

        int arrayStart = workingCopy.indexOf(VERTICAL_ARRAY, 0);

        while (arrayStart > 0) {
            int arrayEnd = workingCopy.indexOf("]", arrayStart);

            String excerpt = workingCopy.substring(arrayStart, arrayEnd);

            excerpt = excerpt.replaceAll("\\s+", "");

            workingCopy.replace(arrayStart, arrayEnd, excerpt);

            arrayStart = workingCopy.indexOf(VERTICAL_ARRAY, arrayStart); //iterate forward
        }

        return workingCopy.toString();
    }

}
