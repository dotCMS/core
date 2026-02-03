# dotCMS UVE SDK

The `@dotcms/uve` SDK adds live editing to your JavaScript app using the dotCMS Universal Visual Editor (UVE). It provides low-level tools that power our framework-specific SDKs, such as [`@dotcms/react`](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/react/README.md) and [`@dotcms/angular`](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/angular/README.md).

> ⚠️ We **do not recommend using this SDK directly** for most use cases, you should use a [framework SDK that handles setup](#Getting Started: Recommended Examples), rendering, and event wiring for you.

With `@dotcms/uve`, framework SDKs are able to:
- Make pages and contentlets editable
- Respond to editor events (content updates, mode changes)
- Trigger modal or inline editing experiences
- Sync app routing with the dotCMS editor

## Table of Contents

-   [Before You Use @dotcms/uve](#before-you-use-dotcmsuve)
    -   [Getting Started: Recommended Examples](#getting-started-recommended-examples)
    -   [🚩 Custom Setup: Manual Rendering (Not Recommended)](#-custom-setup-manual-rendering-not-recommended)
-   [Prerequisites & Setup](#prerequisites--setup)
    -   [Get a dotCMS Environment](#get-a-dotcms-environment)
    -   [Create a dotCMS API Key](#create-a-dotcms-api-key)
    -   [Installation](#installation)
    -   [Using the SDK with TypeScript](#using-the-sdk-with-typescript)
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
-   [Style Editor](#style-editor)
    -   [What is the Style Editor?](#what-is-the-style-editor)
    -   [Quick Start](#quick-start)
    -   [`defineStyleEditorSchema()`](#definestyleeditorschemaform)
    -   [Field Types](#field-types)
        -   [`styleEditorField.input()`](#styleeditorfieldinputconfig)
        -   [`styleEditorField.dropdown()`](#styleeditorfielddropdownconfig)
        -   [`styleEditorField.radio()`](#styleditorfieldradioconfig)
        -   [`styleEditorField.checkboxGroup()`](#styleeditorfieldcheckboxgroupconfig)
    -   [`registerStyleEditorSchemas()`](#registerstyleeditorschemasschemas)
    -   [`useStyleEditorSchemas()` (React Hook)](#usestyleeditorschemasschemas-react-hook)
    -   [Accessing Style Values](#accessing-style-values)
    -   [Best Practices](#best-practices)
    -   [Complete Example](#complete-example)
    -   [Current Capabilities and Limitations](#current-capabilities-and-limitations)
-   [Troubleshooting](#troubleshooting)
    -   [Common Issues & Solutions](#common-issues--solutions)
    -   [Debugging Tips](#debugging-tips)
    -   [Still Having Issues?](#still-having-issues)
-   [Support](#support)
-   [Contributing](#contributing)
-   [Licensing](#licensing)

## Before You Use @dotcms/uve

### Getting Started: Recommended Examples

We strongly recommend using one of our official framework SDKs, which are designed to handle UVE integration, routing, rendering, and more—out of the box. These examples are the best way to get started:

-   [dotCMS Angular SDK: Angular Example](https://github.com/dotCMS/core/tree/main/examples/angular) – Ideal for Angular apps 🅰️
-   [dotCMS React SDK: NextJS Example](https://github.com/dotCMS/core/tree/main/examples/react) – Ideal for NextJS projects ⚛️
-   [dotCMS React SDK: Astro Example](https://github.com/dotCMS/core/tree/main/examples/astro) – Ideal for Astro projects 🌌

These examples handle UVE integration, routing, rendering, and more—out of the box. **If you're building a headless dotCMS front-end, start there.**

### 🚩 Custom Setup: Manual Rendering (Not Recommended)

> 💡 We recommend using one of our official framework SDKs, which are designed to handle UVE integration, routing, rendering, and more—out of the box.

You can use `@dotcms/uve` directly, but **it’s not recommended or supported** unless you’re building a highly custom integration. Here’s how the pieces fit together:

1. **You must use `@dotcms/client` to fetch content and page data.**
2. **You must render pages based on dotCMS’s layout schema.**
3. **You must apply the correct `data-dot-*` attributes to containers and contentlets.**

Here's a minimal setup using `@dotcms/client` and `@dotcms/uve`:

1. Initializa the Client and get the page response:

```ts
// getPage.ts
import { createDotCMSClient } from '@dotcms/client';
import { initUVE, createUVESubscription } from '@dotcms/uve';

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
};
```

2. Initialize the UVE and subscribe to changes:

> ⚠️ The `initUVE()` function only works with a `PageResponse` returned by `@dotcms/client`. If you try to pass in data from another source or build your own structure, it won't initialize properly.

```ts
import { initUVE, createUVESubscription } from '@dotcms/uve';
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

> 📚 For a complete guide, here is a full tutorial:
> 👉 [dotCMS Page Rendering Architecture]( https://dev.dotcms.com/docs/dotcms-page-rendering-architecture)

dotCMS pages are structured as nested layout objects:

-   A `PageAsset` contains a `layout` object
-   The `layout` includes rows, columns, containers, and contentlets

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

## Prerequisites & Setup

### Get a dotCMS Environment

#### Version Compatibility

-   **Recommended**: dotCMS Evergreen
-   **Minimum**: dotCMS v25.05
-   **Best Experience**: Latest Evergreen release

#### Environment Setup

**For Production Use:**

-   ☁️ [Cloud hosting options](https://www.dotcms.com/pricing) - managed solutions with SLA
-   🛠️ [Self-hosted options](https://dev.dotcms.com/docs/current-releases) - deploy on your infrastructure

**For Testing & Development:**

-   🧑🏻‍💻 [dotCMS demo site](https://demo.dotcms.com/dotAdmin/#/public/login) - perfect for trying out the SDK
-   📘 [Learn how to use the demo site](https://dev.dotcms.com/docs/demo-site)
-   📝 Read-only access, ideal for building proof-of-concepts

**For Local Development:**

-   🐳 [Docker setup guide](https://github.com/dotCMS/core/tree/main/docker/docker-compose-examples/single-node-demo-site)
-   💻 [Local installation guide](https://dev.dotcms.com/docs/quick-start-guide)

### Configure The Universal Visual Editor App

For a step-by-step guide on setting up the Universal Visual Editor, check out our [easy-to-follow instructions](https://dev.dotcms.com/docs/uve-headless-config) and get started in no time!

### Installation

```bash
npm install @dotcms/uve@latest
```

### Using the SDK with TypeScript

All interfaces and types are available through the `@dotcms/types` package:

```bash
npm install @dotcms/types@latest --save-dev
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

`initUVE` is a function that initializes the Universal Visual Editor (UVE). It sets up the necessary communication between your app and the editor, enabling seamless integration and interaction.

| Input    | Type                 | Required | Description                                 |
| -------- | -------------------- | -------- | ------------------------------------------- |
| `config` | `DotCMSPageResponse` | ✅       | The page Response from the `@dotcms/client` |

#### Usage

```ts
const { destroyUVESubscriptions } = initUVE(pageResponse);
```

> ⚠️ If you don't provide a `pageResponse`, we can't assure that the UVE will be initialized correctly.

### `getUVEState()`

`getUVEState` is a function that returns the [UVE state](#uve-state) if UVE is active.

#### Usage

```tsx
import { getUVEState } from '@dotcms/uve';
import { UVE_MODE } from '@dotcms/types';

const myEditButton = () => {
    const uveState = getUVEState();

    if (uveState?.mode === UVE_MODE.EDIT) {
        return <button>Edit</button>;
    }

    return null;
};
```

#### UVE State

-   `dotCMSHost`: The host URL of the DotCMS instance
-   `experimentId`: The ID of the current experiment
-   `languageId`: The language ID of the current page set on the UVE
-   `mode`: The current editor mode (`'preview'`, `'edit'`, `'live'`)
-   `persona`: The persona of the current page set on the UVE
-   `publishDate`: The publish date of the current page set on the UVE
-   `variantName`: The name of the current variant

### `createUVESubscription(eventType, callback)`

`createUVESubscription` is a function that allows your application to dynamically interact with UVE by subscribing to events such as content changes or navigation updates. This enables your app to respond in real-time to user actions and editor events, enhancing the interactive experience.

| Input       | Type           | Required | Description                               |
| ----------- | -------------- | -------- | ----------------------------------------- |
| `eventType` | `UVEEventType` | ✅       | [The event to subscribe to](#event-types) |
| `callback`  | `Function`     | ✅       | Called when the event is triggered        |

#### Usage

```ts
import { createUVESubscription } from '@dotcms/uve';
import { UVEEventType } from '@dotcms/types';

const sub = createUVESubscription(UVEEventType.CONTENT_CHANGES, (newPageResponse) => {
    // do something when the content changes
});

// Later, when you want to unsubscribe
sub.unsubscribe();
```

#### Event Types

-   `UVEEventType.CONTENT_CHANGES`: Triggered when the content of the page changes.
-   `UVEEventType.PAGE_RELOAD`: Triggered when the page is reloaded.
-   `UVEEventType.REQUEST_BOUNDS`: Triggered when the editor requests the bounds of the page.
-   `UVEEventType.IFRAME_SCROLL`: Triggered when the iframe is scrolled.
-   `UVEEventType.IFRAME_SCROLL_END`: Triggered when the iframe has stopped scrolling.
-   `UVEEventType.CONTENTLET_HOVERED`: Triggered when a contentlet is hovered.

### `editContentlet(contentlet)`

`editContentlet` is a function that opens the dotCMS modal editor for any contentlet in or out of page area.

| Input        | Type               | Required | Description                      |
| ------------ | ------------------ | -------- | -------------------------------- |
| `contentlet` | `Contentlet<T>` | ✅       | The contentlet you want to edit. |

#### Usage

```tsx
import { editContentlet, getUVEState } from '@dotcms/uve';
import { UVE_MODE } from '@dotcms/types';

const myEditButton = ({ contentlet }) => {
    const uveState = getUVEState();

    if (uveState?.mode === UVE_MODE.EDIT) {
        return <button onClick={() => editContentlet(contentlet)}>Edit</button>;
    }

    return null;
};
```

### `initInlineEditing(type, data)`

`initInlineEditing` is a function that triggers inline editing for supported field types (WYSIWYG or Block Editor).

| Input       | Type                         | Required | Description                                                                    |
| ----------- | ---------------------------- | -------- | ------------------------------------------------------------------------------ |
| `type`      | `DotCMSInlineEditingType`    | ✅       | `'BLOCK_EDITOR'` or `'WYSIWYG'`                                                |
| `fieldData` | `DotCMSInlineEditingPayload` | ✅       | [Field content required to enable inline editing](#dotcmsinlineeditingpayload) |

#### Usage

```ts
import { initInlineEditing, getUVEState } from "@dotcms/uve";
import { UVE_MODE } from "@dotcms/types";

const MyBanner = ({ contentlet }) => {
  const uveState = getUVEState();

  const handleClick = () => {
    if (uveState?.mode === UVE_MODE.EDIT) {
      const { inode, contentType, title } = contentlet;
      initInlineEditing("BLOCK_EDITOR", {
        inode,
        contentType,
        content: title,
        fieldName: "title",
      });
    }
  };
  return (
    <div>
      <h1 onClick={handleClick}>{contentlet.title}</h1>
      <p>{contentlet.description}</p>
    </div>
  );
};
```

#### DotCMSInlineEditingPayload

-   `inode` (string): The inode of the contentlet to edit.
-   `contentType` (string): The content type of the contentlet to edit.
-   `fieldName` (string): The name of the field to edit.
-   `content` (string): The content of the field to edit.

### `enableBlockEditorInline(contentlet, fieldName)`

`enableBlockEditorInline` is a shortcut to [enable inline block editing](https://dev.dotcms.com/docs/block-editor#BlockInlineEditor) for a field.

| Input        | Type                    | Required | Description                                                    |
| ------------ | ----------------------- | -------- | -------------------------------------------------------------- |
| `contentlet` | `DotCMSBasicContentlet` | ✅       | The target contentlet                                          |
| `fieldName`  | `string`                | ✅       | [Name of the block field to edit](#dotcmsinlineeditingpayload) |

#### Usage

```tsx
import { enableBlockEditorInline, getUVEState } from '@dotcms/uve';
import { UVE_MODE } from '@dotcms/types';

const MyBanner = ({ contentlet }) => {
    const uveState = getUVEState();

    const handleClick = () => {
        if (uveState?.mode === UVE_MODE.EDIT) {
            enableBlockEditorInline(contentlet, 'blockContent');
        }
    };

    return <MyBlockEditorRender onClick={handleClick} />;
};
```

### `updateNavigation(pathname)`

`updateNavigation` is a function that notifies UVE that navigation has changed (e.g., in SPAs).

| Input      | Type     | Required | Description                |
| ---------- | -------- | -------- | -------------------------- |
| `pathname` | `string` | ✅       | The new pathname to update |

#### Usage

```tsx
import { updateNavigation } from '@dotcms/uve';

updateNavigation('/navigate-to-this-new-page');
```

### `reorderMenu(config?)`

`reorderMenu` is a function that opens the UVE menu editor to reorder navigation links.

| Input     | Type                      | Required | Description                                                |
| --------- | ------------------------- | -------- | ---------------------------------------------------------- |
| `config?` | `DotCMSReorderMenuConfig` | ❌       | [Optional config for reordering](#dotcmsreordermenuconfig) |

#### Usage

```ts
import { reorderMenu } from '@dotcms/uve';

reorderMenu({ startLevel: 2, depth: 3 });
```

#### DotCMSReorderMenuConfig

-   `startLevel` (number): The level to start reordering from
-   `depth` (number): The depth of the menu to reorder

### `sendMessageToUVE(message)`

`sendMessageToUVE` is a low-level function to send custom messages to UVE.

| Input     | Type                                       | Required | Description                  |
| --------- | ------------------------------------------ | -------- | ---------------------------- |
| `message` | [`DotCMSUVEMessage<T>`](#dotcmsuvemessage) | ✅       | Object with action + payload |

#### Usage

```ts
sendMessageToUVE({
  action: DotCMSUVEAction.CUSTOM_EVENT,
  payload: { type: 'MyEvent', data: {...} }
});
```

#### DotCMSUVEMessage<T>

| Event (DotCMSUVEAction)            | Payload (T)                                                   |
| ---------------------------------- | ------------------------------------------------------------- | ------- |
| `NAVIGATION_UPDATE`                | `{ url: string }`                                             |
| `SET_BOUNDS`                       | `DotCMSContainerBound[]`                                      |
| `SET_CONTENTLET`                   | `DotCMSBasicContentlet`                                       |
| `IFRAME_SCROLL`                    | `'up'                                                         | 'down'` |
| `IFRAME_SCROLL_END`                | ---                                                           |
| `REORDER_MENU`                     | `DotCMSReorderMenuConfig`                                     |
| `INIT_INLINE_EDITING`              | `DotCMSInlineEditingPayload`                                  |
| `COPY_CONTENTLET_INLINE_EDITING`   | `{ dataset: { inode, language, fieldName: this.fieldName } }` |
| `UPDATE_CONTENTLET_INLINE_EDITING` | `{ content: string, dataset: { inode, langId, fieldName } }`  |
| `GET_PAGE_DATA`                    | ---                                                           |
| `CLIENT_READY`                     | ---                                                           |
| `EDIT_CONTENTLET`                  | `DotCMSBasicContentlet`                                       |

## Style Editor

### What is the Style Editor?

The Style Editor is a powerful feature that enables content authors and developers to define dynamic, real-time editable properties for contentlets within the Universal Visual Editor (UVE). This allows for live customization of component appearance, layout, typography, colors, and any other configurable aspects without requiring code changes or page reloads.

**Key Benefits:**

-   **Real-Time Visual Editing**: Modify component styles and see changes instantly in the editor
-   **Content-Specific Customization**: Different content types can have unique style schemas, and the same contentlet could have different styles depending on if it is located in a different container or page
-   **Developer-Controlled**: Developers define which properties are editable and how they're presented
-   **Flexible Configuration**: Support for text inputs, dropdowns, radio buttons, and checkbox groups
-   **Type-Safe**: Full TypeScript support with type inference for option values

**Use Cases:**

-   Adjust typography (font size, family, weight)
-   Configure layouts (grid columns, alignment, spacing)
-   Customize colors and themes
-   Toggle component features (borders, shadows, decorations)
-   Control responsive behavior
-   Modify animation settings

### Quick Start

**1. Install the required packages:**

```bash
npm install @dotcms/uve@latest
npm install @dotcms/types@latest --save-dev
```

**2. Define a style editor schema:**

```typescript
import { defineStyleEditorSchema, styleEditorField } from '@dotcms/uve';

const mySchema = defineStyleEditorSchema({
    contentType: 'BlogPost',
    sections: [
        {
            title: 'Typography',
            fields: [
                styleEditorField.dropdown({
                    id: 'font-size',
                    label: 'Font Size',
                    options: [
                        { label: 'Small (14px)', value: '14px' },
                        { label: 'Medium (16px)', value: '16px' },
                        { label: 'Large (18px)', value: '18px' }
                    ]
                })
            ]
        }
    ]
});
```

**3. Register the schema:**

**Using React:**

```typescript
import { useStyleEditorSchemas } from '@dotcms/react';

function MyComponent() {
    useStyleEditorSchemas([mySchema]);

    return <div>Your component content</div>;
}
```

**Using vanilla JavaScript:**

```typescript
import { registerStyleEditorSchemas } from '@dotcms/uve';

registerStyleEditorSchemas([mySchema]);
```

### `defineStyleEditorSchema(form)`

`defineStyleEditorSchema` creates a normalized style editor schema that UVE can process. It validates your form definition and converts it into the format expected by the Universal Visual Editor.

| Input  | Type               | Required | Description                                            |
| ------ | ------------------ | -------- | ------------------------------------------------------ |
| `form` | `StyleEditorForm` | ✅       | The form definition with content type, sections, and fields |

**Returns:** `StyleEditorFormSchema` - A normalized schema ready for registration with UVE

#### StyleEditorForm Structure

```typescript
interface StyleEditorForm {
    contentType: string; // The content type identifier
    sections: StyleEditorSection[]; // Array of form sections
}

interface StyleEditorSection {
    title: string; // Section heading displayed in the editor
    fields: StyleEditorField[]; // Array of field definitions
}
```

#### Usage

```typescript
import { defineStyleEditorSchema, styleEditorField } from '@dotcms/uve';

const schema = defineStyleEditorSchema({
    contentType: 'Activity',
    sections: [
        {
            title: 'Typography',
            fields: [
                styleEditorField.input({
                    id: 'heading-size',
                    label: 'Heading Size',
                    inputType: 'number',
                    placeholder: '24'
                }),
                styleEditorField.dropdown({
                    id: 'font-family',
                    label: 'Font Family',
                    options: ['Arial', 'Helvetica', 'Georgia']
                })
            ]
        },
        {
            title: 'Layout',
            fields: [
                styleEditorField.radio({
                    id: 'alignment',
                    label: 'Text Alignment',
                    options: ['Left', 'Center', 'Right']
                })
            ]
        }
    ]
});
```

**⚠️ Important Notes:**

-   Each field must have a unique `id` within the schema
-   The `contentType` must match the content type in your dotCMS instance
-   Schemas are only processed when UVE is in EDIT mode

### Field Types

The Style Editor supports four field types, each designed for specific use cases. Use the `styleEditorField` factory functions to create type-safe field definitions.

#### `styleEditorField.input(config)`

Creates a text or number input field for free-form entry.

**Configuration:**

```typescript
interface StyleEditorInputFieldConfig {
    id: string; // Unique identifier
    label: string; // Display label
    inputType: StyleEditorFieldInputType; // Input type
    placeholder?: string; // Optional placeholder text
}
```

**Use Cases:**

-   Custom values (e.g., font sizes, margins, colors)
-   Numeric settings (e.g., animation duration, opacity)
-   Text values (e.g., CSS class names, custom IDs)

**Examples:**

```typescript
// Number input for pixel values
styleEditorField.input({
    id: 'padding-top',
    label: 'Top Padding (px)',
    inputType: 'number',
    placeholder: '16'
});

// Text input for custom CSS
styleEditorField.input({
    id: 'custom-class',
    label: 'Custom CSS Class',
    inputType: 'text',
    placeholder: 'my-custom-style'
});

// Number input with decimal values
styleEditorField.input({
    id: 'opacity',
    label: 'Opacity',
    inputType: 'number',
    placeholder: '1.0'
});
```

#### `styleEditorField.dropdown(config)`

Creates a dropdown (select) field with predefined options. Users can select one value from the list.

**Configuration:**

```typescript
interface StyleEditorDropdownField {
    id: string; // Unique identifier
    label: string; // Display label
    options: StyleEditorOption[]; // Array of options
}

type StyleEditorOption = { label: string; value: string };
```

**Use Cases:**

-   Predefined sizes (e.g., small, medium, large)
-   Font families or style presets
-   Color themes
-   Any single-choice selection from a list

**Examples:**

```typescript
// Font size options
const FONT_SIZES = [
    { label: 'Extra Small (12px)', value: '12px' },
    { label: 'Small (14px)', value: '14px' },
    { label: 'Medium (16px)', value: '16px' },
    { label: 'Large (18px)', value: '18px' },
    { label: 'Extra Large (24px)', value: '24px' }
];

styleEditorField.dropdown({
    id: 'font-size',
    label: 'Font Size',
    options: FONT_SIZES
});

// Theme selection
styleEditorField.dropdown({
    id: 'theme',
    label: 'Color Theme',
    options: [
        { label: 'Light Theme', value: 'light' },
        { label: 'Dark Theme', value: 'dark' },
        { label: 'High Contrast', value: 'high-contrast' }
    ]
});
```


#### `styleEditorField.radio(config)`

Creates a radio button group for single-choice selection. Optionally supports images for visual selection.

**Configuration:**

```typescript
interface StyleEditorRadioField {
    id: string; // Unique identifier
    label: string; // Display label
    options: StyleEditorRadioOption[]; // Array of options
    columns?: 1 | 2; // Layout: 1 or 2 columns (default: 1)
}

type StyleEditorRadioOption = {
    label: string;
    value: string;
    imageURL?: string; // Optional preview image
};
```

**Use Cases:**

-   Layout selection with visual previews
-   Alignment options (left, center, right)
-   Style variants with images
-   Any single-choice where visual feedback is helpful

**Examples:**

```typescript
// Simple text options
styleEditorField.radio({
    id: 'text-align',
    label: 'Text Alignment',
    options: [
        { label: 'Left', value: 'left' },
        { label: 'Center', value: 'center' },
        { label: 'Right', value: 'right' },
        { label: 'Justify', value: 'justify' }
    ]
});

// Two-column layout with images
const LAYOUT_OPTIONS = [
    {
        label: 'Left Sidebar',
        value: 'left',
        imageURL: 'https://example.com/layouts/left-sidebar.png'
    },
    {
        label: 'Right Sidebar',
        value: 'right',
        imageURL: 'https://example.com/layouts/right-sidebar.png'
    },
    {
        label: 'Full Width',
        value: 'full',
        imageURL: 'https://example.com/layouts/full-width.png'
    },
    {
        label: 'Split View',
        value: 'split',
        imageURL: 'https://example.com/layouts/split-view.png'
    }
];

styleEditorField.radio({
    id: 'page-layout',
    label: 'Page Layout',
    columns: 2, // Display in 2-column grid
    options: LAYOUT_OPTIONS
});

// Font weight selection
styleEditorField.radio({
    id: 'font-weight',
    label: 'Font Weight',
    options: [
        { label: 'Normal', value: '400' },
        { label: 'Medium', value: '500' },
        { label: 'Semi-Bold', value: '600' },
        { label: 'Bold', value: '700' }
    ]
});
```

**💡 Image Guidelines:**

-   Use clear, recognizable preview images
-   Recommended size: 200x150px or similar aspect ratio
-   Use consistent image dimensions within a radio group
-   Images should clearly differentiate between options

#### `styleEditorField.checkboxGroup(config)`

Creates a group of checkboxes for multi-selection. Each checkbox returns a boolean value (checked/unchecked).

**Configuration:**

```typescript
interface StyleEditorCheckboxGroupField {
    id: string; // Unique identifier for the group
    label: string; // Display label for the group
    options: StyleEditorCheckboxOption[]; // Array of checkbox options
}

interface StyleEditorCheckboxOption {
    label: string; // Display text for the checkbox
    key: string; // Unique identifier (NOT 'value')
}
```

**⚠️ Important:** Checkbox options use `key` instead of `value` because the actual value is boolean (true/false).

**Use Cases:**

-   Text decorations (bold, italic, underline)
-   Feature toggles (enable shadows, borders, animations)
-   Multiple style attributes
-   Any multi-select boolean options

**Examples:**

```typescript
// Typography settings
styleEditorField.checkboxGroup({
    id: 'text-style',
    label: 'Text Style',
    options: [
        { label: 'Bold', key: 'bold' },
        { label: 'Italic', key: 'italic' },
        { label: 'Underline', key: 'underline' },
        { label: 'Strikethrough', key: 'strikethrough' }
    ]
});

// Component features
styleEditorField.checkboxGroup({
    id: 'component-features',
    label: 'Component Features',
    options: [
        { label: 'Show Shadow', key: 'shadow' },
        { label: 'Show Border', key: 'border' },
        { label: 'Enable Animation', key: 'animate' },
        { label: 'Rounded Corners', key: 'rounded' }
    ]
});

// Responsive behavior
styleEditorField.checkboxGroup({
    id: 'responsive',
    label: 'Responsive Options',
    options: [
        { label: 'Hide on Mobile', key: 'hide-mobile' },
        { label: 'Hide on Tablet', key: 'hide-tablet' },
        { label: 'Full Width on Mobile', key: 'full-width-mobile' }
    ]
});
```

**Return Value Structure:**

```typescript
// Example return value when checkboxes are checked
{
  "bold": true,
  "italic": false,
  "underline": true,
  "strikethrough": false
}
```

### `registerStyleEditorSchemas(schemas)`

`registerStyleEditorSchemas` registers one or more style editor schemas with UVE. This function should be called during your component initialization to make the schemas available in the editor.

| Input     | Type                        | Required | Description                                      |
| --------- | --------------------------- | -------- | ------------------------------------------------ |
| `schemas` | `StyleEditorFormSchema[]` | ✅       | Array of normalized schemas from `defineStyleEditorSchema` |

**Returns:** `void`

**Behavior:**

-   Only registers schemas when UVE is in **EDIT** mode
-   Silently returns if UVE is not in EDIT mode
-   Validates that each schema has a `contentType` property
-   Logs a warning and skips schemas without `contentType`
-   Sends validated schemas to UVE via internal messaging

#### Usage

```typescript
import { defineStyleEditorSchema, styleEditorField, registerStyleEditorSchemas } from '@dotcms/uve';

// Create schemas
const blogSchema = defineStyleEditorSchema({
    contentType: 'BlogPost',
    sections: [
        {
            title: 'Typography',
            fields: [
                styleEditorField.dropdown({
                    id: 'font-size',
                    label: 'Font Size',
                    options: ['14px', '16px', '18px']
                })
            ]
        }
    ]
});

const activitySchema = defineStyleEditorSchema({
    contentType: 'Activity',
    sections: [
        {
            title: 'Layout',
            fields: [
                styleEditorField.radio({
                    id: 'layout',
                    label: 'Layout',
                    options: ['Left', 'Right', 'Center']
                })
            ]
        }
    ]
});

// Register multiple schemas at once
registerStyleEditorSchemas([blogSchema, activitySchema]);
```

**⚠️ Important Notes:**

-   Call this function after UVE initialization (`initUVE`)
-   Schemas are only processed in EDIT mode
-   Missing `contentType` will cause the schema to be skipped
-   You can register multiple schemas for different content types

### `useStyleEditorSchemas(schemas)` (React Hook)

**Available in:** `@dotcms/react` package

`useStyleEditorSchemas` is a React hook that simplifies schema registration by automatically handling the component lifecycle. It registers schemas when the component mounts and re-registers if the schemas array reference changes.

| Input    | Type                        | Required | Description                     |
| -------- | --------------------------- | -------- | ------------------------------- |
| `schemas` | `StyleEditorFormSchema[]` | ✅       | Array of normalized form schemas |

**Returns:** `void`

**Behavior:**

-   Registers schemas on component mount
-   Re-registers when the `schemas` array reference changes
-   Internally calls `registerStyleEditorSchemas()`
-   Safe to call in multiple components

#### Usage

```typescript
import { useStyleEditorSchemas } from '@dotcms/react';
import { defineStyleEditorSchema, styleEditorField } from '@dotcms/uve';

function BlogPostEditor() {
    // Define schemas
    const schemas = [
        defineStyleEditorSchema({
            contentType: 'BlogPost',
            sections: [
                {
                    title: 'Typography',
                    fields: [
                        styleEditorField.dropdown({
                            id: 'font-size',
                            label: 'Font Size',
                            options: [
                                { label: '14px', value: '14px' },
                                { label: '16px', value: '16px' },
                                { label: '18px', value: '18px' },
                                { label: '24px', value: '24px' }
                            ]
                        }),
                        styleEditorField.radio({
                            id: 'font-weight',
                            label: 'Font Weight',
                            options: [
                                { label: 'Normal', value: 'normal' },
                                { label: 'Bold', value: 'bold' }
                            ]
                        })
                    ]
                }
            ]
        })
    ];

    // Register schemas automatically
    useStyleEditorSchemas(schemas);

    return (
        <div>
            <h1>Blog Post Editor</h1>
            {/* Your component content */}
        </div>
    );
}
```

**💡 Performance Tip:** For better performance in components that re-render frequently, you can optionally use `useMemo` to prevent re-creating the schema on every render:

```typescript
import { useMemo } from 'react';

function BlogPostEditor() {
    const schemas = useMemo(
        () => [
            defineStyleEditorSchema({
                /* schema definition */
            })
        ],
        [] // Empty deps = create once
    );

    useStyleEditorSchemas(schemas);
    return <div>Content</div>;
}
```

### Accessing Style Values

Style Editor values are managed internally by UVE and passed to your components through the `dotStyleProperties` attribute. This attribute is available in your contentlet component props.

#### In React Components

When rendering contentlets, style properties are accessed through the `dotStyleProperties` prop:

```typescript
import { DotCMSContentlet } from '@dotcms/types';

interface ActivityProps {
    contentlet: DotCMSContentlet;
    dotStyleProperties?: Record<string, any>;
}

function Activity(props: ActivityProps) {
    const { title, description, dotStyleProperties } = props; // Contentlet information

    // Access style values using dot notation or bracket notation
    const fontSize = dotStyleProperties?.['font-size'];
    const textAlign = dotStyleProperties?.text;
    const layout = dotStyleProperties?.layout;

    return (
        <div style={{ fontSize, textAlign }}>
            <h1>{title}</h1>
            <p>{description}</p>
        </div>
    );
}
```

#### Value Types by Field Type

**Input Field:**

```typescript
// Returns: string (text) or number (number input)
const fontSize: string = '16px';
const padding: number = 24;
```

**Dropdown Field:**

```typescript
// Returns: string (the selected value)
const theme: string = 'light';
const fontFamily: string = 'Arial';
```

**Radio Field:**

```typescript
// Returns: string (the selected value)
const layout: string = 'left';
const alignment: string = 'center';
```

**Checkbox Group:**

```typescript
// Returns: Record<string, boolean> (object with key-value pairs)
const textStyles: Record<string, boolean> = {
    bold: true,
    italic: false,
    underline: true,
    strikethrough: false
};

// Access individual values
if (textStyles.bold) {
    // Apply bold styling
}
```

#### Applying Style Values

Use the style values to conditionally render styles, classes, or component variants:

```typescript
function BlogPost(props) {
    const { title, body, dotStyleProperties } = props;

    // Example: Apply dynamic font size
    const fontSize = dotStyleProperties?.['font-size'] || '16px';

    // Example: Apply layout classes
    const layout = dotStyleProperties?.layout || 'default';
    const layoutClass = `layout-${layout}`;

    // Example: Apply checkbox group values
    const textStyles = dotStyleProperties?.['text-style'] || {};
    const textStyleClasses = [
        textStyles.bold ? 'font-bold' : '',
        textStyles.italic ? 'font-italic' : '',
        textStyles.underline ? 'text-underline' : ''
    ]
        .filter(Boolean)
        .join(' ');

    return (
        <div className={`${layoutClass} ${textStyleClasses}`} style={{ fontSize }}>
            <h1>{title}</h1>
            <p>{body}</p>
        </div>
    );
}
```

**💡 Note:** The `dotStyleProperties` prop is automatically passed to your contentlet components by the framework SDK when UVE is active and style schemas are registered.

### Best Practices

#### 1. Use Meaningful IDs and Labels

```typescript
// ✅ Good: Clear, descriptive IDs and labels
styleEditorField.dropdown({
    id: 'heading-font-size',
    label: 'Heading Font Size',
    options: [
        { label: 'Small (18px)', value: '18px' },
        { label: 'Medium (24px)', value: '24px' },
        { label: 'Large (32px)', value: '32px' }
    ]
});

// ❌ Bad: Vague IDs and labels
styleEditorField.dropdown({
    id: 'size',
    label: 'Size',
    options: ['18px', '24px', '32px']
});
```

#### 2. Group Related Fields in Sections

```typescript
// ✅ Good: Logical grouping by functionality
defineStyleEditorSchema({
    contentType: 'BlogPost',
    sections: [
        {
            title: 'Typography',
            fields: [
                /* font-related fields */
            ]
        },
        {
            title: 'Layout',
            fields: [
                /* layout-related fields */
            ]
        },
        {
            title: 'Colors',
            fields: [
                /* color-related fields */
            ]
        }
    ]
});

// ❌ Bad: All fields in one section
defineStyleEditorSchema({
    contentType: 'BlogPost',
    sections: [
        {
            title: 'Settings',
            fields: [
                /* all fields mixed together */
            ]
        }
    ]
});
```

#### 3. Provide Clear Option Labels

```typescript
// ✅ Good: Descriptive labels with context
styleEditorField.dropdown({
    id: 'font-size',
    label: 'Font Size',
    options: [
        { label: 'Extra Small (12px)', value: '12px' },
        { label: 'Small (14px)', value: '14px' },
        { label: 'Medium (16px)', value: '16px' },
        { label: 'Large (18px)', value: '18px' }
    ]
});

// ❌ Bad: Unclear labels
styleEditorField.dropdown({
    id: 'font-size',
    label: 'Font Size',
    options: ['XS', 'S', 'M', 'L']
});
```

#### 4. Use Appropriate Field Types

```typescript
// ✅ Good: Radio with images for visual layouts
styleEditorField.radio({
    id: 'page-layout',
    label: 'Layout',
    columns: 2,
    options: [
        { label: 'Left', value: 'left', imageURL: '...' },
        { label: 'Right', value: 'right', imageURL: '...' }
    ]
});

// ✅ Good: Dropdown for text-only options
styleEditorField.dropdown({
    id: 'font-family',
    label: 'Font',
    options: ['Arial', 'Georgia', 'Verdana']
});

// ✅ Good: Checkbox group for boolean flags
styleEditorField.checkboxGroup({
    id: 'text-decorations',
    label: 'Text Decorations',
    options: [
        { label: 'Bold', key: 'bold' },
        { label: 'Italic', key: 'italic' }
    ]
});
```

#### 5. Validate Content Type Matching

```typescript
// ✅ Good: Content type matches your dotCMS content type
defineStyleEditorSchema({
    contentType: 'BlogPost', // Matches content type in dotCMS
    sections: [
        /* ... */
    ]
});

// ❌ Bad: Typo or mismatch
defineStyleEditorSchema({
    contentType: 'blog-post', // Won't match 'BlogPost' in dotCMS
    sections: [
        /* ... */
    ]
});
```

#### 6. Provide Sensible Defaults

When using style properties, always provide fallback defaults:

```typescript
// ✅ Good: Fallback values prevent errors
const fontSize = dotStyleProperties?.['font-size'] || '16px';
const layout = dotStyleProperties?.layout || 'default';
const textStyles = dotStyleProperties?.['text-style'] || {};

// ❌ Bad: No fallbacks (could cause errors)
const fontSize = dotStyleProperties?.['font-size'];
const layout = dotStyleProperties?.layout;
```

### Complete Example

Here's a comprehensive example demonstrating all Style Editor features:

```typescript
import { useStyleEditorSchemas } from '@dotcms/react';
import { defineStyleEditorSchema, styleEditorField } from '@dotcms/uve';

export function BlogPostStyleEditor() {
    // Define option constants
    const FONT_SIZES = [
        { label: 'Extra Small (12px)', value: '12px' },
        { label: 'Small (14px)', value: '14px' },
        { label: 'Medium (16px)', value: '16px' },
        { label: 'Large (18px)', value: '18px' },
        { label: 'Extra Large (24px)', value: '24px' },
        { label: 'Huge (32px)', value: '32px' }
    ];

    const FONT_FAMILIES = [
        { label: 'Arial', value: 'arial' },
        { label: 'Georgia', value: 'georgia' },
        { label: 'Helvetica', value: 'helvetica' },
        { label: 'Times New Roman', value: 'times' },
        { label: 'Verdana', value: 'verdana' },
        { label: 'Courier New', value: 'courier' }
    ];

    const LAYOUT_OPTIONS = [
        {
            label: 'Left Sidebar',
            value: 'sidebar-left',
            imageURL: 'https://example.com/layouts/sidebar-left.png'
        },
        {
            label: 'Right Sidebar',
            value: 'sidebar-right',
            imageURL: 'https://example.com/layouts/sidebar-right.png'
        },
        {
            label: 'Full Width',
            value: 'full-width',
            imageURL: 'https://example.com/layouts/full-width.png'
        },
        {
            label: 'Centered',
            value: 'centered',
            imageURL: 'https://example.com/layouts/centered.png'
        }
    ];

    const COLOR_THEMES = [
        { label: 'Light Theme', value: 'light' },
        { label: 'Dark Theme', value: 'dark' },
        { label: 'High Contrast', value: 'high-contrast' },
        { label: 'Sepia', value: 'sepia' }
    ];

    // Define schema (optionally use useMemo to prevent re-creation on every render)
    const schemas = [
            defineStyleEditorSchema({
                contentType: 'BlogPost',
                sections: [
                    {
                        title: 'Typography',
                        fields: [
                            styleEditorField.dropdown({
                                id: 'heading-font-size',
                                label: 'Heading Font Size',
                                options: FONT_SIZES
                            }),
                            styleEditorField.dropdown({
                                id: 'body-font-size',
                                label: 'Body Font Size',
                                options: FONT_SIZES.slice(0, 4) // Only smaller sizes
                            }),
                            styleEditorField.dropdown({
                                id: 'font-family',
                                label: 'Font Family',
                                options: FONT_FAMILIES
                            }),
                            styleEditorField.input({
                                id: 'line-height',
                                label: 'Line Height',
                                inputType: 'number',
                                placeholder: '1.5'
                            }),
                            styleEditorField.checkboxGroup({
                                id: 'text-style',
                                label: 'Text Style',
                                options: [
                                    { label: 'Bold Headings', key: 'bold-headings' },
                                    { label: 'Italic Quotes', key: 'italic-quotes' },
                                    { label: 'Underline Links', key: 'underline-links' }
                                ]
                            })
                        ]
                    },
                    {
                        title: 'Layout',
                        fields: [
                            styleEditorField.radio({
                                id: 'page-layout',
                                label: 'Page Layout',
                                columns: 2,
                                options: LAYOUT_OPTIONS
                            }),
                            styleEditorField.radio({
                                id: 'content-width',
                                label: 'Content Width',
                                options: [
                                    { label: 'Narrow (800px)', value: '800px' },
                                    { label: 'Medium (1000px)', value: '1000px' },
                                    { label: 'Wide (1200px)', value: '1200px' },
                                    { label: 'Extra Wide (1400px)', value: '1400px' }
                                ]
                            }),
                            styleEditorField.input({
                                id: 'section-spacing',
                                label: 'Section Spacing (px)',
                                inputType: 'number',
                                placeholder: '40'
                            })
                        ]
                    },
                    {
                        title: 'Colors & Theme',
                        fields: [
                            styleEditorField.dropdown({
                                id: 'color-theme',
                                label: 'Color Theme',
                                options: COLOR_THEMES
                            }),
                            styleEditorField.input({
                                id: 'primary-color',
                                label: 'Primary Color',
                                inputType: 'text',
                                placeholder: '#007bff'
                            }),
                            styleEditorField.input({
                                id: 'secondary-color',
                                label: 'Secondary Color',
                                inputType: 'text',
                                placeholder: '#6c757d'
                            }),
                            styleEditorField.input({
                                id: 'background-color',
                                label: 'Background Color',
                                inputType: 'text',
                                placeholder: '#ffffff'
                            })
                        ]
                    },
                    {
                        title: 'Component Features',
                        fields: [
                            styleEditorField.checkboxGroup({
                                id: 'features',
                                label: 'Enable Features',
                                options: [
                                    { label: 'Drop Shadow', key: 'shadow' },
                                    { label: 'Border', key: 'border' },
                                    { label: 'Rounded Corners', key: 'rounded' },
                                    { label: 'Smooth Animations', key: 'animate' },
                                    { label: 'Hover Effects', key: 'hover-effects' }
                                ]
                            }),
                            styleEditorField.checkboxGroup({
                                id: 'responsive',
                                label: 'Responsive Options',
                                options: [
                                    { label: 'Hide on Mobile', key: 'hide-mobile' },
                                    { label: 'Stack on Tablet', key: 'stack-tablet' },
                                    {
                                        label: 'Full Width on Mobile',
                                        key: 'full-width-mobile'
                                    }
                                ]
                            })
                        ]
                    }
                ]
            })
        ];

    // Register schemas with UVE
    useStyleEditorSchemas(schemas);

    return (
        <div>
            <h1>Blog Post Style Editor</h1>
            <p>Style editor schema is registered and available in UVE edit mode.</p>
        </div>
    );
}

// Example: Using style properties in a component
export function BlogPostRenderer(props) {
    const { title, body, dotStyleProperties } = props;

    // Extract style values with defaults
    const headingSize = dotStyleProperties?.['heading-font-size'] || '24px';
    const bodySize = dotStyleProperties?.['body-font-size'] || '16px';
    const fontFamily = dotStyleProperties?.['font-family'] || 'arial';
    const lineHeight = dotStyleProperties?.['line-height'] || '1.5';
    const layout = dotStyleProperties?.['page-layout'] || 'full-width';
    const contentWidth = dotStyleProperties?.['content-width'] || '1000px';
    const sectionSpacing = dotStyleProperties?.['section-spacing'] || 40;
    const theme = dotStyleProperties?.['color-theme'] || 'light';
    const primaryColor = dotStyleProperties?.['primary-color'] || '#007bff';
    const backgroundColor = dotStyleProperties?.['background-color'] || '#ffffff';

    // Extract checkbox group values
    const textStyle = dotStyleProperties?.['text-style'] || {};
    const features = dotStyleProperties?.features || {};
    const responsive = dotStyleProperties?.responsive || {};

    // Build CSS classes based on values
    const containerClasses = [
        `layout-${layout}`,
        `theme-${theme}`,
        features.shadow ? 'has-shadow' : '',
        features.border ? 'has-border' : '',
        features.rounded ? 'has-rounded' : '',
        features.animate ? 'has-animations' : '',
        responsive['hide-mobile'] ? 'hide-mobile' : '',
        responsive['stack-tablet'] ? 'stack-tablet' : ''
    ]
        .filter(Boolean)
        .join(' ');

    return (
        <div
            className={containerClasses}
            style={{
                fontFamily,
                lineHeight,
                backgroundColor,
                maxWidth: contentWidth,
                paddingTop: `${sectionSpacing}px`,
                paddingBottom: `${sectionSpacing}px`
            }}
        >
            <h1
                style={{
                    fontSize: headingSize,
                    fontWeight: textStyle['bold-headings'] ? 'bold' : 'normal',
                    color: primaryColor
                }}
            >
                {title}
            </h1>

            <div
                style={{
                    fontSize: bodySize
                }}
            >
                {body}
            </div>
        </div>
    );
}
```

**This example demonstrates:**

-   ✅ Organized option constants
-   ✅ Logical section grouping (Typography, Layout, Colors, Features)
-   ✅ All four field types (input, dropdown, radio, checkboxGroup)
-   ✅ Visual layout selection with images
-   ✅ Checkbox groups for boolean flags
-   ✅ Clear, descriptive labels
-   ✅ Safe value extraction with defaults using `dotStyleProperties`
-   ✅ Dynamic styling based on style values

### Current Capabilities and Limitations:

When **defining styles** for a contentlet within a page using **Style Editor**, the following behaviors might occur:

| Scenario                                          | Behavior                                                                          | Result                                      |
|---------------------------------------------------|-----------------------------------------------------------------------------------|---------------------------------------------|
| Same Contentlet, Different Containers, Same Page  | Page A: { Container_1: contentlet_1, Container_2: contentlet_1 }                  | 🎨 Styles are different                     |
| Same Contentlet, Same Container, Different Pages  | Page A: { Container_1: contentlet_1 }, Page B: { Container_1: contentlet_1 }      | 🎨 Styles are different                     |
| Copying a Page with Styled Content                | Creating Page B as a copy of Page A, where Page A includes styled content         | ✅ Styles preserved, 🎨 Styles are different |
| Moving Styled Content to Same Container Type      | system-container → system-container                                               | ✅ Styles preserved                          |
| Moving Styled Content to Different Container Type | system-container → custom-container                                               | ⚠️ Styles lost                              |
| Adding, Deleting, or Moving Unstyled Content      | Performing any structural change on the page that does not involve styled content | if any: ✅ Styles preserved                  |

> **NOTE:** (🎨 Styles are different) means the capability to define distinct styles, even when utilizing the identical Contentlet.

The only known limitation is that moving a contentlet with defined styles between different container types (5th scenario), results in the loss of those styles. See the [technical details document](https://docs.google.com/document/d/1UiuJlIn8ZjybIB-0oHeoTLXZo1k-YExITEqEMfyvVlU/edit?tab=t.0) for our planned solution.

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
3. **Network Monitoring**
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

## Support

We offer multiple channels to get help with the dotCMS UVE SDK:

-   **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository
-   **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions
-   **Stack Overflow**: Use the tag `dotcms-uve` when posting questions
-   **Enterprise Support**: Enterprise customers can access premium support through the [dotCMS Support Portal](https://helpdesk.dotcms.com/support/)

When reporting issues, please include:

-   SDK version you're using
-   dotCMS version
-   Minimal reproduction steps
-   Expected vs. actual behavior

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the dotCMS UVE SDK! If you'd like to contribute, please follow these steps:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

## Licensing

dotCMS comes in multiple editions and as such is dual-licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization, and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds several enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).

This SDK is part of dotCMS's dual-licensed platform (GPL 3.0 for Community, commercial license for Enterprise).

[Learn more ](https://www.dotcms.com)at [dotcms.com](https://www.dotcms.com).
