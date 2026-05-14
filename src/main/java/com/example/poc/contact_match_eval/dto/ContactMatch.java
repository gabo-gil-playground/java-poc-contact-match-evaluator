package com.example.poc.contact_match_eval.dto;

/**
 * Contact match DTO (using Java record: immutable, concise).
 *
 * @param contactIdSource the {@link String} unique contact identifier (source)
 * @param contactIdMatch  the {@link String} unique contact identifier (match)
 * @param accuracy        the {@link String} match accuracy level
 */
public record ContactMatch(
    String contactIdSource,
    String contactIdMatch,
    String accuracy
) {
}