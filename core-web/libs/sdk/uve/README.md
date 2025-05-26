# dotCMS UVE SDK

The `@dotcms/uve` SDK is a powerful tool that enables live editing in your JavaScript applications by seamlessly integrating with the **dotCMS Universal Visual Editor (UVE)**. It provides a comprehensive low-level API to make pages and contentlets editable, respond to editor events, and support in-place editing, layout updates, and real-time content previews. Whether you're using a framework-specific library or the core SDK for custom implementations, `@dotcms/uve` offers the flexibility and control needed to enhance your app's editing capabilities. Use it to:

-   Make pages and contentlets editable
-   Respond to editor events (content updates, mode changes)
-   Trigger modal or inline editing experiences
-   Sync app routing with the dotCMS editor

## Table of Contents

-   [Prerequisites & Setup](#prerequisites--setup)
    -   [Get a dotCMS Instance](#get-a-dotcms-instance)
    -   [Create a dotCMS API Key](#create-a-dotcms-api-key)
    -   [Installation](#installation)
    -   [TypeScript Support](#typescript-support)
-   [Quickstart](#quickstart)
    -   [Important Notes](#important-notes)
    -   [Starter Example Project 🚀](#starter-example-project-)
-   [API Reference](#api-reference)
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

The Universal Visual Editor (UVE) is a powerful tool that allows you to edit your dotCMS content in real-time. To use the UVE, you need to configure the UVE application.

1. Go to the **dotCMS admin panel**.
2. Click on **Settings** > **Apps** > **Universal Visual Editor (UVE)**.
3. Select the dotCMS Site you want to use the UVE on.
4. Add the following basic configuration:

```json
{
    "config": [
        {
            "pattern": ".*",
            "url": "https://your-app-url.com"
        }
    ]
}
```

5. Click on **Save**.

For detailed instructions, please refer to the [Universal Visual Editor Configuration for Headless Pages
](https://dev.dotcms.com/docs/uve-headless-config).

### Installation

```bash
npm install @dotcms/uve@next
```

### TypeScript Support

The UVE SDK is written in TypeScript and provides comprehensive type definitions. All interfaces and types are available through the `@dotcms/types` package:

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

That's it! You can now start using the UVE SDK in your application.

## Quickstart

The `@dotcms/uve` SDK is a versatile toolkit that provides a low-level API for integrating with the dotCMS Universal Visual Editor (UVE). It doesn't enforce a specific workflow, allowing you to initialize the UVE editor and then freely call other functions as needed. This flexibility makes it ideal for custom implementations.

### Important Notes:

1. **dotCMS Integration**: This SDK is designed to work **exclusively within the dotCMS environment**, specifically inside the UVE App.
2. **Framework-Specific Libraries**: For higher-level integrations with frameworks like React or Angular, please refer to our [example projects](#starter-examples) for optimized implementations.

Here's a quick example to get you started:

```typescript
import { initUVE, editContentlet } from '@dotcms/uve';

// Initialize the UVE
const UVE_PARAMS = {
    languageId: '1',
    depth: 3
};

initUVE({ params: UVE_PARAMS });

// Get the edit button
const myEditButton = document.getElementById('my-edit-button');

// Add an event listener to the edit button
myEditButton.addEventListener('click', () => {
    const contentlet = document.getElementById('my-contentlet').dataset.contentlet;
    // Open the modal editor for the contentlet
    editContentlet(contentlet);
});
```

This example demonstrates how to initialize the UVE and interact with contentlets, providing a foundation for further customization and integration.

### Starter Example Project 🚀

Looking to get started quickly? We've got you covered! Here are some example projects that demonstrate how to use the UVE SDK with different frameworks:

-   [Angular Example](https://github.com/dotCMS/core/tree/main/examples/angular) - Integration with Angular 🅰️
-   [Next.js Example](https://github.com/dotCMS/core/tree/main/examples/nextjs) - Integration with Next.js ⚛️
-   [Astro Example](https://github.com/dotCMS/core/tree/main/examples/astro) - Integration with Astro 🌌

> [!TIP]
> These starter projects are more than just examples, they follow all our best practices. We highly recommend using them as the base for your next dotCMS Headless project! 💡

## API Reference

### `initUVE(config?: DotCMSUVEConfig)`

**Overview**: Initializing UVE is the crucial first step to connect your application with the dotCMS Universal Visual Editor (UVE). It sets up the necessary communication between your app and the editor, enabling seamless integration and interaction.

**Parameters**:

| Name     | Type              | Required | Description                               |
| -------- | ----------------- | -------- | ----------------------------------------- |
| `config` | `DotCMSUVEConfig` | ❌       | Optional setup for language, persona, etc |

**Usage:**

```ts
const { destroyUVESubscriptions } = initUVE({ params });
```

#### UVE Configuration

-   `graphql` (object - optional): The graphql query to execute
    -   `query` (string): The graphql query to execute
    -   `variables` (object): The variables to pass to the graphql query.
-   `params` (object - optional): The parameters to pass to the page API
    -   `languageId` (string): The language ID of the current page set on the UVE.
    -   `personaId` (string): The persona ID of the current page set on the UVE.
    -   `publishDate` (string): The publish date of the current page set on the UVE.
    -   `depth` (0-3): The depth of the current page set on the UVE.
    -   `fireRules` (boolean): Indicates whether you want to fire the rules set on the page.
    -   `variantName` (string): The name of the current variant.

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

**Overview**: The `enableBlockEditorInline` function is a shortcut to enable inline block editing for a field.

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
