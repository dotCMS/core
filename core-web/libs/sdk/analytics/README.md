# dotCMS Content Analytics SDK (@dotcms/analytics)

Lightweight JavaScript SDK for tracking content-aware events in dotCMS. Works in vanilla JS and React apps. Angular & Vue support coming soon.

## 🚀 Quick Start

### Standalone (Script Tag)

```html
<script
    src="ca.min.js"
    data-analytics-server="https://demo.dotcms.com"
    data-analytics-auth="SITE_AUTH"
    data-analytics-auto-page-view="true"
    data-analytics-debug="false"></script>
```

**npm (ES Module)**

```bash
npm install @dotcms/analytics
```

```javascript
import { initializeContentAnalytics } from '@dotcms/analytics';

const analytics = initializeContentAnalytics({
    siteAuth: 'SITE_AUTH',
    server: 'https://demo.dotcms.com'
});

analytics.track('page-loaded');
```

### React (In Development)

```bash
npm install @dotcms/analytics
```

```tsx
import { DotContentAnalytics } from '@dotcms/analytics/react';

const config = {
    siteAuth: 'SITE_AUTH',
    server: 'https://demo.dotcms.com',
    autoPageView: true // Optional, default is true in React
};

export function AppRoot() {
    return <DotContentAnalytics config={config} />;
}
```

> **Note:** React API is subject to change during development.

## 📘 Core Concepts

### Page Views

The `pageView()` method tracks page navigation events. **It automatically enriches the event with comprehensive data**, including:

-   **Page data**: URL, title, referrer, path, protocol, search params, etc.
-   **Device data**: Screen resolution, viewport size, language, user agent
-   **UTM parameters**: Campaign tracking data (source, medium, campaign, etc.)
-   **Context**: Site key, session ID, user ID, timestamp

You can optionally include custom data that will be sent **in addition** to all the automatic enrichment.

**Method signature:**

```typescript
pageView(customData?: Record<string, unknown>): void
```

**Behavior:**

-   **Standalone (IIFE)**: Auto-tracked only if `data-analytics-auto-page-view="true"`; otherwise call `window.dotAnalytics.pageView()` manually.
-   **React**: In development (API may change)
-   Custom data is optional and gets attached to the pageview event under the `custom` property alongside all automatically captured data.

### Custom Events

The `track()` method allows you to track any custom user action with a unique event name and optional properties.

**Method signature:**

```typescript
track(eventName: string, properties?: Record<string, unknown>): void
```

**Important:**

-   `eventName` cannot be `"pageview"` (reserved for page view tracking)
-   `eventName` should be a descriptive string like `"button-click"`, `"form-submit"`, `"video-play"`, etc.
-   `properties` is optional and can contain any custom data relevant to the event

### Sessions

-   30-minute timeout
-   Resets at midnight UTC
-   New session if UTM campaign changes

### Identity

-   Anonymous user ID persisted across sessions
-   Stored in `dot_analytics_user_id`

## ⚙️ Configuration Options

| Option | Type | Required | Default | Description |
| -------------- | --------- | -------- | ----------------------------------- | -------------------------------------- |
| `siteAuth` | `string` | ✅ | - | Site auth from dotCMS Analytics app |
| `server` | `string` | ✅ | - | Your dotCMS server URL |
| `debug` | `boolean` | ❌ | `false` | Enable verbose logging |
| `autoPageView` | `boolean` | ❌ | React: `true` / Standalone: `false` | Auto track page views on route changes |
| `queueConfig` | `QueueConfig` | ❌ | See below | Event batching configuration |

### Queue Configuration

The `queueConfig` option controls event batching:

-   **`false`**: Disable queuing, send events immediately
-   **`undefined` (default)**: Enable queuing with default settings
-   **`QueueConfig` object**: Enable queuing with custom settings

| Option           | Type     | Default | Description                                      |
| ---------------- | -------- | ------- | ------------------------------------------------ |
| `eventBatchSize` | `number` | `15`    | Max events per batch - auto-sends when reached   |
| `flushInterval`  | `number` | `5000`  | Time between flushes - sends pending events (ms) |

**How it works:**

-   ✅ Send immediately when `eventBatchSize` reached (e.g., 15 events)
-   ✅ Send pending events every `flushInterval` (e.g., 5 seconds)
-   ✅ Auto-flush on page navigation/close using `visibilitychange` + `pagehide` events
-   Example: If you have 10 events and 5 seconds pass → sends those 10

**About page unload handling:**

The SDK uses modern APIs (`visibilitychange` + `pagehide`) instead of `beforeunload`/`unload` to ensure:

-   ✅ Better reliability on mobile devices
-   ✅ Compatible with browser back/forward cache (bfcache)
-   ✅ Events are sent via `navigator.sendBeacon()` for guaranteed delivery
-   ✅ No negative impact on page performance

**Example: Disable queuing for immediate sends**

```javascript
const analytics = initializeContentAnalytics({
    siteAuth: 'abc123',
    server: 'https://your-dotcms.com',
    queue: false // Send events immediately without batching
});
```

**Example: Custom queue config**

```javascript
const analytics = initializeContentAnalytics({
    siteAuth: 'abc123',
    server: 'https://your-dotcms.com',
    queue: {
        eventBatchSize: 10, // Auto-send when 10 events queued
        flushInterval: 3000 // Or send every 3 seconds
    }
});
```

## 🛠️ Usage Examples

### Vanilla JavaScript

**Basic Page View**

```javascript
// After init with the <script> tag, dotAnalytics is added to the window
// Automatically captures: page, device, UTM, context (session, user, site)
window.dotAnalytics.pageView();
```

**Page View with Additional Custom Data**

```javascript
// All automatic data (page, device, UTM, context) is STILL captured
// Plus your custom properties are added under 'custom'
window.dotAnalytics.pageView({
    contentType: 'blog',
    category: 'technology',
    author: 'john-doe',
    wordCount: 1500
});

// The server receives: page + device + utm + context + custom (your data)
```

**Track Custom Events**

```javascript
// Basic event tracking
window.dotAnalytics.track('cta-click', {
    button: 'Buy Now',
    location: 'hero-section'
});

// Track form submission
window.dotAnalytics.track('form-submit', {
    formName: 'contact-form',
    formType: 'lead-gen'
});

// Track video interaction
window.dotAnalytics.track('video-play', {
    videoId: 'intro-video',
    duration: 120,
    autoplay: false
});
```

**Advanced: Manual Init with Custom Properties**

```javascript
const analytics = initializeContentAnalytics({
    siteAuth: 'abc123',
    server: 'https://your-dotcms.com',
    debug: true,
    autoPageView: false
});

// Track custom events with properties
analytics.track('product-view', {
    productId: 'SKU-12345',
    category: 'Electronics',
    price: 299.99,
    inStock: true
});

// Manual page view with custom data
// Automatic enrichment (page, device, UTM, context) + your custom data
analytics.pageView({
    section: 'checkout',
    step: 'payment',
    cartValue: 299.99
});
```

### React

> **Note:** React integration is currently in development. The API may change.

**Basic Setup**

```tsx
import { DotContentAnalytics } from '@dotcms/analytics/react';

const config = {
    siteAuth: 'SITE_KEY',
    server: 'https://demo.dotcms.com',
    autoPageView: true
};

export function AppRoot() {
    return <DotContentAnalytics config={config} />;
}
```

**Using the Hook**

```tsx
import { useContentAnalytics } from '@dotcms/analytics/react';

function MyComponent() {
    const { track, pageView } = useContentAnalytics({
        siteAuth: 'SITE_AUTH',
        server: 'https://demo.dotcms.com'
    });

    // Track custom events (same API as vanilla JS)
    const handleClick = () => {
        track('button-click', { label: 'CTA Button' });
    };

    return <button onClick={handleClick}>Click Me</button>;
}
```

## API Reference

```typescript
interface DotCMSAnalytics {
    /**
     * Track a page view event with optional custom data
     * @param customData - Optional object with custom properties to attach to the pageview
     */
    pageView: (customData?: Record<string, unknown>) => void;

    /**
     * Track a custom event
     * @param eventName - Name of the custom event (cannot be "pageview")
     * @param properties - Optional object with event-specific properties
     */
    track: (eventName: string, properties?: Record<string, unknown>) => void;
}
```

### Page View Event Structure

When you call `pageView(customData?)`, the SDK **automatically enriches** the event with comprehensive data and sends:

```typescript
{
    context: {                     // 🤖 AUTOMATIC - Identity & Session
        site_key: string;          //    Your site key
        session_id: string;        //    Current session ID
        user_id: string;           //    Anonymous user ID
        device: {                  // 🤖 AUTOMATIC - Device & Browser Info
            screen_resolution: string;  // Screen size
            language: string;           // Browser language
            viewport_width: string;     // Viewport width
            viewport_height: string;    // Viewport height
        }
    },
    events: [{
        event_type: "pageview",
        local_time: string,        // 🤖 AUTOMATIC - ISO 8601 timestamp with timezone
        data: {
            page: {                // 🤖 AUTOMATIC - Page Information
                url: string;       //    Full URL
                title: string;     //    Page title
                referrer: string;  //    Referrer URL
                path: string;      //    Path
                doc_host: string;  //    Hostname
                doc_protocol: string;  //  Protocol (http/https)
                doc_search: string;    //  Query string
                doc_hash: string;      //  URL hash
                doc_encoding: string;  //  Character encoding
            },
            utm?: {                // 🤖 AUTOMATIC - Campaign Tracking (if present in URL)
                source: string;    //    utm_source
                medium: string;    //    utm_medium
                campaign: string;  //    utm_campaign
                term: string;      //    utm_term
                content: string;   //    utm_content
            },
            custom?: {             // 👤 YOUR DATA (optional)
                // Any custom properties you pass to pageView(customData)
                contentType?: string;
                category?: string;
                author?: string;
                // ... any other properties
            }
        }
    }]
}
```

**Key Points:**

-   🤖 Most data is captured **automatically** - you don't need to provide it
-   👤 `custom` is where **your optional data** goes
-   All automatic data is always captured, even if you don't pass `customData`

### Custom Event Structure

When you call `track(eventName, properties)`, the following structure is sent:

```typescript
{
    context: {
        site_key: string;      // Your site key
        session_id: string;    // Current session ID
        user_id: string;       // Anonymous user ID
        device: {              // 🤖 AUTOMATIC - Device & Browser Info
            screen_resolution: string;  // Screen size
            language: string;           // Browser language
            viewport_width: string;     // Viewport width
            viewport_height: string;    // Viewport height
        }
    },
    events: [{
        event_type: string,    // Your custom event name
        local_time: string,    // ISO 8601 timestamp
        data: {
            custom: {
                // Your properties object
            }
        }
    }]
}
```

## Under the Hood

### Storage Keys

-   `dot_analytics_user_id`
-   `dot_analytics_session_id`
-   `dot_analytics_session_utm`
-   `dot_analytics_session_start`

### Editor Detection

Analytics are disabled when inside the dotCMS editor.

## Debugging & Troubleshooting

**Not seeing events?**

-   Ensure `siteKey` & `server` are correct
-   Enable debug mode
-   Check network requests to: `https://your-server/api/v1/analytics/content/event`
-   Avoid using inside dotCMS editor (auto-disabled)

Standalone attributes to verify:

-   `data-analytics-auth` (required)
-   `data-analytics-server` (optional, defaults to current origin)
-   `data-analytics-auto-page-view` (`true` to enable)
-   `data-analytics-debug` (`true` to enable)

## Roadmap

-   Scroll depth & file download tracking
-   Form interaction analytics
-   Angular & Vue support
-   Realtime dashboard

## dotCMS Support

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

## How To Contribute

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the DotCMS Analytics SDK! If you'd like to contribute, please follow these steps:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

## Licensing Information

dotCMS comes in multiple editions and as such is dual-licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization, and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds several enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).

This SDK is part of dotCMS's dual-licensed platform (GPL 3.0 for Community, commercial license for Enterprise).

[Learn more ](https://www.dotcms.com)at [dotcms.com](https://www.dotcms.com).
