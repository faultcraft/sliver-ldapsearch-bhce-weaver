# BHCE Weaver Manual v0.1.0

Sogon Security

---

## 1. Overview

BHCE Weaver is a Java 21 command-line tool that converts multi-pass Sliver C2 `sa-ldapsearch` BOF output into BloodHound Community Edition v6 JSON. It solves the multi-pass partial object problem: Sliver collects LDAP data one query at a time across many passes, producing partial objects that existing tools cannot handle. Weaver merges these partial objects by distinguished name, classifies them into AD object types, resolves relationships, and writes BH-CE v6 compatible JSON.

### Key characteristics

- **Merge-before-classify.** Partial objects from multiple sa-ldapsearch passes are merged by DN before any classification or relationship resolution.
- **No data loss.** Attribute union semantics -- merging never discards attributes, only adds.
- **BH-CE v6 output contract.** Output passes BH-CE's API validation: no null values in typed properties, every object has an ObjectIdentifier, all relationship targets are valid SIDs/GUIDs.
- **Parsers never throw.** Malformed input degrades to partial output with warnings.
- **ProGuard obfuscated.** Default build produces an obfuscated fat JAR.

---

## 2. Installation

### Requirements

- **Java 21** or later (LTS).
- **Maven 3.9+** (for building from source).

### Build from source

```bash
mvn clean package
```

This produces two JARs in `target/`:

- `bhce-weaver-0.1.0.jar` -- standard fat JAR (~11 MB)
- `bhce-weaver-0.1.0-obfuscated.jar` -- ProGuard-obfuscated JAR (~8.6 MB, recommended)

Run:

```bash
java -jar target/bhce-weaver-0.1.0-obfuscated.jar --help
```

### Build without obfuscation

To skip ProGuard during development:

```bash
mvn clean package -P '!default' -P dev
```

Or remove the ProGuard plugin execution from `pom.xml`.

---

## 3. Quick Start

### Basic conversion

```bash
java -jar bhce-weaver-0.1.0-obfuscated.jar convert \
  -i /path/to/sliver-logs/ \
  -o ./output
```

### Convert and upload to BH-CE

```bash
java -jar bhce-weaver-0.1.0-obfuscated.jar convert \
  -i /path/to/sliver-logs/ \
  -o ./output \
  --bh-url http://bloodhound:8080 \
  --bh-token-id YOUR_TOKEN_ID \
  --bh-token-key YOUR_TOKEN_KEY
```

### Upload previously generated files

```bash
java -jar bhce-weaver-0.1.0-obfuscated.jar upload \
  -o ./output \
  --bh-url http://bloodhound:8080 \
  --bh-token-id YOUR_TOKEN_ID \
  --bh-token-key YOUR_TOKEN_KEY
```

---

## 4. CLI Reference

### Global options

| Flag | Description |
|------|-------------|
| `--help` | Show usage help |
| `--version` | Print version and exit |

### `convert` subcommand

Convert Sliver sa-ldapsearch output to BH-CE v6 JSON.

| Flag | Description | Default |
|------|-------------|---------|
| `-i, --input` | Sliver JSON log file or directory (required) | -- |
| `-o, --output` | Output directory | `./output` |
| `-p, --properties-level` | Detail level: `Standard`, `Member`, `All` | `All` |
| `--since` | Only process entries on or after this date (`YYYY-MM-DD`) | -- |
| `--domain` | Only process entries for this domain | -- |
| `--zip` | Compress output into a single zip file | false |
| `--debug` | Enable debug logging | false |
| `--bh-url` | BH-CE server URL (enables upload after conversion) | -- |
| `--bh-token-id` | BH-CE API token ID | -- |
| `--bh-token-key` | BH-CE API token key | -- |

### `upload` subcommand

Upload previously generated JSON files to BH-CE.

| Flag | Description | Default |
|------|-------------|---------|
| `-o, --output` | Directory containing JSON/ZIP files | `./output` |
| `--bh-url` | BH-CE server URL (required) | -- |
| `--bh-token-id` | BH-CE API token ID (required) | -- |
| `--bh-token-key` | BH-CE API token key (required) | -- |
| `--debug` | Enable debug logging | false |

---

## 5. Pipeline Stages

The conversion pipeline processes data in 8 stages. Each stage is a pure function that takes the output of the previous stage.

### 5.1 Sliver Log Parse

Reads Sliver C2 console log files in NDJSON format. Each line is a JSON object with `level`, `type`, `msg`, and `time` fields. The parser uses a state machine to track command boundaries:

1. A line with `"type":"command"` and `msg` containing `sa-ldapsearch` marks a new command.
2. A line with `"msg":"Successfully executed sa-ldapsearch"` confirms the command ran.
3. A line with `"msg":"Got output:\n..."` contains the LDAP results.

The parser resolves input paths: a file is read directly, a directory is searched recursively for `.json` files.

### 5.2 LDAP Block Parse

Splits the raw text output into individual LDAP entries. Each entry is delimited by a line of 20 hyphens (`--------------------`). Within each block, lines are parsed as `key: value` pairs. Multi-valued attributes (same key appearing multiple times) are collected into lists.

### 5.3 DN Merge

Groups entries by distinguished name (case-insensitive) and merges their attributes. When the same DN appears across multiple sa-ldapsearch passes, the merger performs attribute union: every attribute from every pass is kept, with duplicate values deduplicated. This ensures that partial objects from different queries combine into complete objects.

Optional `--domain` filtering happens at this stage, discarding entries whose DN does not end with the specified domain suffix.

### 5.4 Classify

Routes each merged entry to an AD object type. Classification uses a two-tier strategy:

1. **samAccountType** (primary): The `samAccountType` attribute maps directly to User (805306368), Computer (805306369), Group (268435456/268435457), or Domain (805306370).
2. **objectClass** (fallback): When samAccountType is missing or doesn't match, the `objectClass` attribute is checked for structural types (organizationalUnit, groupPolicyContainer, container) and ADCS types (pKICertificateTemplate, pKIEnrollmentService, certificationAuthority).

The classifier produces typed model objects: `BloodHoundUser`, `BloodHoundComputer`, `BloodHoundGroup`, `BloodHoundDomain`, `BloodHoundOU`, `BloodHoundGPO`, `BloodHoundContainer`, `BloodHoundCertTemplate`, `BloodHoundEnterpriseCA`, `BloodHoundRootCA`, `BloodHoundAIACA`, `BloodHoundNTAuthStore`, `BloodHoundIssuancePolicy`.

### 5.5 Relationship Resolve

Resolves inter-object relationships using DN, SID, and GUID indexes built from the classified objects. This stage runs seven sub-resolvers in sequence:

1. **ACL Resolver**: Parses NT security descriptors (binary `nTSecurityDescriptor` attribute), extracts DACL ACE entries, and maps access masks and object types to BH-CE edge types (GenericAll, WriteDacl, WriteOwner, AddMember, ForceChangePassword, etc.). Owner SID is extracted and added as an "Owns" relationship.

2. **Containment Resolver**: Builds parent-child relationships from the DN hierarchy. Each object's parent is found by stripping the first RDN from its DN. Produces bidirectional Contains/ContainedBy edges.

3. **Group Membership Resolver**: Resolves group membership from three sources: the `memberOf` attribute on users/computers/groups, the `member` attribute on group objects, and `primaryGroupId` resolution (maps to the domain's SID + RID).

4. **GPO Link Resolver**: Parses the `gpLink` attribute on domains and OUs. Each link entry contains a GPO DN and an options bitmask (bit 1 = enforced). Produces GPO link entries with GUID and IsEnforced flag.

5. **Trust Resolver**: Matches `trustedDomain` objects to their parent domain by comparing the trust's local domain DN. Sets trust direction, type, and transitivity from trust attributes.

6. **ADCS Resolver**: Resolves ADCS-specific relationships: published certificate templates by name matching to Enterprise CA objects, CA hosting computer by `dNSHostName` resolution, and issuance policy group links by DN lookup.

7. **Delegation Resolver**: Extracts hostnames from constrained delegation SPNs (`msDS-AllowedToDelegateTo`), resolves them to computer objects by `dNSHostName` matching, and creates AllowedToDelegate edges.

See [SECURITY-DESCRIPTORS.md](SECURITY-DESCRIPTORS.md) for details on NT security descriptor parsing and ACE resolution.

### 5.6 Validate

Checks every object for null properties and missing ObjectIdentifier values. Objects that fail validation are logged as warnings but still included in output (BH-CE may reject them, but dropping data silently would violate the no-data-loss invariant).

### 5.7 JSON Write

Writes per-type JSON files with BH-CE v6 meta headers. Each file contains:

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

Type version numbers: domains and GPOs use version 5, all other types use version 6.

File naming: `{type}_{YYYYMMDD}_{HHMMSS}.json` (e.g., `users_20260924_153000.json`).

With `--zip`, all JSON files are compressed into a single zip archive.

### 5.8 Upload

Uploads generated files to a BH-CE server via its file-upload API. Authentication uses HMAC-SHA256 with a three-stage chained signing scheme:

1. Sign the date string with the token key.
2. Sign the operation string ("bhesignature") with the date signature.
3. Sign the request digest with the operation signature.

The upload flow: create an upload job via POST, upload each file via POST, then signal completion via POST end.

---

## 6. Supported AD Object Types

| Type | BH-CE Kind | Source |
|------|-----------|--------|
| User | `Base` | samAccountType 805306368 |
| Computer | `Base` | samAccountType 805306369 |
| Group | `Base` | samAccountType 268435456/268435457 |
| Domain | `Base` | samAccountType 805306370 or objectClass domainDNS |
| OU | `Base` | objectClass organizationalUnit |
| GPO | `Base` | objectClass groupPolicyContainer |
| Container | `Base` | objectClass container |
| Certificate Template | `Base` | objectClass pKICertificateTemplate |
| Enterprise CA | `Base` | objectClass pKIEnrollmentService |
| Root CA | `Base` | objectClass certificationAuthority (CN=Root) |
| AIA CA | `Base` | objectClass certificationAuthority (CN=AIA) |
| NTAuth Store | `Base` | objectClass certificationAuthority (CN=NTAuth) |
| Issuance Policy | `Base` | objectClass msPKI-Enterprise-Oid |
| Domain Trust | standalone | objectClass trustedDomain |

---

## 7. Output Format

### TypedProperties

All object properties use Go-compatible typed defaults to satisfy BH-CE's API validation:

| Go type | Default |
|---------|---------|
| `bool` | `false` |
| `int` | `0` |
| `string` | `""` |
| `[]string` | `[]` |

This ensures no `null` values appear in the output JSON, which would cause BH-CE import failures.

### Relationship edges

Relationships are encoded in each object's `Aces` list (for ACL-derived edges) and type-specific fields:

- **Users/Computers**: `PrimaryGroupSID`, `SPNTargets`, `AllowedToDelegate`, `HasSIDHistory`
- **Groups**: `Members` list
- **Domains**: `Trusts`, `Links` (GPO), `ChildObjects`
- **OUs**: `Links` (GPO), `ChildObjects`
- **Containers**: `ChildObjects`
- **Enterprise CAs**: `EnabledCertTemplates`, `HostingComputer`

---

## 8. Collecting Data

See [COLLECTING.md](COLLECTING.md) for a complete guide to collecting sa-ldapsearch data from Sliver C2.

---

## 9. BloodHound Ingestion

See [BLOODHOUND-INGESTION.md](BLOODHOUND-INGESTION.md) for a guide to importing Weaver output into BH-CE and verifying the data.

---

## 10. Security Considerations

- **Token keys are never logged.** The `--bh-token-key` value is used only for HMAC signing and never appears in log output.
- **Path traversal prevention.** Output directory validation rejects paths containing `..` segments.
- **ProGuard obfuscation.** The default build obfuscates all internal classes. Only the CLI entry points, model objects, and third-party libraries retain their original names.
- **No network access by default.** The tool only contacts a BH-CE server when `--bh-url` is explicitly provided.

---

## 11. Troubleshooting

### No objects classified

If Weaver reports 0 objects after classification:

1. Verify the input files are Sliver C2 console logs in NDJSON format (one JSON object per line).
2. Check that the logs contain `sa-ldapsearch` command output (look for `"type":"command"` lines mentioning `sa-ldapsearch`).
3. Ensure the sa-ldapsearch output includes `samAccountType` or `objectClass` attributes. Without these, objects cannot be classified.

### BH-CE rejects uploaded data

1. Run `convert` without `--bh-url` first and inspect the JSON output.
2. Look for objects with empty `ObjectIdentifier` -- these lack an `objectSid` or `objectGUID`.
3. Ensure all sa-ldapsearch passes are included. Missing passes may leave objects without the attributes needed for relationship resolution.

### Missing relationships

Relationships require data from multiple sa-ldapsearch queries:

- **Group membership**: Requires either `memberOf` on the member or `member` on the group.
- **ACLs**: Requires `nTSecurityDescriptor` in binary format (base64-encoded).
- **Containment**: Derived from DN hierarchy, so all objects must have a `distinguishedName`.
- **GPO links**: Requires `gpLink` on domain and OU objects.
- **ADCS**: Requires certificate template, Enterprise CA, and related objects from PKI containers.

### Debug logging

Add `--debug` to any command to enable verbose logging:

```bash
java -jar bhce-weaver-0.1.0-obfuscated.jar convert \
  -i /path/to/logs/ -o ./output --debug
```

---

## 12. Development

### Project structure

```
src/main/java/com/faultcraft/weaver/
  cli/           ConvertCommand, UploadCommand, picocli wiring
  parse/         SliverLogParser, LdapBlockParser
  merge/         DnMerger
  ad/
    classify/    ObjectClassifier
    model/       BloodHound* model classes, TypedProperties
    resolve/     RelationshipResolver + sub-resolvers
    security/    SecurityDescriptorParser, AceResolver, AccessMask
  bloodhound/
    writer/      BhceJsonWriter
    upload/      BhceUploadClient
  util/          SidParser, GuidParser, TimestampParser, WellKnownSids
```

### Running tests

```bash
mvn test
```

244 tests covering all pipeline stages, model serialization, binary parsing, and end-to-end conversion.

### Coding standards

See `CODING-STANDARD.md` for mechanical rules (60-line method cap, 600-line class cap, immutable returns) and `TESTING-STANDARD.md` for test philosophy.

---

## 13. Companion Documents

| Document | Purpose |
|----------|---------|
| [COLLECTING.md](COLLECTING.md) | Sliver sa-ldapsearch data collection guide |
| [BLOODHOUND-INGESTION.md](BLOODHOUND-INGESTION.md) | BH-CE import and verification guide |
| [SECURITY-DESCRIPTORS.md](SECURITY-DESCRIPTORS.md) | NT security descriptor and ACE parsing reference |
| `ARCHITECTURE.md` | Internal architecture specification |
| `CODING-STANDARD.md` | Coding rules and conventions |
| `TESTING-STANDARD.md` | Test tiers and philosophy |
| `DESIGN-GUIDE.md` | Design judgment and cohesion |
