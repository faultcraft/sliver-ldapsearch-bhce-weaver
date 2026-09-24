# Collecting sa-ldapsearch Data from Sliver C2

This guide covers collecting the LDAP data that BHCE Weaver needs from a Sliver C2 engagement.

---

## 1. Prerequisites

- An active Sliver C2 session with an implant on a domain-joined host.
- The `sa-ldapsearch` BOF loaded in Sliver.
- Sufficient privileges to query LDAP (any authenticated domain user is enough for most queries; `nTSecurityDescriptor` requires elevated access or specific DACLs).

---

## 2. Recommended Queries

Run these queries in sequence from a Sliver console session. Each query targets a specific set of attributes needed for complete BH-CE ingestion.

### Core objects (users, computers, groups)

```
sa-ldapsearch "(objectClass=user)" samAccountName samAccountType objectSid objectGUID distinguishedName userAccountControl memberOf primaryGroupID description adminCount
```

```
sa-ldapsearch "(objectClass=computer)" samAccountName samAccountType objectSid objectGUID distinguishedName userAccountControl operatingSystem dNSHostName servicePrincipalName msDS-AllowedToDelegateTo
```

```
sa-ldapsearch "(objectClass=group)" samAccountName samAccountType objectSid objectGUID distinguishedName member adminCount description
```

### Structural objects (domains, OUs, GPOs, containers)

```
sa-ldapsearch "(objectClass=domainDNS)" objectSid objectGUID distinguishedName gpLink ms-DS-MachineAccountQuota
```

```
sa-ldapsearch "(objectClass=organizationalUnit)" objectGUID distinguishedName gpLink description
```

```
sa-ldapsearch "(objectClass=groupPolicyContainer)" objectGUID distinguishedName displayName gPCFileSysPath
```

### Security descriptors (ACLs)

```
sa-ldapsearch "(objectClass=*)" distinguishedName nTSecurityDescriptor
```

This query retrieves the binary security descriptor for every object. It requires elevated access or explicit read permissions on the `nTSecurityDescriptor` attribute.

### ADCS objects

```
sa-ldapsearch "(objectClass=pKICertificateTemplate)" objectGUID distinguishedName displayName msPKI-Cert-Template-OID msPKI-Certificate-Name-Flag msPKI-Enrollment-Flag msPKI-Private-Key-Flag msPKI-RA-Signature pKIExtendedKeyUsage msPKI-Certificate-Application-Policy cACertificate
```

```
sa-ldapsearch "(objectClass=pKIEnrollmentService)" objectGUID distinguishedName displayName dNSHostName cACertificate certificateTemplates
```

```
sa-ldapsearch "(objectClass=certificationAuthority)" objectGUID distinguishedName cACertificate
```

### Trust relationships

```
sa-ldapsearch "(objectClass=trustedDomain)" objectGUID distinguishedName trustDirection trustType trustAttributes flatName
```

---

## 3. Log Collection

Sliver C2 writes console output as NDJSON (newline-delimited JSON) log files. Each line is a JSON object with `level`, `type`, `msg`, and `time` fields.

### Locating logs

Sliver console logs are typically found in:

- `~/.sliver-client/logs/` (default location)
- Custom paths if configured via Sliver's `--log-file` flag

### Log format

Weaver expects the standard Sliver console log format:

```json
{"level":"info","type":"command","msg":"sa-ldapsearch \"(objectClass=user)\" samAccountName","time":"2026-01-15T10:00:00Z"}
{"level":"info","msg":"Successfully executed sa-ldapsearch","time":"2026-01-15T10:00:01Z"}
{"level":"info","msg":"Got output:\n--------------------\ndistinguishedName: CN=jsmith,CN=Users,DC=corp,DC=local\nsamAccountName: jsmith\n...","time":"2026-01-15T10:00:02Z"}
```

### Multiple log files

Weaver accepts both individual files and directories as input. When given a directory, it recursively finds all `.json` files and processes them. Entries from all files are merged by DN, so it does not matter how the logs are split across files.

---

## 4. Tips

### Run all queries

The more attribute data you collect, the more complete the BH-CE output. Missing attributes result in empty properties or unresolved relationships, not errors.

### Include security descriptors

ACL data (`nTSecurityDescriptor`) is the largest single source of relationship edges. Without it, Weaver cannot produce ACL-based edges (GenericAll, WriteDacl, WriteOwner, ForceChangePassword, etc.).

### Multiple passes are fine

Weaver was designed for multi-pass collection. Running the same query twice just produces duplicate entries that get deduplicated during DN merge. Running different queries adds new attributes to existing objects.

### Date filtering

Use `--since YYYY-MM-DD` to process only entries from a specific engagement window. This is useful when log files contain data from multiple engagements.

### Domain filtering

Use `--domain corp.local` to process only objects from a specific domain. This filters by DN suffix during the merge stage.
