# 02 · Themes

The theme is the HTML shell that wraps the template's layout. Templates are in [05-templates.md](../core/05-templates.md); containers in [03-containers.md](03-containers.md). VTL syntax: [velocity.md](velocity.md).

## Theme (`/application/themes/<name>/`)

**The site's CSS lives in the theme folder** — alongside `template.vtl`, referenced from the
shell via `${dotTheme.path}`, never a hardcoded path. If you were handed a `DESIGN.md`, this is
where its tokens become real: the design system's colors, type scale and spacing become CSS
custom properties in a stylesheet here. There is no separate design step and no other home for
it.

- Only `template.vtl` required (html shell + layout loop). `#dotParse` partials via `${dotTheme.path}<name>.vtl` — never hardcode paths.
- Grid: `.width` / `.leftOffset` are 1–12 → map onto CSS Grid.
- The layout loop renders containers with `$render.eval($column.draw())` — see below. Getting this wrong is the single most expensive failure in a theme build.
- Config (host/contact/brand) centralized; derive host from `$host.identifier`. Namespace CSS classes (BEM + prefix).
- SEO fallback chain (`$URLMapContent` → `$dotPageContent` → default) as one `#macro`. Robots `searchEngineIndex` is multi-select → `.selectedValues.contains('index')`.

## The layout loop — render containers with `$render.eval($column.draw())`

This is the scaffold. Copy it; don't reinvent the last hop.

```velocity
#foreach($row in $dotThemeLayout.body.rows)
  <div class="row">
    #foreach($column in $row.columns)
      <div class="col" style="grid-column: $column.leftOffset / span $column.width;">
        $render.eval($column.draw())
      </div>
    #end
  </div>
#end
```

`$column.draw()` emits the correct `#parseContainer` directive for every container in that column; `$render.eval()` executes it.

**Do NOT hand-roll `#parseContainer($container.identifier, $container.uuid)`.** It renders an **empty string with HTTP 200 and no error** — the page comes up with the theme, header and footer intact and every content slot missing. Two compounding reasons:

- `#parseContainer` takes **three** arguments. `$column.draw()` emits `#parseContainer('<path>', '<uuid>', true)`; a two-arg call silently renders nothing.
- `ContainerUUID`'s getter is `getUUID()`, so Velocity resolves it as **`.UUID`** — `$container.uuid` is an empty string. (`.identifier` works fine, which makes the call look correct.)

The macro *is* defined, so a wrong call renders empty rather than printing its own source — it reads as a placement or container-permissions problem, not a syntax one. The containers are fine; only the assembly is broken. **Diagnostic:** print `$!{column.draw()}` in the theme and compare it against what you wrote.
