# DotCMS SDK Analytics Installation Guide

This skill provides step-by-step instructions for installing and configuring the `@dotcms/analytics` SDK in the Next.js example project at `/core/examples/nextjs`.

## Overview

The `@dotcms/analytics` SDK is dotCMS's official JavaScript library for tracking content-aware events and analytics. It provides:

- Automatic page view tracking
- Custom event tracking
- Session management (30-minute timeout)
- Anonymous user identity tracking
- UTM campaign parameter tracking
- Event batching/queuing for performance

## 🚨 Important: Understanding the Analytics Components

**CRITICAL**: `useContentAnalytics()` **ALWAYS requires config as a parameter**. The hook does NOT use React Context.

### Component Roles

1. **`<DotContentAnalytics />`** - Auto Page View Tracker
   - Only purpose: Automatically track pageviews on route changes
   - **NOT a React Context Provider**
   - Does **NOT** provide config to child components
   - Place in root layout for automatic pageview tracking

2. **`useContentAnalytics(config)`** - Manual Tracking Hook
   - Used for custom event tracking
   - **ALWAYS requires config parameter**
   - Import centralized config in each component that uses it

### Correct Usage Pattern

```javascript
// 1. Create centralized config file (once)
// /src/config/analytics.config.js
export const analyticsConfig = {
  siteAuth: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY,
  server: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_HOST,
  autoPageView: true,
  debug: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_DEBUG === "true",
};

// 2. Add DotContentAnalytics to layout for auto pageview tracking (optional)
// /src/app/layout.js
import { DotContentAnalytics } from "@dotcms/analytics/react";
import { analyticsConfig } from "@/config/analytics.config";

<DotContentAnalytics config={analyticsConfig} />

// 3. Import config in every component that uses the hook
// /src/components/MyComponent.js
import { useContentAnalytics } from "@dotcms/analytics/react";
import { analyticsConfig } from "@/config/analytics.config";

const { track } = useContentAnalytics(analyticsConfig); // ✅ Config required!
```

**Why centralize config?** While you must import it in each component, centralizing prevents duplication and makes updates easier.

## Quick Setup Summary

Here's the complete setup flow:

```
1. Install package
   └─> npm install @dotcms/analytics

2. Create centralized config file
   └─> /src/config/analytics.config.js
       └─> export const analyticsConfig = { siteAuth, server, debug, ... }

3. (Optional) Add DotContentAnalytics for auto pageview tracking
   └─> /src/app/layout.js
       └─> import { analyticsConfig } from "@/config/analytics.config"
       └─> <DotContentAnalytics config={analyticsConfig} />

4. Import config in EVERY component that uses the hook
   └─> /src/components/MyComponent.js
       └─> import { analyticsConfig } from "@/config/analytics.config"
       └─> const { track } = useContentAnalytics(analyticsConfig) // ✅ Config required!
```

**Key Benefits of Centralized Config**:
- ✅ Single source of truth for configuration values
- ✅ Easy to update environment variables in one place
- ✅ Consistent config across all components
- ✅ Better than duplicating config in every file

## Installation Steps

### 1. Install the Package

Navigate to the Next.js example directory and install the package:

```bash
cd /core/examples/nextjs
npm install @dotcms/analytics
```

### 2. Verify Installation

Check that the package was added to `package.json`:

```bash
grep "@dotcms/analytics" package.json
```

Expected output: `"@dotcms/analytics": "latest"` or similar version.

### 3. Create Centralized Analytics Configuration

Create a dedicated configuration file to centralize your analytics settings. This makes it easier to maintain and reuse across your application.

**File**: `/core/examples/nextjs/src/config/analytics.config.js`

```javascript
/**
 * Centralized analytics configuration for dotCMS Content Analytics
 *
 * This configuration is used by:
 * - DotContentAnalytics provider in layout.js
 * - useContentAnalytics() hook when used standalone (optional)
 *
 * Environment variables required:
 * - NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY
 * - NEXT_PUBLIC_DOTCMS_ANALYTICS_HOST
 * - NEXT_PUBLIC_DOTCMS_ANALYTICS_DEBUG (optional)
 */
export const analyticsConfig = {
  siteAuth: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY,
  server: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_HOST,
  autoPageView: true, // Automatically track page views on route changes
  debug: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_DEBUG === "true",
  queue: {
    eventBatchSize: 15, // Send when 15 events are queued
    flushInterval: 5000, // Or send every 5 seconds (ms)
  },
};
```

**Benefits of this approach**:
- ✅ Single source of truth for analytics configuration
- ✅ Easy to import and reuse across components
- ✅ Centralized environment variable management
- ✅ Type-safe and IDE autocomplete friendly
- ✅ Easy to test and mock in unit tests

### 4. Configure Analytics in Next.js Layout

Update the root layout file to include the analytics provider using the centralized config.

**File**: `/core/examples/nextjs/src/app/layout.js`

```javascript
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({ subsets: ["latin"] });

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className={inter.className}>{children}</body>
    </html>
  );
}
```

**Updated with Analytics** (using centralized config):

```javascript
import { Inter } from "next/font/google";
import { DotContentAnalytics } from "@dotcms/analytics/react";
import { analyticsConfig } from "@/config/analytics.config";
import "./globals.css";

const inter = Inter({ subsets: ["latin"] });

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <DotContentAnalytics config={analyticsConfig} />
        {children}
      </body>
    </html>
  );
}
```

### 4. Add Environment Variables

Create or update `.env.local` file in the Next.js project root:

**File**: `/core/examples/nextjs/.env.local`

```bash
# dotCMS Analytics Configuration
NEXT_PUBLIC_DOTCMS_SITE_AUTH=your_site_auth_key_here
NEXT_PUBLIC_DOTCMS_SERVER=https://your-dotcms-server.com
```

**Important**: Replace `your_site_auth_key_here` with your actual dotCMS Analytics site auth key. This can be obtained from the Analytics app in your dotCMS instance.

### 5. Add `.env.local` to `.gitignore`

Ensure the environment file is not committed to version control:

```bash
# Check if already ignored
grep ".env.local" /core/examples/nextjs/.gitignore

# If not present, add it
echo ".env.local" >> /core/examples/nextjs/.gitignore
```

## Usage Examples

### Basic Setup (Automatic Page Views)

With the configuration above, page views are automatically tracked on every route change. No additional code needed!

### Manual Page View with Custom Data

Track page views with additional context:

```javascript
"use client";

import { useEffect } from "react";
import { useContentAnalytics } from "@dotcms/analytics/react";
import { analyticsConfig } from "@/config/analytics.config";

function MyComponent() {
  // ✅ ALWAYS pass config - import from centralized config file
  const { pageView } = useContentAnalytics(analyticsConfig);

  useEffect(() => {
    // Track page view with custom data
    pageView({
      contentType: "blog",
      category: "technology",
      author: "john-doe",
      wordCount: 1500,
    });
  }, []);

  return <div>Content here</div>;
}
```

### Track Custom Events

Track specific user interactions:

```javascript
"use client";

import { useContentAnalytics } from "@dotcms/analytics/react";
import { analyticsConfig } from "@/config/analytics.config";

function CallToActionButton() {
  // ✅ ALWAYS pass config - import from centralized config file
  const { track } = useContentAnalytics(analyticsConfig);

  const handleClick = () => {
    // Track custom event
    track("cta-click", {
      button: "Buy Now",
      location: "hero-section",
      price: 299.99,
    });
  };

  return <button onClick={handleClick}>Buy Now</button>;
}
```

### Form Submission Tracking

```javascript
"use client";

import { useContentAnalytics } from "@dotcms/analytics/react";
import { analyticsConfig } from "@/config/analytics.config";

function ContactForm() {
  const { track } = useContentAnalytics(analyticsConfig);

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Track form submission
    track("form-submit", {
      formName: "contact-form",
      formType: "lead-gen",
      source: "homepage",
    });

    // Submit form...
  };

  return <form onSubmit={handleSubmit}>{/* Form fields */}</form>;
}
```

### Video/Media Interaction Tracking

```javascript
"use client";

import { useContentAnalytics } from "@dotcms/analytics/react";
import { analyticsConfig } from "@/config/analytics.config";

function VideoPlayer({ videoId }) {
  const { track } = useContentAnalytics(analyticsConfig);

  const handlePlay = () => {
    track("video-play", {
      videoId,
      duration: 120,
      autoplay: false,
    });
  };

  const handleComplete = () => {
    track("video-complete", {
      videoId,
      watchPercentage: 100,
    });
  };

  return (
    <video onPlay={handlePlay} onEnded={handleComplete}>
      {/* Video sources */}
    </video>
  );
}
```

### E-commerce Product View Tracking

```javascript
"use client";

import { useEffect } from "react";
import { useContentAnalytics } from "@dotcms/analytics/react";
import { analyticsConfig } from "@/config/analytics.config";

function ProductPage({ product }) {
  const { track } = useContentAnalytics(analyticsConfig);

  useEffect(() => {
    // Track product view
    track("product-view", {
      productId: product.sku,
      productName: product.title,
      category: product.category,
      price: product.price,
      inStock: product.inventory > 0,
    });
  }, [product]);

  return <div>{/* Product details */}</div>;
}
```

## Configuration Options

### Analytics Config Object

| Option         | Type                   | Required | Default                | Description                                     |
| -------------- | ---------------------- | -------- | ---------------------- | ----------------------------------------------- |
| `siteAuth`     | `string`               | Yes      | -                      | Site authentication key from dotCMS Analytics   |
| `server`       | `string`               | Yes      | -                      | Your dotCMS server URL                          |
| `debug`        | `boolean`              | No       | `false`                | Enable verbose logging for debugging            |
| `autoPageView` | `boolean`              | No       | `true` (React)         | Automatically track page views on route changes |
| `queue`        | `QueueConfig \| false` | No       | Default queue settings | Event batching configuration                    |

### Queue Configuration

Controls how events are batched and sent:

| Option           | Type     | Default | Description                                    |
| ---------------- | -------- | ------- | ---------------------------------------------- |
| `eventBatchSize` | `number` | `15`    | Max events per batch - auto-sends when reached |
| `flushInterval`  | `number` | `5000`  | Time in ms between flushes                     |

**Disable Queuing** (send immediately):

```javascript
const analyticsConfig = {
  siteAuth: "your_key",
  server: "https://your-server.com",
  queue: false, // Send events immediately
};
```

## Data Captured Automatically

The SDK automatically enriches events with:

### Page View Events

- **Page Data**: URL, title, referrer, path, protocol, search params, hash
- **Device Data**: Screen resolution, viewport size, language, user agent
- **UTM Parameters**: Campaign tracking (source, medium, campaign, term, content)
- **Context**: Site key, session ID, user ID, timestamp

### Custom Events

- **Context**: Site key, session ID, user ID
- **Device Data**: Screen resolution, language, viewport dimensions
- **Custom Properties**: Any data you pass to `track()`

## Session Management

- **Duration**: 30-minute timeout of inactivity
- **Reset Conditions**:
  - At midnight UTC
  - When UTM campaign changes
- **Storage**: Uses `dot_analytics_session_id` in localStorage

## Identity Tracking

- **Anonymous User ID**: Persisted across sessions
- **Storage Key**: `dot_analytics_user_id`
- **Behavior**: Generated automatically on first visit, reused on subsequent visits

## Testing & Debugging

### Enable Debug Mode

Set `debug: true` in config to see verbose logging:

```javascript
const analyticsConfig = {
  siteAuth: "your_key",
  server: "https://your-server.com",
  debug: true, // Enable debug logging
};
```

### Verify Events in Network Tab

1. Open browser DevTools � Network tab
2. Filter by: `/api/v1/analytics/content/event`
3. Perform actions in your app
4. Check request payloads to see captured data

### Check Storage

Open browser DevTools � Application � Local Storage:

- `dot_analytics_user_id` - Anonymous user identifier
- `dot_analytics_session_id` - Current session ID
- `dot_analytics_session_utm` - UTM campaign data
- `dot_analytics_session_start` - Session start timestamp

## Troubleshooting

### Events Not Appearing

1. **Verify Configuration**:

   - Check `siteAuth` and `server` are correct
   - Enable `debug: true` to see console logs

2. **Check Network Requests**:

   - Look for requests to `/api/v1/analytics/content/event`
   - Verify they're returning 200 status

3. **Editor Mode Detection**:

   - Analytics are automatically disabled inside dotCMS editor
   - Test in preview or published mode

4. **Environment Variables**:
   - Ensure `.env.local` is loaded (restart dev server if needed)
   - Verify variable names start with `NEXT_PUBLIC_`

### Queue Not Flushing

- Check `eventBatchSize` - might not be reaching threshold
- Verify `flushInterval` is appropriate for your use case
- Events auto-flush on page navigation/close via `visibilitychange`

### Session Not Persisting

- Check localStorage is enabled in browser
- Verify no browser extensions are blocking storage
- Check console for storage-related errors

### Config File Issues

1. **Import Path Not Found**:
   ```javascript
   // ❌ Error: Cannot find module '@/config/analytics.config'
   ```
   - Verify the file exists at `/src/config/analytics.config.js`
   - Check your `jsconfig.json` or `tsconfig.json` has the `@` alias configured:
     ```json
     {
       "compilerOptions": {
         "paths": {
           "@/*": ["./src/*"]
         }
       }
     }
     ```

2. **Undefined Config Values**:
   ```javascript
   // Config shows undefined for siteAuth or server
   ```
   - Verify environment variables are set in `.env.local`
   - Restart dev server after changing `.env.local`
   - Check variable names start with `NEXT_PUBLIC_`

3. **Config Not Updated**:
   - Clear Next.js cache: `rm -rf .next`
   - Restart dev server: `npm run dev`

## Integration with Existing Next.js Example

The Next.js example at `/core/examples/nextjs` already uses other dotCMS SDK packages:

- `@dotcms/client` - Core API client
- `@dotcms/experiments` - A/B testing
- `@dotcms/react` - React components
- `@dotcms/types` - TypeScript types
- `@dotcms/uve` - Universal Visual Editor

Adding analytics complements these by providing:

- Usage tracking across all content types
- User behavior insights
- Campaign performance metrics
- Content engagement analytics

## API Reference

### Component: `DotContentAnalytics`

```typescript
interface AnalyticsConfig {
  siteAuth: string;
  server: string;
  debug?: boolean;
  autoPageView?: boolean;
  queue?: QueueConfig | false;
}

interface QueueConfig {
  eventBatchSize?: number;
  flushInterval?: number;
}

<DotContentAnalytics config={analyticsConfig} />;
```

### Hook: `useContentAnalytics`

```typescript
interface ContentAnalyticsHook {
  pageView: (customData?: Record<string, unknown>) => void;
  track: (eventName: string, properties?: Record<string, unknown>) => void;
}

// ✅ CORRECT: Always pass config - import from centralized config file
import { analyticsConfig } from "@/config/analytics.config";
const { pageView, track } = useContentAnalytics(analyticsConfig);
```

**CRITICAL**: The hook **ALWAYS requires config as a parameter**. There is no provider pattern for the hook - `<DotContentAnalytics />` is only for auto pageview tracking and does NOT provide context to child components.

**Always import and pass the centralized config** from `/config/analytics.config.js` to ensure consistency.

### Methods

#### `pageView(customData?)`

Track a page view with optional custom data. Automatically captures page, device, UTM, and context data.

**Parameters**:

- `customData` (optional): Object with custom properties to attach

**Example**:

```javascript
pageView({
  contentType: "product",
  category: "electronics",
});
```

#### `track(eventName, properties?)`

Track a custom event with optional properties.

**Parameters**:

- `eventName` (required): String identifier for the event (cannot be "pageview")
- `properties` (optional): Object with event-specific data

**Example**:

```javascript
track("button-click", {
  label: "Subscribe",
  location: "sidebar",
});
```

## Best Practices

1. **Centralize Configuration**: Create a dedicated config file (`/config/analytics.config.js`) for all analytics settings
   ```javascript
   // ✅ GOOD: Centralized config file
   // /config/analytics.config.js
   export const analyticsConfig = {
     siteAuth: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY,
     server: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_HOST,
     debug: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_DEBUG === "true",
     autoPageView: true,
   };

   // ❌ BAD: Inline config in multiple files
   // component1.js
   const config = { siteAuth: "...", server: "..." };
   // component2.js
   const config = { siteAuth: "...", server: "..." }; // Duplicate!
   ```

2. **Always Import and Pass Config**: The hook requires config as a parameter
   ```javascript
   // ✅ CORRECT: Import centralized config in every component
   // MyComponent.js
   import { analyticsConfig } from "@/config/analytics.config";
   const { track } = useContentAnalytics(analyticsConfig);

   // ❌ WRONG: Inline config duplication
   // MyComponent.js
   const { track } = useContentAnalytics({
     siteAuth: "...",  // Duplicated!
     server: "..."     // Duplicated!
   });
   ```

3. **Use DotContentAnalytics for Auto PageViews**: Add to layout for automatic tracking
   ```javascript
   // layout.js - For automatic pageview tracking only
   import { analyticsConfig } from "@/config/analytics.config";
   <DotContentAnalytics config={analyticsConfig} />
   ```

4. **Environment Variables**: Always use environment variables for sensitive config (siteAuth)

3. **Event Naming**: Use consistent, descriptive event names (e.g., `cta-click`, not just `click`)

4. **Custom Data**: Include relevant context in event properties

5. **Queue Configuration**: Use default queue settings unless you have specific performance needs

6. **Debug Mode**: Enable only in development, disable in production

7. **Auto Page Views**: Keep enabled for SPAs (Next.js) to track route changes

## Related Resources

- Analytics SDK README: `/core/core-web/libs/sdk/analytics/README.md`
- Package Location: `/core/core-web/libs/sdk/analytics/`
- Next.js Example: `/core/examples/nextjs/`

## Quick Command Reference

```bash
# Install package
cd /core/examples/nextjs
npm install @dotcms/analytics

# Start Next.js dev server
npm run dev

# Build for production
npm run build

# Start production server
npm run start

# Verify installation
npm list @dotcms/analytics
```
