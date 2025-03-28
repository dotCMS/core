# DotCMS UVE SDK

A JavaScript SDK for interacting with DotCMS Universal Visual Editor (UVE).

## Installation

The UVE SDK is automatically included in DotCMS installations. For external usage:

```bash
yarn add @dotcms/uve-sdk
```


## Entry Points

The library exposes three main entry points:

- **`@dotcms/uve`**: Provides everything developers need to communicate with UVE.

- **`@dotcms/uve/types`**: Offers TypeScript types, interfaces, and other structures to help users organize their code properly.

- **`@dotcms/uve/internal`**: Exposes internal functions used by other SDKs, allowing users to create their own SDK if needed.

---

## Functions

### `createUVESubscription`

Creates a subscription to a UVE event.

**Parameters:**
- `eventType` - The type of event to subscribe to.
- `callback` - The callback function that will be called when the event occurs.

**Returns:**
- An event subscription that can be used to unsubscribe.

**Example:**
```typescript
// Subscribe to page changes
const subscription = createUVESubscription(UVEEventType.CONTENT_CHANGES, (changes) => {
  console.log('Content changes:', changes);
});
```

---

### `getUVEState`

Retrieves the current UVE state.

**Returns:**
- A `UVEState` object if running inside the editor, or `undefined` otherwise.

The state includes:
- `mode`: The current editor mode (preview, edit, live).
- `languageId`: The language ID of the current page set in the UVE.
- `persona`: The persona of the current page set in the UVE.
- `variantName`: The name of the current variant.
- `experimentId`: The ID of the current experiment.
- `publishDate`: The publish date of the current page set in the UVE.

> **Note:** If any of these properties are absent, it means the value is the default one.

**Example:**
```typescript
const editorState = getUVEState();
if (editorState?.mode === 'edit') {
  // Enable editing features
}
```

---

### `editContentlet`

Allows you to edit a contentlet in the editor.

Calling this function within the editor prompts the UVE to open a dialog to edit the specified contentlet.

**Parameters:**
- `contentlet<T>` - The contentlet to edit.

**Example:**
```typescript
editContentlet(myContentlet);
```

---

### `reorderMenu`

Reorders the menu based on the provided configuration.

**Parameters:**
- `config` (optional): Configuration for reordering the menu.
  - `startLevel` (default: `1`): The starting level of the menu to reorder.
  - `depth` (default: `2`): The depth of the menu to reorder.

This function constructs a URL for the reorder menu page with the specified `startLevel` and `depth`, then sends a message to the editor to perform the reorder action.

**Example:**
```typescript
reorderMenu({ startLevel: 2, depth: 3 });
```

---

### `initInlineEditing`

Initializes inline editing in the editor.

**Example:**
```typescript
initInlineEditing();
```

---

### `sendMessageToEditor`

Sends a message to the dotCMS page editor.

**Example:**
```typescript
sendMessageToEditor({ type: 'CUSTOM_MESSAGE', payload: { key: 'value' } });
```
