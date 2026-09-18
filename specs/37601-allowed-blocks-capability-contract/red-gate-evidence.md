# Red gate evidence — T010 (AC-007, AC-008)

Run on branch `37601-allowed-blocks-capability-contract`, base `main` @ `667fc831ee`, with **no
production file modified**.

```
pnpm exec nx test new-block-editor -- capability-keys
```

```
 ❯ capability-keys.i1.spec.ts (5 tests | 3 failed)
   ✓ sanity: the producer is reachable and non-empty
   × slash-menu catalog: every blockName is producible
   × toolbar and popovers: every gate literal is producible
   ✓ does not mistake popover ids or mark lookups for capability gates
   ✓ scans the files it claims to scan
 ❯ capability-keys.i2.spec.ts (2 tests | 1 failed)
   ✓ sanity: both sides are reachable and non-empty
   × has no option that nothing consumes

AssertionError: Gated on keys the Settings tab cannot produce: aiContent, aiImage.
AssertionError: Gated on keys the Settings tab cannot produce:
  youtube (components/toolbar/toolbar.component.ts,
           components/asset-by-url-popover/asset-by-url-popover.component.ts)
AssertionError: The Settings tab offers options nothing in the editor consults:
  aiContentPrompt, aiImagePrompt.

 Test Files  2 failed (2)
      Tests  3 failed | 4 passed (7)
```

**Exactly the five keys specified in plan.md**, and the two guard tests pass — `link` and `emoji`
are *not* reported, confirming #37539 and #37442 genuinely ungated them.

## Two real defects in the invariant, found by the first Red run

Recorded because both are the kind of thing that makes a contract decorative, and both were caught
by tests written specifically to catch them.

### 1. Prose about a gate was being read as a gate

The first run reported `link` and `emoji` as violations. They are not gates: they are **inside
comments** explaining why those capabilities were ungated —
`toolbar.component.html:377` ("`isAllowed('link')` was true ONLY on a field with no restriction"),
and `editor-extensions.ts:134,182` for both keys.

Left unfixed, the invariant would have demanded that someone "fix" capabilities that are already
correct, and the quickest way to silence it would have been to **delete the explanation of why the
bug happened**. `stripComments()` now runs before the scan. The guard test
(*"does not mistake popover ids or mark lookups for capability gates"*) is what caught it.

### 2. The most important consumer was not being scanned

I2 reported `dotContent` as an orphan. It is not — it is gated at `editor-extensions.ts:160`, a
file absent from the original `SCANNED_FILES`. That file holds **11 gates**, and they are the
consequential ones: they decide which TipTap extensions are registered at all, so a bad key there
does not hide a button, it makes the editor unable to parse content using that node.

`extensions/editor-extensions.ts` has been added to `SCANNED_FILES`. A false *positive* in I2
revealed a false *negative* in I1 — which is the argument for keeping both directions.

## Correction to the planning documents

`research.md` R5 and `data-model.md` enumerate "seventeen consulted keys". That count is **low**:
it was produced by a grep whose character class excluded digits, so `heading1`–`heading6` were
missed. The helper reads `blockName` structurally and was never affected. Corrected in both
documents.
