# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Commands

### Development Commands

```bash
# Start development server with Turbopack
npm run dev

# Build for production
npm run build

# Start production server
npm start

# Lint code (ESLint 9 flat config)
npm run lint

# Install dependencies
npm install
```

### Environment Setup

```bash
# Copy environment template
cp .env.local.example .env.local

# Edit environment variables
# NEXT_PUBLIC_DOTCMS_HOST - Your dotCMS instance URL
# NEXT_PUBLIC_DOTCMS_AUTH_TOKEN - API key from dotCMS
# NEXT_PUBLIC_DOTCMS_SITE_ID - Site identifier in dotCMS
```

## Architecture Overview

This is a **Next.js 16 application** using **App Router** that integrates with **dotCMS** as a headless CMS. The application demonstrates a fully editable website where content is managed in dotCMS and rendered through Next.js with visual editing capabilities. The codebase is written in **TypeScript** (strict mode; `tsconfig.json` configures the `@/*` path alias).

### Key Technologies

- **Next.js 16** with App Router and Turbopack
- **React 19** with client components
- **TypeScript 5** (strict mode)
- **dotCMS SDK** packages for content management
- **Tailwind CSS v4** for styling
- **ESLint 9** with flat config (`eslint.config.mjs`)
- **Universal Visual Editor (UVE)** for in-context editing

### dotCMS SDK Dependencies

- `@dotcms/client` - Core API client for fetching content
- `@dotcms/react` - React components and hooks for rendering
- `@dotcms/uve` - Universal Visual Editor integration
- `@dotcms/types` - TypeScript type definitions
- `@dotcms/experiments` - A/B testing capabilities

## Project Structure

```
src/
├── app/                     # Next.js App Router pages (SSR, .tsx)
│   ├── [[...slug]]/         # Dynamic catch-all routing
│   │   └── page.tsx         # Main page component
│   ├── blog/                # Blog-specific routes
│   ├── layout.tsx           # Root layout component
│   ├── not-found.tsx        # 404 page
│   └── globals.css          # Global styles
├── components/
│   ├── content-types/       # Components for dotCMS Content Types
│   │   ├── index.ts         # Content type to component mapping (pageComponents)
│   │   └── *.tsx            # Individual content type components
│   ├── editor/              # UVE editor components (EditButton, ReorderMenuButton)
│   ├── header/ footer/      # Site chrome
│   └── forms/               # Form components
├── config/
│   └── dotcms.config.ts     # Typed, centralized environment access
├── hooks/                   # Custom React hooks (useIsEditMode, useDebounce)
├── lib/
│   └── dotCMSClient.ts      # dotCMS API client configuration
├── types/
│   └── content.ts           # Shared TypeScript interfaces
├── utils/
│   ├── getDotCMSPage.ts     # Cached page fetching utility (page + GraphQL content)
│   ├── pageResponse.ts      # Typed page-response guards
│   ├── queries.ts           # GraphQL queries
│   └── imageLoader.ts       # Custom Next.js image loader
└── views/
    ├── Page.tsx             # Main page rendering component
    ├── DetailPage.tsx
    └── BlogListingPage.tsx
```

## Key Patterns

### Content Fetching Pattern

Content is fetched server-side using the dotCMS client:

```ts
// Server-side page fetching (cached via React `cache()`)
const pageContent = await getDotCMSPage(path);

// Client-side page preparation for editing
const { pageAsset, content } = useEditableDotCMSPage(pageContent);
```

### Content Type Mapping

Each dotCMS Content Type maps to a React component:

```ts
// In src/components/content-types/index.ts
export const pageComponents = {
    Activity: Activity,
    Banner: Banner,
    Product: Product,
    // Key must match Content Type variable name in dotCMS
};
```

### Dynamic Routing Strategy

Uses catch-all routes (`[[...slug]]`) to handle dotCMS's flexible URL patterns:

- Handles root path (`/`) and nested paths (`/about`, `/blog/post`)
- Supports dotCMS vanity URLs and redirects
- Prevents 404 errors for content accessible via multiple paths

### Server/Client Component Split

- **App Router pages** (src/app/): Server-side rendered, no React hooks
- **View components** (src/views/): Client-side with "use client", can use hooks
- **Content components**: Can be either server or client components

## dotCMS Integration

### Content Rendering Flow

1. **Fetch**: Server-side page fetching via `getDotCMSPage()`
2. **Prepare**: Client-side editing preparation via `useEditableDotCMSPage()`
3. **Render**: Layout rendering via `DotCMSLayoutBody`
4. **Map**: Content type mapping to React components

### Universal Visual Editor (UVE)

The UVE enables in-context editing:

- Configured in dotCMS with pattern `(.*)` pointing to `http://localhost:3000`
- Allows content editors to edit pages directly in the Next.js frontend
- Requires `useEditableDotCMSPage` hook for proper integration

### Environment Variables

- `NEXT_PUBLIC_DOTCMS_HOST`: dotCMS instance URL
- `NEXT_PUBLIC_DOTCMS_AUTH_TOKEN`: API key with read permissions
- `NEXT_PUBLIC_DOTCMS_SITE_ID`: Site identifier for multi-site setups

## Development Workflow

### Adding New Content Types

1. Create React component in `src/components/content-types/` (`.tsx`)
2. Add mapping in `src/components/content-types/index.ts`
3. Component receives contentlet data as props — declare a typed props interface
4. Access content fields directly: `function MyType({ title, description }: MyTypeProps) { ... }`

### Testing Content Changes

1. Make changes in dotCMS admin
2. Content appears immediately in Next.js (no caching by default)
3. Use UVE for visual editing directly on the frontend

### Image Handling

- Custom image loader (`src/utils/imageLoader.ts`) configured in `next.config.ts`
- Remote patterns configured for dotCMS host
- Images served through dotCMS with optimization

## Configuration Files

### next.config.ts

- Configures image optimization for dotCMS assets
- Sets up rewrites for dotCMS admin routes (`/dA/*`)
- Handles redirects for index pages
- Disables React Strict Mode for UVE compatibility

### tsconfig.json

- TypeScript strict mode enabled
- Defines the `@/*` path alias mapping to `src/*`

### eslint.config.mjs

- ESLint 9 flat config (extends `eslint-config-next`)
- Run via `npm run lint` (the `lint` script invokes `eslint` directly)

### package.json

- Uses Next.js 16 with React 19 and TypeScript 5
- Turbopack enabled for faster development
- dotCMS SDK packages (`@dotcms/client`, `@dotcms/react`, `@dotcms/uve`, `@dotcms/types`, `@dotcms/experiments`)

## Common Troubleshooting

### Content Not Displaying

- Check environment variables are set correctly
- Verify API token has proper permissions
- Ensure Content Type mapping exists in `pageComponents`

### UVE Not Working

- Verify UVE configuration in dotCMS points to correct Next.js URL
- Check `useEditableDotCMSPage` is used in client components
- Ensure dotCMS can reach the Next.js development server

### Build Issues

- Run `npm run build` to check for production build issues
- Verify all environment variables are available at build time
- Check that server components don't use client-only features
