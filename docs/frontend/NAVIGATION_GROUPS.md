# Portlet navigation groups

A portlet can declare an optional `parent` init-param to appear in a translated flyout inside its existing layout group:

```xml
<portlet>
    <portlet-name>dotAuth</portlet-name>
    <display-name>dotAuth</display-name>
    <portlet-class>com.dotcms.spring.portlet.PortletController</portlet-class>
    <init-param>
        <name>parent</name>
        <value>Identity-Access</value>
    </init-param>
    <portlet-url>/dotAuth</portlet-url>
</portlet>
```

Define the parent label in the language bundle:

```properties
Identity-Access=Identity Access
```

Users, Roles, Tools, dotAuth, Users (Legacy), and Roles & Tools (Legacy) use this group by default. Custom portlets can use the same key or declare another key. Missing translations display the key itself.

## Behavior

- Portlets with the same nonblank parent key are grouped **within each layout**. The same key in another layout creates a separate group.
- The parent appears at the first child's position; children keep their existing relative order. Portlets without a parent, including blank/whitespace-only values, stay flat.
- Groups contain only the portlets supplied by the existing menu API. A parent does not add portlets to a layout, grant permissions, or expose a new route.
- A parent with one child remains a group. No parent appears when none of its children are present.
- Hovering or clicking a parent opens its flyout. Keyboard users can reach the menu with Tab, move with arrow keys, and open a parent with Right Arrow or activation. Escape closes the flyout and restores focus; clicking outside also closes it.
- Clicking an open parent does not toggle it closed. Clicking a child still navigates normally but leaves the flyout open, like the main navigation flyout.
- Leaving the parent or its flyout starts a 300 ms close delay. Entering either cancels the delay, allowing the pointer to cross between them without dismissing the flyout.
- Grouping works with both expanded and collapsed navigation. The active child's parent is highlighted.
- Roles & Tools (Legacy) shows qualified labels such as `Identity Access › Users`. Tools keeps qualified labels in its catalog and nests included children beneath their parent heading, matching navigation. Catalogs can be searched by the translated parent label. Placement and removal still use unchanged portlet IDs.
- Tools uses the same resizable divider and initial 22%/78% panel split as Roles. Drag the divider, or focus its handle and use the arrow keys, to change the tree and catalog widths. Names that still overflow reveal their full label in a tooltip on hover.
- Existing portlet routes, layout IDs, bookmarks, and dynamic Angular module loading are unchanged. `/api/v1/menu` stays flat and carries the parent key through its existing `initParams` map.

## Configuration scope

This is one optional grouping level, not a recursive portlet hierarchy. The parent value names a language key, not a parent portlet ID. The XML configuration is loaded through the existing portlet configuration lifecycle; deploy/reload it using the same procedure as other portlet init-params.

## Reordering upgraded sections

Older sections may retain retired tool IDs such as `dynamic-plugins` for rollback compatibility. Reordering or editing a section may keep those existing entries without registering them again. Only newly added tools must be registered and placeable; existing entries cannot be duplicated or blank, and an unregistered tool cannot be copied from another section. Reordering never silently removes legacy entries.

## Ordering groups and children in Tools

The included-tools tree places a parent where its first child occurs and preserves relative child order, just like navigation. Drag a parent to move all its children as one block; drag a child to reorder it within that parent. Focus a drag handle and use Up/Down Arrow for keyboard reordering. Children cannot be dropped into a different parent: change a custom tool's Parent through Edit Tool instead.

Grouping uses the catalog's explicit `parent` key and unqualified `label`, never a delimiter parsed from the display title. The existing qualified `title` is unchanged for older clients. A catalog without the new metadata remains flat. Reorders still submit the full flat ID list; retained legacy entries are not dropped or reassigned.

## Custom tools

Tools' New Tool and Edit Tool dialogs include an optional **Parent** field. The Roles & Tools (Legacy) New Tool dialog also includes it. Enter a label or a language key, such as `Identity-Access`. The raw value is stored in the custom tool's `parent` init-param; it is translated only when displayed. Empty or whitespace-only input means no parent. Clearing an existing parent returns the tool to the flat navigation list.

The custom-tool API accepts `parent` on `POST` / `PUT /api/v1/portlet/custom`. Values are trimmed. On create, an omitted value means no parent. On update, omission preserves the existing parent for older clients; an explicit empty string clears it. `GET /api/v1/portlet/custom/{id}` returns the raw `parent`, or `""` when none is configured.

Saving a tool notifies open navigation menus to reload. The Tools screen re-reads the saved catalog row so its qualified label updates immediately, without storing the translated prefix as the tool's name.

No new database field or schema migration is required. Shipped tools remain configured through XML, not through the custom-tool editor. Translations beyond the default English label can be provided through the normal language-bundle mechanism.
