# dotCMS Vue SDK

The `@dotcms/vue` SDK is the dotCMS official Vue 3 library. It empowers Vue developers to build powerful, editable websites and applications in no time, with full support for the Universal Visual Editor (UVE).

## Table of Contents

-   [Prerequisites & Setup](#prerequisites--setup)
    -   [Get a dotCMS Environment](#get-a-dotcms-environment)
    -   [Configure The Universal Visual Editor App](#configure-the-universal-visual-editor-app)
    -   [Create a dotCMS API Key](#create-a-dotcms-api-key)
    -   [Installation](#installation)
    -   [dotCMS Client Configuration](#dotcms-client-configuration)
    -   [Proxy Configuration for Static Assets](#proxy-configuration-for-static-assets)
-   [Quickstart: Render a Page with dotCMS](#quickstart-render-a-page-with-dotcms)
    -   [Example Project](#example-project-)
-   [SDK Reference](#sdk-reference)
    -   [createDotCMSVue / useDotCMSClient](#createdotcmsvue--usedotcmsclient)
    -   [DotCMSLayoutBody](#dotcmslayoutbody)
    -   [DotCMSEditableText](#dotcmseditabletext)
    -   [DotCMSBlockEditorRenderer](#dotcmsblockeditorrenderer)
    -   [DotCMSShow](#dotcmsshow)
    -   [useEditableDotCMSPage](#useeditabledotcmspage)
    -   [useDotCMSShowWhen](#usedotcmsshowwhen)
    -   [createDotCMSImageLoader](#createdotcmsimageloader)
    -   [toPlain](#toplain)
-   [Troubleshooting](#troubleshooting)
    -   [Common Issues & Solutions](#common-issues--solutions)
    -   [Debugging Tips](#debugging-tips)
    -   [Still Having Issues?](#still-having-issues)
-   [Support](#support)
-   [Contributing](#contributing)
-   [Licensing](#licensing)

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

When configuring the UVE app, point it at your Vue app's URL (e.g. `http://localhost:5173`):

```json
{ "config": [{ "pattern": "(.*)", "url": "http://localhost:5173" }] }
```

### Create a dotCMS API Key

> [!TIP]
> Make sure your API Token has read-only permissions for Pages, Folders, Assets, and Content. Using a key with minimal permissions follows security best practices.

This integration requires an API Key with read-only permissions for security best practices:

1. Go to the **dotCMS admin panel**.
2. Click on **System** > **Users**.
3. Select the user you want to create the API Key for.
4. Go to **API Access Key** and generate a new key.

For detailed instructions, please refer to the [dotCMS API Documentation - Read-only token](https://dev.dotcms.com/docs/rest-api-authentication#ReadOnlyToken).

### Installation

```bash
npm install @dotcms/vue@latest
```

Requires **Vue 3.4+** (declared as a peer dependency). The install also brings in:

-   `@dotcms/uve`: Enables interaction with the [Universal Visual Editor](https://dev.dotcms.com/docs/uve-headless-config) for real-time content editing
-   `@dotcms/client`: Provides the core client functionality for fetching and managing dotCMS data
-   `@tinymce/tinymce-vue`: Powers inline text editing in [`DotCMSEditableText`](#dotcmseditabletext)

### dotCMS Client Configuration

Install the dotCMS Vue plugin once at startup. It builds the client and provides
it to the whole app, so components retrieve it with `useDotCMSClient()` instead
of importing a module-level singleton — the Vue analog of Angular's
`provideDotCMSClient`.

```ts
// main.ts
import { createApp } from 'vue';
import { createDotCMSVue } from '@dotcms/vue';

import App from './App.vue';

const app = createApp(App);

app.use(
    createDotCMSVue({
        dotcmsUrl: import.meta.env.VITE_DOTCMS_HOST,
        authToken: import.meta.env.VITE_DOTCMS_AUTH_TOKEN, // Optional for public content
        siteId: import.meta.env.VITE_DOTCMS_SITE_ID, // Optional site identifier/name
        requestOptions: {
            // The UVE needs fresh data so in-context edits are reflected immediately.
            cache: 'no-cache'
        }
    })
);

app.mount('#app');
```

Then, in any component:

```vue
<script setup lang="ts">
import { useDotCMSClient } from '@dotcms/vue';

const client = useDotCMSClient();
const { pageAsset } = await client.page.get('/');
</script>
```

> **Using the client outside a component?** Code that runs before/outside a
> component `setup` — e.g. a Vue Router page loader — can't call
> `useDotCMSClient()`. Keep a reference to the plugin and read its `.client`:
>
> ```ts
> export const dotCMSVue = createDotCMSVue({ ...config });
> export const dotCMSClient = dotCMSVue.client; // same instance the plugin provides
> ```

If you prefer to manage the client yourself, `createDotCMSClient` from
`@dotcms/client` is still available and works with all of this SDK's components
and composables — the plugin is a convenience, not a requirement.

```typescript
import { createDotCMSClient } from '@dotcms/client';

export const dotCMSClient = createDotCMSClient({
    dotcmsUrl: import.meta.env.VITE_DOTCMS_HOST,
    authToken: import.meta.env.VITE_DOTCMS_AUTH_TOKEN,
    siteId: import.meta.env.VITE_DOTCMS_SITE_ID,
    requestOptions: { cache: 'no-cache' }
});
```

### Proxy Configuration for Static Assets

Configure a proxy to leverage the powerful dotCMS image API, allowing you to resize and serve optimized images efficiently. This enhances application performance and improves the user experience.

#### 1. Configure Vite

```ts
// vite.config.ts
import { defineConfig } from 'vite';

export default defineConfig({
    server: {
        proxy: {
            '/dA': {
                target: 'https://your-dotcms-instance.com',
                changeOrigin: true
            }
        }
    }
});
```

Learn more about Vite configuration [here](https://vitejs.dev/config/).

#### 2. Usage in Components

Once configured, image URLs in your components will automatically be proxied to your dotCMS instance:

> 📚 Learn more about [Image Resizing and Processing in dotCMS](https://www.dotcms.com/blog/image-resizing-and-processing-in-dotcms-with-angular-and-nextjs).

```vue
<script setup lang="ts">
import type { DotCMSBasicContentlet } from '@dotcms/types';

defineProps<{ contentlet: DotCMSBasicContentlet }>();
</script>

<template>
    <img :src="`/dA/${contentlet.inode}`" :alt="contentlet.title" />
</template>
```

## Quickstart: Render a Page with dotCMS

The following example shows how to quickly set up a basic dotCMS page renderer in your Vue application. It demonstrates how to:

-   Fetch a dotCMS page and render it with `DotCMSLayoutBody`
-   Map content types to your own Vue components
-   Keep the page editable and live-updating inside the Universal Visual Editor

Because a Vue composable must be called synchronously in `setup`, the recommended pattern splits fetching from rendering: a **fetcher** loads the page, then mounts a **renderer** child that receives the resolved page and calls `useEditableDotCMSPage`.

```vue
<!-- PageView.vue — fetches the page, then mounts the renderer -->
<script setup lang="ts">
import { shallowRef, onMounted } from 'vue';

import PageRenderer from './PageRenderer.vue';
import { dotCMSClient } from './dotCMSClient';

// shallowRef keeps the response a plain object (see toPlain / reactivity note below).
const pageResponse = shallowRef<Awaited<ReturnType<typeof dotCMSClient.page.get>> | null>(null);

onMounted(async () => {
    pageResponse.value = await dotCMSClient.page.get('/');
});
</script>

<template>
    <PageRenderer v-if="pageResponse" :page-response="pageResponse" />
</template>
```

```vue
<!-- PageRenderer.vue — renders the resolved page -->
<script setup lang="ts">
import { DotCMSLayoutBody, useEditableDotCMSPage } from '@dotcms/vue';
import { computed } from 'vue';

import Blog from './content-types/Blog.vue';
import Product from './content-types/Product.vue';

const props = defineProps<{ pageResponse: unknown }>();

const pageComponents = {
    Blog,
    Product
};

// Returns a reactive ref that live-updates while editing in the UVE.
const page = useEditableDotCMSPage(props.pageResponse as never);
const pageAsset = computed(() => page.value?.pageAsset);
</script>

<template>
    <DotCMSLayoutBody v-if="pageAsset" :page="pageAsset" :components="pageComponents" />
</template>
```

### Example Project 🚀

Looking to get started quickly? Our [Vue.js starter project](https://github.com/dotCMS/core/tree/main/examples/vuejs) is the perfect launchpad. This Vite + TypeScript + Tailwind template demonstrates everything you need:

📦 Fetch and render dotCMS pages with best practices
🧩 Map components to different content types
🔍 Listing pages with search functionality
📝 Detail pages with the block editor
📈 Image and asset optimization for better performance
✨ Seamless editing via the Universal Visual Editor (UVE)
⚡️ Vue Composition API + reactive live updates

## SDK Reference

All components, composables and utilities are imported from `@dotcms/vue`.

### createDotCMSVue / useDotCMSClient

`createDotCMSVue(config)` returns a Vue plugin that builds a dotCMS client and
provides it app-wide. `useDotCMSClient()` retrieves that client from any
component. See [dotCMS Client Configuration](#dotcms-client-configuration) for
the full setup.

```ts
import { createDotCMSVue } from '@dotcms/vue';

const plugin = createDotCMSVue({ dotcmsUrl, authToken /* …DotCMSClientConfig */ });
app.use(plugin);
```

| Export                    | Signature                                            | Description                                                                                     |
| ------------------------- | ---------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| `createDotCMSVue(config)` | `(config: DotCMSClientConfig) => DotCMSVuePlugin`    | Vue plugin for `app.use()`. `config` is the same object accepted by `createDotCMSClient`.       |
| `useDotCMSClient()`       | `() => DotCMSClient`                                 | Returns the provided client. Throws if the plugin was not installed. Call it inside `setup`.    |
| `DotCMSVuePlugin.client`  | `DotCMSClient`                                        | The created client instance, for use outside components (e.g. router loaders).                  |

### DotCMSLayoutBody

`DotCMSLayoutBody` renders the layout for a dotCMS page (rows → columns → containers → contentlets), dispatching each contentlet to the mapped component. It supports both production and development modes.

| Prop         | Type                       | Required | Default        | Description                                                          |
| ------------ | -------------------------- | -------- | -------------- | -------------------------------------------------------------------- |
| `page`       | `DotCMSPageAsset`          | ✅       | -              | The page asset containing the layout to render                       |
| `components` | `Record<string, Component>`| ✅       | `{}`           | [Map of content type → Vue component](#component-mapping)            |
| `mode`       | `DotCMSPageRendererMode`   | ❌       | `'production'` | [Rendering mode ('production' or 'development')](#layout-body-modes) |

#### Usage

```vue
<script setup lang="ts">
import { DotCMSLayoutBody } from '@dotcms/vue';
import type { DotCMSPageAsset } from '@dotcms/types';

import Blog from './content-types/Blog.vue';
import Product from './content-types/Product.vue';

defineProps<{ page: DotCMSPageAsset }>();

const components = {
    Blog,
    Product
};
</script>

<template>
    <DotCMSLayoutBody :page="page" :components="components" />
</template>
```

#### Layout Body Modes

-   `production`: Performance-optimized mode that only renders content with explicitly mapped components, leaving unmapped content empty.
-   `development`: Debug-friendly mode that renders a fallback for unmapped content types and shows visual indicators for empty containers and missing mappings. **Inside the UVE, edit mode is detected automatically** — the editor `data-dot-*` metadata is always emitted regardless of the `mode` prop.

#### Component Mapping

The `components` prop maps content type variable names to Vue components. Each contentlet's fields are passed to the matched component as props.

```typescript
const components = {
    Blog: MyBlogCard,
    Product: MyProductCard,
    CustomNoComponent: MyFallback // optional: rendered for unmapped types in dev mode
};
```

-   Keys (e.g. `Blog`, `Product`): must match your [content type variable names](https://dev.dotcms.com/docs/content-types#VariableNames) in dotCMS (they are case-sensitive — e.g. `webPageContent`, `calendarEvent`).
-   Values: the Vue component that renders that content type. Declare a typed props interface for the fields you use — do **not** spread the full `DotCMSBasicContentlet` type onto a component, as Vue runtime-validates every declared prop and dotCMS ships some fields (e.g. `language`, `modDate`) with types that differ from the type defs.
-   The special `CustomNoComponent` key is the fallback rendered when no mapping exists.

> [!TIP]
> Always use the exact content type variable name from dotCMS as the key. You can find it in the Content Types section of your dotCMS admin panel.

#### Per-contentlet slots

Beyond mapping by content type, you can override a **specific** contentlet by
its `identifier` using a named slot: `#contentlet-<identifier>`. When present,
the slot renders instead of the mapped component — the Vue analog of the React
SDK's `slots` prop, useful for one-off custom markup or a pre-rendered node.

The contentlet is exposed as the slot's scope prop.

```vue
<template>
    <DotCMSLayoutBody :page="page" :components="components">
        <!-- Renders instead of the Blog component for this one contentlet -->
        <template #contentlet-a1b2c3d4="{ contentlet }">
            <FeaturedBlog :blog="contentlet" />
        </template>
    </DotCMSLayoutBody>
</template>
```

Contentlets without a matching slot fall back to the `components` mapping as usual.

### DotCMSEditableText

`DotCMSEditableText` enables inline editing of a single text field in dotCMS. Inside the UVE in edit mode it mounts a TinyMCE editor; everywhere else it renders the field's current value.

| Prop         | Type                          | Required | Default   | Description                                                        |
| ------------ | ----------------------------- | -------- | --------- | ------------------------------------------------------------------ |
| `contentlet` | `T extends DotCMSBasicContentlet` | ✅   | -         | The contentlet containing the editable field                       |
| `fieldName`  | `keyof T`                     | ✅       | -         | Name of the field to edit (must be a valid key of the contentlet)  |
| `mode`       | `'plain' \| 'minimal' \| 'full'` | ❌    | `'plain'` | TinyMCE toolbar preset. `full` enables the full style bubble menu.  |
| `format`     | `'text' \| 'html'`            | ❌       | `'text'`  | `text` renders HTML as plain text; `html` interprets HTML markup    |

#### Usage

```vue
<script setup lang="ts">
import { DotCMSEditableText } from '@dotcms/vue';
import type { DotCMSBasicContentlet } from '@dotcms/types';

const props = defineProps<{ contentlet: DotCMSBasicContentlet }>();
</script>

<template>
    <div class="banner">
        <img :src="`/dA/${contentlet.inode}`" :alt="contentlet.title" />
        <h2>
            <DotCMSEditableText :contentlet="contentlet" field-name="title" />
        </h2>
    </div>
</template>
```

#### Editor Integration

-   Detects UVE edit mode and enables inline TinyMCE editing.
-   Sends the edited content back to the editor on blur, without opening the full content dialog.
-   The TinyMCE script is loaded from your dotCMS instance (`dotCMSHost`), so no extra TinyMCE install is required.

### DotCMSBlockEditorRenderer

`DotCMSBlockEditorRenderer` renders [Block Editor](https://dev.dotcms.com/docs/block-editor) content from dotCMS, with support for custom block renderers.

| Prop              | Type                        | Required | Default | Description                                                     |
| ----------------- | --------------------------- | -------- | ------- | --------------------------------------------------------------- |
| `blocks`          | `BlockEditorNode`           | ✅       | -       | The Block Editor field value to render                          |
| `customRenderers` | `CustomRenderer`            | ❌       | -       | Custom Vue components for specific block types / content types  |
| `className`       | `string`                    | ❌       | -       | CSS class applied to the container                              |
| `style`           | `CSSProperties`             | ❌       | -       | Inline styles for the container                                 |
| `isDevMode`       | `boolean`                   | ❌       | `false` | When `true`, shows a visible message for invalid/unknown blocks |

> **`className`/`style` vs native `class`/`style`:** the `className` and `style`
> props mirror the React SDK so the same code shape works across frameworks. This
> component renders one of two root elements (an error box or the content
> container), so Vue's automatic attribute fallthrough does **not** apply — a
> native `class="prose"` on the tag will not reach the container. Use the
> `className` and `style` props to style the container:
>
> ```vue
> <DotCMSBlockEditorRenderer
>     :blocks="contentlet.body"
>     class-name="prose max-w-none"
>     :style="{ marginTop: '1rem' }" />
> ```

#### Usage

A custom renderer is a Vue component that receives the block as a `node` prop and the rendered children in its default `<slot />`.

```vue
<!-- CustomHeading.vue -->
<script setup lang="ts">
import type { CustomRendererProps } from '@dotcms/vue';

defineProps<CustomRendererProps>();
</script>

<template>
    <h1 class="my-heading"><slot /></h1>
</template>
```

```vue
<!-- DetailPage.vue -->
<script setup lang="ts">
import { DotCMSBlockEditorRenderer, type CustomRenderer } from '@dotcms/vue';
import type { DotCMSBasicContentlet } from '@dotcms/types';

import CustomHeading from './CustomHeading.vue';

const props = defineProps<{ contentlet: DotCMSBasicContentlet }>();

const customRenderers: CustomRenderer = {
    heading: CustomHeading
};
</script>

<template>
    <DotCMSBlockEditorRenderer
        :blocks="contentlet.myBlockEditorField"
        :custom-renderers="customRenderers" />
</template>
```

#### Recommendations

-   Should not be used together with [`DotCMSEditableText`](#dotcmseditabletext) on the same field.
-   Be mindful that the CSS cascade can affect the look and feel of your blocks.
-   Only works with [Block Editor fields](https://dev.dotcms.com/docs/block-editor). For other text fields, use [`DotCMSEditableText`](#dotcmseditabletext).

### DotCMSShow

`DotCMSShow` conditionally renders its slot content based on the current UVE mode — useful for editor-only affordances like Edit or Reorder buttons.

| Prop   | Type       | Required | Default          | Description                                              |
| ------ | ---------- | -------- | ---------------- | -------------------------------------------------------- |
| `when` | `UVE_MODE` | ❌       | `UVE_MODE.EDIT`  | The UVE mode in which the slot content should be shown   |

#### Usage

```vue
<script setup lang="ts">
import { DotCMSShow } from '@dotcms/vue';
import { UVE_MODE } from '@dotcms/types';
</script>

<template>
    <DotCMSShow :when="UVE_MODE.EDIT">
        <button>Edit</button>
    </DotCMSShow>
</template>
```

📚 Learn more about the `UVE_MODE` enum in the [dotCMS UVE documentation](https://dev.dotcms.com/docs/universal-visual-editor).

### useEditableDotCMSPage

`useEditableDotCMSPage` wires a page into the Universal Visual Editor and returns a reactive ref that live-updates as an editor makes changes. Outside the UVE it is a pass-through — the returned ref simply holds the initial response.

| Param          | Type                          | Required | Description                                   |
| -------------- | ----------------------------- | -------- | --------------------------------------------- |
| `pageResponse` | `DotCMSComposedPageResponse`  | ✅       | The page response from `client.page.get()`    |

**Returns** a `Ref<DotCMSComposedPageResponse>` — access `.value.pageAsset` and `.value.content`.

When you use the composable, it:

1. Initializes the UVE with your page data
2. Keeps the editor navigation in sync
3. Subscribes to content changes and swaps in the new page automatically when content is edited, blocks are added/removed, layout changes, or components are moved
4. Cleans up all listeners on unmount

#### Usage

```vue
<script setup lang="ts">
import { DotCMSLayoutBody, useEditableDotCMSPage } from '@dotcms/vue';
import { computed } from 'vue';

const props = defineProps<{ pageResponse: unknown }>();

const page = useEditableDotCMSPage(props.pageResponse as never);
const pageAsset = computed(() => page.value?.pageAsset);

const components = {
    /* content-type → component map */
};
</script>

<template>
    <DotCMSLayoutBody v-if="pageAsset" :page="pageAsset" :components="components" />
</template>
```

> [!IMPORTANT]
> Store the page response in a `shallowRef` (not a deep `ref`). A deep `ref` wraps the whole page in reactive Proxies, which the UVE cannot `postMessage` to the editor (`DataCloneError`). See [`toPlain`](#toplain).

### useDotCMSShowWhen

`useDotCMSShowWhen` returns a reactive boolean for whether the current UVE mode matches — useful for mode-based logic outside of the template.

| Param  | Type       | Required | Description                        |
| ------ | ---------- | -------- | ---------------------------------- |
| `when` | `UVE_MODE` | ✅       | The UVE mode to check against      |

#### Usage

```vue
<script setup lang="ts">
import { useDotCMSShowWhen } from '@dotcms/vue';
import { UVE_MODE } from '@dotcms/types';

const isEditMode = useDotCMSShowWhen(UVE_MODE.EDIT); // Readonly<Ref<boolean>>
</script>

<template>
    <button v-if="isEditMode">Edit</button>
</template>
```

### createDotCMSImageLoader

`createDotCMSImageLoader(dotcmsUrl?)` returns a function that turns a dotCMS asset identifier (or path) into an optimized image URL via the dotCMS image API (the `/dA/` route, which handles resizing and optimization). It's the Vue analog of Angular's `provideDotCMSImageLoader` — Vue has no `IMAGE_LOADER` token, so you call the returned function directly.

Absolute `http(s)://…` URLs are returned unchanged, so mixing dotCMS assets with external/stock imagery just works.

| Argument    | Type     | Required | Default | Description                                                                              |
| ----------- | -------- | -------- | ------- | ---------------------------------------------------------------------------------------- |
| `dotcmsUrl` | `string` | ❌       | `''`    | Base URL of your dotCMS instance. Omit (empty) for site-relative `/dA/…` behind a proxy. |

The returned loader is `(src: string, options?) => string`, where `options` is `{ width?, quality?, languageId? }` (`quality` defaults to `50`, `languageId` to `'1'`).

```ts
import { createDotCMSImageLoader } from '@dotcms/vue';

// Absolute (production)
const image = createDotCMSImageLoader(import.meta.env.VITE_DOTCMS_HOST);
image(contentlet.inode, { width: 800 });
// → https://demo.dotcms.com/dA/<inode>/800w/50q?language_id=1

// Site-relative (dev proxy) — omit the host so the Vite /dA proxy handles it
const proxied = createDotCMSImageLoader();
proxied(contentlet.inode, { width: 800 }); // → /dA/<inode>/800w/50q?language_id=1
```

```vue
<script setup lang="ts">
import { createDotCMSImageLoader } from '@dotcms/vue';
import type { DotCMSBasicContentlet } from '@dotcms/types';

const props = defineProps<{ contentlet: DotCMSBasicContentlet }>();

const image = createDotCMSImageLoader(import.meta.env.VITE_DOTCMS_HOST);
</script>

<template>
    <img :src="image(contentlet.inode, { width: 800 })" :alt="contentlet.title" />
</template>
```

### toPlain

`toPlain` deep-unwraps a Vue reactive value (refs / reactive proxies) into a plain, structured-clone-safe object. Use it before passing reactive data to UVE editor actions (`editContentlet`, `enableBlockEditorInline`, …) from `@dotcms/uve`, which send their argument to the editor via `postMessage`.

```vue
<script setup lang="ts">
import { toPlain } from '@dotcms/vue';
import { editContentlet } from '@dotcms/uve';
import type { DotCMSBasicContentlet } from '@dotcms/types';

const props = defineProps<{ contentlet: DotCMSBasicContentlet }>();

const onEdit = () => editContentlet(toPlain(props.contentlet));
</script>
```

## Troubleshooting

### Common Issues & Solutions

#### Universal Visual Editor (UVE)

1. **UVE not loading**: the page renders but the editor tools/overlays don't appear.
    - **Possible causes**: incorrect UVE app configuration; missing `useEditableDotCMSPage`; the app is not running inside the UVE iframe.
    - **Solutions**: verify the UVE app URL matches your Vue dev server; ensure the page tree is wrapped with `useEditableDotCMSPage`; confirm `data-dot-object="contentlet"` attributes are present on rendered contentlets (they only appear in edit mode).

2. **`DataCloneError: could not be cloned`** when loading in the editor.
    - **Cause**: a Vue reactive Proxy was sent to the editor via `postMessage`.
    - **Solutions**: store the page response in a `shallowRef`, not a deep `ref`; use [`toPlain`](#toplain) before calling UVE editor actions with reactive data.

3. **Content edits don't update the page live.**
    - **Cause**: usually a duplicate Vue instance — if `vue` is bundled into a consumer copy, the SDK's reactivity is a different instance from your app's. Ensure a single `vue` is installed.

#### Missing Content

1. **Components not rendering**: empty spaces where content should appear.
    - **Solutions**: check the `components` map registration; verify content type variable names match exactly (case-sensitive); use `mode="development"` for detailed logging.

2. **Prop validation warnings** (e.g. *Invalid prop: type check failed for prop "modDate"*).
    - **Cause**: a content-type component declares props by extending the full contentlet type; dotCMS ships some system fields with types that differ from the defs.
    - **Solution**: declare only the specific fields your component reads.

#### Development Setup

1. **`npm install` fails**: clear the npm cache (`npm cache clean --force`), delete `node_modules`, and reinstall; verify your Node.js version.

2. **Runtime errors about missing imports**: check that all SDK imports come from `@dotcms/vue`, and that peer dependencies (`vue`, `@dotcms/client`, `@dotcms/uve`, `@dotcms/types`) are installed.

### Debugging Tips

1. **Enable development mode**

    ```vue
    <DotCMSLayoutBody :page="pageAsset" :components="components" mode="development" />
    ```

    This shows detailed messages, renders fallbacks for unmapped components, and highlights empty containers.

2. **Check the browser console and network tab** for errors, and watch for 401/403 (auth) responses.

3. **Inspect the DOM** inside the UVE iframe — contentlets should carry `data-dot-object="contentlet"` and containers `data-dot-object="container"` in edit mode.

### Still Having Issues?

If you're still experiencing problems after trying these solutions:

1. Search existing [GitHub issues](https://github.com/dotCMS/core/issues)
2. Ask questions on the [community forum](https://community.dotcms.com/)
3. Create a new issue with detailed reproduction steps, environment information, error messages, and code samples

## Support

We offer multiple channels to get help with the dotCMS Vue SDK:

-   **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose).
-   **Community Forum**: Join our [community discussions](https://community.dotcms.com/).
-   **Enterprise Support**: Enterprise customers can access premium support through the [dotCMS Support Portal](https://www.dotcms.com/support).

When reporting issues, please include:

-   SDK version you're using
-   Vue version
-   Minimal reproduction steps
-   Expected vs. actual behavior

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the dotCMS Vue SDK! If you'd like to contribute, please:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

## Licensing

dotCMS is available under either the [Business Source License 1.1 (BSL)](https://www.dotcms.com/bsl) or a commercial license.

Under the BSL, dotCMS can be used at no cost by individual developers, small businesses or agencies under $5M in total finances, and by larger organizations in non-production environments. Every BSL release automatically converts to GPL v3 four years after its release date. For full terms and FAQs, visit [dotcms.com/bsl](https://www.dotcms.com/bsl) and [dotcms.com/bsl-faq](https://www.dotcms.com/bsl-faq).

Production use in larger organizations, along with access to managed cloud, SLAs, support, and enterprise capabilities, is available under a commercial license from dotCMS. For details on commercial plans, features, and support options, see [dotcms.com/pricing](https://www.dotcms.com/pricing).
