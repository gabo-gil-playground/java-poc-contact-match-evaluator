package com.example.poc.contact_match_eval.dto;

import java.util.List;
import java.util.Map;

/**
 * Container record for contact field indexes used in hash-based matching.
 *
 * @param byEmail     index of contacts grouped by normalized email
 * @param byZipCode   index of contacts grouped by zip code
 * @param byFirstName index of contacts grouped by normalized first name
 * @param byLastName  index of contacts grouped by normalized last name
 * @param byAddress   index of contacts grouped by normalized address
 */
public record ContactIndexes(
    Map<String, List<Contact>> byEmail,
    Map<Long, List<Contact>> byZipCode,
    Map<String, List<Contact>> byFirstName,
    Map<String, List<Contact>> byLastName,
    Map<String, List<Contact>> byAddress
) {
}