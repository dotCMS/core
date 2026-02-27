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

# Lint code
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

This is a **Next.js 15 application** using **App Router** that integrates with **dotCMS** as a headless CMS. The application demonstrates a fully editable website where content is managed in dotCMS and rendered through Next.js with visual editing capabilities.

### Key Technologies

- **Next.js 15** with App Router and Turbopack
- **React 19** with client components
- **dotCMS SDK** packages for content management
- **Tailwind CSS** for styling
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
├── app/                     # Next.js App Router pages (SSR)
│   ├── [[...slug]]/         # Dynamic catch-all routing
│   │   └── page.js          # Main page component
│   ├── blog/                # Blog-specific routes
│   ├── layout.js            # Root layout component
│   └── globals.css          # Global styles
├── components/
│   ├── content-types/       # Components for dotCMS Content Types
│   │   ├── index.js         # Content type to component mapping
│   │   └── *.js             # Individual content type components
│   ├── editor/              # UVE editor components
│   └── forms/               # Form components
├── hooks/                   # Custom React hooks
├── utils/
│   ├── dotCMSClient.js      # dotCMS API client configuration
│   ├── getDotCMSPage.js     # Page fetching utility
│   └── queries.js           # GraphQL queries
└── views/
    └── Page.js              # Main page rendering component
```

## Key Patterns

### Content Fetching Pattern

Content is fetched server-side using the dotCMS client:

```javascript
// Server-side page fetching
const pageContent = await getDotCMSPage(path);

// Client-side page preparation for editing
const { pageAsset, content } = useEditableDotCMSPage(pageContent);
```

### Content Type Mapping

Each dotCMS Content Type maps to a React component:

```javascript
// In src/components/content-types/index.js
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

1. Create React component in `src/components/content-types/`
2. Add mapping in `src/components/content-types/index.js`
3. Component receives contentlet data as props
4. Access content fields directly: `const { title, description } = props;`

### Testing Content Changes

1. Make changes in dotCMS admin
2. Content appears immediately in Next.js (no caching by default)
3. Use UVE for visual editing directly on the frontend

### Image Handling

- Custom image loader configured in `next.config.js`
- Remote patterns configured for dotCMS host
- Images served through dotCMS with optimization

## Configuration Files

### next.config.js

- Configures image optimization for dotCMS assets
- Sets up rewrites for dotCMS admin routes (`/dA/*`)
- Handles redirects for index pages
- Disables React Strict Mode for UVE compatibility

### package.json

- Uses Next.js 15 with React 19
- Turbopack enabled for faster development
- Latest dotCMS SDK packages from `next` channel

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
