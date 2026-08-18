# Spec-Kit Team Session

A run-of-show for teaching [Spec-Kit](SPEC_KIT_QUICK_START.md) to a team or a new-hire cohort.
Written for whoever facilitates; devs only need the quick start.

---

## Why run a session at all

The flow only clicks once you've hit the gates yourself. Two of them stop a run and wait for a
human — the spec review on PR 1, and the TDD approval gate inside `/speckit-implement` — and
reading about them lands very differently from watching a run halt.

---

## Run of show

~60 minutes, everyone at their own machine, one screen shared. Pick **one small real bug**
beforehand — a bug over a feature, because the spec stays legible on screen. You can't wait
for a real merge inside the hour, so also prepare a branch with an **already-merged spec**
that's through `/speckit-tasks`, and switch to it at step 3.

1. **(5 min)** Why spec-first: review happens on the contract, not the diff — [quick start §3](SPEC_KIT_QUICK_START.md). This is the
   part that changes how the team works — spend the time.
2. **(10 min)** `/speckit-specify-fix` on the real bug, live. Open PR 1 with just the spec
   and let the room review it — that review *is* the lesson.
3. **(15 min)** `/speckit-plan`. Stop and narrate when the ADR hook fires — this is the
   moment "binding architectural input" becomes concrete.
4. **(15 min)** `/speckit-tasks`, then walk the generated `[GATE]` tasks.
5. **(10 min)** `/speckit-implement` **on the prepared branch, to the first gate only.**
   Running it cold to completion will not fit the hour.
6. **(5 min)** `git status` — show what's tracked and what's ignored.

Record it. It costs nothing, covers absentees, and captures the real stumbles. Re-record
rather than edit when it drifts.

---

## After the session

- Post the recording link and the [quick start](SPEC_KIT_QUICK_START.md) in `#eng`.
- Collect the places people got stuck — each one is a bug in the quick start, not in the
  attendee. Open a PR against it.

---

*Maintained by the Engineering Team. Questions in `#eng`.*
