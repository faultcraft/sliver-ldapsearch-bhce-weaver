# BHCE Weaver Design Guide

**Purpose.** How to write and review code here so it stays cohesive and maintainable. This is the judgment layer above `CODING-STANDARD.md`. The standard is a set of mechanical, enforceable rules (line caps, imports, error-handling shapes); **this guide is about design**. When the two are in tension, see section 7.

> **The numeric caps in `CODING-STANDARD.md` (method and class line limits) are smoke detectors, not targets.** A method over the cap is a *signal* to ask "does this do more than one thing?" -- not a licence to slice it at the limit and move on. Fixing the signal without fixing the design is **compliance theater**, and it is forbidden here (it produces worse code than the original).

---

## 1. Design for the reader

Every unit -- function, class, module -- has **one responsibility** and a **name that states it**. A reader should understand what a function does from its name and signature, and understand its body without scrolling or holding a mental stack.

If you cannot name a function after a single responsibility (you reach for `andX`, `part2`, `helper`, `process`, `handle`, or a name that describes a *line range*), the extraction is wrong. Re-cut it.

---

## 2. Function decomposition -- the rule we keep getting wrong

When a function is too long, **extract by responsibility, and return the result.**

- **Name for the concept, never the slice.** `resolveGroupMemberships(objects)` -- good. `processPartTwo(out, ...)` / `processRelationshipsAndAcls(out, ...)` -- forbidden (two concepts bundled because they fit in ~50 lines; the name is a line label).
- **Prefer pure functions that return a value over procedures that mutate a shared out-parameter.** Thread state as return values and compose them; do not pass a mutable accumulator down a chain just to keep slicing. Mutating a caller's collection is acceptable only when the function's *whole job* is to enrich that object (e.g. `attachRelationships(objects, acls)`), and the name says so.
- **Each extracted piece must be independently meaningful.** If a slice is not something you could name and understand on its own, the original function was not actually doing too much -- it was cohesive, and the cap is the wrong signal here. **Flag it (section 7); do not mangle it.**

---

## 3. When to introduce an abstraction -- and when not to

Add structure only when it **removes** complexity.

**Do:**
- A record when a cluster of values travels together (kill "3 primitives in a row" signatures -- see section 4, primitive obsession).
- A small class/module when data + the behavior over it recur together.
- A sealed interface when there is a genuine *open or fixed set* of variants (AD object types, ACE types).
- **Data over control flow:** a lookup table / catalog beats a long `if/else-if` or `switch` chain when the variation is really *data* (well-known SIDs, samAccountType mappings, ACE right definitions).

**Do not:**
- Add a design pattern because it has a name. A `Strategy` with one implementation, a `Factory` that calls one constructor, a `Manager`/`Helper`/`Util` grab-bag class -- these are over-engineering.
- Wrap single-use, single-caller logic in an interface "for flexibility" that does not exist yet.
- Create indirection a reader must chase to understand one thing.

The test: *does this abstraction let a reader hold less in their head?* If not, delete it.

---

## 4. Anti-patterns we reject (by name)

- **Compliance theater** -- changing code shape only to satisfy a cap or linter, without improving the design. Never edit `CODING-STANDARD.md` to make violating code "pass"; fix the code (or section 7).
- **Procedural sprawl / out-parameter threading** -- arbitrary slices passing a mutable accumulator. See section 2.
- **God function / god class** -- many responsibilities in one unit. The main pipeline orchestrator may be explicitly documented as an exception; nothing else is.
- **Primitive obsession / stringly-typed** -- long parameter lists of strings/ints that are really one concept. Introduce a record.
- **Feature envy** -- a function that mostly reaches into another object's data; move the behavior to where the data lives.
- **Leaky mutable state** -- returning or storing a reference to an internal mutable collection. Return immutable views.
- **Boolean/flag parameters** that switch a function between two behaviors -- usually two functions.
- **Deep nesting** -- guard-clause and return early instead of arrowhead `if` nests.

---

## 5. Design principles for this codebase

- **Immutability by default.** Records for data; immutable collections on returns; no leaking internal mutable state.
- **Encapsulation.** Minimum visibility by default; expose the minimum surface.
- **Composition over inheritance.** Inheritance only for a real is-a with shared contract.
- **One concept per unit.** Pipeline stages = orchestration. Model types = data. Parsers = parsing. A pipeline stage should *orchestrate*, not *implement* -- if a stage contains a substantial algorithm, that algorithm belongs in its own module.
- **Errors:** fail fast for bugs; parsers/input handlers degrade gracefully to partial output and never crash the caller; catch the **narrowest** error type that satisfies the contract.

---

## 6. Review checklist (apply to every change, and every extraction)

1. Does each function do exactly one thing, and does its **name say that thing**?
2. Can a reader understand the body without scrolling or tracking hidden state?
3. Is there an **out-parameter that should be a return value**?
4. Is there repeated structure (same 5 lines, same triple of primitives) that wants a helper or a record?
5. Did I add an abstraction that is not paying for itself? (Delete it.)
6. **Am I changing shape only to hit a number?** If yes -- stop, and either design it properly or escalate (section 7).
7. Does a genuinely cohesive function still exceed a cap? -> section 7, do not mangle.

---

## 7. When the standard and good design conflict

`CODING-STANDARD.md` is authoritative and **must not be edited** to dodge work. But a mechanical cap can occasionally fight a genuinely cohesive design (a parser state machine, a large switch over AD object types, an algorithm that reads best whole). When that happens:

- **Do not** mangle the code into meaningless fragments to pass the cap.
- **Do not** edit the standard.
- **Do** leave the function cohesive and **flag the specific tension to the maintainer** with a one-line rationale, so they can decide (adjust the standard, or accept the refactor). The maintainer owns that call -- you do not make it silently either way.
