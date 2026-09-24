# Changelog

All notable changes to this project will be documented in this file.

---

## [0.1.1] -- 2026-09-24

### Added
- LICENSE file (BSD 3-Clause)

### Changed
- Rewrote README.md with pipeline diagram, docs links, and license section
- Removed LAB-SPEC.md from repository (added to .gitignore)

---

## [0.1.0] -- 2026-09-24

### Added
- Project scaffolding: Maven build, ProGuard obfuscation (default), CI/release workflows
- Project standards: coding standard, testing standard, design guide, architecture spec
- Git hooks: commit-msg (attribution filter), post-commit (VM_SHARED auto-push)
- Skeleton Main.java with picocli dispatch (--help, --version)
- CLI layer: ConvertCommand and UploadCommand with picocli flag parsing and validation
- Structured logging facade with [weaver] prefix and debug mode toggle
- Sliver NDJSON log parser with command-tracking state machine and --since filtering
- LDAP block parser: hyphen-delimited block splitting, key:value extraction, multi-valued attributes
- DN merger: case-insensitive DN grouping, attribute union with deduplication, domain filtering
- Utility layer: SID parser, GUID parser (Microsoft mixed-endian), timestamp parsers (FILETIME, GeneralizedTime), well-known SID registry (50+ entries)
- AD object models: all 13 BH-CE v6 types (User, Computer, Group, Domain, OU, GPO, Container, CertTemplate, EnterpriseCA, RootCA, AIACA, NTAuthStore, IssuancePolicy)
- DomainTrust standalone class for trust relationship serialization
- TypedProperties with Go-compatible typed defaults (bool->false, int->0, string->"", list->[])
- UacFlags helper for UserAccountControl bitmask parsing
- PKI helpers: enrollment flags, certificate name flags, private key flags, CA flags, OID registry
- CertificateParser utility for X.509 certificate property extraction
- Helper enums: PropertiesLevel, TrustDirection, TrustType
- Object classifier: samAccountType-first routing with objectClass fallback for structural/ADCS types
- NT security descriptor parser: MS-DTYP 2.4.6 binary format, DACL extraction, owner SID, SE_DACL_PROTECTED
- ACE record with type/flags/mask/objectType/inheritedType/SID fields
- ACE resolver: context-aware right mapping (21 BH-CE edge types) with object-type routing
- Access mask constants: AD-specific mapped values matching stored SD format
- Well-known GUIDs: extended rights, write properties, self-writes, LAPS attributes, PKI-specific attributes
- Relationship resolver orchestrator: ACL, containment, group membership, GPO links, trusts, ADCS, delegation
- ContainmentResolver: DN hierarchy parent-child with bidirectional Contains/ContainedBy edges
- GroupMembershipResolver: memberOf, member attribute, primaryGroupId with deduplication
- GpoLinkResolver: gpLink parsing with enforced flag propagation to domain/OU link lists
- TrustResolver: trust attribute matching to domain objects with direction/type/transitivity
- AdcsResolver: published template linking, CA hosting computer resolution, issuance policy group links
- DelegationResolver: constrained delegation SPN-to-computer hostname resolution
- BH-CE JSON writer: per-type files with v6 meta headers, validation, path traversal prevention, optional zip
- BH-CE upload client: HMAC-SHA256 three-stage chained signing, file-upload API flow with job management
- Full pipeline integration: Sliver log -> LDAP parse -> DN merge -> classify -> resolve -> validate -> write
- ConvertCommand wired end-to-end with --zip and --bh-url/--bh-token-id/--bh-token-key upload
- UploadCommand wired for standalone upload of previously generated files
- 244 unit tests across all modules (including end-to-end pipeline tests)
- README.md with usage instructions, pipeline description, and CLI reference
- docs/MANUAL.md: comprehensive manual covering installation, CLI reference, pipeline stages, output format, troubleshooting
- docs/COLLECTING.md: sa-ldapsearch data collection guide with recommended queries
- docs/BLOODHOUND-INGESTION.md: BH-CE import and verification guide with Cypher queries
- docs/SECURITY-DESCRIPTORS.md: NT security descriptor and ACE parsing reference
- scripts/build-manual-pdf.sh: pandoc/xelatex PDF generation
- scripts/lua/codeblock-lang.lua: default code block language for PDF syntax highlighting
- scripts/lua/pdf-mdlink-strip.lua: strip .md links in PDF output

### Fixed
- ProGuard keep rules: changed `info.picocli.**` to `picocli.**` (picocli's actual Java package), added `*Annotation*` to keepattributes to prevent annotation stripping
