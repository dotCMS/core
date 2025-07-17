# Migration Guide: @dotcms/react Alpha to Stable

This guide helps you migrate from the alpha version (`0.0.1-alpha.54`) to the stable version (`latest`) of the `@dotcms/react` SDK.

## Overview

The stable version of `@dotcms/react` introduces significant architectural changes that provide better TypeScript support, improved performance, and a more intuitive API. The main changes focus on component naming, prop structure, and the introduction of new hooks and utilities.

## Breaking Changes

### 1. Core Component Renaming

**Alpha Version:**
```jsx
import { DotcmsLayout } from '@dotcms/react';
```

**Stable Version:**
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

**Stable Version:**
```jsx
<DotCMSLayoutBody 
    page={pageAsset} 
    components={componentsMap} 
    mode="development" // or "production"
/>
```

### 3. Hook Usage Changes

**Alpha Version:**
```jsx
import { usePageAsset } from '../hooks/usePageAsset';

export function MyPage({ pageAsset, nav }) {
    pageAsset = usePageAsset(pageAsset);
    // Component logic
}
```

**Stable Version:**
```jsx
import { useEditableDotCMSPage } from '@dotcms/react';

export function MyPage({ pageResponse, nav }) {
    const { pageAsset } = useEditableDotCMSPage(pageResponse);
    // Component logic
}
```

### 4. Client Configuration Changes

**Alpha Version:**
```jsx
import { DotCmsClient } from '@dotcms/client';

export const client = DotCmsClient.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.NEXT_PUBLIC_DOTCMS_AUTH_TOKEN,
    siteId: "your-site-id",
    requestOptions: {
        cache: "no-cache",
    }
});
```

**Stable Version:**
```jsx
import { createDotCMSClient } from '@dotcms/client';

export const dotCMSClient = createDotCMSClient({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token',
    siteId: 'your-site-id'
});
```

## New Features in Stable Version

### 1. DotCMSEditableText Component

The stable version introduces a new component for inline text editing:

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

export const dotCMSClient = createDotCMSClient({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.NEXT_PUBLIC_DOTCMS_AUTH_TOKEN,
    siteId: "your-site-id"
});
```

### Step 6: Update Page Data Fetching

**Before:**
```jsx
const pageAsset = await client.page.get({
    ...params,
    depth: 3,
});
```

**After:**
```jsx
const pageResponse = await dotCMSClient.page.get({
    ...params,
    depth: 3,
});
```

## Component-Specific Changes

### Banner Component Migration

**Alpha Version:**
```jsx
import { DotEditableText } from '@dotcms/react';

function Banner(contentlet) {
    return (
        <h2>
            <DotEditableText
                contentlet={contentlet}
                fieldName="title"
            />
        </h2>
    );
}
```

**Stable Version:**
```jsx
import { DotCMSEditableText } from '@dotcms/react';

function Banner(contentlet) {
    return (
        <h2>
            <DotCMSEditableText
                contentlet={contentlet}
                fieldName="title"
                mode="plain"
                format="text"
            />
        </h2>
    );
}
```

### Block Editor Component Migration

**Alpha Version:**
```jsx
import { BlockEditorRenderer } from '@dotcms/react';

function Blog({ blogContent, ...contentlet }) {
    return (
        <BlockEditorRenderer
            editable={true}
            contentlet={contentlet}
            blocks={blogContent}
            fieldName="blogContent"
            customRenderers={{
                Activity: ActivityBlock,
                paragraph: CustomParagraph,
            }}
        />
    );
}
```

**Stable Version:**
```jsx
import { DotCMSBlockEditorRenderer } from '@dotcms/react';

function Blog({ blogContent, ...contentlet }) {
    return (
        <DotCMSBlockEditorRenderer
            blocks={blogContent}
            customRenderers={{
                Activity: ActivityBlock,
                paragraph: CustomParagraph,
            }}
        />
    );
}
```

## Environment Variables

Update your `.env.local` file:

**Alpha Version:**
```env
NEXT_PUBLIC_DOTCMS_AUTH_TOKEN=YOUR_API_TOKEN
NEXT_PUBLIC_DOTCMS_HOST=http://localhost:8080
NEXT_PUBLIC_EXPERIMENTS_API_KEY=analytic-api-key-from-dotcms-portlet
NEXT_PUBLIC_EXPERIMENTS_DEBUG=true
```

**Stable Version:**
```env
NEXT_PUBLIC_DOTCMS_AUTH_TOKEN=YOUR_API_TOKEN
NEXT_PUBLIC_DOTCMS_HOST=http://localhost:8080/
NEXT_PUBLIC_DOTCMS_SITE_ID=your-site-id
NEXT_PUBLIC_DOTCMS_MODE='production'
```

## TypeScript Support

The stable version provides better TypeScript support with improved type definitions. Make sure to import types from `@dotcms/types`:

```typescript
import type { DotCMSPageResponse, DotCMSPageAsset } from '@dotcms/types';
import { UVE_MODE } from '@dotcms/types';
```

## Common Migration Issues

### 1. Component Not Rendering

**Issue:** Components not rendering after migration.

**Solution:** Ensure you're using the correct prop names:
- `pageContext` → `page`
- `config` → `mode`

### 2. Hook Not Working

**Issue:** Page updates not working in edit mode.

**Solution:** Use `useEditableDotCMSPage` instead of the custom `usePageAsset` hook.

### 3. Types Not Found

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