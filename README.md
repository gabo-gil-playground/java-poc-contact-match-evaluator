# Contact Match Evaluation

A Spring Boot Web application that reads contact data from an input CSV file, evaluates field similarity between contacts, and outputs match results with accuracy levels to an output CSV file.

## Overview

This application serves as a proof-of-concept for contact matching evaluation. It processes contact records by comparing field values across the input dataset and generates accuracy-ranked match pairs. The matching logic is triggered automatically on application startup via the `@EventListener(ApplicationReadyEvent.class)` mechanism.

## Environment Configuration

> **Technology Stack**: The project is built with **Java 25** and **Spring Boot 4**. These versions are defined in `build.gradle.kts` and can be changed there.

The application uses environment variables to configure file paths. You can set these in your shell or through your deployment configuration.

| Variable | Description | Default |
|----------|-------------|---------|
| `APP_CONTACT_INPUT_PATH` | Full path to the input CSV file containing contacts | `src/main/resources/Code Assessment - Find Duplicates Input - SampleCodecsv (11).csv` |
| `APP_CONTACT_OUTPUT_PATH` | Full path where the output CSV file will be written | `src/main/resources/contact-match-result.csv` |

### Example Configuration

```bash
export APP_CONTACT_INPUT_PATH=/path/to/contacts.csv
export APP_CONTACT_OUTPUT_PATH=/path/to/output/matches.csv
```

Alternatively, you can override these in `application.yaml`:

```yaml
app:
  contact:
    input:
      path: /custom/path/input.csv
    output:
      path: /custom/path/output.csv
```

## Input CSV Format

The input file must contain the following columns:

| Column | Description |
|--------|-------------|
| `contactid` | Unique contact identifier |
| `name` | First name |
| `name1` | Last name |
| `email` | Email address |
| `postalZip` | Postal/ZIP code |
| `address` | Street address |

## Output CSV Format

The output file contains match results with three columns:

| Column | Description |
|--------|-------------|
| `ContactID Source` | Source contact identifier |
| `ContactID Match` | Matched contact identifier |
| `Accuracy` | Match accuracy level: `High`, `Medium`, `Low`, or record excluded if no match |

## Assumptions

The implementation follows these design assumptions:

1. **File Configuration**: Input and output file paths are configured via environment variables (`APP_CONTACT_INPUT_PATH`, `APP_CONTACT_OUTPUT_PATH`) or Spring properties in `application.yaml`.

2. **Duplicate Handling**: Contact records with duplicate IDs are identified and removed before evaluation. These contacts are excluded from both matching and output.

3. **Field Similarity Evaluation**: Two contacts are considered potential matches when their corresponding field values are identical after normalization. All five fields are evaluated independently.

4. **String Normalization**: Field values are normalized before comparison by trimming whitespace and converting to lowercase. Empty strings and null values are treated equivalently. Special characters (hyphens, underscores, etc.) are included in comparisons without transformation.

5. **Accuracy Levels**: Match accuracy is determined by the number of matching fields between two contacts:
   - **None (discarded)**: 0 matching fields
   - **Low**: 1-2 matching fields
   - **Medium**: 3 matching fields
   - **High**: 4-5 matching fields

6. **Sort Order**: Results are sorted in ascending order by source contact ID, then by accuracy priority in descending order (High, Medium, Low), and finally by match contact ID in ascending order.

7. **Bidirectional / Reciprocal Matching**: The matching logic is symmetric — if contact A matches contact B, the system also generates the reverse pair (B → A) as an independent output record. Both records share the same accuracy level and are subject to the same sort and deduplication rules.

## Implementation Details

### Algorithm Overview

The contact matching evaluation uses a hash-based indexing strategy to achieve O(n) time complexity:

1. **Deduplication**: Remove all contact records with duplicate IDs from the input list
2. **Index Building**: Create five independent indexes (one per field) mapping normalized field values to the contacts that share them
3. **Match Finding**: For each source contact, query all field indexes to find candidate matches and count the number of matching fields
4. **Accuracy Computation**: Assign accuracy levels based on the match count
5. **Sorting**: Order results by source ID, match ID, and accuracy priority

### Field Indexes

The system maintains indexes for the following fields:
- First name
- Last name
- Email
- ZIP code
- Address

Each index maps normalized field values to lists of contacts that share those values, enabling efficient candidate lookup.

### String Matching

String values are normalized using the following rules:
- Null values are treated as empty strings
- Whitespace is trimmed from both ends
- Characters are converted to lowercase
- Empty strings are excluded from indexing

## Examples

### High Accuracy Match

When two contacts have matching first name, last name, email, ZIP code, and address:

| contactid | name | name1 | email | postalZip | address |
|-----------|------|-------|-------|-----------|---------|
| 30 | Cyrus | Solis | erat.in.consectetuer@aol.ca | 71669 | "P.O. Box 447, 4294 Ante Ave" |
| 303030 | cyrus | SOLIS | ERAT.in.consectetuer@aol.ca | 71669 | "P.O. Box 447, 4294 Ante Ave" |

**Output:**

| ContactID Source | ContactID Match | Accuracy |
|-----------------|-----------------|----------|
| 30 | 303030 | High |
| 303030 | 30 | High |

---

### Medium Accuracy Match

When two contacts share exactly three matching fields:

| contactid | name | name1 | email | postalZip | address |
|-----------|------|-------|-------|-----------|---------|
| 359 | Orli | Stafford | lorem.ipsum@aol.net | 68640 | "P.O. Box 658, 9382 A, Avenue" |
| 359359 | OTHER | other | LOREM.IPSUM@AOL.NET | 68640 | "P.O. Box 658, 9382 A, Avenue" |

**Output:**

| ContactID Source | ContactID Match | Accuracy |
|-----------------|-----------------|----------|
| 359 | 359359 | Medium |
| 359359 | 359 | Medium |

---

### Low Accuracy Match

When two contacts share only one or two matching fields:

| contactid | name | name1 | email | postalZip | address |
|-----------|------|-------|-------|-----------|---------|
| 60 | Gage | Glover | magna.a.neque@outlook.couk | 86876 | 712-8209 Sagittis St. |
| 606060 | Gage | Other | other@other.net | 123456 | Other St. |

**Output:**

| ContactID Source | ContactID Match | Accuracy |
|-----------------|-----------------|----------|
| 60 | 606060 | Low |
| 606060 | 60 | Low |

---

### Descending Order with Multiple Accuracy Levels

When multiple contacts match a single source with varying accuracy:

| contactid | name | name1 | email | postalZip | address |
|-----------|------|-------|-------|-----------|---------|
| 60 | Gage | Glover | magna.a.neque@outlook.couk | 86876 | 712-8209 Sagittis St. |
| 606060 | Gage | GLOVER | other@other.net | 86876 | 712-8209 Sagittis St. |
| 606061 | Gage | GLOVER | other@other.net | 86876 | Other St. |
| 606062 | gage | floverr | other@other.net | 123456 | Other St. |

**Output:**

| ContactID Source | ContactID Match | Accuracy |
|-----------------|-----------------|----------|
| 60 | 606060 | High |
| 60 | 606061 | Medium |
| 60 | 606062 | Low |
| 606060 | 60 | High |
| 606060 | 606061 | High |
| 606060 | 606062 | Low |
| 606061 | 606060 | High |
| 606061 | 60 | Medium |
| 606061 | 606062 | Medium |
| 606062 | 606061 | Medium |
| 606062 | 60 | Low |
| 606062 | 606060 | Low |

---

### Mixed Matching with Partial Data

When contacts have partial or missing field values, matching is based on available fields:

| contactid | name | name1 | email | postalZip | address |
|-----------|------|-------|-------|-----------|---------|
| 900 | B | G | | | Maecenas street. |
| 925 | B | G | | | "Montes, street." |
| 940 | I | W | | | 6944 Ipsum. Avenue |
| 808080 | A | A | a@a.net | 654321 | A street |
| 900900 | B | G | | | Maecenas street. |

**Output:**

| ContactID Source | ContactID Match | Accuracy |
|-----------------|-----------------|----------|
| 900 | 900900 | High |
| 900 | 925 | Medium |
| 900 | 940 | Low |
| 925 | 900 | Medium |
| 925 | 900900 | Medium |
| 925 | 940 | Low |
| 940 | 900 | Low |
| 940 | 925 | Low |
| 940 | 900900 | Low |
| 900900 | 900 | High |
| 900900 | 925 | Medium |
| 900900 | 940 | Low |

---

### Empty Input

When the input file contains no contact records:

**Output:** Empty file with only headers

| ContactID Source | ContactID Match | Accuracy |
|-----------------|-----------------|----------|
| (no records) | | |

---

### Empty Contact ID

When contact records have empty or null identifiers:

| contactid | name | name1 | email | postalZip | address |
|-----------|------|-------|-------|-----------|---------|
| | | | | | |
| | | | | | |
| | | | | | |

**Output:** Empty file with only headers

| ContactID Source | ContactID Match | Accuracy |
|-----------------|-----------------|----------|
| (no records) | | |

---

### Empty Strings Treated as Null

When contact fields contain only empty strings:

| contactid | name | name1 | email | postalZip | address |
|-----------|------|-------|-------|-----------|---------|
| 1 | | | | | |
| 2 | | | | | |

**Output:** Empty file with only headers

| ContactID Source | ContactID Match | Accuracy |
|-----------------|-----------------|----------|
| (no records) | | |

---

### Single Contact

When the input contains only one contact record:

| contactid | name | name1 | email | postalZip | address |
|-----------|------|-------|-------|-----------|---------|
| 30 | Cyrus | Solis | erat.in.consectetuer@aol.ca | 71669 | "P.O. Box 447, 4294 Ante Ave" |

**Output:** Empty file with only headers

| ContactID Source | ContactID Match | Accuracy |
|-----------------|-----------------|----------|
| (no records) | | |

---

### Duplicate Contact IDs

When multiple contacts share the same ID, all are excluded:

| contactid | name | name1 | email | postalZip | address |
|-----------|------|-------|-------|-----------|---------|
| 30 | Cyrus | Solis | erat.in.consectetuer@aol.ca | 71669 | "P.O. Box 447, 4294 Ante Ave" |
| 30 | Cyrus | Solis | erat.in.consectetuer@aol.ca | 71669 | "P.O. Box 447, 4294 Ante Ave" |
| 30 | Cyrus | Solis | erat.in.consectetuer@aol.ca | 71669 | "P.O. Box 447, 4294 Ante Ave" |
| 60 | Gage | Glover | magna.a.neque@outlook.couk | 86876 | 712-8209 Sagittis St. |
| 60 | Gage | Glover | magna.a.neque@outlook.couk | 86876 | 712-8209 Sagittis St. |

**Output:** Empty file with only headers

| ContactID Source | ContactID Match | Accuracy |
|-----------------|-----------------|----------|
| (no records) | | |

---

## Author

- [Gabo](https://www.linkedin.com/in/gabogil/)

## GitHub

- [gabo-gil-playground](https://github.com/gabo-gil-playground)

## Site

- [Gabo](https://gabogil.com/)
