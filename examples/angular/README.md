# Angular Client-Side Rendering with dotCMS Integration

This Angular project demonstrates how to implement editable dotCMS pages using Angular Client-Side Rendering (CSR). It showcases best practices for integrating dotCMS content management with Angular's client-side rendering capabilities.

### Content Management Features
- **Dynamic Page Rendering**: Automatic page generation from dotCMS routing
- **Content Type Components**: 10+ pre-built components for common content types
- **Block Editor Integration**: Rich text content with dotCMS Block Editor
- **Image Management**: Optimized image handling with dotCMS image API
- **GraphQL Queries**: Advanced content filtering and search capabilities

### Visual Editing (UVE)
- **Live Content Editing**: Edit content directly in the browser
- **Visual Page Builder**: In-context editing experience
- **Contentlet Editing**: Direct editing of individual content pieces

### Technical Integration
- **Client-Side Rendering**: Full CSR with Angular standalone components
- **Type Safety**: Complete TypeScript interfaces for all content types
- **Modern Angular**: Using Angular signals, control flow, and latest patterns

For the official Angular documentation, visit: [Angular Documentation](https://angular.dev)

## Table of Contents

- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [Setup](#setup)
  - [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Architecture Overview](#architecture-overview)
  - [Routing Strategy](#routing-strategy)
  - [Page Rendering](#page-rendering)
  - [Folder Structure](#folder-structure)
- [Development Workflow](#development-workflow)
- [Universal Visual Editor](#universal-visual-editor)
- [Style Editor](#style-editor)
- [Troubleshooting](#troubleshooting)
- [Additional Resources](#additional-resources)

## Prerequisites

- Node.js (version 18 or higher) and npm installed
- Access to a dotCMS instance (you can use https://demo.dotcms.com if you don't have your own)
- A valid AUTH token for the target dotCMS instance ([How to create an API token](https://auth.dotcms.com/docs/latest/rest-api-authentication#creating-an-api-token-in-the-ui))

## Getting Started

### Setup

```bash
git clone -n --depth=1 --filter=tree:0 https://github.com/dotCMS/core
cd core
git sparse-checkout set --no-cone examples/angular
git checkout
```

### Configuration

To configure the Angular app to use your dotCMS instance:

1. Open the project folder in your code editor
2. Navigate to `src/environments`
3. Open `environment.development.ts` and update the following variables:
   - `authToken`: Your dotCMS auth token
   - `dotcmsUrl`: URL of your dotCMS instance (e.g., https://demo.dotcms.com)

   ```typescript
   export const environment = {
     production: false,
     authToken: "YOUR_AUTH_TOKEN_HERE",
     dotcmsUrl: "https://demo.dotcms.com",
   };
   ```

⚠️ **Security Note**: Ensure that the `authToken` used here has [read-only permissions](https://www.dotcms.com/docs/latest/user-permissions#FrontEndBackEnd) to minimize security risks in client-side applications.

## Running the Application

### Development Server

```bash
ng serve
```

Navigate to `http://localhost:4200/`. The application will automatically reload when source files are modified.

### Building for Production

```bash
ng build
```

Build artifacts are stored in the `dist/` directory with performance optimizations.

### Running Tests

```bash
ng test
ng e2e
```

## Architecture Overview

### Routing Strategy

The application uses a strategic combination of catch-all and specific routing in `app.routes.ts`:

- **Specific routes**: Custom pages like `/blog/post/:slug`, `/activities/:slug`
- **Catch-all route (`**`)**: Handles all dotCMS-generated pages through a single `PageComponent`

This approach eliminates the need to duplicate dotCMS folder/page structure in Angular routing, preventing developer intervention for every new route.

### Page Rendering

Pages are rendered using the `<dotcms-layout-body>` component from `@dotcms/angular` library. This component:
- Renders all page rows, columns, and content
- Accepts a component map via the `components` input
- Maps content type variable names to Angular components
- Automatically passes full content objects to components

Example component mapping:
```typescript
const DYNAMIC_COMPONENTS = {
  Banner: BannerComponent,
  Product: ProductComponent,
  Activity: ActivityComponent
};
```

### Folder Structure

```
src/app/
├── components/           # Standard site-wide components
│   ├── header/
│   ├── footer/
│   └── navigation/
└── dotcms/              # dotCMS-specific components
    ├── pages/           # Page components (see app.routes.ts)
    ├── components/      # Content type components
    └── types/           # TypeScript interfaces
```

## Development Workflow

1. **New Content Types**: Add component mapping to `DYNAMIC_COMPONENTS` in `page.ts`
2. **Custom Pages**: Create specific routes in `app.routes.ts`
3. **Site Components**: Add to `components/` folder for site-wide usage
4. **dotCMS Components**: Add to `dotcms/components/` for content rendering

## Universal Visual Editor

To enable the Universal Visual Editor in dotCMS, follow these steps:

1. In your dotCMS instance, navigate to the "Apps" page
2. Find the "UVE - Universal Visual Editor" app and click on it
3. Then locate the site where you want to enable the UVE and click on it
4. In the configuration field add the following:

```json
{
  "config": [
    {
      "pattern": ".*",
      "url": "http://localhost:4200"
    }
  ]
}
```

5. Click on the "Save" button to save the changes.
6. Now edit any page and you will see the UVE.

If you want more information about the UVE, please refer to the [dotCMS UVE Documentation](https://dotcms.com/docs/latest/universal-visual-editor-uve).

## Style Editor

The Style Editor enables content editors to customize component appearance (typography, colors, layouts, etc.) directly in the Universal Visual Editor without code changes. Style properties are defined by developers and made editable through style editor schemas.

### Defining a Style Editor Schema

Create a schema file (e.g., `src/app/dotcms/style-editor-schemas/schemas.ts`) that defines editable style properties for your content types:

```typescript
import { defineStyleEditorSchema, styleEditorField } from '@dotcms/uve';

export const BANNER_SCHEMA = defineStyleEditorSchema({
    contentType: 'Banner', // Must match your dotCMS Content Type
    sections: [
        {
            title: 'Typography',
            fields: [
                styleEditorField.dropdown({
                    id: 'title-size',
                    label: 'Title Size',
                    options: [
                        { label: 'Small', value: 'text-4xl' },
                        { label: 'Medium', value: 'text-5xl' },
                        { label: 'Large', value: 'text-6xl' },
                    ]
                }),
                styleEditorField.checkboxGroup({
                    id: 'title-style',
                    label: 'Title Style',
                    options: [
                        { label: 'Bold', key: 'bold' },
                        { label: 'Italic', key: 'italic' },
                    ]
                }),
            ]
        },
        {
            title: 'Layout',
            fields: [
                styleEditorField.radio({
                    id: 'text-alignment',
                    label: 'Text Alignment',
                    options: [
                        { label: 'Left', value: 'left' },
                        { label: 'Center', value: 'center' },
                        { label: 'Right', value: 'right' },
                    ]
                }),
            ]
        },
    ]
});
```

### Field Types

The Style Editor supports four field types:

- **`styleEditorField.input()`** - Text or number input for custom values
- **`styleEditorField.dropdown()`** - Dropdown with predefined options
- **`styleEditorField.radio()`** - Radio buttons for single selection (supports images)
- **`styleEditorField.checkboxGroup()`** - Multiple checkboxes (returns object with boolean values)

### Registering Schemas

Register your schemas in your page component's `ngOnInit` method:

```typescript
import { registerStyleEditorSchemas } from '@dotcms/uve';
import { BANNER_SCHEMA, ACTIVITY_SCHEMA } from '../../style-editor-schemas/schemas';

export class PageComponent implements OnInit {
  ngOnInit() {
    registerStyleEditorSchemas([BANNER_SCHEMA, ACTIVITY_SCHEMA]);
    // ... rest of initialization
  }
}
```

### Using Style Properties in Components

Style properties are automatically passed to your contentlet components via the `dotStyleProperties` property on the contentlet. Use Angular signals and computed properties to access them:

```typescript
import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-banner',
  // ...
})
export class BannerComponent {
  contentlet = input.required<Banner>();

  // Extract dotStyleProperties as a computed signal
  dotStyleProperties = computed(() => 
    this.contentlet().dotStyleProperties as BannerDotStyleProperties
  );

  // Extract individual style values with defaults
  titleSize = computed(() => 
    this.dotStyleProperties()?.['title-size'] || 'text-6xl'
  );
  titleStyle = computed(() => 
    this.dotStyleProperties()?.['title-style'] || {}
  );
  textAlignment = computed(() => 
    this.dotStyleProperties()?.['text-alignment'] || 'center'
  );

  // Build dynamic classes using computed signals
  titleClasses = computed(() => {
    const style = this.titleStyle();
    const classes = [
      this.titleSize(),
      style.bold ? 'font-bold' : 'font-normal',
      style.italic ? 'italic' : '',
    ].filter(Boolean);
    return classes.join(' ');
  });

  // Use in template
  // <h2 [class]="titleClasses()">{{ contentlet().title }}</h2>
}
```

**Value Types:**
- **Input/Dropdown/Radio**: Returns a string (the selected value)
- **Checkbox Group**: Returns an object with boolean values (e.g., `{ bold: true, italic: false }`)

**Best Practices:**
- Always provide default values when accessing style properties
- Use `computed()` signals for reactive style property extraction
- Use meaningful field IDs that match your styling logic
- Group related fields into logical sections
- The `contentType` in your schema must exactly match your dotCMS Content Type variable name

For complete Style Editor documentation, see the [@dotcms/uve Style Editor guide](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/uve/README.md#style-editor).

## Troubleshooting

If you encounter issues while setting up or running the dotCMS Angular example, here are some common problems and their solutions:

<details>
<summary><strong>Authentication errors (401 Unauthorized)</strong></summary>

This often occurs when the environment variables are not set correctly.

**Solution:**

- Double-check that you've updated the `authToken` in `src/environments/environment.development.ts` with a valid token.
- Ensure the token has the necessary permissions (at least read access) for the content you're trying to fetch.
- Verify that the token hasn't expired. If it has, generate a new one in the dotCMS UI.
</details>

<details>
<summary><strong>Connection issues</strong></summary>

If you're having trouble connecting to the dotCMS instance:

**Solution:**

- Verify that the `dotcmsUrl` in `src/environments/environment.development.ts` is correct.
- Check if you can access the dotCMS instance directly through a web browser.
- If using `https://demo.dotcms.com`, remember it restarts every 24 hours. You might need to wait or try again later.
- Ensure your network allows connections to the dotCMS instance (check firewalls, VPNs, etc.).
</details>

<details>
<summary><strong>Missing pages or content</strong></summary>

If you're getting 404 errors for pages that should exist:

**Solution:**

- Ensure the page exists in your dotCMS instance. For example, if you're trying to access `/about`, make sure an "about" page exists in dotCMS.
- Check if the content types used in the example match those in your dotCMS instance.
- Verify that the content has been published and is not in draft status.
</details>

<details>
<summary><strong>Outdated dependencies or version conflicts</strong></summary>

If you're experiencing unexpected behavior or errors related to dependencies:

**Solution:** Perform a clean reinstall of all dependencies by running:

```bash
rm -rf node_modules && rm package-lock.json && npm install
```

This command will:

1. Remove the `node_modules` directory
2. Delete the `package-lock.json` file
3. Perform a fresh install of all dependencies

After this, restart your development server:

```bash
ng serve
```

</details>

<details>
<summary><strong>Build errors or stale cache</strong></summary>

If you're experiencing build errors or changes aren't reflected in the running application:

**Solution:** Clear the Angular build cache and rebuild the project:

```bash
ng cache clean
ng build --configuration=development
ng serve
```

This sequence of commands will:

1. Clear the Angular build cache
2. Rebuild the project with development configuration
3. Start the development server

This is recommended when:

- You've made significant changes to your project configuration
- You're experiencing unexplainable build errors
- Your changes aren't reflected in the running application despite saving and restarting the dev server
- You've recently updated Angular or other critical dependencies
</details>

<details>
<summary><strong>Universal Visual Editor (UVE) not working</strong></summary>

If the Universal Visual Editor is not functioning as expected:

**Solution:**

- Ensure you've correctly configured the UVE in your dotCMS instance as described in the [Universal Visual Editor](#universal-visual-editor) section.
- Verify that your Angular application is running on `http://localhost:4200` (or update the UVE configuration if using a different port).
- Check that you're accessing the dotCMS edit mode from the correct URL.
- Clear your browser cache and try again.
</details>

If you continue to experience issues after trying these solutions, please check the [dotCMS documentation](https://dotcms.com/docs/) or reach out to the dotCMS community for further assistance.

## Additional Resources

- [Angular CLI Documentation](https://angular.dev/tools/cli)
- [dotCMS Angular Library](https://www.dotcms.com/docs/latest/angular-integration)
- [Angular Best Practices](https://angular.dev/best-practices)
- [dotCMS GraphQL API](https://www.dotcms.com/docs/latest/graphql-api)
- [dotCMS Universal Visual Editor](https://www.dotcms.com/docs/latest/universal-visual-editor)
