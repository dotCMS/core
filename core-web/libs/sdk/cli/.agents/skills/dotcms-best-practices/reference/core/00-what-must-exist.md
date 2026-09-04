# 00 · What a page needs to render

True for **both delivery modes**. A missing item yields a blank or shell-only page
with **HTTP 200** — never an error, which is why every build ends in a verify step.

1. **A published site** ([01](01-site.md)). It carries the identifier everything else
   is scoped to, and an unpublished site serves nothing.
2. **Page → published template.** The template's `layout` defines the rows, columns
   and container slots, and every container it names must already exist
   ([06](06-containers.md)). Re-publish the template after any layout change
   ([05](05-templates.md)).
3. **Content placed into a slot** ([09](09-placement.md)). Placement is a full
   replacement — omitted slots are cleared.
4. **Publish everything, page last.** LIVE changes only on publish: re-publish the
   page after creating content, changing placement, or editing its template.

Your delivery mode adds its own requirements on top:

- **VTL-rendered** → [../vtl/00-wiring.md](../vtl/00-wiring.md)
- **Headless, Next.js** → [../nextjs/00-connect.md](../nextjs/00-connect.md)
