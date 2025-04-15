# DotCMS UVE SDK

A JavaScript library to connect your dotCMS pages with the Universal Visual Editor (UVE) and enable content authors to edit pages in real time.

> **BETA VERSION NOTICE:** This SDK is currently in beta. APIs, functionality, and documentation may change significantly in the stable release.

## Table of Contents

- [Installation](#installation)
- [Entry Points](#entry-points)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
  - [Editor State](#editor-state)
  - [Event Subscriptions](#event-subscriptions)
  - [Content Editing](#content-editing)
  - [Navigation & UI](#navigation--ui)
  - [Messaging](#messaging)
- [Types Reference](#types-reference)
- [Examples](#examples)
- [Contributing](#contributing)
- [License](#license)

## Installation

The UVE SDK is automatically included in DotCMS installations. For external usage:

```bash
# Using npm
npm install @dotcms/uve-sdk

# Using yarn
yarn add @dotcms/uve-sdk
```

## Entry Points

The library exposes three main entry points:

- **`@dotcms/uve`**: Provides everything developers need to communicate with UVE.

- **`@dotcms/uve/types`**: Offers TypeScript types, interfaces, and other structures to help users organize their code properly.

## Getting Started

To use the UVE SDK in your project, import the necessary functions:

```typescript
import { getUVEState, createUVESubscription, editContentlet } from '@dotcms/uve';
import { UVEEventType, UVE_MODE } from '@dotcms/uve/types';

// Check if we're in the editor
const uveState = getUVEState();
if (uveState?.mode === UVE_MODE.EDIT) {
  console.log('Running in edit mode!');
  
  // Subscribe to content changes
  const subscription = createUVESubscription(UVEEventType.CONTENT_CHANGES, (changes) => {
    console.log('Content updated:', changes);
    // Update your UI with the new content
  });
  
  // Later, when no longer needed
  subscription.unsubscribe();
}
```

## API Reference

### Editor State

#### `getUVEState`

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

### Event Subscriptions

#### `createUVESubscription`

Subscribe to page changes and other UVE events. Receive a callback that will be called with the updated content of the page.

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

// Later, unsubscribe when no longer needed
subscription.unsubscribe();
```

### Content Editing

#### `editContentlet`

Allows you to edit a contentlet in the editor.

Calling this function within the editor prompts the UVE to open a dialog to edit the specified contentlet.

**Parameters:**
- `contentlet<T>` - The contentlet to edit.

**Example:**
```typescript
// Edit a contentlet
editContentlet(myContentlet);
```

#### `initInlineEditing`

Initializes inline editing in the editor.

**Parameters:**
- `type` - The type of inline editing ('BLOCK_EDITOR' or 'WYSIWYG').
- `data` - (Optional) Data for the inline editing session.

**Example:**
```typescript
// Initialize block editor
initInlineEditing('BLOCK_EDITOR', {
  inode: 'abc123',
  language: 1,
  contentType: 'Blog',
  fieldName: 'body',
  content: { /* content data */ }
});
```

### Navigation & UI

#### `reorderMenu`

Reorders the menu based on the provided configuration.

**Parameters:**
- `config` (optional): Configuration for reordering the menu.
  - `startLevel` (default: `1`): The starting level of the menu to reorder.
  - `depth` (default: `2`): The depth of the menu to reorder.

This function constructs a URL for the reorder menu page with the specified `startLevel` and `depth`, then sends a message to the editor to perform the reorder action.

**Example:**
```typescript
// Reorder menu starting from level 2 with depth of 3
reorderMenu({ startLevel: 2, depth: 3 });
```

### Messaging

#### `sendMessageToUVE`

The `sendMessageToUVE` function allows you to send messages to the dotCMS page editor. This is useful for triggering specific actions or updating the editor's state.

This function is primarily used within other library functions but can be helpful if you need to trigger specific behavior by sending a message to the UVE.

**Example:**
```typescript
sendMessageToUVE({ 
  action: DotCMSUVEAction.CUSTOM_MESSAGE, 
  payload: { key: 'value' } 
});
```

### Available Message Types (DotCMSUVEAction)

| **Type**                           | **Description**                                                                                   |
|--------------------------------------|---------------------------------------------------------------------------------------------------|
| `NAVIGATION_UPDATE`                  | Notifies the dotCMS editor that the page has changed.                                             |
| `SET_BOUNDS`                         | Sends the position of rows, columns, containers, and contentlets to the editor.                  |
| `SET_CONTENTLET`                     | Sends information about the currently hovered contentlet.                                         |
| `IFRAME_SCROLL`                      | Informs the editor that the page is being scrolled.                                               |
| `IFRAME_SCROLL_END`                  | Notifies the editor that scrolling has stopped.                                                   |
| `PING_EDITOR`                        | Pings the editor to check if the page is inside the editor.                                       |
| `INIT_INLINE_EDITING`                | Initializes the inline editing mode in the editor.                                                |
| `COPY_CONTENTLET_INLINE_EDITING`     | Opens the "Copy Contentlet" dialog to duplicate and edit a contentlet inline.                    |
| `UPDATE_CONTENTLET_INLINE_EDITING`   | Triggers the save action for inline-edited contentlets.                                           |
| `REORDER_MENU`                       | Triggers the menu reorder action with a specified configuration.                                  |
| `GET_PAGE_DATA`                      | Requests the current page information from the editor.                                            |
| `CLIENT_READY`                       | Indicates that the client has completed initialization.                                            |
| `EDIT_CONTENTLET`                    | Opens the contentlet editing dialog in the editor.                                                |

## Types Reference

The SDK provides TypeScript types to assist in development:

### UVE State and Modes

```typescript
// UVE State Interface
interface UVEState {
  mode: UVE_MODE;
  persona: string | null;
  variantName: string | null;
  experimentId: string | null;
  publishDate: string | null;
  languageId: string | null;
}

// UVE Mode Enum
enum UVE_MODE {
  EDIT = 'EDIT_MODE',
  PREVIEW = 'PREVIEW_MODE',
  LIVE = 'LIVE',
  UNKNOWN = 'UNKNOWN'
}
```

### UVE Events

```typescript
// Event Types Enum
enum UVEEventType {
  CONTENT_CHANGES = 'changes',
  PAGE_RELOAD = 'page-reload',
  REQUEST_BOUNDS = 'request-bounds',
  IFRAME_SCROLL = 'iframe-scroll',
  CONTENTLET_HOVERED = 'contentlet-hovered'
}

// Event Subscription Interface
interface UVEEventSubscription {
  unsubscribe: () => void;
  event: string;
}
```

## Examples

### Basic Usage

```typescript
import { getUVEState, createUVESubscription } from '@dotcms/uve';
import { UVEEventType, UVE_MODE } from '@dotcms/uve/types';

// Check if we're in the editor
const uveState = getUVEState();
if (uveState) {
  console.log(`Running in ${uveState.mode} mode`);
  
  // Initialize components based on editor state
  if (uveState.mode === UVE_MODE.EDIT) {
    enableEditFeatures();
  }
}
```

### Content Updates

```typescript
import { createUVESubscription } from '@dotcms/uve';
import { UVEEventType } from '@dotcms/uve/types';

// Subscribe to content changes
const subscription = createUVESubscription(UVEEventType.CONTENT_CHANGES, (changes) => {
  // Update your UI with the new content
  updateUI(changes);
});

// Clean up when component unmounts
function cleanup() {
  subscription.unsubscribe();
}
```

### Inline Editing Integration

```typescript
import { initInlineEditing } from '@dotcms/uve';

// Integrate with a block editor component
function setupBlockEditor(element, contentData) {
  element.addEventListener('click', () => {
    initInlineEditing('BLOCK_EDITOR', {
      inode: contentData.inode,
      language: contentData.languageId,
      contentType: contentData.contentType,
      fieldName: 'body',
      content: contentData.content
    });
  });
}
```

## Contributing

We welcome contributions to the UVE SDK! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
