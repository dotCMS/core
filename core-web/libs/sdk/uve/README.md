# dotCMS UVE SDK

The @dotcms/uve SDK adds live editing to your JavaScript app using the dotCMS Universal Visual Editor (UVE). It gives you low-level tools to make pages and contentlets editable, trigger modals or inline editors, and respond to real-time changes.

Use this SDK directly or as the foundation for a framework-specific integration. With it, you can:

-   Make pages and contentlets editable
-   Respond to editor events (content updates, mode changes)
-   Trigger modal or inline editing experiences
-   Sync app routing with the dotCMS editor

## Table of Contents

-   [Before You Use @dotcms/uve](#before-you-use-dotcmsuve)
    -   [Getting Started: Recommended Examples](#getting-started-recommended-examples)
    -   [🚩 Custom Setup: Manual Rendering (Not Recommended)](#-custom-setup-manual-rendering-not-recommended)
-   [Prerequisites & Setup](#prerequisites--setup)
    -   [Get a dotCMS Instance](#get-a-dotcms-instance)
    -   [Create a dotCMS API Key](#create-a-dotcms-api-key)
    -   [Installation](#installation)
    -   [TypeScript Support](#typescript-support)
-   [SDK Reference](#sdk-reference)
    -   [`initUVE()`](#inituveconfig-dotcmsuveconfig)
    -   [`getUVEState()`](#getuvestate)
    -   [`createUVESubscription()`](#createuvesubscriptioneventtype-callback)
    -   [`editContentlet()`](#editcontentletcontentlet)
    -   [`initInlineEditing()`](#initinlineeditingtype-data)
    -   [`enableBlockEditorInline()`](#enableblockeditorinlinecontentlet-fieldname)
    -   [`updateNavigation()`](#updatenavigationpathname)
    -   [`reorderMenu()`](#reordermenuconfig)
    -   [`sendMessageToUVE()`](#sendmessagetouvemessage)
-   [Troubleshooting](#troubleshooting)
    -   [Common Issues & Solutions](#common-issues--solutions)
    -   [Debugging Tips](#debugging-tips)
    -   [Still Having Issues?](#still-having-issues)
-   [dotCMS Support](#dotcms-support)
-   [How To Contribute](#how-to-contribute)
-   [Licensing Information](#licensing-information)
-   [Troubleshooting](#troubleshooting)

## Before You Use @dotcms/uve

### Getting Started: Recommended Examples

We strongly recommend using one of our official framework SDKs, which are designed to handle UVE integration, routing, rendering, and more—out of the box. These examples are the best way to get started:

* [dotCMS Angular SDK: Angular Example](https://github.com/dotCMS/core/tree/main/examples/angular) – Ideal for Angular apps 🅰️
* [dotCMS React SDK: NextJS Example](https://github.com/dotCMS/core/tree/main/examples/react) – Ideal for NextJS projects ⚛️
* [dotCMS React SDK: Astro Example](https://github.com/dotCMS/core/tree/main/examples/astro) – Ideal for Astro projects 🌌

These examples handle UVE integration, routing, rendering, and more—out of the box. **If you're building a headless dotCMS front-end, start there.**

### 🚩 Custom Setup: Manual Rendering (Not Recommended)

You can use `@dotcms/uve` directly, but it’s not recommended or supported unless you’re building a highly custom integration. Here’s how the pieces fit together:

1. **You must use `@dotcms/client` to fetch content and page data.**
2. **You must render pages based on dotCMS’s layout schema.**
3. **You must apply the correct `data-dot-*` attributes to containers and contentlets.**

Here's a minimal setup using `@dotcms/client` and `@dotcms/uve`:

1. Initializa the Client and get the page response:

```ts
// getPage.ts
import { createDotCMSClient } from '@dotcms/client/next';
import { initUVE, createUVESubscription } from '@dotcms/uve/next';

const dotCMSClient = createDotCMSClient({
  dotcmsUrl: 'https://your-dotcms-instance.com',
  authToken: 'your-api-key',
  siteId: 'your-site-id'
});

const getPage = async () => {
  const pageResponse = await dotCMSClient.page.get('/', {
    languageId: '1'
  });

  return pageResponse;
}
```

2. Initialize the UVE and subscribe to changes:

> ⚠️ The `initUVE()` function only works with a `PageResponse` returned by `@dotcms/client`. If you try to pass in data from another source or build your own structure, it won't initialize properly.

```ts
import { initUVE, createUVESubscription } from '@dotcms/uve/next';
import { getPage } from './getPage';

const pageResponse = await getPage();

initUVE(pageResponse);
createUVESubscription('changes', (newPageResponse) => {
  // Handle page updates (e.g. re-render)
});

```

> ⚠️ This only sets up the editor connection. You are responsible for rendering the page structure (rows, columns, containers, contentlets) using your own UI components.

3. Create a custom render for the page:

```tsx
// 🔧 Render the page layout (you must implement this component)
<MyDotCMSPage pageAsset={pageResponse.pageAsset} />
```

> ⚠️ Below is a simplified breakdown of how dotCMS layouts are structured and how you might render them manually.

#### 🔄 How to Render a dotCMS Page

dotCMS pages are structured as nested layout objects:

* A `PageAsset` contains a `layout` object
* The `layout` includes rows, columns, containers, and contentlets

Here’s a basic pseudocode outline:

```jsx
<Page>
  {layout.body.rows.map(row => (
    <Row>
      {row.columns.map(column => (
        <Column>
          {column.containers.map(container => (
            <Container data-dot-object="container" ...>
              {container.contentlets.map(contentlet => (
                <Contentlet data-dot-object="contentlet" ...>
                  {renderContentletByType(contentlet)}
                </Contentlet>
              ))}
            </Container>
          ))}
        </Column>
      ))}
    </Row>
  ))}
</Page>
```

Each contentlet is rendered according to its content type:

```ts
function renderContentletByType(contentlet) {
  switch(contentlet.contentType) {
    case 'text': return <TextBlock contentlet={contentlet} />;
    case 'image': return <ImageBlock contentlet={contentlet} />;
    case 'video': return <VideoBlock contentlet={contentlet} />;
    default: return null;
  }
}
```

To make the layout editable, be sure to apply all required `data-dot-*` attributes on containers and contentlets.

For a complete guide, see this tutorial:
👉 [How to Build a Custom dotCMS Renderer](https://www.dotcms.com/blog/developer-tutorial-how-to-use-dotcms-and-react-to-build-single-page-apps)

---


> 💡 Unless you are building your own front-end framework (which we don’t recommend), use `@dotcms/react` or `@dotcms/angular`. These handle rendering and editor integration for you.

## Prerequisites & Setup

### Get a dotCMS Instance

**For Production Use:**

-   ☁️ [Cloud hosting options](https://www.dotcms.com/pricing) - managed solutions with SLA
-   🛠️ [Self-hosted options](https://dev.dotcms.com/docs/current-releases) - deploy on your infrastructure

**For Testing & Development:**

-   📝 [dotCMS demo site](https://dev.dotcms.com/docs/demo-site) - perfect for trying out the SDK
-   📝 Read-only access, ideal for building proof-of-concepts

**For Local Development:**

-   🐳 [Docker setup guide](https://github.com/dotCMS/core/tree/main/docker/docker-compose-examples/single-node-demo-site)
-   💻 [Local installation guide](https://dev.dotcms.com/docs/quick-start-guide)

#### Version Requirements

-   **Recommended**: dotCMS Evergreen
-   **Minimum**: dotCMS v25.05
-   **Best Experience**: Latest Evergreen release

### Configure The Universal Visual Editor App

For a step-by-step guide on setting up the Universal Visual Editor, check out our [easy-to-follow instructions](https://dev.dotcms.com/docs/uve-headless-config) and get started in no time!

### Installation

```bash
npm install @dotcms/uve@next
```

### TypeScript Support

All interfaces and types are available through the `@dotcms/types` package:

```bash
npm install @dotcms/types@next --save-dev
```

#### Common Types

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

## SDK Reference

### `initUVE(config?: DotCMSUVEConfig)`

**Overview**: Initializing UVE is the crucial first step to connect your application with the dotCMS Universal Visual Editor (UVE). It sets up the necessary communication between your app and the editor, enabling seamless integration and interaction.

**Parameters**:

| Name     | Type              | Required | Description                               |
| -------- | ----------------- | -------- | ----------------------------------------- |
| `config` | `DotCMSPageResponse` | ✅       | The page Response from the `@dotcms/client` |

**Usage:**

```ts
const { destroyUVESubscriptions } = initUVE(pageResponse);
```

> ⚠️ If you don't provide a `pageResponse`, we can't assure that the UVE will be initialized correctly.

### `getUVEState()`

**Overview**: Returns current editor state if UVE is active.

**Usage:**

```ts
const uveState = getUVEState();
if (state?.mode === 'edit') {
    showEditorUI();
}
```

**UVE State**

-   `dotCMSHost`: The host URL of the DotCMS instance
-   `experimentId`: The ID of the current experiment
-   `languageId`: The language ID of the current page set on the UVE
-   `mode`: The current editor mode (`'preview'`, `'edit'`, `'live'`)
-   `persona`: The persona of the current page set on the UVE
-   `publishDate`: The publish date of the current page set on the UVE
-   `variantName`: The name of the current variant

### `createUVESubscription(eventType, callback)`

**Overview**: The `createUVESubscription` function allows your application to dynamically interact with UVE by subscribing to events such as content changes or navigation updates. This enables your app to respond in real-time to user actions and editor events, enhancing the interactive experience.

**Parameters**:

| Name        | Type           | Required | Description                        |
| ----------- | -------------- | -------- | ---------------------------------- |
| `eventType` | `UVEEventType` | ✅       | The event to subscribe to          |
| `callback`  | `Function`     | ✅       | Called when the event is triggered |

**Usage:**

```ts
const sub = createUVESubscription(UVEEventType.CONTENT_CHANGES, updateFn);
sub.unsubscribe();
```

**Event Types**

-   `UVEEventType.CONTENT_CHANGES`: Triggered when the content of the page changes.
-   `UVEEventType.PAGE_RELOAD`: Triggered when the page is reloaded.
-   `UVEEventType.REQUEST_BOUNDS`: Triggered when the editor requests the bounds of the page.
-   `UVEEventType.IFRAME_SCROLL`: Triggered when the iframe is scrolled.
-   `UVEEventType.IFRAME_SCROLL_END`: Triggered when the iframe has stopped scrolling.
-   `UVEEventType.CONTENTLET_HOVERED`: Triggered when a contentlet is hovered.

### `editContentlet(contentlet)`

**Overview**: The `editContentlet` function opens the dotCMS modal editor for any contentlet in or out of page area.

**Parameters**:

| Name         | Type            | Required | Description                                                                                                           |
| ------------ | --------------- | -------- | --------------------------------------------------------------------------------------------------------------------- |
| `contentlet` | `Contentlet<T>` | ✅       | The `contentlet` you want to edit. <br> **Note:** The `contentlet` must be a valid contentlet in the dotCMS instance. |

```ts
editContentlet(myContentlet);
```

### `initInlineEditing(type, data)`

**Overview**: The `initInlineEditing` function triggers inline editing for supported field types (WYSIWYG or Block Editor).

**Parameters**:

| Name        | Type                         | Required | Description                     |
| ----------- | ---------------------------- | -------- | ------------------------------- |
| `type`      | `DotCMSInlineEditingType`    | ✅       | `'BLOCK_EDITOR'` or `'WYSIWYG'` |
| `fieldData` | `DotCMSInlineEditingPayload` | ✅       | Field data and config           |

**Usage:**

```ts
initInlineEditing('WYSIWYG', {
    inode,
    contentType,
    fieldName: 'body',
    content
});
```

**DotCMSInlineEditingPayload**

-   `inode` (string): The inode of the contentlet to edit.
-   `contentType` (string): The content type of the contentlet to edit.
-   `fieldName` (string): The name of the field to edit.
-   `content` (string): The content of the field to edit.

### `enableBlockEditorInline(contentlet, fieldName)`

**Overview**: The `enableBlockEditorInline` function is a shortcut to [enable inline block editing](https://dev.dotcms.com/docs/block-editor#BlockInlineEditor) for a field.

**Parameters**:

| Name         | Type                    | Required | Description                     |
| ------------ | ----------------------- | -------- | ------------------------------- |
| `contentlet` | `DotCMSBasicContentlet` | ✅       | The target contentlet           |
| `fieldName`  | `string`                | ✅       | Name of the block field to edit |

**Usage:**

```ts
enableBlockEditorInline(contentlet, 'blockContent');
```

### `updateNavigation(pathname)`

**Overview**: The `updateNavigation` function notifies UVE that navigation has changed (e.g., in SPAs).

**Parameters**:

| Name       | Type     | Required | Description                |
| ---------- | -------- | -------- | -------------------------- |
| `pathname` | `string` | ✅       | The new pathname to update |

**Usage:**

```ts
updateNavigation('/navigate-to-this-new-page');
```

### `reorderMenu(config?)`

**Overview**: The `reorderMenu` function triggers the UVE menu editor to reorder navigation links.

**Parameters**:

| Name     | Type                      | Required | Description                    |
| -------- | ------------------------- | -------- | ------------------------------ |
| `config` | `DotCMSReorderMenuConfig` | ❌       | Optional config for reordering |

**Usage:**

```ts
reorderMenu({ startLevel: 2, depth: 3 });
```

**DotCMSReorderMenuConfig**

-   `startLevel` (number): The level to start reordering from
-   `depth` (number): The depth of the menu to reorder

### `sendMessageToUVE(message)`

**Overview**: The `sendMessageToUVE` function is a low-level function to send custom messages to UVE.

**Parameters**:

| Name      | Type                  | Required | Description                  |
| --------- | --------------------- | -------- | ---------------------------- |
| `message` | `DotCMSUVEMessage<T>` | ✅       | Object with action + payload |

**Usage:**

```ts
sendMessageToUVE({
  action: DotCMSUVEAction.CUSTOM_EVENT,
  payload: { type: 'MyEvent', data: {...} }
});
```

**DotCMSUVEMessage<T>**

-   `action` (DotCMSUVEAction): The action to perform
-   `payload` (T): The payload for the action

## Troubleshooting

### Common Issues & Solutions

#### Memory Management

1. **Memory Leaks**: Application experiences memory leaks
    - **Possible Causes**:
        - Failing to call `destroyUVESubscriptions()` on unmount
    - **Solutions**:
        - Always call `destroyUVESubscriptions()` when your component unmounts to clean up subscriptions

#### Editor State

1. **Undefined State**: `getUVEState()` returns undefined
    - **Possible Causes**:
        - Application not running inside the dotCMS editor
    - **Solutions**:
        - Ensure your application is running within the dotCMS environment when calling `getUVEState()`

#### Event Handling

1. **Unsubscribed Events**: Events not unsubscribed leading to unexpected behavior
    - **Possible Causes**:
        - Not unsubscribing from events
    - **Solutions**:
        - Always unsubscribe from events using the `unsubscribe()` method to prevent memory leaks

#### Inline Editing

1. **Invalid Contentlet or Field**: `initInlineEditing()` requires valid contentlet and field name
    - **Possible Causes**:
        - Incorrect contentlet or field name
    - **Solutions**:
        - Verify that the contentlet and field name are correct and exist in the dotCMS instance

#### Non-existent Page Navigation

1. **Navigation to a non-existent page**: May break content sync in UVE and make the editor redirect to the home page
    - **Possible Causes**:
        - Navigation to a non-existent page
    - **Solutions**:
        - Ensure the page exists in the dotCMS instance

#### Menu Reordering

1. **UI Action Requirement**: `reorderMenu()` must be called from a UI action
    - **Possible Causes**:
        - Attempting to auto-trigger `reorderMenu()`
    - **Solutions**:
        - Ensure `reorderMenu()` is triggered by a user action within the UI

### Debugging Tips

1. **Ensure you are in the UVE Context**
    - Check if you are in the UVE context by calling `getUVEState()`
    - If you are not in the UVE context, you will not be able to use the UVE SDK correctly
2. **Check Browser Console**
    - Check for errors in the browser console
    - Check for errors in the browser network tab
2. **Network Monitoring**
    - Use browser dev tools to monitor API calls
    - Check for 401/403 errors (auth issues)
    - Verify asset loading paths

### Still Having Issues?

If you're still experiencing problems after trying these solutions:

1. Search existing [GitHub issues](https://github.com/dotCMS/core/issues)
2. Check our [community forum](https://community.dotcms.com/)
3. Create a new issue with:
    - Detailed reproduction steps
    - Environment information
    - Error messages
    - Code samples

## dotCMS Support

We offer multiple channels to get help with the dotCMS UVE SDK:

-   **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.
-   **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions.
-   **Stack Overflow**: Use the tag `dotcms-uve` when posting questions.

When reporting issues, please include:

-   SDK version you're using
-   dotCMS version
-   Minimal reproduction steps
-   Expected vs. actual behavior

Enterprise customers can access premium support through the [dotCMS Support Portal](https://dev.dotcms.com/docs/help).

## How To Contribute

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the DotCMS UVE SDK! If you'd like to contribute, please follow these steps:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

## Licensing Information

dotCMS comes in multiple editions and as such is dual-licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization, and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds several enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).

This SDK is part of dotCMS's dual-licensed platform (GPL 3.0 for Community, commercial license for Enterprise).

[Learn more ](https://www.dotcms.com)at [dotcms.com](https://www.dotcms.com).
