# BHCE Weaver

Reads [Sliver C2](https://github.com/BishopFox/sliver) console logs containing [`sa-ldapsearch`](https://github.com/trustedsec/CS-Situational-Awareness-BOF) BOF output, merges the LDAP results, and writes [BloodHound Community Edition](https://github.com/SpecterOps/BloodHound) v6 JSON.

`sa-ldapsearch` runs raw LDAP queries against Active Directory from a C2 implant. Each query returns whatever attributes were requested, so the same AD object can appear across many query results with different attributes each time. One query might pull `samAccountType` and `memberOf` for users; a later query might pull `nTSecurityDescriptor` for those same users. On large domains, even a single attribute like `nTSecurityDescriptor` might be sharded across dozens of queries to keep response sizes manageable.

Weaver handles all of this. It reads the Sliver console logs directly, merges every entry that shares a distinguished name regardless of how many queries produced it, classifies objects (including ADCS types that require `objectClass`), parses `nTSecurityDescriptor` binary blobs into ACL edges, resolves relationships (group membership, OU containment, GPO links, domain trusts, ADCS chains, delegation), and writes per-type BH-CE v6 JSON with correct Go-compatible typed properties.

Without Weaver, getting `sa-ldapsearch` output into BloodHound CE requires a chain of log-splitting scripts, a DN merge script, an external ingest tool, backfill scripts for missing attributes, monkey-patches for crash bugs, and post-processing fixers for JSON type mismatches. Weaver replaces that entire chain with a single command.

## Requirements

- Java 21+
- Sliver C2 console log files (NDJSON format) containing `sa-ldapsearch` output
- (Optional) BloodHound CE v6 instance for direct upload

## Build

```bash
mvn clean package
```

Produces two JARs in `target/`:

- `bhce-weaver-0.1.1.jar` -- fat JAR
- `bhce-weaver-0.1.1-obfuscated.jar` -- ProGuard-obfuscated (default for ops)

Run tests:

```bash
mvn test              # unit tests
mvn verify            # unit + JAR integration tests
```

## Usage

### Convert

```bash
java -jar bhce-weaver-0.1.1-obfuscated.jar convert \
  -i /path/to/sliver-logs/ \
  -o ./output
```

| Flag | Description |
|------|-------------|
| `-i, --input` | Sliver JSON log file or directory (required) |
| `-o, --output` | Output directory (default: `./output`) |
| `-p, --properties-level` | Detail level: Standard, Member, All (default: All) |
| `--since` | Only process entries on or after this date (YYYY-MM-DD) |
| `--domain` | Only process entries for this domain |
| `--zip` | Compress output into a single zip file |
| `--debug` | Enable debug logging |

### Convert and Upload

```bash
java -jar bhce-weaver-0.1.1-obfuscated.jar convert \
  -i /path/to/sliver-logs/ \
  -o ./output \
  --bh-url http://bloodhound:8080 \
  --bh-token-id YOUR_TOKEN_ID \
  --bh-token-key YOUR_TOKEN_KEY
```

### Upload Only

Upload previously generated JSON files to a BloodHound CE instance:

```bash
java -jar bhce-weaver-0.1.1-obfuscated.jar upload \
  -o ./output \
  --bh-url http://bloodhound:8080 \
  --bh-token-id YOUR_TOKEN_ID \
  --bh-token-key YOUR_TOKEN_KEY
```

## Pipeline

```
Sliver NDJSON console logs
  |  [1] Log Parse    -- extract sa-ldapsearch BOF output from Sliver console logs
  |  [2] LDAP Parse   -- split hyphen-delimited result blocks into key:value entries
  |  [3] DN Merge     -- group entries by distinguished name, union attributes across passes
  |  [4] Classify     -- route merged objects to AD types (samAccountType + objectClass fallback)
  |  [5] Resolve      -- containment, membership, GPO links, trusts, ACLs, ADCS, delegation
  |  [6] Validate     -- reject null properties and missing identifiers
  |  [7] JSON Write   -- per-type BH-CE v6 JSON files
  v  [8] Upload       -- (optional) HMAC-signed upload to BH-CE file-upload API
BH-CE v6 JSON
```

## Supported AD Object Types

Users, Computers, Groups, Domains, OUs, GPOs, Containers, Certificate Templates, Enterprise CAs, Root CAs, AIA CAs, NTAuth Stores, Issuance Policies, Domain Trusts.

## Output

BH-CE v6 JSON with typed properties using Go-compatible defaults (`bool=false`, `int=0`, `string=""`, `list=[]`). Each type produces a timestamped file: `users_YYYYMMDD_HHMMSS.json`, `computers_YYYYMMDD_HHMMSS.json`, etc.

## Documentation

- [Manual](docs/MANUAL.md) -- installation, CLI reference, pipeline stages, output format, troubleshooting
- [Collecting Data](docs/COLLECTING.md) -- sa-ldapsearch query guide
- [BH-CE Ingestion](docs/BLOODHOUND-INGESTION.md) -- import and verification with Cypher queries
- [Security Descriptors](docs/SECURITY-DESCRIPTORS.md) -- NT SD and ACE parsing reference

## Security

- All parsed LDAP attribute values treated as untrusted (target-controlled data)
- Parsers never throw on malformed input -- degrade to partial output
- Path traversal prevention on output directories
- BH-CE API token keys never logged
- ProGuard obfuscation enabled by default

## Dependencies

- [picocli](https://picocli.info/) -- CLI parsing
- [Jackson](https://github.com/FasterXML/jackson) -- JSON serialization
- [Bouncy Castle](https://www.bouncycastle.org/) -- ASN.1 / X.509 certificate parsing
- [JUnit 5](https://junit.org/junit5/) -- testing

## License

BSD 3-Clause. See [LICENSE](LICENSE).
