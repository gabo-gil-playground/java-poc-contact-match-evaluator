package com.example.poc.contact_match_eval.service;

import com.example.poc.contact_match_eval.constant.Constants;
import com.example.poc.contact_match_eval.dto.Contact;
import com.example.poc.contact_match_eval.dto.ContactMatch;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Unit test cases for {@link ContactMatchService}
 */
class ContactMatchServiceTest {
    @TempDir
    Path tempDir;

    /**
     * Generates duplicated id input tests cases values
     *
     * @return {@link Stream < Arguments >}
     */
    private static Stream<Arguments> duplicatedInputTestCases() {
        return Stream.of(
            arguments("duplicated_ids",
                List.of(
                    new Contact("30", "Cyrus", "Solis", "erat.in.consectetuer@aol.ca", 71669L, "\"P.O. Box 447, 4294 Ante Ave\""),
                    new Contact("30", "Cyrus", "Solis", "erat.in.consectetuer@aol.ca", 71669L, "\"P.O. Box 447, 4294 Ante Ave\""),
                    new Contact("30", "Cyrus", "Solis", "erat.in.consectetuer@aol.ca", 71669L, "\"P.O. Box 447, 4294 Ante Ave\""),
                    new Contact("60", "Gage", "Glover", "magna.a.neque@outlook.couk", 86876L, "712-8209 Sagittis St."),
                    new Contact("60", "Gage", "Glover", "magna.a.neque@outlook.couk", 86876L, "712-8209 Sagittis St.")
                )
            ),
            arguments("duplicated_ids_with_extra_unmatch_contact",
                List.of(
                    new Contact("30", "Cyrus", "Solis", "erat.in.consectetuer@aol.ca", 71669L, "\"P.O. Box 447, 4294 Ante Ave\""),
                    new Contact("30", "Graiden", "None", "none", 0L, "None"),
                    new Contact("60", "Gage", "Glover", "magna.a.neque@outlook.couk", 86876L, "712-8209 Sagittis St."),
                    new Contact("60", "Graiden", "None", "none", 0L, "None"),
                    new Contact("253", "Graiden", "Buck", "bibendum.donec.felis@yahoo.edu", 80549L, "983-4655 Erat Avenue")
                )
            ),
            arguments("duplicated_ids_with_extra_match_contact",
                List.of(
                    new Contact("30", "Cyrus", "Solis", "erat.in.consectetuer@aol.ca", 71669L, "\"P.O. Box 447, 4294 Ante Ave\""),
                    new Contact("30", "Cyrus", "Solis", "erat.in.consectetuer@aol.ca", 71669L, "\"P.O. Box 447, 4294 Ante Ave\""),
                    new Contact("60", "Gage", "Glover", "magna.a.neque@outlook.couk", 86876L, "712-8209 Sagittis St."),
                    new Contact("60", "Gage", "Glover", "magna.a.neque@outlook.couk", 86876L, "712-8209 Sagittis St."),
                    new Contact("260", "Iona", "Glover", "diam@yahoo.net", 69586L, "\"P.O. Box 836, 3506 Mauris St.\"")
                )
            )
        );
    }

    /**
     * Generates valid input tests cases values
     *
     * @return {@link Stream < Arguments >}
     */
    private static Stream<Arguments> validInputTestCases() {
        return Stream.of(
            arguments("high_accuracy_match",
                List.of(
                    new Contact("30", "Cyrus", "Solis", "erat.in.consectetuer@aol.ca", 71669L, "\"P.O. Box 447, 4294 Ante Ave\""),
                    new Contact("303030", " cyrus", "SOLIS ", " ERAT.in.consectetuer@aol.ca ", 71669L, "\"P.O. Box 447, 4294 Ante Ave\"")
                ),
                List.of(
                    new ContactMatch("30", "303030", Constants.MATCH_ACCURACY_HIGH),
                    new ContactMatch("303030", "30", Constants.MATCH_ACCURACY_HIGH)
                )
            ),
            arguments("medium_accuracy_match",
                List.of(
                    new Contact("359", "Orli", "Stafford", "lorem.ipsum@aol.net", 68640L, "\"P.O. Box 658, 9382 A, Avenue\""),
                    new Contact("359359", "OTHER ", " other", "LOREM.IPSUM@AOL.NET", 68640L, "\"P.O. Box 658, 9382 A, Avenue\"")
                ),
                List.of(
                    new ContactMatch("359", "359359", Constants.MATCH_ACCURACY_MEDIUM),
                    new ContactMatch("359359", "359", Constants.MATCH_ACCURACY_MEDIUM)
                )
            ),
            arguments("low_accuracy_match",
                List.of(
                    new Contact("60", "Gage", "Glover", "magna.a.neque@outlook.couk", 86876L, "712-8209 Sagittis St."),
                    new Contact("606060", "  Gage  ", "Other", "other@other.net", 123456L, "Other St.")
                ),
                List.of(
                    new ContactMatch("60", "606060", Constants.MATCH_ACCURACY_LOW),
                    new ContactMatch("606060", "60", Constants.MATCH_ACCURACY_LOW)
                )
            ),
            arguments("desc_order_match",
                List.of(
                    new Contact("60", "Gage", "Glover", "magna.a.neque@outlook.couk", 86876L, "712-8209 Sagittis St."),
                    new Contact("606060", "  Gage  ", "Glover", "other@other.net", 86876L, "712-8209 Sagittis St."),
                    new Contact("606061", "Gage  ", " GLOVER", "other@other.net", 86876L, "Other St."),
                    new Contact("606062", "gage", "floverr", "other@other.net", 123456L, "Other St.")
                ),
                List.of(
                    new ContactMatch("60", "606060", Constants.MATCH_ACCURACY_HIGH),
                    new ContactMatch("60", "606061", Constants.MATCH_ACCURACY_MEDIUM),
                    new ContactMatch("60", "606062", Constants.MATCH_ACCURACY_LOW),
                    new ContactMatch("606060", "60", Constants.MATCH_ACCURACY_HIGH),
                    new ContactMatch("606060", "606061", Constants.MATCH_ACCURACY_HIGH),
                    new ContactMatch("606060", "606062", Constants.MATCH_ACCURACY_LOW),
                    new ContactMatch("606061", "606060", Constants.MATCH_ACCURACY_HIGH),
                    new ContactMatch("606061", "60", Constants.MATCH_ACCURACY_MEDIUM),
                    new ContactMatch("606061", "606062", Constants.MATCH_ACCURACY_MEDIUM),
                    new ContactMatch("606062", "606061", Constants.MATCH_ACCURACY_MEDIUM),
                    new ContactMatch("606062", "60", Constants.MATCH_ACCURACY_LOW),
                    new ContactMatch("606062", "606060", Constants.MATCH_ACCURACY_LOW)
                )
            ),
            arguments("mixed_match",
                List.of(
                    new Contact("900", "B", "G", null, null, "Maecenas street."),
                    new Contact("925", "B", "G", null, null, "\"Montes, street.\""),
                    new Contact("940", "I", "W", null, null, "6944 Ipsum. Avenue"),
                    new Contact("808080", "A", "A", "a@a.net", 654321L, "A street"),
                    new Contact("900900", "B", "G", null, null, "Maecenas street.")
                ),
                List.of(
                    new ContactMatch("900", "900900", Constants.MATCH_ACCURACY_HIGH),
                    new ContactMatch("900", "925", Constants.MATCH_ACCURACY_MEDIUM),
                    new ContactMatch("900", "940", Constants.MATCH_ACCURACY_LOW),
                    new ContactMatch("925", "900", Constants.MATCH_ACCURACY_MEDIUM),
                    new ContactMatch("925", "900900", Constants.MATCH_ACCURACY_MEDIUM),
                    new ContactMatch("925", "940", Constants.MATCH_ACCURACY_LOW),
                    new ContactMatch("940", "900", Constants.MATCH_ACCURACY_LOW),
                    new ContactMatch("940", "925", Constants.MATCH_ACCURACY_LOW),
                    new ContactMatch("940", "900900", Constants.MATCH_ACCURACY_LOW),
                    new ContactMatch("900900", "900", Constants.MATCH_ACCURACY_HIGH),
                    new ContactMatch("900900", "925", Constants.MATCH_ACCURACY_MEDIUM),
                    new ContactMatch("900900", "940", Constants.MATCH_ACCURACY_LOW)
                )
            )
        );
    }

    /**
     * Generates edge input tests cases values
     *
     * @return {@link Stream < Arguments >}
     */
    private static Stream<Arguments> edgeInputTestCases() {
        return Stream.of(
            arguments("empty_input", Collections.emptyList(), Collections.emptyList()),
            arguments("empty_contact_id",
                List.of(
                    new Contact("", "", "", "", null, ""),
                    new Contact(null, null, null, null, null, ""),
                    new Contact(null, null, null, null, null, "")
                ),
                Collections.emptyList()
            ),
            arguments("empty_string_equivalent_to_null",
                List.of(
                    new Contact("1", "", "", "", null, ""),
                    new Contact("2", "", "", "", null, "")
                ),
                Collections.emptyList()
            ),
            arguments("just_one_contact",
                List.of(
                    new Contact("30", "Cyrus", "Solis", "erat.in.consectetuer@aol.ca", 71669L, "\"P.O. Box 447, 4294 Ante Ave\"")
                ),
                Collections.emptyList()
            )
        );
    }

    /**
     * Scenario:
     * Executes [{@link ContactMatchService#evaluateContactMatching()}] when contact id are duplicated
     * Expectation:
     * An empty contact match list should be created
     */
    @ParameterizedTest(name = "scenario: {0}")
    @MethodSource("duplicatedInputTestCases")
    void whenDuplicatedInputTestCasesShouldReturnEmptyMatchList(
        final String scenarioName,
        final List<Contact> contactList
    ) throws Exception {
        File inputFile = createCsvInputFile("input_test_" + scenarioName, contactList);
        File outputFile = tempDir.resolve("output_test_" + scenarioName + ".csv").toFile();

        ContactMatchService contactMatchService = new ContactMatchService();
        ReflectionTestUtils.setField(contactMatchService, "inputPath", inputFile.getAbsolutePath());
        ReflectionTestUtils.setField(contactMatchService, "outputPath", outputFile.getAbsolutePath());

        contactMatchService.evaluateContactMatching();

        List<ContactMatch> result = readOutputFile(outputFile);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    /**
     * Scenario:
     * Executes [{@link ContactMatchService#evaluateContactMatching()}] when contact values are valid
     * Expectation:
     * A contact match list with high, medium and low accuracy levels should be created
     */
    @ParameterizedTest(name = "scenario: {0}")
    @MethodSource("validInputTestCases")
    void whenValidInputTestCasesShouldReturnExpectedMatchList(
        final String scenarioName,
        final List<Contact> contactList,
        final List<ContactMatch> expectedContacMatchtList
    ) throws Exception {
        File inputFile = createCsvInputFile("input_test_" + scenarioName, contactList);
        File outputFile = tempDir.resolve("output_test_" + scenarioName + ".csv").toFile();

        ContactMatchService contactMatchService = new ContactMatchService();
        ReflectionTestUtils.setField(contactMatchService, "inputPath", inputFile.getAbsolutePath());
        ReflectionTestUtils.setField(contactMatchService, "outputPath", outputFile.getAbsolutePath());

        contactMatchService.evaluateContactMatching();

        List<ContactMatch> result = readOutputFile(outputFile);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedContacMatchtList, result);
    }

    /**
     * Scenario:
     * Executes [{@link ContactMatchService#evaluateContactMatching()}] when contacts are empty or not valid (edge cases)
     * Expectation:
     * An empty contact match list should be created
     */
    @ParameterizedTest(name = "scenario: {0}")
    @MethodSource("edgeInputTestCases")
    void whenEdgeInputTestCasesShouldReturnEmptyMatchList(
        final String scenarioName,
        final List<Contact> contactList
    ) throws Exception {
        File inputFile = createCsvInputFile("input_test_" + scenarioName, contactList);
        File outputFile = tempDir.resolve("output_test_" + scenarioName + ".csv").toFile();

        ContactMatchService contactMatchService = new ContactMatchService();
        ReflectionTestUtils.setField(contactMatchService, "inputPath", inputFile.getAbsolutePath());
        ReflectionTestUtils.setField(contactMatchService, "outputPath", outputFile.getAbsolutePath());

        contactMatchService.evaluateContactMatching();

        List<ContactMatch> result = readOutputFile(outputFile);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    /**
     * Creates a CSV input file by file name scenario and contact list
     *
     * @param filename    {@link String} file name scenario
     * @param contactList {@link List<Contact>} contact list
     * @return {@link File} csv input file
     */
    private File createCsvInputFile(String filename, List<Contact> contactList) throws IOException {
        String headers = String.join(
            ",",
            Constants.INPUT_HEADER_CONTACT_ID,
            Constants.INPUT_HEADER_FIRST_NAME,
            Constants.INPUT_HEADER_LAST_NAME,
            Constants.INPUT_HEADER_EMAIL,
            Constants.INPUT_HEADER_POSTAL_ZIP,
            Constants.INPUT_HEADER_ADDRESS
        );

        StringBuilder inputFileContent = new StringBuilder(headers).append("\n");

        for (Contact contact : contactList) {
            String row = String.join(
                ",",
                contact.contactId(),
                contact.firstName(),
                contact.lastName(),
                contact.email(),
                (contact.zipCode() != null ? contact.zipCode().toString() : ""),
                contact.address()
            );
            inputFileContent.append(row).append("\n");
        }

        File inputFile = tempDir.resolve(filename + ".csv").toFile();
        Files.writeString(inputFile.toPath(), inputFileContent.toString());

        return inputFile;
    }

    /**
     * Creates a contact match list from CSV output file
     *
     * @param outputFile {@link File} the output file
     * @return {@link List<ContactMatch>} from output file
     */
    private List<ContactMatch> readOutputFile(File outputFile) throws IOException, CsvValidationException {
        List<ContactMatch> contactMatchList = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            reader.readNext(); // skip header                                                                                                                                             LSP

            String[] row;
            while ((row = reader.readNext()) != null) {
                contactMatchList.add(new ContactMatch(
                    row[0].trim(),
                    row[1].trim(),
                    row[2].trim()
                ));
            }
        }

        return contactMatchList;
    }
}