package com.example.poc.contact_match_eval.constant;

/**
 * Application constants values (avoid hard-coded text, magic numbers and repeated values).
 */
public interface Constants {
    String MATCH_ACCURACY_LOW = "Low";
    String MATCH_ACCURACY_MEDIUM = "Medium";
    String MATCH_ACCURACY_HIGH = "High";

    int MATCH_THRESHOLD_LOW = 3;
    int MATCH_THRESHOLD_MEDIUM = 3;

    String INPUT_HEADER_CONTACT_ID = "contactid";
    String INPUT_HEADER_FIRST_NAME = "name";
    String INPUT_HEADER_LAST_NAME = "name1";
    String INPUT_HEADER_EMAIL = "email";
    String INPUT_HEADER_POSTAL_ZIP = "postalZip";
    String INPUT_HEADER_ADDRESS = "address";

    String OUTPUT_HEADER_CONTACT_ID_SOURCE = "ContactID Source";
    String OUTPUT_HEADER_CONTACT_ID_MATCH = "ContactID Match";
    String OUTPUT_HEADER_ACCURACY = "Accuracy";
}