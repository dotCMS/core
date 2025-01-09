# @dotcms/analytics

`@dotcms/analytics` is the official dotCMS JavaScript library for Content Analytics that helps track events and analytics in your webapps. Available for both browser and React applications.

## Features

-   **Simple Browser Integration**: Easy to implement via script tags using IIFE implementation
-   **React Support**: Built-in React components and hooks for seamless integration
-   **Event Tracking**: Simple API to track custom events with additional properties
-   **Automatic PageView**: Option to automatically track page views
-   **Debug Mode**: Optional debug logging for development

## Installation

```bash
npm install @dotcms/analytics
```

Or include the script in your HTML page:

```html
<script src="ca.min.js"></script>
```

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
    apiKey: 'your-api-key-from-dotcms-analytics-app',
    server: 'https://your-dotcms-instance.com'
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
    apiKey: 'your-api-key-from-dotcms-analytics-app',
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

## Browser Configuration

The script can be configured using data attributes:

-   **data-analytics-server**: URL of the server where events will be sent. If not provided, the current domain will be used
-   **data-analytics-debug**: Enables debug logging
-   **data-analytics-auto-page-view**: Recommended for IIFE implementation. Enables automatic page view tracking
-   **data-analytics-key**: **(Required)** API key for authentication

```html
<!-- Example configuration -->
<script
    src="ca.min.js"
    data-analytics-server="http://localhost:8080"
    data-analytics-key="dev-key-123"
    data-analytics-auto-page-view
    data-analytics-debug></script>

<!-- Without automatic tracking - events must be sent manually -->
<script
    src="ca.min.js"
    data-analytics-server="http://localhost:8080"
    data-analytics-debug
    data-analytics-key="dev-key-123"></script>
```

## Roadmap

The following features are planned for future releases:

2. **Headless Support**
    - Angular integration for event tracking

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
