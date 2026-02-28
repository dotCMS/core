# @dotcms/analytics

Content Analytics SDK for tracking content-aware events in dotCMS-powered React applications.

## Quick Start

### 1. Install

```bash
npm install @dotcms/analytics
```

### 2. Create a centralized config file

```javascript
// src/config/analytics.config.js
export const analyticsConfig = {
    siteAuth: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY,
    server: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_HOST,
    autoPageView: true,
    debug: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_DEBUG === 'true',
    impressions: true,
    clicks: true
};
```

### 3. Add automatic page view tracking to your layout

```jsx
// src/app/layout.js
import { DotContentAnalytics } from '@dotcms/analytics/react';
import { analyticsConfig } from '@/config/analytics.config';

export default function RootLayout({ children }) {
    return (
        <html lang="en">
            <body>
                <DotContentAnalytics config={analyticsConfig} />
                {children}
            </body>
        </html>
    );
}
```

### 4. Track events in your components

```jsx
'use client';

import { useContentAnalytics } from '@dotcms/analytics/react';
import { analyticsConfig } from '@/config/analytics.config';

function ContactForm() {
    const { conversion } = useContentAnalytics(analyticsConfig);

    const handleSubmit = (e) => {
        e.preventDefault();
        // ... submit form logic ...

        // Track conversion ONLY after successful submission
        conversion('form-submit', {
            formName: 'contact-us',
            formType: 'lead-gen'
        });
    };

    return <form onSubmit={handleSubmit}>{/* form fields */}</form>;
}
```

## Understanding the Components

The SDK exports two React primitives. Understanding their roles is critical for correct usage.

### `<DotContentAnalytics />` -- Automatic Page View Tracker

-   Its **only purpose** is to automatically track page views on route changes
-   It is **NOT** a React Context Provider
-   It does **NOT** share config with child components
-   Place it once in your root layout

### `useContentAnalytics(config)` -- Manual Tracking Hook

-   Used for custom events, conversions, and manual page views
-   **ALWAYS requires `config` as a parameter** -- it does not read from context
-   Import the centralized config in every component that uses the hook

```jsx
// Every component that tracks events must import config explicitly
import { useContentAnalytics } from '@dotcms/analytics/react';
import { analyticsConfig } from '@/config/analytics.config';

const { track, pageView, conversion } = useContentAnalytics(analyticsConfig);
```

> **Why centralize config?** You must import it in each component, but having a single file prevents duplication and makes updates easier.

## Configuration

### Environment Variables

Add these to your `.env.local` file:

```bash
NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY=YOUR_ANALYTICS_SITE_KEY
NEXT_PUBLIC_DOTCMS_ANALYTICS_HOST=http://localhost:8080
NEXT_PUBLIC_DOTCMS_ANALYTICS_DEBUG=true
```

| Variable | Description |
| --- | --- |
| `NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY` | Site auth key from the Content Analytics app in dotCMS |
| `NEXT_PUBLIC_DOTCMS_ANALYTICS_HOST` | URL where your dotCMS instance is running |
| `NEXT_PUBLIC_DOTCMS_ANALYTICS_DEBUG` | Set to `"true"` to enable verbose console logging |

### Config Options

| Option | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| `siteAuth` | `string` | Yes | -- | Site auth from dotCMS Analytics app |
| `server` | `string` | Yes | -- | Your dotCMS server URL |
| `debug` | `boolean` | No | `false` | Enable verbose logging |
| `autoPageView` | `boolean` | No | `true` | Auto track page views on route changes |
| `queue` | `QueueConfig \| false` | No | See below | Event batching configuration |
| `impressions` | `ImpressionConfig \| boolean` | No | `false` | Content impression tracking |
| `clicks` | `boolean` | No | `false` | Content click tracking (300ms throttle) |

### Queue Configuration

Controls how events are batched before being sent to the server:

-   **`false`**: Disable queuing, send events immediately
-   **`undefined` (default)**: Enable queuing with default settings
-   **`QueueConfig` object**: Custom settings

| Option | Type | Default | Description |
| --- | --- | --- | --- |
| `eventBatchSize` | `number` | `15` | Max events per batch -- auto-sends when reached |
| `flushInterval` | `number` | `5000` | Time between flushes in milliseconds |

How it works:

-   Sends immediately when `eventBatchSize` is reached
-   Sends pending events every `flushInterval` milliseconds
-   Auto-flushes on page navigation/close using `visibilitychange` + `pagehide`
-   Uses `navigator.sendBeacon()` for reliable delivery on page unload

```javascript
// Disable queuing (send immediately)
export const analyticsConfig = {
    siteAuth: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY,
    server: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_HOST,
    queue: false
};

// Custom queue settings
export const analyticsConfig = {
    siteAuth: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY,
    server: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_HOST,
    queue: {
        eventBatchSize: 10,
        flushInterval: 3000
    }
};
```

### Impression Tracking

Controls automatic tracking of content visibility in the viewport:

-   **`false` or `undefined` (default)**: Disabled
-   **`true`**: Enabled with default settings
-   **`ImpressionConfig` object**: Custom settings

| Option | Type | Default | Description |
| --- | --- | --- | --- |
| `visibilityThreshold` | `number` | `0.5` | Min percentage visible (0.0 to 1.0) |
| `dwellMs` | `number` | `750` | Min time visible in milliseconds |
| `maxNodes` | `number` | `1000` | Max elements to track (performance limit) |

How it works:

-   Tracks contentlets marked with `dotcms-contentlet` class and `data-dot-*` attributes
-   Uses Intersection Observer API for performance
-   Only fires when an element is visible for the configured threshold and dwell time
-   One impression per contentlet per session (no duplicates)
-   Automatically disabled in dotCMS editor mode

```javascript
// Enable with defaults (50% visible, 750ms dwell)
export const analyticsConfig = {
    // ...required fields
    impressions: true
};

// Custom thresholds
export const analyticsConfig = {
    // ...required fields
    impressions: {
        visibilityThreshold: 0.7,
        dwellMs: 1000,
        maxNodes: 500
    }
};
```

### Click Tracking

Controls automatic tracking of user clicks on content elements:

-   **`false` or `undefined` (default)**: Disabled
-   **`true`**: Enabled with 300ms throttle

How it works:

-   Tracks clicks on `<a>` and `<button>` elements within contentlets
-   Contentlets must be marked with `dotcms-contentlet` class and `data-dot-*` attributes
-   Captures semantic attributes (`href`, `aria-label`, `data-*`) and excludes CSS classes
-   Throttles rapid clicks to prevent duplicates (300ms)
-   Automatically disabled in dotCMS editor mode

**Captured data per click:**

-   **Content Info**: `identifier`, `inode`, `title`, `content_type`
-   **Element Info**: `text`, `type` (a/button), `id`, `class`, `href`, `attributes`
-   **Position Info**: `viewport_offset_pct`, `dom_index`

You can enrich click data using `data-*` attributes in your HTML:

```html
<a
    href="/signup"
    id="cta-signup"
    data-category="primary-cta"
    data-campaign="summer-sale"
    aria-label="Sign up for free trial">
    Start Free Trial
</a>

<button data-action="download" data-file-type="pdf" data-category="lead-magnet">
    Download Whitepaper
</button>
```

## Core Concepts

### Event Types

The SDK sends four types of events, identified by `event_type`:

| Event Type | Trigger | Requires |
| --- | --- | --- |
| `pageview` | Automatically on route change, or manually via `pageView()` | `autoPageView: true` or manual call |
| `content_impression` | When a contentlet becomes visible in the viewport | `impressions` config enabled |
| `content_click` | When a user clicks a link/button inside a contentlet | `clicks` config enabled |
| `conversion` | Explicitly via `conversion()` after a successful business action | Manual call |

### Page Views

The `pageView()` method tracks page navigation events. It **automatically enriches** the event with:

-   **Page data**: URL, title, referrer, path, protocol, search params
-   **Device data**: Screen resolution, viewport size, language, user agent
-   **UTM parameters**: Campaign tracking data (source, medium, campaign, term, content)
-   **Context**: Site key, session ID, user ID, timestamp

You can optionally pass custom data that will be sent **in addition** to all the automatic enrichment.

### Conversion Tracking

The `conversion()` method tracks user conversions (purchases, downloads, sign-ups, etc.).

**Only track conversions after a successful action or completed goal.** Tracking on clicks or attempts (before success) diminishes their value as conversion metrics. Track when:

-   Purchase is completed and payment is confirmed
-   Download is successfully completed
-   Sign-up form is submitted and account is created
-   Any business goal is actually achieved

### Custom Events

The `track()` method tracks any custom user action with a unique event name and optional properties.

-   `eventName` cannot be `"pageview"` or `"conversion"` (reserved)
-   Use descriptive names: `"button-click"`, `"form-submit"`, `"video-play"`, etc.

### Sessions

-   30-minute inactivity timeout
-   Resets at midnight UTC
-   New session if UTM campaign changes

### Identity

-   Anonymous user ID persisted across sessions
-   Stored in `dot_analytics_user_id`

## Usage Examples

### Automatic Page View Tracking

Add `DotContentAnalytics` to your root layout. No additional code is needed -- page views are tracked on every route change.

```jsx
// src/app/layout.js
import { DotContentAnalytics } from '@dotcms/analytics/react';
import { analyticsConfig } from '@/config/analytics.config';

export default function RootLayout({ children }) {
    return (
        <html lang="en">
            <body>
                <DotContentAnalytics config={analyticsConfig} />
                {children}
            </body>
        </html>
    );
}
```

### Manual Page View with Custom Data

```jsx
'use client';

import { useEffect } from 'react';
import { useContentAnalytics } from '@dotcms/analytics/react';
import { analyticsConfig } from '@/config/analytics.config';

function BlogPost({ post }) {
    const { pageView } = useContentAnalytics(analyticsConfig);

    useEffect(() => {
        pageView({
            contentType: 'blog',
            category: post.category,
            author: post.author,
            wordCount: post.wordCount
        });
    }, []);

    return <article>{/* post content */}</article>;
}
```

### Custom Event Tracking

```jsx
'use client';

import { useContentAnalytics } from '@dotcms/analytics/react';
import { analyticsConfig } from '@/config/analytics.config';

function CallToAction() {
    const { track } = useContentAnalytics(analyticsConfig);

    const handleClick = () => {
        track('cta-click', {
            button: 'Buy Now',
            location: 'hero-section',
            price: 299.99
        });
    };

    return <button onClick={handleClick}>Buy Now</button>;
}
```

### Conversion Tracking (Real-World Example)

This example is based on the Contact Us form in the [Next.js example app](https://github.com/dotCMS/core/tree/main/examples/nextjs):

```jsx
'use client';

import { useState } from 'react';
import { useContentAnalytics } from '@dotcms/analytics/react';
import { analyticsConfig } from '@/config/analytics.config';

export default function ContactUs({ description }) {
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isSuccess, setIsSuccess] = useState(false);
    const { conversion } = useContentAnalytics(analyticsConfig);

    const handleSubmit = (e) => {
        e.preventDefault();
        setIsSubmitting(true);

        // Simulate form submission
        setTimeout(() => {
            setIsSuccess(true);

            // Track conversion ONLY after successful submission
            conversion('form-submit', {
                formName: 'contact-us',
                formType: 'lead-gen'
            });
        }, 3000);
    };

    return (
        <form onSubmit={handleSubmit}>
            {/* form fields */}
            <button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Submitting...' : 'Submit'}
            </button>
        </form>
    );
}
```

### E-commerce Purchase Conversion

```jsx
'use client';

import { useContentAnalytics } from '@dotcms/analytics/react';
import { analyticsConfig } from '@/config/analytics.config';

function CheckoutButton({ product, quantity }) {
    const { conversion } = useContentAnalytics(analyticsConfig);

    const handlePurchase = async () => {
        // Process payment...
        const result = await processPayment(product, quantity);

        if (result.success) {
            // Track conversion ONLY after confirmed payment
            conversion('purchase', {
                value: product.price * quantity,
                currency: 'USD',
                productId: product.sku,
                category: product.category
            });
        }
    };

    return <button onClick={handlePurchase}>Complete Purchase</button>;
}
```

## API Reference

### `<DotContentAnalytics />`

React component for automatic page view tracking on route changes.

```typescript
interface DotContentAnalyticsProps {
    config: DotCMSAnalyticsConfig;
}
```

Place once in your root layout. Wraps the internal tracker in `<Suspense>` for Next.js App Router compatibility.

### `useContentAnalytics(config)`

React hook that returns tracking methods. **Always requires config as a parameter.**

```typescript
function useContentAnalytics(config: DotCMSAnalyticsConfig): DotCMSAnalytics;

interface DotCMSAnalytics {
    /** Track a page view with optional custom data */
    pageView: (customData?: Record<string, unknown>) => void;

    /** Track a custom event (eventName cannot be "pageview" or "conversion") */
    track: (eventName: string, properties?: Record<string, unknown>) => void;

    /** Track a conversion after a successful business action */
    conversion: (name: string, options?: Record<string, unknown>) => void;
}
```

### `DotCMSAnalyticsConfig`

```typescript
interface DotCMSAnalyticsConfig {
    server: string;
    siteAuth: string;
    debug?: boolean;
    autoPageView?: boolean;
    queue?: QueueConfig | false;
    impressions?: ImpressionConfig | boolean;
    clicks?: boolean;
}

interface QueueConfig {
    eventBatchSize?: number;
    flushInterval?: number;
}

interface ImpressionConfig {
    visibilityThreshold?: number;
    dwellMs?: number;
    maxNodes?: number;
}
```

### Event Payload Structures

#### Page View Event

When you call `pageView(customData?)`, the SDK sends:

```typescript
{
    context: {
        site_key: string;          // Your site key
        session_id: string;        // Current session ID
        user_id: string;           // Anonymous user ID
        device: {
            screen_resolution: string;
            language: string;
            viewport_width: string;
            viewport_height: string;
        }
    },
    events: [{
        event_type: "pageview",
        local_time: string,        // ISO 8601 timestamp with timezone
        data: {
            page: {                // Captured automatically
                url: string;
                title: string;
                referrer: string;
                path: string;
                doc_host: string;
                doc_protocol: string;
                doc_search: string;
                doc_hash: string;
                doc_encoding: string;
            },
            utm?: {                // Captured automatically (if present in URL)
                source: string;
                medium: string;
                campaign: string;
                term: string;
                content: string;
            },
            custom?: {             // Your optional data from pageView(customData)
                // Any properties you pass
            }
        }
    }]
}
```

#### Custom Event

When you call `track(eventName, properties)`:

```typescript
{
    context: { /* same as above */ },
    events: [{
        event_type: string,        // Your custom event name
        local_time: string,
        data: {
            custom: {
                // Your properties object
            }
        }
    }]
}
```

#### Conversion Event

When you call `conversion(name, options)`:

```typescript
{
    context: { /* same as above */ },
    events: [{
        event_type: "conversion",
        local_time: string,
        data: {
            conversion: {
                name: string;      // Your conversion name
            },
            page: {
                url: string;
                title: string;
            },
            custom?: {             // Your optional data from options parameter
                // All properties from options
            }
        }
    }]
}
```

#### Click Event

When click tracking is enabled and a user clicks on a contentlet element:

```json
{
    "content": {
        "identifier": "abc123",
        "inode": "xyz789",
        "title": "Product Page",
        "content_type": "Page"
    },
    "element": {
        "text": "Start Free Trial",
        "type": "a",
        "id": "cta-signup",
        "class": "btn btn-primary",
        "href": "/signup",
        "attributes": [
            "data-category:primary-cta",
            "data-campaign:summer-sale",
            "aria-label:Sign up for free trial"
        ]
    },
    "position": {
        "viewport_offset_pct": 45.2,
        "dom_index": 2
    }
}
```

## Under the Hood

### Storage Keys

| Key | Purpose |
| --- | --- |
| `dot_analytics_user_id` | Anonymous user identifier (persisted across sessions) |
| `dot_analytics_session_id` | Current session ID |
| `dot_analytics_session_utm` | UTM campaign data for the session |
| `dot_analytics_session_start` | Session start timestamp |

### Editor Detection

Analytics are automatically disabled when inside the dotCMS Universal Visual Editor (UVE). No events are sent in editor mode.

### Endpoint

All events are sent via `POST` to:

```
{server}/api/v1/analytics/content/event
```

Where `{server}` is the `server` value from your config.

## Debugging & Troubleshooting

### Enable Debug Mode

Set `debug: true` in your config to see verbose logging in the browser console.

### Verify Events in the Network Tab

1. Open browser DevTools > Network tab
2. Filter by `/api/v1/analytics/content/event`
3. Perform actions in your app
4. Inspect request payloads to see captured data

### Check Storage

Open browser DevTools > Application > Local Storage and look for:

-   `dot_analytics_user_id`
-   `dot_analytics_session_id`
-   `dot_analytics_session_utm`
-   `dot_analytics_session_start`

### Common Issues

**Events not appearing?**

-   Verify `siteAuth` and `server` are correct in your config
-   Enable `debug: true` to see console logs
-   Ensure environment variables are set in `.env.local` (restart dev server after changes)
-   Check that variable names start with `NEXT_PUBLIC_`
-   Analytics are auto-disabled in dotCMS editor mode -- test in preview or published mode

**Queue not flushing?**

-   Check `eventBatchSize` -- the threshold might not be reached yet
-   Verify `flushInterval` is appropriate for your use case
-   Events auto-flush on page navigation/close via `visibilitychange`

**Session not persisting?**

-   Check that localStorage is enabled in the browser
-   Verify no browser extensions are blocking storage

## Support

We offer multiple channels to get help with the dotCMS Analytics SDK:

-   **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.
-   **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions.
-   **Stack Overflow**: Use the tag `dotcms-analytics` when posting questions.
-   **Enterprise Support**: Enterprise customers can access premium support through the [dotCMS Support Portal](https://helpdesk.dotcms.com/support/).

When reporting issues, please include:

-   SDK version you're using
-   Framework/library version (if applicable)
-   Minimal reproduction steps
-   Expected vs. actual behavior

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the dotCMS Analytics SDK! If you'd like to contribute, please follow these steps:

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
