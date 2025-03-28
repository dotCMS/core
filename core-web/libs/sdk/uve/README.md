# DotCMS UVE SDK

A JavaScript library to connect your dotCMS pages with the Universal Visual Editor (UVE) and enable content authors to edit pages in real time.  

## Installation

The UVE SDK is automatically included in DotCMS installations. For external usage:

```bash
yarn add @dotcms/uve-sdk
```


## Entry Points

The library exposes three main entry points:

- **`@dotcms/uve`**: Provides everything developers need to communicate with UVE.

- **`@dotcms/uve/types`**: Offers TypeScript types, interfaces, and other structures to help users organize their code properly.

---

## Functions

### `createUVESubscription`

Subscribe to the pages changes. Receive a callback that will be called with the updated content of the page. 

**Parameters:**
- `eventType` - The type of event to subscribe to.
- `callback` - The callback function that will be called when the event occurs.

**Returns:**
- An event subscription that can be used to unsubscribe.

**Example:**  
***typescript
// Subscribe to page changes
const subscription = createUVESubscription(UVEEventType.CONTENT_CHANGES, (changes) => {
console.log('Content changes:', changes);
});

// Unsubscribe

subscription.unsubscribe()
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

### `sendMessageToUVE`

The `sendMessageToUVE` function allows you to send messages to the dotCMS page editor. This is useful for triggering specific actions or updating the editor's state.

This function is primarily used within other libraries but can be helpful if you need to trigger specific behavior by sending a message to the UVE.

**Example:**
```typescript
sendMessageToEditor({ type: 'CUSTOM_MESSAGE': DotCMSUVEAction, payload: { key: 'value' } });
```

### Available Message Types (DotCMSUVEAction)

| **Type**                           | **Description**                                                                                   |
|--------------------------------------|---------------------------------------------------------------------------------------------------|
| `set-url`                            | Notifies the dotCMS editor that the page has changed.                                             |
| `set-bounds`                         | Sends the position of rows, columns, containers, and contentlets to the editor.                  |
| `set-contentlet`                     | Sends information about the currently hovered contentlet.                                         |
| `scroll`                             | Informs the editor that the page is being scrolled.                                               |
| `scroll-end`                         | Notifies the editor that scrolling has stopped.                
| `init-inline-editing`                | Initializes the inline editing mode in the editor.                                                |
| `copy-contentlet-inline-editing`     | Opens the "Copy Contentlet" dialog to duplicate and edit a contentlet inline.                    |
| `update-contentlet-inline-editing`   | Triggers the save action for inline-edited contentlets.                                           |
| `reorder-menu`                       | Triggers the menu reorder action with a specified configuration.                                  |
| `get-page-data`                      | Requests the current page information from the editor.                                            |
| `client-ready`                       | Indicates that the client has sent a GraphQL query to the editor.                                 |
| `edit-contentlet`                    | Opens the contentlet editing dialog in the editor.                                                |
