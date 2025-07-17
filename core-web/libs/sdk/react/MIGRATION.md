# Migration Guide: @dotcms/react Alpha to 1.0.X

The 1.0.X version of `@dotcms/react` introduces significant architectural changes that provide better TypeScript support, improved performance, and a more intuitive API. This guide helps you migrate from the alpha version to the 1.0.X version (`latest`) of the `@dotcms/react` SDK.

## Breaking Changes

### 1. Core Component Renaming

**Alpha Version:**
```jsx
import { DotcmsLayout } from '@dotcms/react';
```

**1.0.X Version:**
```jsx
import { DotCMSLayoutBody } from '@dotcms/react';
```

### 2. Component Props Structure

**Alpha Version:**
```jsx
<DotcmsLayout
    pageContext={{
        pageAsset,
        components: componentsMap,
    }}
    config={{
        pathname,
        editor: {
            params: {
                depth: 3,
            },
        },
    }}
/>
```

**1.0.X Version:**
```jsx
<DotCMSLayoutBody
    page={pageAsset}
    components={componentsMap}
    mode="development" // or "production"
/>
```

🚨 The `pageAsset` is part of the object response from the method `client.page.get` in the `@dotcms/client` library. [Learn more](https://www.npmjs.com/package/@dotcms/client)

### 3. Hook Usage Changes

**Alpha Version:**
```jsx
import { usePageAsset } from '../hooks/usePageAsset';

export function MyPage({ pageAsset, nav }) {
    pageAsset = usePageAsset(pageAsset);
    // Component logic
}
```

**1.0.X Version:**
```jsx
import { useEditableDotCMSPage } from '@dotcms/react';

export function MyPage({ pageResponse, nav }) {
    const { pageAsset } = useEditableDotCMSPage(pageResponse);
    // Component logic
}
```

🚨 The `pageResponse` is the object response from the method `client.page.get` in the `@dotcms/client` library. [Learn more](https://www.npmjs.com/package/@dotcms/client)

## New Features in 1.0.X Version

### 1. DotCMSEditableText Component

The 1.0.X version introduces a new component for inline text editing:

```jsx
import { DotCMSEditableText } from '@dotcms/react';

function Banner(contentlet) {
    return (
        <h2>
            <DotCMSEditableText
                contentlet={contentlet}
                fieldName="title"
                mode="plain" // or "full"
                format="text" // or "html"
            />
        </h2>
    );
}
```

### 2. DotCMSBlockEditorRenderer Component

New component for rendering Block Editor content:

```jsx
import { DotCMSBlockEditorRenderer } from '@dotcms/react';

function DetailPage({ contentlet }) {
    return (
        <DotCMSBlockEditorRenderer
            blocks={contentlet.blockEditorField}
            customRenderers={{
                customBannerBlock: MyCustomBannerBlock,
                h1: MyCustomH1
            }}
        />
    );
}
```

### 3. DotCMSShow Component

Conditional rendering based on UVE mode:

```jsx
import { DotCMSShow } from '@dotcms/react';
import { UVE_MODE } from '@dotcms/types';

function MyComponent() {
    return (
        <DotCMSShow when={UVE_MODE.EDIT}>
            <div>This will only render in UVE EDIT mode</div>
        </DotCMSShow>
    );
}
```

### 4. useDotCMSShowWhen Hook

Hook for conditional logic based on UVE mode:

```jsx
import { useDotCMSShowWhen } from '@dotcms/react';
import { UVE_MODE } from '@dotcms/types';

function MyEditButton() {
    const isEditMode = useDotCMSShowWhen(UVE_MODE.EDIT);

    if (isEditMode) {
        return <button>Edit</button>;
    }

    return null;
}
```

## Step-by-Step Migration Process

### Step 1: Update Dependencies

Update your `package.json`:

```json
{
    "dependencies": {
        "@dotcms/client": "latest",
        "@dotcms/react": "latest",
        "@dotcms/types": "latest",
        "@dotcms/uve": "latest"
    }
}
```

### Step 2: Update Component Imports

Replace all instances of:
- `DotcmsLayout` → `DotCMSLayoutBody`
- `DotEditableText` → `DotCMSEditableText`
- `BlockEditorRenderer` → `DotCMSBlockEditorRenderer`

### Step 3: Update Component Usage

**Before:**
```jsx
<DotcmsLayout
    pageContext={{
        pageAsset,
        components: componentsMap,
    }}
    config={{
        pathname,
        editor: {
            params: {
                depth: 3,
            },
        },
    }}
/>
```

**After:**
```jsx
<DotCMSLayoutBody
    page={pageAsset}
    components={componentsMap}
    mode="development"
/>
```

### Step 4: Update Hook Usage

**Before:**
```jsx
import { usePageAsset } from '../hooks/usePageAsset';

function MyPage({ pageAsset, nav }) {
    pageAsset = usePageAsset(pageAsset);
    // ...
}
```

**After:**
```jsx
import { useEditableDotCMSPage } from '@dotcms/react';

function MyPage({ pageResponse, nav }) {
    const { pageAsset } = useEditableDotCMSPage(pageResponse);
    // ...
}
```

### Step 5: Update Client Configuration

**Before:**
```jsx
import { DotCmsClient } from '@dotcms/client';

export const client = DotCmsClient.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.NEXT_PUBLIC_DOTCMS_AUTH_TOKEN,
    siteId: "your-site-id"
});
```

**After:**
```jsx
import { createDotCMSClient } from '@dotcms/client';

export const client = createDotCMSClient({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.NEXT_PUBLIC_DOTCMS_AUTH_TOKEN,
    siteId: "your-site-id"
});
```

### Step 6: Update Page Data Fetching

**Before:**
```javascript
const pageData = await client.page.get({
    path: '/your-page-path',
    language_id: 1, // underscore naming
    personaId: 'optional-persona-id'
});
```

**After (1.0.X):**
```javascript
const { pageAsset } = await client.page.get('/your-page-path', {
    languageId: 1, // camelCase naming
    personaId: 'optional-persona-id'
});
```

## TypeScript Support

The 1.0.X version provides better TypeScript support with improved type definitions. Make sure to import types from `@dotcms/types`:

```typescript
import type { DotCMSPageResponse, DotCMSPageAsset } from '@dotcms/types';
import { UVE_MODE } from '@dotcms/types';
```

## Common Migration Issues

### 1. Hook Not Working

**Issue:** Page updates not working in edit mode.

**Solution:** Use `useEditableDotCMSPage` instead of the custom `usePageAsset` hook.

### 2. Types Not Found

**Issue:** TypeScript errors about missing types.

**Solution:** Install and import from `@dotcms/types`:
```bash
npm install @dotcms/types
```

## Testing Your Migration

1. **Development Mode:** Test your application in development mode to see detailed error messages
2. **Edit Mode:** Test the Universal Visual Editor functionality
3. **Production Mode:** Test in production mode for performance optimization

## Additional Resources

- [dotCMS React SDK Documentation](https://dev.dotcms.com/docs/react-sdk)
- [Universal Visual Editor Guide](https://dev.dotcms.com/docs/uve-headless-config)
- [Next.js Example Project](https://github.com/dotCMS/core/tree/main/examples/nextjs)

## Support

If you encounter issues during migration:
- Check the [GitHub Issues](https://github.com/dotCMS/core/issues)
- Join the [Community Forum](https://community.dotcms.com/)
- For enterprise customers: [dotCMS Support Portal](https://helpdesk.dotcms.com/support/)
