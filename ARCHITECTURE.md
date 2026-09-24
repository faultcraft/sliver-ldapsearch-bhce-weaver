# BHCE Weaver Architecture Specification

**Version 0.1.1** | Last updated: 2026-09-24

BHCE Weaver is a Java 21 pipeline tool that converts multi-pass Sliver C2 sa-ldapsearch BOF output into BloodHound Community Edition v6 JSON.

Companion documents: `CODING-STANDARD.md` (mechanical rules), `DESIGN-GUIDE.md` (judgment and cohesion), `TESTING-STANDARD.md` (test philosophy and tiers).

---

## 1. Mission and Constraints

BHCE Weaver solves the **multi-pass partial object problem**: Sliver's `sa-ldapsearch` collects LDAP data one query at a time across 10+ passes. Each pass returns partial objects. Existing tools expect complete objects in a single entry and fail on partial data (crashes, null values rejected by BH-CE's API, unclassified objects, missing relationships).

**Core invariants:**

- **Merge before classify.** Partial objects from multiple passes are merged by distinguished name before any classification or relationship resolution occurs.
- **No data loss.** Attribute union semantics -- merging never discards attributes, only adds.
- **BH-CE v6 output contract.** Output JSON must pass BH-CE's Go API validation: no null values in typed properties, every object has an ObjectIdentifier, all relationship targets are valid SIDs/GUIDs.
- **Parsers never throw.** Malformed input degrades to partial output with warnings, never crashes.
- **Clean-room implementation.** All parsing and conversion logic written from protocol specs and format documentation.

---

## 2. High-Level Architecture

Single-process Java 21 batch pipeline. No persistent state, no database. Input flows through a linear pipeline of parsing, merging, classification, relationship resolution, and output stages.

**Package map:**

```
com.faultcraft.weaver
+-- cli/            -- picocli subcommands (convert, upload)
+-- sliver/         -- Sliver NDJSON console log parser (state machine)
+-- ldap/           -- LDAP text block parser (key:value extraction)
+-- merge/          -- DN-based entry merger (case-insensitive, attribute union)
+-- ad/             -- Active Directory object processing
|   +-- model/      -- AD object types (User, Computer, Group, Domain, OU, GPO, etc.)
|   +-- acl/        -- Security descriptor and ACE parsing
|   +-- classify/   -- samAccountType/objectClass-based classification
|   +-- resolve/    -- Relationship resolution (membership, containment, GPOs, trusts, ADCS)
+-- bloodhound/     -- BloodHound CE integration
|   +-- writer/     -- BH-CE v6 JSON output (per-type files, validation)
|   +-- upload/     -- BH-CE HMAC-SHA256 API upload client
+-- util/           -- SID/GUID conversion, timestamp parsing, well-known SID registry
```

---

## 3. Pipeline Data Flow

```
Sliver NDJSON console logs (-i)
  |
  v
[1. Sliver Log Parser] -- NDJSON state machine, extracts sa-ldapsearch output blocks
  |                       Filters by --since timestamp
  v
[2. LDAP Block Parser] -- Splits on separator lines, parses key:value pairs into Map<String,List<String>>
  |
  v
[3. DN Merger]          -- Groups entries by distinguishedName (case-insensitive)
  |                       Unions attributes across passes, filters by --domain
  v
[4. Object Classifier]  -- Routes by samAccountType / objectClass into typed model objects
  |                       (User, Computer, Group, Domain, OU, GPO, Container,
  |                        CertTemplate, EnterpriseCA, RootCA, AIACA, NTAuthStore, IssuancePolicy)
  v
[5. Relationship Resolver] -- ACL parsing (ntSecurityDescriptor -> typed ACE rights)
  |                           Group membership, OU containment, GPO links
  |                           Domain trusts, ADCS relationships, delegation
  v
[6. Output Validator]   -- Ensures no null values, ObjectIdentifier present on all objects
  |
  v
[7. BH-CE JSON Writer]  -- Writes per-type JSON files (users.json, computers.json, etc.)
  |                        BH-CE v6 format with meta header
  v
[8. BH-CE Uploader]     -- Optional: HMAC-SHA256 signed upload to BH-CE file-upload API
                           (--bh-url, --bh-token-id, --bh-token-key)
```

---

## 4. Sliver Log Parser

Parses NDJSON (newline-delimited JSON) Sliver console logs. Each line is a JSON object with fields including `Stdout`, `Stderr`, timestamp, command context.

**State machine:**
- Tracks pending sa-ldapsearch commands via a deque
- Extracts stdout blocks that contain LDAP output
- Filters by `--since` timestamp to support incremental processing
- Handles interleaved commands from multiple operators

**Output:** List of raw LDAP text blocks (strings).

---

## 5. LDAP Block Parser

Splits raw LDAP text on separator lines (`--------------------`), strips status lines (`[*]`, `[+]`, `[-]`), and parses `key: value` pairs.

**Handles:**
- Multi-valued attributes (same key appears multiple times)
- Base64-encoded binary values (prefixed with `::`)
- Continuation lines (leading whitespace)
- Empty blocks and malformed entries

**Output:** `List<Map<String, List<String>>>` -- each map is one LDAP entry's attributes.

---

## 6. DN Merger

Groups parsed entries by `distinguishedName` (case-insensitive). For each DN, unions all attributes across passes. Partial objects from 10+ LDAP queries are assembled into complete objects.

**Semantics:**
- Case-insensitive DN matching
- Attribute union: if pass 1 has `sAMAccountName` and pass 3 has `memberOf`, the merged object has both
- Multi-valued attributes: values are unioned (deduplicated)
- Optional `--domain` filter: only emit objects whose DN ends with the specified domain suffix

**Output:** `Map<String, Map<String, List<String>>>` -- DN to merged attributes.

---

## 7. AD Object Models

Records for each AD object type. Each model:
- Constructs from raw `Map<String, List<String>>` attributes
- Provides typed property accessors with Go-compatible defaults (missing bool -> false, missing int -> 0, missing string -> "", missing list -> [])
- Serializes to BH-CE v6 JSON format via `toJson()` method

**Object types:**
- `BloodHoundUser` -- domain user accounts
- `BloodHoundComputer` -- domain computer accounts
- `BloodHoundGroup` -- security and distribution groups
- `BloodHoundDomain` -- domain objects
- `BloodHoundOU` -- organizational units
- `BloodHoundGPO` -- group policy objects
- `BloodHoundContainer` -- AD containers
- `BloodHoundCertTemplate` -- certificate templates
- `BloodHoundEnterpriseCA` -- enterprise certificate authorities
- `BloodHoundRootCA` -- root certificate authorities
- `BloodHoundAIACA` -- AIA certificate authorities
- `BloodHoundNTAuthStore` -- NTAuth certificate stores
- `BloodHoundIssuancePolicy` -- certificate issuance policies

---

## 8. ACL and Security Descriptor Parsing

Parses raw `ntSecurityDescriptor` binary blobs into typed ACE (Access Control Entry) records. Implements the NT security descriptor format from the MS-DTYP specification.

**Supported ACE types:**
- ACCESS_ALLOWED_ACE, ACCESS_ALLOWED_OBJECT_ACE
- ACCESS_DENIED_ACE, ACCESS_DENIED_OBJECT_ACE

**Resolved rights:**
- GenericAll, GenericWrite, WriteDacl, WriteOwner
- ExtendedRight (ForceChangePassword, DS-Replication-Get-Changes, etc.)
- WriteProperty (specific attribute writes)
- AddMember, AddSelf

**Well-known SID registry:** Maps well-known SIDs (S-1-5-32-544, etc.) to names.

---

## 9. Relationship Resolution

After classification, resolves relationships between objects:

- **Group membership:** `memberOf` attribute -> `MemberOf` edges
- **OU containment:** DN hierarchy -> `Contains` edges
- **GPO links:** `gPLink` attribute -> `GPLink` edges
- **Domain trusts:** `trustDirection`, `trustType`, `trustAttributes` -> trust edges
- **ADCS relationships:** template -> CA -> NTAuth -> root CA chains
- **Delegation:** constrained/unconstrained delegation flags
- **Admin relationships:** AdminTo, CanRDP, CanPSRemote, ExecuteDCOM from local group membership

---

## 10. BH-CE JSON Output

Writes per-type JSON files in BH-CE v6 format:

```json
{
    "meta": {
        "methods": 0,
        "type": "users",
        "count": 42,
        "version": 6
    },
    "data": [ ... ]
}
```

**Validation before write:**
- No null values in typed properties
- Every object has a non-empty `ObjectIdentifier`
- All relationship target identifiers are non-empty

---

## 11. BH-CE Upload Client

Optional HMAC-SHA256 signed upload to the BH-CE file-upload API.

**Authentication flow:**
1. Create upload job via POST
2. For each JSON file: compute HMAC-SHA256 digest with chained signing, upload via PUT
3. Mark upload complete via POST

---

## 12. CLI Interface

Picocli-based CLI:

```
weaver convert -i <sliver-log.json> -o <output-dir> [options]
weaver upload -o <output-dir> --bh-url <url> --bh-token-id <id> --bh-token-key <key>
```

**Flags:**
- `-i`, `--input` -- Sliver NDJSON console log file (required for convert)
- `-o`, `--output` -- Output directory for BH-CE JSON files (default: `./output`)
- `-p`, `--parser` -- Parser type (default: `sliver`)
- `--since` -- Only process log entries after this timestamp
- `--domain` -- Filter objects to this domain suffix
- `--debug` -- Verbose logging
- `--bh-url` -- BloodHound CE API URL (for upload)
- `--bh-token-id` -- BH-CE API token ID (for upload)
- `--bh-token-key` -- BH-CE API token key (for upload)

---

## 13. Build System

- **Maven** with shade plugin for single fat JAR.
- **Java 21** (LTS), records, sealed interfaces, pattern matching, text blocks.
- **ProGuard** obfuscation runs by default in the package phase.
- **Dependencies:** picocli (CLI), Jackson (JSON), Bouncy Castle (ASN.1/X.509), JUnit 5.
- Reproducible builds via `project.build.outputTimestamp`.

---

## 14. Design Principles (Summary)

1. **Pipeline, not a service.** Batch processing from input files to output files. No persistent state.
2. **Merge first.** All partial objects assembled before any classification or relationship work.
3. **Immutability.** Records for all value types. Immutable collections on returns.
4. **Parsers never throw.** Degrade to partial output on malformed input.
5. **No null in output.** Go-compatible typed defaults for all missing properties.
6. **Clean room.** All functionality implemented from protocol specs and format documentation.

---

## 15. File and Directory Layout

```
bhce-weaver/
+-- pom.xml
+-- src/main/java/com/faultcraft/weaver/
|   +-- Main.java                  -- entry point, picocli dispatch
|   +-- cli/                       -- picocli subcommands
|   +-- sliver/                    -- Sliver log parser
|   +-- ldap/                      -- LDAP text block parser
|   +-- merge/                     -- DN-based merger
|   +-- ad/
|   |   +-- model/                 -- AD object type records
|   |   +-- acl/                   -- Security descriptor parsing
|   |   +-- classify/              -- Object classification
|   |   +-- resolve/               -- Relationship resolution
|   +-- bloodhound/
|   |   +-- writer/                -- BH-CE v6 JSON writer
|   |   +-- upload/                -- BH-CE API client
|   +-- util/                      -- SID/GUID/timestamp utilities
+-- src/test/java/                 -- mirrors src/main/java
+-- src/test/resources/fixtures/   -- recorded Sliver logs and LDAP output
```
