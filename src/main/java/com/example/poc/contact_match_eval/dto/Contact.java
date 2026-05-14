package com.example.poc.contact_match_eval.dto;

/**
 * Contact DTO (using Java record: immutable, concise).
 *
 * @param contactId the {@link String} unique contact identifier
 * @param firstName the {@link String} contact first name
 * @param lastName  the {@link String} contact last name
 * @param email     the {@link String} contact email
 * @param zipCode   the {@link Long} contact zip code
 * @param address   the {@link String} contact address
 */
public record Contact(
    String contactId,
    String firstName,
    String lastName,
    String email,
    Long zipCode,
    String address
) {
}