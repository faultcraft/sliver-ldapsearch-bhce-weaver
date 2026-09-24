# BloodHound CE Ingestion Guide

This guide covers importing BHCE Weaver output into BloodHound Community Edition v6 and verifying the data.

---

## 1. Output Files

Weaver produces per-type JSON files in the output directory:

```
output/
  users_20260924_153000.json
  computers_20260924_153000.json
  groups_20260924_153000.json
  domains_20260924_153000.json
  ous_20260924_153000.json
  gpos_20260924_153000.json
  containers_20260924_153000.json
  cert_templates_20260924_153000.json
  enterprise_cas_20260924_153000.json
  root_cas_20260924_153000.json
  aia_cas_20260924_153000.json
  ntauth_stores_20260924_153000.json
  issuance_policies_20260924_153000.json
```

With `--zip`, these are compressed into a single archive: `weaver_20260924_153000.zip`.

---

## 2. Import Methods

### Method 1: Direct upload (recommended)

Upload during conversion:

```bash
java -jar bhce-weaver-0.1.0-obfuscated.jar convert \
  -i /path/to/logs/ -o ./output \
  --bh-url http://bloodhound:8080 \
  --bh-token-id YOUR_TOKEN_ID \
  --bh-token-key YOUR_TOKEN_KEY
```

Or upload previously generated files:

```bash
java -jar bhce-weaver-0.1.0-obfuscated.jar upload \
  -o ./output \
  --bh-url http://bloodhound:8080 \
  --bh-token-id YOUR_TOKEN_ID \
  --bh-token-key YOUR_TOKEN_KEY
```

### Method 2: BH-CE web UI

1. Open the BH-CE web interface.
2. Navigate to the file upload section.
3. Upload individual JSON files or the zip archive.
4. Wait for ingestion to complete.

### Method 3: BH-CE API (curl)

```bash
# Create upload job
JOB_ID=$(curl -s -X POST http://bloodhound:8080/api/v2/file-upload/start \
  -H "Authorization: Bearer $TOKEN" | jq -r '.data.id')

# Upload each file
for f in output/*.json; do
  curl -X POST "http://bloodhound:8080/api/v2/file-upload/$JOB_ID" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d @"$f"
done

# Signal completion
curl -X POST "http://bloodhound:8080/api/v2/file-upload/$JOB_ID/end" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 3. API Token Setup

To use direct upload, create an API token in BH-CE:

1. Log into the BH-CE web interface as an admin.
2. Navigate to Administration > API tokens.
3. Create a new token. Note the **Token ID** and **Token Key**.
4. The token key is shown only once. Store it securely.

Pass the token to Weaver:

```bash
--bh-token-id 01234567-89ab-cdef-0123-456789abcdef
--bh-token-key AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
```

---

## 4. Verification

After import, verify the data in BH-CE:

### Object counts

Check that the number of objects in BH-CE matches Weaver's output:

```cypher
MATCH (n) RETURN labels(n), count(n) ORDER BY count(n) DESC
```

### Domain structure

Verify the domain and its child objects:

```cypher
MATCH (d:Domain) RETURN d.name, d.objectid
```

### Relationships

Check that key relationships were imported:

```cypher
MATCH ()-[r:MemberOf]->() RETURN count(r) AS memberOf
MATCH ()-[r:Contains]->() RETURN count(r) AS contains
MATCH ()-[r:GenericAll]->() RETURN count(r) AS genericAll
MATCH ()-[r:Owns]->() RETURN count(r) AS owns
```

### Attack paths

Run standard BH-CE queries to confirm attack paths resolve:

- Shortest path to Domain Admins
- Kerberoastable users
- Users with DCSync rights
- ADCS misconfiguration paths

---

## 5. Troubleshooting

### Import shows 0 objects

- Verify the JSON files are valid: `cat output/users_*.json | python3 -m json.tool`
- Check that the `meta.type` field matches the expected type name.
- Ensure the `data` array is not empty.

### Duplicate objects

If the same environment was imported multiple times, BH-CE may show duplicates. Clear the database before re-importing, or use BH-CE's deduplication by `ObjectIdentifier`.

### Missing edges

Edges depend on data from multiple sa-ldapsearch passes. See the [COLLECTING.md](COLLECTING.md) guide for the required queries. Common missing data:

- **No ACL edges**: `nTSecurityDescriptor` attribute was not collected.
- **No group membership**: Neither `memberOf` nor `member` attribute was collected.
- **No containment**: Objects lack `distinguishedName` (unlikely but possible with malformed queries).
- **No GPO links**: Domain and OU objects lack the `gpLink` attribute.

### Version mismatch

Weaver produces BH-CE v6 JSON. If your BH-CE server is running an older version, the import may fail. Upgrade to BH-CE v6 or later.
