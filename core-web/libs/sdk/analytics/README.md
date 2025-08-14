# dotCMS Content Analytics SDK (@dotcms/analytics)

Lightweight JavaScript SDK for tracking content-aware events in dotCMS. Works in vanilla JS and React apps. Angular & Vue support coming soon.

## 🚀 Quick Start

### Vanilla JavaScript

**CDN (Script Tag - Auto Page View)**

```html
<script
    src="ca.min.js"
    data-server="https://demo.dotcms.com"
    data-site-key="SITE_KEY"
    data-auto-page-view></script>
```

**npm (ES Module)**

```bash
npm install @dotcms/analytics
```

```javascript
import { initializeContentAnalytics } from '@dotcms/analytics';

const analytics = initializeContentAnalytics({
    siteKey: 'SITE_KEY',
    server: 'https://demo.dotcms.com'
});

analytics.track('page-loaded');
```

### React

```bash
npm install @dotcms/analytics
```

```tsx
import { DotContentAnalyticsProvider } from '@dotcms/analytics/react';

const config = {
    siteKey: 'SITE_KEY',
    server: 'https://demo.dotcms.com',
    autoPageView: true // Optional, default is true
};

<DotContentAnalyticsProvider config={config}>
    <App />
</DotContentAnalyticsProvider>;
```

## 📘 Core Concepts

### Events

Track any user action as an event using `track('event-name', { payload })`.

### Page Views

Tracked automatically (or manually) on route changes.

### Sessions

-   30-minute timeout
-   Resets at midnight UTC
-   New session if UTM campaign changes

### Identity

-   Anonymous user ID persisted across sessions
-   Stored in `dot_analytics_user_id`

## ⚙️ Configuration Options

| Option         | Type       | Required | Default | Description                            |
| -------------- | ---------- | -------- | ------- | -------------------------------------- |
| `siteKey`      | `string`   | ✅       | -       | Site key from dotCMS Analytics app     |
| `server`       | `string`   | ✅       | -       | Your dotCMS server URL                 |
| `debug`        | `boolean`  | ❌       | `false` | Enable verbose logging                 |
| `autoPageView` | `boolean`  | ❌       | `true`  | Auto track page views on route changes |
| `redirectFn`   | `function` | ❌       | -       | Custom handler for redirects           |

## 🛠️ Usage Examples

### Vanilla JavaScript

**Manual Page View & Events**

```javascript
// After init with the <script> tag the dotAnalytics is added to the window.
window.dotAnalytics.track('cta-click', { button: 'Buy Now' });
window.dotAnalytics.pageView();
```

**Advanced: Manual Init with Custom Properties**

```javascript
const analytics = initializeContentAnalytics({
    siteKey: 'abc123',
    server: 'https://your-dotcms.com',
    debug: true,
    autoPageView: false
});

analytics.track('custom-event', {
    category: 'Marketing',
    value: 'Banner Clicked'
});

analytics.pageView();
```

### React

**Track Events**

```tsx
const { track } = useContentAnalytics();
track('cta-click', { label: 'Download PDF' });
```

**Manual Page View**

```tsx
const { pageView } = useContentAnalytics();
useEffect(() => {
    pageView();
}, []);
```

**Advanced: Manual Tracking with Router**

```tsx
import { useLocation } from 'react-router-dom';
const { pageView } = useContentAnalytics();
const location = useLocation();

useEffect(() => {
    pageView();
}, [location]);
```

## API Reference

```typescript
interface DotCMSAnalytics {
    track: (eventName: string, payload?: Record<string, unknown>) => void;
    pageView: () => void;
}
```

**Enriched AnalyticsEvent includes:**

-   `context`: siteKey, sessionId, userId
-   `page`: URL, title, referrer, path
-   `device`: screen size, language, viewport
-   `utm`: source, medium, campaign, term, etc.

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

## Roadmap

-   Scroll depth & file download tracking
-   Form interaction analytics
-   Angular & Vue support
-   Realtime dashboard

## dotCMS Support

We offer multiple channels to get help with the dotCMS React SDK:

-   **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.
-   **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions.
-   **Stack Overflow**: Use the tag `dotcms-react` when posting questions.
-   **Enterprise Support**: Enterprise customers can access premium support through the [dotCMS Support Portal](https://helpdesk.dotcms.com/support/).

When reporting issues, please include:

-   SDK version you're using
-   React version
-   Minimal reproduction steps
-   Expected vs. actual behavior

## How To Contribute

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the DotCMS UVE SDK! If you'd like to contribute, please follow these steps:

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
