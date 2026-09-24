# BHCE Weaver Coding Standard

Production Hardened -- zero ambiguity, static-analysis enforceable.

Java 21.

---

## 1. Module/Class Structure

### 1.1 Visibility
- Modules and types are internal by default. Expose publicly only when accessed from another module or the extension API.
- Document why a type is designed for inheritance. Default to sealed/final.

### 1.2 Size Limits
- **Hard cap: 600 lines per module/class.** Modules above 600 lines must be split.
- Modules above 600 lines are tracked in TODO until split.
- Exception: a central dispatch hub may exceed this, but each handler method must obey method limits.

### 1.3 Field/Member Ordering
Within a module or class, members appear in this order:
1. Constants (compile-time, immutable)
2. Static/module-level patterns and formatters
3. Immutable instance fields
4. Mutable instance fields (with comment explaining why mutable)
5. Constructors / factory functions
6. Public methods
7. Internal/package-private methods
8. Private methods
9. Nested types (value types, enums, inner classes)

### 1.4 Value Types
All data-only types use records. No manual equality/hash/string-representation on plain data holders. If a type has only immutable fields and no mutation, it should be a record.

---

## 2. Functions/Methods

### 2.1 Size Limits
- **Hard cap: 60 lines per function/method.** Extract helpers if longer.
- Exception: parser functions that switch over many format variants may reach 80 lines if splitting would obscure the format's structure.

### 2.2 Naming
- camelCase for methods and local variables.
- UPPER_SNAKE_CASE for compile-time constants.
- No abbreviations except universally understood ones: `db`, `id`, `ip`, `url`, `csv`, `json`, `xml`, `html`, `dns`, `os`, `io`, `dn`, `sid`, `acl`, `ace`, `guid`, `oid`, `ca`, `gpo`, `ou`.
- Boolean functions: `is_foo()`, `has_foo()`, `can_foo()`. Never bare adjective (`enabled()` -> `isEnabled()`).

### 2.3 Parameters
- No more than 5 parameters. Use a record or builder if more are needed.
- Annotate or document any parameter that may be null. Unannotated parameters are assumed non-null.

---

## 3. Error Handling

### 3.1 Parsers
- Parsers **never throw** to callers. Malformed input -> return empty/partial output, log a warning with a recognizable prefix (`[weaver]`).
- Every parser must handle: truncated input, empty input, binary garbage, encoding errors.
- Parser error messages include the filename and a summary of what went wrong.

### 3.2 Internal Errors
- Use a dedicated application exception type for conditions that indicate a bug.
- Propagate I/O errors explicitly; do not swallow them.
- Never catch the broadest possible exception type. Catch the specific type.
- **Exception:** parser-dispatch loops may catch broadly at the isolation boundary to implement S3.1 ("parsers never throw"). A single rogue parser must not abort the entire ingest batch. The catch logs the failure and continues.

### 3.3 Resource Management
- All resources that require cleanup (file handles, connections, streams) must use try-with-resources.

---

## 4. Output and Logging

### 4.1 No Direct stdout/stderr
- Direct stdout writes are permitted **only** in: the main entry point and build-time tooling.
- All other output goes through a structured output interface (writer, logger, UI component).
- Direct stderr writes are permitted in parsers for malformed-input warnings (prefixed with `[weaver]`), and in the main entry point for fatal errors.

### 4.2 User-Facing Messages
- No raw string concatenation for user-visible messages with 3+ interpolations. Use `String.format()` or `String.formatted()`.
- 1-2 interpolations: concatenation is acceptable.
- Terminal output respects a color abstraction. Never embed raw ANSI codes.

---

## 5. Imports/Dependencies

### 5.1 No Wildcard Imports
- Every import is explicit. Wildcard/glob imports are banned.

### 5.2 Ordering
- Group by: project imports, then third-party, then `java.*`/`javax.*`, with blank lines between groups.

---

## 6. Language Features

### 6.1 Type Inference
- `var` permitted where the right-hand side makes the type obvious (constructors, factory calls, resource declarations).
- Not permitted where a reader would need to trace the call chain to determine the type.

### 6.2 Pattern Matching
- Use pattern matching (`instanceof` patterns, switch expressions) for type checks and destructuring instead of cast-after-check.

### 6.3 Multi-Line Strings
- Use text blocks (`"""`) for embedded SQL, templates, JSON fragments, and help text.

### 6.4 Override Annotations
- `@Override` mandatory on every method that overrides a parent type's implementation. No exceptions.

---

## 7. Strings

### 7.1 Building
- 1-2 interpolations: concatenation.
- 3+ interpolations: `String.format()` or `String.formatted()`.
- Loop accumulation: `StringBuilder`.
- Joining collections: `String.join()` or `Collectors.joining()`.

### 7.2 Comparison
- Case-insensitive comparison: use `String.equalsIgnoreCase()` or `String.CASE_INSENSITIVE_ORDER`. Always specify `Locale.ROOT` when case-folding technical strings.

---

## 8. Collections and Types

### 8.1 No Unparameterized Generics
- Every generic type is fully specified. No raw types.

### 8.2 Immutable Returns
- Public methods return `List.copyOf()`, `Map.copyOf()`, `Set.copyOf()`, or `Collections.unmodifiable*` views.
- Internal mutable collections are never leaked to callers.

### 8.3 Optional/Nullable
- Return `Optional<T>` instead of null for "might not exist" results from public methods.
- Never use `Optional` as field types or method parameters.

---

## 9. Testing

### 9.1 Framework
- JUnit 5 (Jupiter). Prefer built-in assertions over third-party assertion libraries.
- Use `@TempDir` for any test that touches the filesystem.

### 9.2 Method Naming
- Pattern: `functionName_condition_expectedResult`.
- Examples: `parseLdapBlock_truncatedInput_returnsPartial`, `mergeDn_caseInsensitive_mergesCorrectly`.
- Short obvious cases may use: `verbNoun` (e.g., `parsesHostAndPorts`).

### 9.3 Parser Fuzz Tests
- Every parser must have a fuzz test covering: empty file, truncated file, binary garbage, encoding edge cases.

---

## 10. Documentation

### 10.1 Public API Docs
- **Required** on: every public type, every public method.
- **Not required** on: private methods, test methods.
- Minimum: one sentence stating what it does. No `@author` tags.
- Parsers: documentation must state what formats are supported.

### 10.2 Inline Comments
- Default: no comments. Code should be self-documenting via naming.
- Permitted when: explaining WHY, not WHAT. Hidden constraints, workarounds, non-obvious invariants.
- Never: commented-out code, TODO without a tracking entry, change-history comments.

---

## 11. Project Structure

### 11.1 Module/Package Layout
- One public type per file. Nested value types and enums inside their parent are acceptable.
- Test files mirror source files: `Foo` -> `FooTest` / `FooIT`.

---

## 12. Security

### 12.1 XML Parsing
- All XML parsers disable external entity resolution (XXE prevention).

### 12.2 Input Validation
- All parsed LDAP attribute values are treated as untrusted data.
- SID/GUID strings validated before use in lookups or output.

### 12.3 Crypto
- HMAC signing for BH-CE API uses `HmacSHA256` via `javax.crypto.Mac`.
- Never use a non-cryptographic PRNG for security operations.

### 12.4 Path Traversal
- Validate that resolved output paths stay within the output directory before writing.
- Never pass user-controlled strings directly to path construction without validation.

---

## 13. Threading/Concurrency

Not applicable for this pipeline tool (single-threaded batch processing).
