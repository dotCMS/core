# 01 · The contract: the type's `Var` = the component-map key

The headless counterpart to VTL's "choose the mechanism" step
([vtl/01](../vtl/01-choose-mechanism.md)). In VTL you pick *where the markup lives* —
container `<Var>.vtl`, widget `widgetCode`, or detail-page VTL. In headless there is
one answer for everything: **every content type that can appear on a page needs a React
component**, and the map key is what connects them.

This is the join between the two halves of the build, and the usual cause of a slot
rendering as "no component":

```ts
export const pageComponents = {
  Book: BookCard,        // key === the content type's Var in dotCMS
  webPageContent: WebPageContent,
};
```

## The key must equal the type's `Var`, case-exact

And **you cannot assume the `Var` equals the type's name.** dotCMS derives it, a name
collision appends a number — a second `Testimonial` becomes `testimonial1` — and casing does
not reliably follow the name. **Read the `Var` back from dotCMS and use that value**
([core/02](../core/02-content-types.md)). A key that differs by one character renders nothing.

Create and name the content type *before* writing its component, so the variable exists to
copy.

## Before you write anything, find the existing map

**Do not create a component map.** In any working app one already exists, and a second one
means half your types silently don't render. Where it lives and how to recognise it — along
with the other four wiring roles — is in [00 §B](00-connect.md). Add a key to what's there.

## A "no component" fallback hides this failure

If the map has a fallback for unmapped types, a missing key does **not** print "no component"
on the page — it renders the fallback, or nothing an author would recognise as an error. So a
missing key and a missing container stub ([core/06](../core/06-containers.md)) look identical
from the browser. Check both.

Rules and lazy-loading: **@dotcms/react README → Component Mapping**.
