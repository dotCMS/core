# dotCMS UVE SDK

The `@dotcms/uve` SDK enables live editing in your JavaScript app by connecting it to the **dotCMS Universal Visual Editor (UVE)**. Use it to support in-place editing, layout updates, and real-time content previews.

---

## Table of Contents

- [What Is It?](#what-is-it)
- [Starter Examples](#starter-examples)
- [Installation](#installation)
- [Supported Environments](#supported-environments)
- [Example Integrations](#example-integrations)
- [Common Patterns](#common-patterns)
- [API Reference](#api-reference)
  - [Initialization & State](#initialization--state)
  - [Subscriptions & Events](#subscriptions--events)
  - [Content Editing](#content-editing)
  - [Navigation & Menu](#navigation--menu)
  - [Communication Utilities](#communication-utilities)
- [TypeScript Support](#typescript-support)
- [dotCMS Support](#dotcms-support)
- [How To Contribute](#how-to-contribute)
- [Licensing Information](#licensing-information)

---

## What Is It?

The `@dotcms/uve` package provides a low-level API to interface with the dotCMS Universal Visual Editor (UVE). Use it to:

- Make pages and contentlets editable
- Respond to editor events (content updates, mode changes)
- Trigger modal or inline editing experiences
- Sync app routing with the dotCMS editor

### SDK Architecture

The UVE SDK is designed with flexibility in mind:

- **Core SDK (`@dotcms/uve`)**: A low-level, framework-agnostic library that provides core functionality for custom implementations. Use this when you need complete control over the integration or are working with an unsupported framework.

- **Framework-Specific Libraries**: We provide optimized implementations for popular frameworks that handle common patterns and offer a more streamlined developer experience:
  - `@dotcms/uve-react` for React applications
  - `@dotcms/uve-angular` for Angular applications

Choose the framework-specific library when available for your project - they're built on top of the core SDK and provide framework-specific features and optimizations. If you're using a different framework or need more control, the core SDK gives you all the tools needed for a custom implementation.

## Starter Examples

To help you get started with the UVE SDK, we provide several example projects demonstrating integration with different frameworks:

- [Angular Example](https://github.com/dotCMS/core/tree/main/examples/angular) - Integration with Angular
- [Next.js Example](https://github.com/dotCMS/core/tree/main/examples/nextjs) - Integration with Next.js
- [Astro Example](https://github.com/dotCMS/core/tree/main/examples/astro) - Integration with Astro

These examples show how to initialize the UVE, handle content editing, and implement navigation synchronization within each framework's specific patterns and lifecycle methods.

---

## Installation

```bash
npm install @dotcms/uve@next
```

---

## Supported Environments

| Browser | Supported Versions |
| ------- | ------------------ |
| Chrome  | Latest 3 versions  |
| Firefox | Latest 3 versions  |
| Edge    | Latest 3 versions  |

---

## Example Integrations

Want to see UVE in action with real frameworks?

* [Next.js Example](https://github.com/dotCMS/core/tree/main/examples/nextjs)
* [Angular Example](https://github.com/dotCMS/core/tree/main/examples/angular)
* [Astro Example](https://github.com/dotCMS/core/tree/main/examples/astro)

Each shows how to:

* Initialize the UVE
* Listen to editor events
* Edit external or dynamic content

---

## Common Patterns

### 1. Enable page editing in React

```tsx
useEffect(() => {
  const { destroyUVESubscriptions } = initUVE();
  const subscription = createUVESubscription(UVEEventType.CONTENT_CHANGES, updatePage);
  return () => {
    destroyUVESubscriptions();
    subscription.unsubscribe();
  };
}, []);
```

---

### 2. Trigger inline block editor editing

```tsx
<div onClick={() => enableBlockEditorInline(contentlet, 'bodyContent')} />
```

---

## API Reference

### Initialization & State

#### `initUVE(config?: DotCMSUVEConfig)`

**Overview**

Initializes UVE and sets up communication between your app and the editor.

**Parameters**:

| Name      | Type              | Required | Description                              |
|-----------|-------------------|----------|------------------------------------------|
| `config`  | `DotCMSUVEConfig` | ❌       | Optional setup for language, persona, etc |

**Returns**: `{ destroyUVESubscriptions: () => void }`

```ts
const { destroyUVESubscriptions } = initUVE({ params });
```

#### DotCMSUVEConfig

- `graphql` (object - optional): The graphql query to execute
  - `query` (string): The graphql query to execute
  - `variables` (object): The variables to pass to the graphql query.
- `params` (object - optional): The parameters to pass to the page API
  - `languageId` (string): The language ID of the current page set on the UVE.
  - `personaId` (string): The persona ID of the current page set on the UVE.
  - `publishDate` (string): The publish date of the current page set on the UVE.
  - `depth` (0-3): The depth of the current page set on the UVE.
  - `fireRules` (boolean): Indicates whether you want to fire the rules set on the page.
  - `variantName` (string): The name of the current variant.

**Editor Integration**

* Required to enable all UVE features like editing, subscriptions, and navigation sync.
* Should be called once per page mount.

**Common Issues**

* Failing to call destroyUVESubscriptions() on unmount may lead to memory leaks.

---

#### `getUVEState()`

**Overview**

Returns current editor state if UVE is active.


**Returns:**

- `UVEState | undefined` - Returns the UVE state object if running inside the editor, undefined otherwise.


```ts
const state = getUVEState();
if (state?.mode === 'edit') { showEditorUI(); }
```

**UVE State**

- `dotCMSHost`: The host URL of the DotCMS instance
- `experimentId`: The ID of the current experiment
- `languageId`: The language ID of the current page set on the UVE
- `mode`: The current editor mode (`'preview'`, `'edit'`, `'live'`)
- `persona`: The persona of the current page set on the UVE
- `publishDate`: The publish date of the current page set on the UVE
- `variantName`: The name of the current variant

**Editor Integration**

* Returns undefined when not running inside the dotCMS editor.

**Common Issues**

* Do not rely on getUVEState() outside the editor context.




---

### Subscriptions & Events

#### `createUVESubscription(eventType, callback)`

**Overview**

Subscribes to UVE events like content changes or navigation updates.

**Parameters**:

| Name        | Type           | Required | Description                        |
| ----------- | -------------- | -------- | ---------------------------------- |
| `eventType` | `UVEEventType` | ✅       | The event to subscribe to          |
| `callback`  | `Function`     | ✅       | Called when the event is triggered |


```ts
const sub = createUVESubscription(UVEEventType.CONTENT_CHANGES, updateFn);
sub.unsubscribe();
```

**Event Types**

- `UVEEventType.CONTENT_CHANGES`: Triggered when the content of the page changes.
- `UVEEventType.PAGE_RELOAD`: Triggered when the page is reloaded.
- `UVEEventType.REQUEST_BOUNDS`: Triggered when the editor requests the bounds of the page.
- `UVEEventType.IFRAME_SCROLL`: Triggered when the iframe is scrolled.
- `UVEEventType.IFRAME_SCROLL_END`: Triggered when the iframe has stopped scrolling.
- `UVEEventType.CONTENTLET_HOVERED`: Triggered when a contentlet is hovered.

**Editor Integration**

* Only triggers when your app is rendered inside the UVE.

**Common Issues**

* Always unsubscribe to prevent memory leaks.


---

### Content Editing

#### `editContentlet(contentlet)`

**Overview**

Opens the dotCMS modal editor for any contentlet in or out of page area.

**Parameters**:

| Name         | Type            | Required | Description                        |
| ------------ | --------------- | -------- | ---------------------------------- |
| `contentlet` | `Contentlet<T>` | ✅        | The contentlet to open for editing |


```ts
editContentlet(myContentlet);
```

**Editor Integration**

* Works inside UVE even if the contentlet is not part of the page.

**Common Issues**

* Fails silently if run outside the editor.

---

#### `initInlineEditing(type, data)`

**Overview**

Triggers inline editing for supported field types (WYSIWYG or Block Editor).

**Parameters**:

| Name   | Type                         | Required | Description                     |
| ------ | ---------------------------- | -------- | ------------------------------- |
| `type` | `DotCMSInlineEditingType`    | ✅        | `'BLOCK_EDITOR'` or `'WYSIWYG'` |
| `data` | `DotCMSInlineEditingPayload` | ✅        | Field data and config           |

**Returns:**

- `void` - Returns nothing.

```ts
initInlineEditing('WYSIWYG', {
  inode,
  contentType,
  fieldName: 'body',
  content
});
```

**Editor Integration**

* Automatically detects UVE and applies inline editing UI.

**Common Issues**

* Requires valid contentlet and field name.

---

#### `enableBlockEditorInline(contentlet, fieldName)`

**Overview**

Shortcut to enable inline block editing for a field.

**Parameters**:

| Name         | Type                    | Required | Description                     |
| ------------ | ----------------------- | -------- | ------------------------------- |
| `contentlet` | `DotCMSBasicContentlet` | ✅        | The target contentlet           |
| `fieldName`  | `string`                | ✅        | Name of the block field to edit |

**Returns:**

- `void` - Returns nothing.

**Editor Integration**

* Only works with Block Editor fields.

**Common Issues**

* Will not work with invalid field names or non-block content.

```ts
enableBlockEditorInline(contentlet, 'blockContent');
```

---

### Navigation & Menu

#### `updateNavigation(pathname)`

**Overview**

Notifies UVE that navigation has changed (e.g., in SPAs).

**Parameters**:

| Name        | Type     | Required | Description                        |
| ----------- | -------- | -------- | ---------------------------------- |
| `pathname`  | `string` | ✅        | The new pathname to update         |

**Returns:**

- `void` - Returns nothing.

```ts
useEffect(() => {
  updateNavigation(location.pathname);
}, [location.pathname]);
```

**Editor Integration**

* Required to sync routing with the editor's current page.

**Common Issues**

* Without this, navigation may break content sync in UVE.

---

#### `reorderMenu(config?)`

**Overview**

Triggers the UVE menu editor to reorder navigation links.

**Parameters**:

| Name     | Type     | Required | Description                        |
| -------- | -------- | -------- | ---------------------------------- |
| `config` | `DotCMSReorderMenuConfig` | ❌       | Optional config for reordering     |

**Returns:**

- `void` - Returns nothing.

```ts
reorderMenu({ startLevel: 2, depth: 3 });
```

**DotCMSReorderMenuConfig**

- `startLevel` (number): The level to start reordering from
- `depth` (number): The depth of the menu to reorder

**Editor Integration**

* Opens the navigation panel within UVE.

**Common Issues**

* Must be called from a UI action — cannot auto-trigger.


---

### Communication Utilities

#### `sendMessageToUVE(message)`

**Overview**

Low-level function to send custom messages to UVE.

**Parameters**:

| Name      | Type                  | Required | Description                  |
| --------- | --------------------- | -------- | ---------------------------- |
| `message` | `DotCMSUVEMessage<T>` | ✅        | Object with action + payload |


```ts
sendMessageToUVE({
  action: DotCMSUVEAction.CUSTOM_EVENT,
  payload: { type: 'MyEvent', data: {...} }
});
```

**DotCMSUVEMessage<T>**

- `action` (DotCMSUVEAction): The action to perform
- `payload` (T): The payload for the action



**Editor Integration**

* Primarily for advanced cases — use higher-level APIs when possible.

**Common Issues**

* Incorrect payload structure may silently fail.

---

## TypeScript Support

The UVE SDK is written in TypeScript and provides comprehensive type definitions. All interfaces and types are available through the `@dotcms/types` package:

```bash
npm install @dotcms/types@next
```

### Common Types

The SDK uses several key types from `@dotcms/types`:

```typescript
import { 
  DotCMSContentlet,
  DotCMSPageResponse,
  DotCMSUVEConfig,
  DotCMSInlineEditingType,
  UVEEventType,
  UVEState
} from '@dotcms/types';
```

For a complete reference of all available types and interfaces, please refer to the [@dotcms/types documentation](https://www.npmjs.com/package/@dotcms/types).

---

## dotCMS Support

Need help? We offer multiple channels to get help with the dotCMS React SDK:

* 🐛 **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.
* 🧠 **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions.
* ❓ **Stack Overflow**: Use the tag `dotcms-react` when posting questions.
* 🛠️ **Enterprise Support**: Available via the [Support Portal](https://helpdesk.dotcms.com/).

When reporting issues, please include:
- SDK version you're using
- React version
- Minimal reproduction steps
- Expected vs. actual behavior

Enterprise customers can access premium support through the [dotCMS Support Portal](https://helpdesk.dotcms.com/).

---

## How To Contribute

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the DotCMS UVE SDK! If you'd like to contribute, please follow these steps:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`) 
5. Open a Pull Request

✅ Please include relevant tests and follow the coding style.

---

## Licensing Information

dotCMS comes in multiple editions and as such is dual licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds a number of enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).
