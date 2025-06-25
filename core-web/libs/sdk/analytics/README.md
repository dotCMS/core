# @dotcms/analytics

`@dotcms/analytics` is the official dotCMS JavaScript library for Content Analytics that helps track events and analytics in your webapps. Available for both browser and React applications.

## Features

-   **Simple Browser Integration**: Easy to implement via script tags using IIFE implementation
-   **React Support**: Built-in React components and hooks for seamless integration
-   **Event Tracking**: Simple API to track custom events with additional properties
-   **Automatic PageView**: Option to automatically track page views with route change detection
-   **Session Management**: Automatic session tracking with 30-minute timeout
-   **Identity Tracking**: Anonymous user identification across sessions
-   **UTM Campaign Tracking**: Automatic extraction and tracking of campaign parameters
-   **Device & Environment Data**: Automatic collection of screen resolution, language, viewport data
-   **Editor Detection**: Automatic detection and exclusion of analytics when inside dotCMS editor
-   **Debug Mode**: Optional debug logging for development and troubleshooting

## Installation

```bash
npm install @dotcms/analytics
```

Or include the script in your HTML page:

```html
<script src="ca.min.js"></script>
```

## Configuration

The analytics library accepts the following configuration options:

| Option         | Type       | Required | Default | Description                                                 |
| -------------- | ---------- | -------- | ------- | ----------------------------------------------------------- |
| `siteKey`      | `string`   | ✅       | -       | Site key obtained from your dotCMS Analytics app            |
| `server`       | `string`   | ✅       | -       | URL of your dotCMS server (e.g., 'https://demo.dotcms.com') |
| `debug`        | `boolean`  | ❌       | `false` | Enable debug logging for development                        |
| `autoPageView` | `boolean`  | ❌       | `true`  | Automatically track page views on route changes             |
| `redirectFn`   | `function` | ❌       | -       | Custom redirect function for handling URL redirections      |

### Automatic Data Collection

The SDK automatically collects and enriches events with:

-   **Session Management**: Tracks user sessions with 30-minute timeout
-   **User Identity**: Generates and persists anonymous user IDs
-   **UTM Parameters**: Extracts campaign tracking parameters from URLs
-   **Device Information**: Screen resolution, viewport size, language
-   **Page Context**: URL, title, referrer, protocol, search parameters
-   **Timestamps**: Local and UTC timestamps with timezone information

### Session Behavior

-   **Session Reset**: Sessions automatically reset at midnight UTC
-   **UTM Change Detection**: New session starts when UTM campaign parameters change
-   **Tab Visibility**: Sessions reactivate when users return to the tab after being inactive

### Storage Keys (for debugging)

The SDK stores data in browser storage using these keys:

-   `dot_analytics_session_id` - Current session identifier
-   `dot_analytics_user_id` - Anonymous user identifier
-   `dot_analytics_session_start` - Session start timestamp
-   `dot_analytics_session_utm` - Cached UTM parameters

## React Integration

### Provider Setup

First, import the provider:

```tsx
import { DotContentAnalyticsProvider } from '@dotcms/analytics/react';
```

Wrap your application with the `DotContentAnalyticsProvider`:

```tsx
// Example configuration
const analyticsConfig = {
    siteKey: 'your-site-key-from-dotcms-analytics-app',
    server: 'https://your-dotcms-instance.com',
    debug: false, // Enable debug logging
    autoPageView: true // Enable automatic page view tracking
};

function App() {
    return (
        <DotContentAnalyticsProvider config={analyticsConfig}>
            <YourApp />
        </DotContentAnalyticsProvider>
    );
}
```

### Tracking Custom Events

Use the `useContentAnalytics` hook to track custom events:

```tsx
import { useContentAnalytics } from '@dotcms/analytics/react';

function Activity({ title, urlTitle }) {
    const { track } = useContentAnalytics();

    // First parameter: custom event name to identify the action
    // Second parameter: object with properties you want to track

    return <button onClick={() => track('btn-click', { title, urlTitle })}>See Details →</button>;
}
```

### Manual Page View Tracking

To manually track page views, first disable automatic tracking in your config:

```tsx
const analyticsConfig = {
    siteKey: 'your-site-key-from-dotcms-analytics-app',
    server: 'https://your-dotcms-instance.com',
    autoPageView: false // Disable automatic tracking
};
```

Then use the `useContentAnalytics` hook in your layout component:

```tsx
import { useContentAnalytics } from '@dotcms/analytics/react';

function Layout({ children }) {
    const { pageView } = useContentAnalytics();

    useEffect(() => {
        pageView({
            // Add any custom properties you want to track
            myCustomValue: '2'
        });
    }, []);

    return <div>{children}</div>;
}
```

## Browser Integration (Standalone)

For non-React applications, you can use the standalone IIFE version.

### Configuration via Data Attributes

The script can be configured using data attributes:

-   **data-analytics-server**: URL of the server where events will be sent. If not provided, the current domain will be used
-   **data-analytics-debug**: Enables debug logging
-   **data-analytics-auto-page-view**: Recommended for IIFE implementation. Enables automatic page view tracking
-   **data-analytics-key**: **(Required)** Site key for authentication

```html
<!-- Basic configuration with automatic page view tracking -->
<script
    src="ca.min.js"
    data-analytics-server="https://your-dotcms-instance.com"
    data-analytics-key="your-site-key"
    data-analytics-auto-page-view
    data-analytics-debug></script>

<!-- Manual tracking only - events must be sent manually -->
<script
    src="ca.min.js"
    data-analytics-server="https://your-dotcms-instance.com"
    data-analytics-key="your-site-key"
    data-analytics-debug></script>
```

### Direct JavaScript Usage

After including the script, the analytics instance is available globally:

```javascript
// Check if analytics is available
if (window.dotAnalytics) {
    // Track custom events
    window.dotAnalytics.track('button-click', {
        buttonText: 'Learn More',
        section: 'hero'
    });

    // Manually trigger page view
    window.dotAnalytics.pageView({
        customProperty: 'value'
    });
} else {
    console.warn('dotAnalytics not initialized');
}
```

### Manual Initialization

You can also initialize analytics manually:

```javascript
import { initializeContentAnalytics } from '@dotcms/analytics';

const analytics = initializeContentAnalytics({
    siteKey: 'your-site-key',
    server: 'https://your-dotcms-instance.com',
    debug: true,
    autoPageView: false
});

if (analytics) {
    // Track events
    analytics.track('custom-event', { key: 'value' });
    analytics.pageView();
}
```

## Usage Examples

### Basic Event Tracking

```tsx
import { useContentAnalytics } from '@dotcms/analytics/react';

function MyComponent() {
    const { track } = useContentAnalytics();

    // Track any custom event
    const handleButtonClick = () => {
        track('button-click', {
            buttonText: 'Download PDF',
            section: 'hero'
        });
    };

    const handleFormSubmit = () => {
        track('form-submit', {
            formType: 'contact',
            hasEmail: true
        });
    };

    return (
        <div>
            <button onClick={handleButtonClick}>Download PDF</button>
            <button onClick={handleFormSubmit}>Submit Form</button>
        </div>
    );
}
```

## API Reference

### React Hook Methods

The `useContentAnalytics` hook returns an object with the following methods:

```typescript
interface DotCMSAnalytics {
    // Track a custom event
    track: (eventName: string, payload?: Record<string, unknown>) => void;

    // Track a page view
    pageView: (payload?: Record<string, unknown>) => void;
}
```

### Event Data Structure

Events are automatically enriched with the following data structure:

```typescript
interface AnalyticsEvent {
    // Basic event information
    event_type: 'pageview' | 'track';
    local_time: string; // ISO timestamp

    // Automatically collected context
    context: {
        site_key: string;
        session_id: string;
        user_id: string;
    };

    // Page information
    page: {
        url: string;
        title: string;
        doc_protocol: string;
        doc_host: string;
        doc_path: string;
        doc_search: string;
        doc_hash: string;
        referrer?: string;
    };

    // Device information
    device: {
        screen_resolution: string;
        language: string;
        viewport_width: string;
        viewport_height: string;
    };

    // UTM campaign parameters (if present)
    utm?: {
        source?: string;
        medium?: string;
        campaign?: string;
        term?: string;
        content?: string;
        id?: string;
    };
}
```

## Troubleshooting

### Common Issues

**Analytics not tracking events:**

-   Verify that `siteKey` and `server` are correctly configured
-   Check browser console for error messages
-   Ensure you're not inside the dotCMS editor (analytics is automatically disabled)
-   Verify the analytics endpoint is accessible: `{server}/api/v1/analytics/content/event`

**React hook errors:**

-   Ensure `useContentAnalytics` is used within `DotContentAnalyticsProvider`
-   Check that the provider's config is valid and initialization succeeded

**Page views not tracking:**

-   Verify `autoPageView` is enabled in configuration
-   For manual tracking, ensure you're calling `pageView()` on route changes
-   Check that the current path is different from the last tracked path

### Debug Mode

Enable debug logging to troubleshoot issues:

```typescript
// React
const config = {
    siteKey: 'your-key',
    server: 'https://your-server.com',
    debug: true // Enable debug logging
};

// Browser
<script
    src="ca.min.js"
    data-analytics-debug
    data-analytics-key="your-key"
    data-analytics-server="https://your-server.com">
</script>
```

## Roadmap

The following features are planned for future releases:

1. **Enhanced Tracking**

    - Scroll depth tracking
    - Form interaction analytics
    - File download tracking

2. **Framework Support**

    - Angular integration for event tracking
    - Vue.js integration

3. **Advanced Features**
    - Real-time analytics dashboard
    - Custom event validation

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. Before any pull requests can be accepted, an automated tool will ask you to agree to the [dotCMS Contributor's Agreement](https://gist.github.com/wezell/85ef45298c48494b90d92755b583acb3).

## Licensing

dotCMS comes in multiple editions and as such is dual licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds a number of enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://dotcms.com/cms-platform/features).

## Support

If you need help or have any questions, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.

## Documentation

Always refer to the official [DotCMS documentation](https://www.dotcms.com/docs/latest/) for comprehensive guides and API references.

## Getting Help

| Source          | Location                                                            |
| --------------- | ------------------------------------------------------------------- |
| Installation    | [Installation](https://dotcms.com/docs/latest/installation)         |
| Documentation   | [Documentation](https://dotcms.com/docs/latest/table-of-contents)   |
| Videos          | [Helpful Videos](http://dotcms.com/videos/)                         |
| Forums/Listserv | [via Google Groups](https://groups.google.com/forum/#!forum/dotCMS) |
| Twitter         | @dotCMS                                                             |
| Main Site       | [dotCMS.com](https://dotcms.com/)                                   |
