package com.example.poc.contact_match_eval.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test cases for {@link Constants}
 */
public class ConstantsTest {

    /**
     * Scenario:
     * Gets {@link Constants} all defined constant fixed values
     * Expectation:
     * All constant fixed values should be retrieved
     */
    @Test
    void when_constants_should_return_values() {
        assertEquals("Low", Constants.MATCH_ACCURACY_LOW);
        assertEquals("Medium", Constants.MATCH_ACCURACY_MEDIUM);
        assertEquals("High", Constants.MATCH_ACCURACY_HIGH);

        assertEquals(3, Constants.MATCH_THRESHOLD_LOW);
        assertEquals(3, Constants.MATCH_THRESHOLD_MEDIUM);

        assertEquals("contactid", Constants.INPUT_HEADER_CONTACT_ID);
        assertEquals("name", Constants.INPUT_HEADER_FIRST_NAME);
        assertEquals("name1", Constants.INPUT_HEADER_LAST_NAME);
        assertEquals("email", Constants.INPUT_HEADER_EMAIL);
        assertEquals("postalZip", Constants.INPUT_HEADER_POSTAL_ZIP);
        assertEquals("address", Constants.INPUT_HEADER_ADDRESS);

        assertEquals("ContactID Source", Constants.OUTPUT_HEADER_CONTACT_ID_SOURCE);
        assertEquals("ContactID Match", Constants.OUTPUT_HEADER_CONTACT_ID_MATCH);
        assertEquals("Accuracy", Constants.OUTPUT_HEADER_ACCURACY);
    }
}