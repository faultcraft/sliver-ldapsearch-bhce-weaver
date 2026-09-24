# BHCE Weaver Testing Standard

Production Hardened -- tests prove the application works, not just that code compiles.

---

## 1. Testing Philosophy

Tests exist to catch failures before the user does. A green build means the application is ready to deliver. If the test suite passes but the deliverable crashes at startup, the test suite has failed its purpose.

**Principles:**

- **Tests prove behavior, not compilation.** A test that merely instantiates a class or imports a module without asserting behavior is worthless. Every test must assert an observable outcome.
- **Test what ships.** Unit tests verify internal logic. Integration tests verify the built artifact. Both are required. Neither replaces the other.
- **TDD where practical.** Write failing tests before implementing features. When fixing a bug, write a test that reproduces it first. This order ensures the test actually catches the defect.
- **No test relaxation.** Never weaken an assertion, skip a test, or change expected values to make a failing test pass. Fix the code, not the test.
- **Every feature ships with tests.** New parsers, commands, model types -- all require tests before the commit. "Tests later" is "tests never."

---

## 2. Test Tiers

### 2.1 Unit Tests

Fast, isolated, no external dependencies. Run on every build.

- **Parser tests.** Every parser: success path with realistic input, empty input, truncated input, malformed input, encoding edge cases.
- **Merger tests.** DN-based merge: case-insensitive matching, attribute union, multi-pass scenarios, missing DNs.
- **Model tests.** AD object records: construction from raw attributes, serialization to BH-CE JSON, default values for missing properties.
- **ACL tests.** Security descriptor parsing: known ACE types, inheritance flags, well-known SIDs.
- **Relationship tests.** Group membership, OU containment, GPO links, domain trusts, ADCS chains.

### 2.2 CLI / API End-to-End Tests

Exercise the application's external interface with real I/O and assertions on structured output.

- Input -> processing -> output round-trips (Sliver log to BH-CE JSON).
- Error paths: missing input, empty input, malformed input files.
- Flag combinations: `--since`, `--domain`, `--debug`.
- Output validation: all written JSON files are valid BH-CE v6 format.

### 2.3 Integration Tests

**Run against the built JAR -- not against raw source.** These tests launch the packaged application as a subprocess, feed it input, and assert on output.

**These exist because unit tests cannot catch:**
- Packaging errors (missing resources, broken manifests)
- Obfuscation errors (ProGuard stripped a keep target)
- Resource loading failures
- Classpath resolution differences between dev and packaged modes

**Test infrastructure:**
- Each test launches the built JAR as a subprocess via `java -jar`
- Input fed via CLI flags; output captured from stdout/stderr and output directory
- Timeout per invocation (30s default)
- Isolated workspace per test (temporary directories, no shared state)
- Test data uses inline fixtures -- realistic fragments of Sliver logs and LDAP output

### 2.4 Build Artifact Verification

Verify packaging correctness without launching the application:

- JAR manifest has correct entry point
- ProGuard obfuscated JAR contains expected keep targets
- Both standard and obfuscated JARs produce identical output for a reference fixture

---

## 3. Test Quality Rules

### 3.1 Naming

Pattern: `functionName_condition_expectedResult`.

Examples: `parseLdapBlock_truncatedInput_returnsPartial`, `mergeEntries_duplicateDn_unionsAttributes`.

Short obvious cases may use: `verbNoun` (e.g., `parsesValidSliverLog`). Legacy names are not force-renamed, but new tests follow the pattern.

### 3.2 Assertions

- Every test contains at least one assertion.
- Prefer specific assertions (`assertEquals`, `assertThrows`, `assertTrue` with message) over generic boolean checks.
- Always include a failure message on boolean assertions.
- Test one behavior per test function. Multiple assertions are fine when they verify different aspects of the same behavior.

### 3.3 Isolation

- Tests must not depend on execution order.
- Tests must not depend on external network, running servers, or user-specific state.
- File I/O uses `@TempDir`. Never write to the real filesystem.
- Tests that capture stdout/stderr must restore them after each test.

### 3.4 Test Data

- Use inline test data (string literals, small files written to temp directories).
- For parsers: include realistic fragments of Sliver console logs and LDAP output.
- Never reference files outside the project directory.

### 3.5 No Flaky Tests

- Tests must be deterministic. No sleeps, no race conditions, no timing-dependent assertions.

---

## 4. Coverage Expectations

Coverage numbers are a floor, not a target. High coverage with weak assertions is worse than moderate coverage with strong assertions.

- **Sliver log parser:** every state transition, partial logs, interleaved commands, timestamp filtering.
- **LDAP block parser:** empty blocks, multi-valued attributes, encoding edge cases, missing DNs.
- **DN merger:** case-insensitive matching, attribute union semantics, conflict resolution.
- **AD models:** every object type, construction from raw attrs, JSON serialization, default property values.
- **ACL parser:** every supported ACE type, inheritance flags, well-known SID resolution.
- **Integration tests:** full pipeline from Sliver log to BH-CE JSON, against the built JAR. Not optional.
- **New features:** ship with tests or do not ship. No "tests later" commits.

---

## 5. Build Lifecycle Integration

### 5.1 Fast Tests (unit + CLI e2e)

Run on every build (`mvn test`). Must pass on every commit. These gate development flow.

### 5.2 Integration Tests

Run during the verification phase (`mvn verify`). Launch the built JAR as a subprocess and assert on its behavior. These gate delivery.

The standard build command must run both phases. A release that has not passed integration tests is not a release.

### 5.3 Multi-Variant Verification

Both standard and ProGuard-obfuscated JARs must pass the identical integration test suite. Obfuscation-specific failures (stripped symbols, missing resources, renamed entry points) are the exact class of defects integration tests exist to catch.

---

## 6. When to Write What

| Change type | Required tests |
|---|---|
| New parser | Unit: correct parse, empty, truncated, malformed, encoding edge cases. Integration: inline fixture round-trip. |
| New AD object model | Unit: construction from raw attrs, JSON serialization, default values. Integration: end-to-end pipeline. |
| New relationship type | Unit: resolution logic. Integration: verify in output JSON. |
| Bug fix | Regression test that reproduces the bug BEFORE the fix. |
| Build / packaging change | Artifact verification test. Run all variants. |
| ProGuard rule change | Rebuild, run entire integration suite against obfuscated JAR. |
| Refactor | All existing tests pass unchanged. New assertions welcome. |

---

## 7. CI Pipeline

The CI workflow must run the full verification phase (not just unit tests) to include integration tests. The release workflow must also run verification.

A release that has not passed integration tests is not a release.
