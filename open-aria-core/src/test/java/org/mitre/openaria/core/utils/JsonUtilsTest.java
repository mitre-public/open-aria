package org.mitre.openaria.core.utils;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mitre.openaria.core.utils.JsonUtils.reformatJson;
import static org.mitre.openaria.core.utils.JsonUtils.removeTopLevelField;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class JsonUtilsTest {

    static class EmbeddedClass {
        String[] stringArray = {"a", "b", "c"};
    }

    static class SimpleTestClass {
        String stringField = "hello";
        int intField = 5;
        EmbeddedClass embedded = new EmbeddedClass();
    }

    @Test
    public void manipulatingJsonWorks() {

        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

        String asJson = gson.toJson(new SimpleTestClass());

        String unformatted = removeTopLevelField("embedded", asJson);
        String formatted = reformatJson(gson, unformatted);

        String unformatedExpectation = "{\"stringField\":\"hello\",\"intField\":5}";
        String formattedExpectation =
            "{\n"
                + "  \"stringField\": \"hello\",\n"
                + "  \"intField\": 5\n"
                + "}";

        assertThat(unformatted, is(unformatedExpectation));
        assertThat(formatted, is(formattedExpectation));
    }

    @Test
    public void removingFieldThatIsAlreadyMissing_doesNotFail_reformatsJson() {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        String asJson = gson.toJson(new SimpleTestClass());

        assertDoesNotThrow(() -> removeTopLevelField("missingField", asJson));

        String after = removeTopLevelField("missingField", asJson);

        assertThat("Formatting is different", asJson, not(equalTo(after)));
        assertThat("Equal after formatting", asJson, equalTo(reformatJson(gson, after)));
    }
}