package com.example.poc.contact_match_eval.service;

import com.example.poc.contact_match_eval.constant.Constants;
import com.example.poc.contact_match_eval.dto.Contact;
import com.example.poc.contact_match_eval.dto.ContactIndexes;
import com.example.poc.contact_match_eval.dto.ContactMatch;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contact match service
 */
@Service
@Slf4j
public class ContactMatchService {

    @Value("${app.contact.input.path:${APP_CONTACT_INPUT_PATH}}")
    private String inputPath;

    @Value("${app.contact.output.path:${APP_CONTACT_OUTPUT_PATH}}")
    private String outputPath;

    /**
     * Application entry point that executes contact matching evaluation after application startup.
     * Reads input CSV, processes contacts and writes match results to output file.
     */
    @EventListener(ApplicationReadyEvent.class) // run at start just for POC / code challenge purposes
    public void evaluateContactMatching() {
        log.info("evaluateContactMatching - start");
        log.debug("evaluateContactMatching - input file path: {}", inputPath);
        log.debug("evaluateContactMatching - output file path: {}", outputPath);

        // IMPORTANT: POC & challenge assumptions
        // 1. just for challenge purposes, the input / output files are configured by env. vars. at application.yaml
        // 2. duplicated contact id are removed and not included as part of evaluation
        // 3. current implementation evaluates contact matching based on field similarity (should be equal)
        // 4. values are trimmed - we are not considering '-', '_', and other special characteres
        // 5. bidirectional / reciprocal contact matching implementation (A -> B and B -> A)
        // 6. accuracy levels: None (0 matches - discarded), Low (<3 matches), Medium (3 matches), High (>3 matches)
        // 7. result sort order by source contact id, match contact id, accuracy descendent (High, Medium, Low)

        List<Contact> contactList = readInputFile();
        log.info("evaluateContactMatching - read {} contacts from input file", contactList.size());

        // also for empty input it writes the output file
        List<ContactMatch> contactMatchList = evaluate(contactList);
        log.info("evaluateContactMatching - find {} contact match values", contactMatchList.size());
        writeOutputFile(contactMatchList);

        log.info("evaluateContactMatching - completed");
    }

    /**
     * Evaluates contact matching using hash-based indexing for O(n) complexity
     *
     * @param contactList the {@link List<Contact>} list of contacts to evaluate
     * @return {@link List<ContactMatch>}
     */
    private List<ContactMatch> evaluate(List<Contact> contactList) {
        log.debug("evaluate - original contact list - size: {}", contactList.size());

        Set<String> duplicatedContactIds = removeDuplicatedContactIds(contactList);

        if (!duplicatedContactIds.isEmpty()) {
            log.warn("evaluate - duplicated contact ids removed before matching: {}", duplicatedContactIds);
        }

        log.debug("evaluate - contact list without duplicated ids - size: {}", contactList.size());

        List<ContactMatch> contactMatchList = findAllContactMatches(contactList, buildContactMatchIndexes(contactList));
        log.debug("evaluate - contact match list - size: {}", contactMatchList.size());

        List<ContactMatch> sortedContactMatchList = sortByAccuracyDescending(contactMatchList);
        log.debug("evaluate - sorted contact match list - size: {}", sortedContactMatchList.size());

        return sortedContactMatchList;
    }

    /**
     * Removes all contacts with duplicate IDs from the contact list
     *
     * @param contactList the {@link List<Contact>} list of contacts to process
     * @return {@link Set<String>}
     */
    private Set<String> removeDuplicatedContactIds(List<Contact> contactList) {
        Map<String, Long> contactIdCountMap = contactList.stream()
            .collect(Collectors.groupingBy(Contact::contactId, Collectors.counting()));

        Set<String> contactIdDuplicatedSet = contactIdCountMap.entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        if (!contactIdDuplicatedSet.isEmpty()) {
            contactList.removeIf(c -> contactIdDuplicatedSet.contains(c.contactId()));
        }

        return contactIdDuplicatedSet;
    }

    /**
     * Builds hash-based indexes for each searchable field
     *
     * @param contactList the {@link List<Contact>} list of contacts to index
     * @return {@link ContactIndexes}
     */
    private ContactIndexes buildContactMatchIndexes(List<Contact> contactList) {
        Map<String, List<Contact>> firstNameIndex = new HashMap<>();
        Map<String, List<Contact>> lastNameIndex = new HashMap<>();
        Map<String, List<Contact>> emailIndex = new HashMap<>();
        Map<Long, List<Contact>> zipCodeIndex = new HashMap<>();
        Map<String, List<Contact>> addressIndex = new HashMap<>();

        for (Contact contact : contactList) {
            String normFirstName = normalizeStringValue(contact.firstName());
            String normLastName = normalizeStringValue(contact.lastName());
            String normEmail = normalizeStringValue(contact.email());
            Long zipCode = contact.zipCode();
            String normAddress = normalizeStringValue(contact.address());

            addContactToIndexMap(firstNameIndex, normFirstName, contact);
            addContactToIndexMap(lastNameIndex, normLastName, contact);
            addContactToIndexMap(emailIndex, normEmail, contact);
            addContactToIndexMap(zipCodeIndex, zipCode, contact);
            addContactToIndexMap(addressIndex, normAddress, contact);
        }

        return new ContactIndexes(emailIndex, zipCodeIndex, firstNameIndex, lastNameIndex, addressIndex);
    }

    /**
     * Adds a contact to an index map if the key is non-empty and non-null
     *
     * @param index   the {@link Map} map to add the contact to
     * @param key     the {@link Object} key used for indexing (may be null or empty)
     * @param contact the {@link Contact} contact to add to the index
     */
    private <K> void addContactToIndexMap(Map<K, List<Contact>> index, K key, Contact contact) {
        if (key == null)
            return;

        if (key instanceof String str && str.isEmpty())
            return;

        index.computeIfAbsent(key, k -> new ArrayList<>()).add(contact);
    }

    /**
     * Finds all matching contacts by comparing each source contact against indexed candidates
     *
     * @param contactList    the {@link List<Contact>} source contacts to match
     * @param contactIndexes the {@link ContactIndexes>} field indexes for fast lookup
     * @return {@link List<ContactMatch>}
     */
    private List<ContactMatch> findAllContactMatches(List<Contact> contactList, ContactIndexes contactIndexes) {
        List<ContactMatch> contactMatchList = new ArrayList<>();

        for (Contact contact : contactList) {
            Map<String, Integer> matchCounts = countContactMatches(contact, contactIndexes);
            contactMatchList.addAll(createContactMatcheList(contact, matchCounts));
        }
        return contactMatchList;
    }

    /**
     * Counts matching fields for each candidate contact by querying each field index
     *
     * @param contact        the {@link Contact} contact to find matches for
     * @param contactIndexes the {@link ContactIndexes>} field indexes to search
     * @return {@link Map<String, Integer>}
     */
    private Map<String, Integer> countContactMatches(Contact contact, ContactIndexes contactIndexes) {
        Map<String, Integer> contactMatchCountsMap = new HashMap<>();

        addContactMatchCandidates(contactIndexes.byFirstName(), normalizeStringValue(contact.firstName()), contact, contactMatchCountsMap);
        addContactMatchCandidates(contactIndexes.byLastName(), normalizeStringValue(contact.lastName()), contact, contactMatchCountsMap);
        addContactMatchCandidates(contactIndexes.byEmail(), normalizeStringValue(contact.email()), contact, contactMatchCountsMap);
        addContactMatchCandidates(contactIndexes.byZipCode(), contact.zipCode(), contact, contactMatchCountsMap);
        addContactMatchCandidates(contactIndexes.byAddress(), normalizeStringValue(contact.address()), contact, contactMatchCountsMap);

        return contactMatchCountsMap;
    }

    /**
     * Adds candidate matches from a specific index to the match counts
     *
     * @param contactIndexes        the {@link Map} map of field value to contact list
     * @param fieldKey              the {@link Object} key to lookup in index
     * @param contact               the {@link Contact} source contact to exclude from matches
     * @param contactMatchCountsMap the {@link Map} accumulator for match counts
     */
    private <K> void addContactMatchCandidates(
        Map<K, List<Contact>> contactIndexes,
        K fieldKey,
        Contact contact,
        Map<String, Integer> contactMatchCountsMap
    ) {
        if (contactIndexes == null || fieldKey == null)
            return;

        List<Contact> candidates = contactIndexes.get(fieldKey);

        if (candidates == null)
            return;

        for (Contact candidate : candidates) {
            if (!candidate.contactId().equals(contact.contactId())) {
                contactMatchCountsMap.merge(candidate.contactId(), 1, Integer::sum);
            }
        }
    }

    /**
     * Creates ContactMatch objects from match counts based on accuracy thresholds
     *
     * @param contact               the {@link Contact} source contact
     * @param contactMatchCountsMap the {@link Map} map of candidate contact id to match count
     * @return {@link List<ContactMatch>}
     */
    private List<ContactMatch> createContactMatcheList(Contact contact, Map<String, Integer> contactMatchCountsMap) {
        List<ContactMatch> contactMatchList = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : contactMatchCountsMap.entrySet()) {
            String accuracy = computeContactMatchAccuracy(entry.getValue());

            if (accuracy != null) {
                contactMatchList.add(new ContactMatch(contact.contactId(), entry.getKey(), accuracy));
            }
        }

        return contactMatchList;
    }

    /**
     * Computes accuracy level based on match count
     *
     * @param contactMatchCount the {@link Integer} number of matching fields
     * @return {@link String}
     */
    private String computeContactMatchAccuracy(int contactMatchCount) {
        if (contactMatchCount == 0)
            return null;
        if (contactMatchCount < Constants.MATCH_THRESHOLD_LOW)
            return Constants.MATCH_ACCURACY_LOW;
        if (contactMatchCount == Constants.MATCH_THRESHOLD_MEDIUM)
            return Constants.MATCH_ACCURACY_MEDIUM;

        return Constants.MATCH_ACCURACY_HIGH;
    }

    /**
     * Sorts contact matches by accuracy in descending order
     *
     * @param matches the {@link List<ContactMatch>} list of matches to sort
     * @return {@link List<ContactMatch>}
     */
    private List<ContactMatch> sortByAccuracyDescending(List<ContactMatch> matches) {
        return matches.stream()
            .sorted(Comparator.comparingInt((ContactMatch m) -> Integer.parseInt(m.contactIdSource()))
                .thenComparing((a, b) -> Integer.compare(accuracySortDescPriorityOrder(b.accuracy()), accuracySortDescPriorityOrder(a.accuracy())))
                .thenComparingInt(m -> Integer.parseInt(m.contactIdMatch())))
            .collect(Collectors.toList());
    }

    /**
     * Returns numeric priority for accuracy sorting
     *
     * @param contactMatchAccuracy the {@link String} accuracy level string
     * @return {@link Integer}
     */
    private int accuracySortDescPriorityOrder(String contactMatchAccuracy) {
        return switch (contactMatchAccuracy) {
            case Constants.MATCH_ACCURACY_HIGH -> 3;
            case Constants.MATCH_ACCURACY_MEDIUM -> 2;
            case Constants.MATCH_ACCURACY_LOW -> 1;
            default -> 0;
        };
    }

    /**
     * Reads the input CSV file and maps each row to a Contact object
     *
     * @return {@link List<Contact>}
     */
    private List<Contact> readInputFile() {
        List<Contact> contactList = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(inputPath, StandardCharsets.UTF_8))) {
            String[] header = reader.readNext();

            if (header == null) {
                log.error("readInputFile - input file is empty or has no header");
                return contactList;
            }

            int contactIdIndex = findColumnIndex(header, Constants.INPUT_HEADER_CONTACT_ID);
            int firstNameIndex = findColumnIndex(header, Constants.INPUT_HEADER_FIRST_NAME);
            int lastName1Index = findColumnIndex(header, Constants.INPUT_HEADER_LAST_NAME);
            int emailIndex = findColumnIndex(header, Constants.INPUT_HEADER_EMAIL);
            int zipCodeIndex = findColumnIndex(header, Constants.INPUT_HEADER_POSTAL_ZIP);
            int addressIndex = findColumnIndex(header, Constants.INPUT_HEADER_ADDRESS);

            String[] row;
            while ((row = reader.readNext()) != null) {
                String contactId = getCellValueAsString(row, contactIdIndex);
                String firstName = getCellValueAsString(row, firstNameIndex);
                String lastName = getCellValueAsString(row, lastName1Index);
                String email = getCellValueAsString(row, emailIndex);
                Long zipCode = getStringValueAsLong(getCellValueAsString(row, zipCodeIndex));
                String address = getCellValueAsString(row, addressIndex);

                contactList.add(new Contact(contactId, firstName, lastName, email, zipCode, address));
            }

        } catch (IOException | CsvValidationException e) {
            log.error("readInputFile - error reading input file: {}", e.getMessage(), e);
        }

        return contactList;
    }

    /**
     * Writes ContactMatch objects to the output CSV file
     *
     * @param contactMatchList the {@link List<ContactMatch>} list of matches to write
     */
    private void writeOutputFile(List<ContactMatch> contactMatchList) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputPath, StandardCharsets.UTF_8))) {
            String[] header = {
                Constants.OUTPUT_HEADER_CONTACT_ID_SOURCE,
                Constants.OUTPUT_HEADER_CONTACT_ID_MATCH,
                Constants.OUTPUT_HEADER_ACCURACY
            };
            writer.writeNext(header);

            List<String[]> data = contactMatchList.stream()
                .map(cm -> new String[]{
                    cm.contactIdSource(),
                    cm.contactIdMatch(),
                    cm.accuracy()
                })
                .collect(Collectors.toList());
            writer.writeAll(data);

            log.info("writeOutputFile - output file written successfully to: {}", outputPath);

        } catch (IOException e) {
            log.error("writeOutputFile - error writing output file: {}", e.getMessage(), e);
        }
    }

    /**
     * Finds the index of a column in the CSV header by name
     *
     * @param headers    the {@link String[]} array of header column names
     * @param columnName the {@link String} name of the column to find
     * @return {@link Integer}
     */
    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(columnName.trim())) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Gets the value from a CSV line at the specified index.
     *
     * @param line  array of column values from a CSV row
     * @param index column index to retrieve
     * @return trimmed value or empty string if index is out of bounds
     */
    private String getCellValueAsString(String[] line, int index) {
        if (index < 0 || index >= line.length) {
            return "";
        }

        return line[index] != null ? line[index].trim() : "";
    }

    /**
     * Gets a String value as Long, returning null if invalid or empty.
     *
     * @param value string value to parse
     * @return parsed Long value or null if not valid
     */
    private Long getStringValueAsLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Normalizes a string value by trimming whitespace and converting to lowercase
     *
     * @param value the {@link String} string to normalize
     * @return {@link String}
     */
    private String normalizeStringValue(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
