# dotCMS React SDK

The `@dotcms/react` SDK is the official React integration library for dotCMS, designed to empower React developers to build powerful, editable websites and applications with minimal effort.

## Table of Contents

* [Quickstart](#quickstart)
* [Example Project](#example-project)
* [What Is It?](#what-is-it)
* [Installation](#installation)
* [Key Concepts](#key-concepts)
* [API Reference](#api-reference)

  * [Components](#components)

    * [DotCMSLayoutBody](#dotcmslayoutbody)
    * [DotCMSShow](#dotcmsshow)
    * [DotCMSBlockEditorRenderer](#dotcmsblockeditorrenderer)
    * [DotCMSEditableText](#dotcmseditabletext)
  * [Hooks](#hooks)

    * [useEditableDotCMSPage](#useeditabledotcmspage)
    * [useDotCMSShowWhen](#usedotcmsshowwhen)
* [FAQ](#faq)
  * [What are the differences between UVE modes?](#what-are-the-differences-between-uve-modes)
  * [What if my components don’t render?](#what-if-my-components-dont-render)
  * [How do I use dotCMS React components with Next.js App Router?](#how-do-i-use-dotcms-react-components-with-nextjs-app-router)
* [dotCMS Support](#dotcms-support)
* [How To Contribute](#how-to-contribute)
* [Licensing Information](#licensing-information)

## Quickstart

Install the SDK and required dependencies:

```bash
npm install @dotcms/react@next @dotcms/uve@next @dotcms/client@next @dotcms/types @tinymce/tinymce-react
```

Render a dotCMS page:

```tsx
import { DotCMSLayoutBody, useEditableDotCMSPage } from '@dotcms/react/next';

const components = {
  'Blog': BlogComponent,
  'Product': ProductComponent,
};

const MyPage = ({ pageResponse }) => {
  const { pageAsset } = useEditableDotCMSPage(pageResponse);
  return (
    <DotCMSLayoutBody 
      page={pageAsset} 
      components={components} 
    />
  );
};

export default MyPage;
```

▶️ Want a full working app? Check the [Next.js example project](https://github.com/dotCMS/core/tree/main/examples/nextjs).

---

## Example Project

We maintain a complete [Next.js starter project](https://github.com/dotCMS/core/tree/main/examples/nextjs) that demonstrates how to:

* Fetch and render dotCMS pages
* Register components for different content types
* Enable editing via the Universal Visual Editor (UVE)
* Use the App Router pattern in Next.js

You can clone it or use `create-next-app` with the example template:

```bash
npx create-next-app dotcms-nextjs-demo --example https://github.com/dotCMS/core/tree/main/examples/nextjs
```

This is the fastest way to get up and running with dotCMS + React.

▶️ Check the [live demo of a Next.js site](https://nextjs-example-sigma-five.vercel.app/) built with the dotCMS React SDK.

---

## What Is It?

The `@dotcms/react` SDK bridges dotCMS content management with React’s component-based system. It includes:

* Components for rendering dotCMS pages and content
* Hooks to support live editing in the Universal Visual Editor (UVE)
* Helpers to conditionally show content based on editing context

Use it to:

* Render dotCMS content with minimal setup
* Build fully editable React pages
* Integrate seamlessly with dotCMS’s Universal Visual Editor (UVE)

---

## Installation

```bash
npm install @dotcms/react@next
```

### Peer Dependencies

Make sure to also install:

```bash
npm install @dotcms/uve@next @dotcms/client@next @dotcms/types @tinymce/tinymce-react
```

---

## Key Concepts

| Term           | Description                                                    |
| -------------- | -------------------------------------------------------------- |
| `pageResponse` | The full object returned from `dotCMSClient.page.get()`              |
| `pageAsset`    | The actual page layout data used by the renderer               |
| UVE            | Universal Visual Editor, dotCMS’s  editing interface           |
| Editable Page  | A page enhanced via `useEditableDotCMSPage()` for live editing |

---

## API Reference

### Components

#### DotCMSLayoutBody

**Overview**: Renders the layout body of a dotCMS page using content mapping, rows, and columns.

**Props**:

| Prop         | Type                                  | Required | Default        | Description                                            |
| ------------ | ------------------------------------- | -------- | -------------- | ------------------------------------------------------ |
| `page`       | `DotCMSPageAsset`                     | ✅        | —              | Page layout asset from dotCMS                          |
| `components` | `Record<string, React.ComponentType>` | ✅        | —              | Map of content type → React component                  |
| `mode`       | `'development' \| 'production'`       | ❌        | `'production'` | Enables visual debug for missing or unrendered content |

**Usage**:

```tsx
const MyPage = ({ pageAsset }) => {
  return <DotCMSLayoutBody page={pageAsset} components={{ Blog: BlogComponent }} />;
};
```

**Component Mapping**:

* The key `Blog` is the content type variable in dotCMS
* The value `BlogComponent` is the React component that renders the blog post

**Mode**:

* `development` mode is used for development and debugging
* `production` mode is used for production


**Editor Integration**:

* Automatically detects if it's inside dotCMS UVE
* Works with useEditableDotCMSPage() for real-time updates

**Common Issues**:

* Missing component mappings cause unrendered containers
* Editing layout structure manually may break UVE sync
* Modifying the layout structure directly may break the editor's ability to track changes

---

#### DotCMSShow

**Overview**: Conditionally renders content based on UVE mode (EDIT, PREVIEW, LIVE).

**Props**:

| Prop       | Type        | Required | Default         | Description                                  |
| ---------- | ----------- | -------- | --------------- | -------------------------------------------- |
| `when`     | `UVE_MODE`  | ❌        | `UVE_MODE.EDIT` | The UVE mode in which children should render |
| `children` | `ReactNode` | ✅        | —               | Content to be conditionally rendered         |

**Usage**:

```tsx
<DotCMSShow when={UVE_MODE.EDIT}>
  <div>This will only render in UVE EDIT mode</div>
</DotCMSShow>
```

**Editor Integration**:

* Automatically detects mode within dotCMS UVE

**Common Issues**:

* Does not auto-update when mode changes after mount.
* For dynamic mode response, use `useDotCMSShowWhen` instead.
* `UVE_MODE.LIVE` is just a view mode within the editor. To target the actual published site outside the editor, don't wrap rendering logic in `DotCMSShow`.

---

#### DotCMSBlockEditorRenderer

**Overview**: Renders rich content from dotCMS Block Editor fields.

**Props**:

| Prop              | Type                  | Required | Default | Description                                            |
| ----------------- | --------------------- | -------- | ------- | ------------------------------------------------------ |
| `blocks`          | `BlockEditorContent`  | ✅        | —       | The block editor content structure to render           |
| `customRenderers` | `CustomRenderer`      | ❌        | `{}`    | Optional map of block types to custom render functions |
| `className`       | `string`              | ❌        | `''`    | CSS class for the container                            |
| `style`           | `React.CSSProperties` | ❌        | `{}`    | Inline styles to apply to the container                |


**Usage**:

```tsx
<DotCMSBlockEditorRenderer blocks={contentlet["YOUR_BLOCK_EDITOR_FIELD"]} />
```

**Editor Integration**:

* Renders exactly as configured in dotCMS editor
* Supports custom renderers for block types

**Common Issues**:

* Should not be used with DotCMSEditableText
* Inherits styling from parent containers, which may cause conflicts
* `DotCMSBlockEditorRenderer` only works with Block Editor fields. For other fields, use [`DotCMSEditableText`](#dotcmseditabletext).

📘 For advanced examples, customization options, and best practices, refer to the [DotCMSBlockEditorRenderer README](https://github.com/dotCMS/core/tree/master/core-web/libs/sdk/react/src/lib/next/components/DotCMSBlockEditorRenderer).

---

#### DotCMSEditableText

**Overview**: Allows inline editing of text, text area, and WYSIWYG fields using TinyMCE.

**Props**:

| Prop         | Type                | Required | Default   | Description                                 |
| ------------ | ------------------- | -------- | --------- | ------------------------------------------- |
| `contentlet` | `object`            | ✅        | —         | The contentlet containing the field         |
| `fieldName`  | `string`            | ✅        | —         | Name of the field to edit                   |
| `mode`       | `'plain' \| 'full'` | ❌        | `'plain'` | Editor mode: plain (text) or full (WYSIWYG) |
| `format`     | `'text' \| 'html'`  | ❌        | `'text'`  | Field format: text or HTML                  |

**Usage**:

```tsx
<DotCMSEditableText contentlet={item} fieldName="title" />
```

**Editor Integration**:

* Detects UVE edit mode and enables inline TinyMCE editing
* Saves on blur without needing full content dialog

**Common Issues**:

* `mode` and `format` must match the actual field type
* `full` mode is only supported for `WYSIWYG` fields. For `text` and `text_area` fields, use `plain` mode.
* Should not be used on Block Editor fields

---

### Hooks

#### useEditableDotCMSPage

**Overview**: Makes a dotCMS page editable in the Universal Visual Editor.

**Parameters**:

| Param          | Type                 | Required | Description                                   |
| -------------- | -------------------- | -------- | --------------------------------------------- |
| `pageResponse` | `DotCMSPageResponse` | ✅        | The page data object from `client.page.get()` |

**Returns**: `DotCMSPageResponse`

**Usage**:

```tsx
const MyPage = ({ pageResponse }) => {
  // Return the same pageResponse object but in working state
  const { pageAsset } = useEditableDotCMSPage(pageResponse);
  return <DotCMSLayoutBody page={pageAsset} components={{ Blog }} />;
};
```

**Editor Integration**:

* Syncs changes in real time
* Detects edit mode, initializes UVE, listens for updates

**Common Issues**:

* Only works with `pageResponse` from `@dotcms/client`
* Has no effect outside the editor

**How It Works**

1\. Taking the page asset from the \`pageResponse\` parameter

2\. Initializing the Universal Visual Editor (UVE) with the parameters from the page asset

3\. Setting up event listeners for the UVE to detect content changes

4\. Retrieving and returning the updated page asset whenever changes are made in the editor

---

#### useDotCMSShowWhen

**Overview**: Returns a boolean indicating whether the app is currently in the specified UVE mode.

**Parameters**:

| Param  | Type       | Required | Description                                     |
| ------ | ---------- | -------- | ----------------------------------------------- |
| `when` | `UVE_MODE` | ✅        | The UVE mode to check for (EDIT, PREVIEW, LIVE) |

**Returns**: `boolean`

**Usage**:

```tsx
const isEditMode = useDotCMSShowWhen(UVE_MODE.EDIT);
```

**Editor Integration**:

* Useful for mode-based behaviors outside of render logic

**Common Issues**:

* Requires proper mode enum from `@dotcms/types`
* For visual elements, prefer `<DotCMSShow>` unless custom logic is needed

---

## FAQ

### What are the differences between UVE modes?

The dotCMS Universal Visual Editor (UVE) supports three modes that affect component behavior:

1. **DRAFT Mode (**`UVE_MODE.EDIT`**)**

   * Content is visible only when editing a page within dotCMS's editor draft mode
   * Ideal for edit controls, admin tools, or inline helpers
   * Visible only to content editors with proper permissions

2. **PREVIEW MODE (**`UVE_MODE.PREVIEW`**)**

   * Content is visible in dotCMS editor's preview panel
   * Useful for preview-only banners, staged content, or workflow messaging

3. **PUBLISHED Mode (**`UVE_MODE.LIVE`**)**

   * Content is shown in the "Published View" mode within the editor
   * Simulates the live site appearance, but is still within the editor
   * ⚠️ Do not confuse with the actual live production environment

Use these modes with:

* `<DotCMSShow when={UVE_MODE.EDIT}>...</DotCMSShow>`
* `const isPreview = useDotCMSShowWhen(UVE_MODE.PREVIEW);`

### What if my components don’t render?

Make sure all content types used in the layout are registered in the `components` object.

You can also enable `mode="development"` in the `<DotCMSLayoutBody />` component. This will display missing components and empty containers, helping you debug layout or registration issues more easily.

---

### How do I use dotCMS React components with Next.js App Router?

In the Next.js App Router, all components are Server Components by default. However, React class components like `DotCMSLayoutBody` must be rendered in a Client Component.

**Recommended Pattern:**

1. Fetch data in a Server Component:

```tsx
// app/page.js (Server)
const pageResponse = await dotCMSClient.page.get('/index');
return <DotCMSPage pageResponse={pageResponse} />;
```

2. Use a Client Component to render dotCMS content:

```tsx
// components/DotCMSPage.js
'use client';
import { useEditableDotCMSPage, DotCMSLayoutBody } from '@dotcms/react/next';

export default function DotCMSPage({ pageResponse }) {
  const editable = useEditableDotCMSPage(pageResponse);
  return <DotCMSLayoutBody page={editable.pageAsset} components={...} />;
}
```

**Common Mistakes to Avoid:**

* Do not render `DotCMSLayoutBody` directly in a Server Component
* Avoid passing only `pageAsset`; pass the full `pageResponse` to enable editing


## dotCMS Support

We offer multiple channels to get help with the dotCMS React SDK:

* **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.
* **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions.
* **Stack Overflow**: Use the tag `dotcms-react` when posting questions.

When reporting issues, please include:

* SDK version you're using
* React version
* Minimal reproduction steps
* Expected vs. actual behavior

Enterprise customers can access premium support through the [dotCMS Support Portal](https://dev.dotcms.com/docs/help).

---

## How To Contribute

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the DotCMS UVE SDK! If you'd like to contribute, please follow these steps:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

---

## Licensing Information

dotCMS comes in multiple editions and as such is dual-licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization, and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds several enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).

This SDK is part of dotCMS’s dual-licensed platform (GPL 3.0 for Community, commercial license for Enterprise).

[Learn more ](https://www.dotcms.com)at [dotcms.com](https://www.dotcms.com).
