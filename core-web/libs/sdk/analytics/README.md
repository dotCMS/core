# @dotcms/analytics

`@dotcms/analytics` is the official dotCMS JavaScript library that helps track events and analytics in your webapps. Currently available as an IIFE (Immediately Invoked Function Expression) module for direct browser usage, with planned future support for React and Next.js applications.

## Features

-   **Simple Browser Integration**: Easy to implement via script tags using IIFE implementation
-   **Event Tracking**: Simple API to track custom events with additional properties
-   **Automatic PageView**: Option to automatically track page views
-   **Debug Mode**: Optional debug logging for development

## Installation

Include the script in your HTML page:

```html
<script src="analytics.iife.js"></script>
```

## Configuration

The script can be configured using data attributes:

-   **data-analytics-server**: URL of the server where events will be sent. If not provided, it defaults to the current location (window.location.href)
-   **data-analytics-debug**: Presence of this attribute enables debug logging (no value needed)
-   **data-analytics-auto-page-view**: Presence of this attribute enables automatic page view tracking (no value needed)
-   **data-analytics-key**: API key for authentication with the analytics server

Examples:

```html
<!-- Basic usage with defaults -->
<script src="ca.min.js"></script>

<!-- With custom server and API key -->
<script
    src="ca.min.js"
    data-analytics-server="http://your-server:8080"
    data-analytics-key="your-api-key"></script>

<!-- With all options -->
<script
    src="ca.min.js"
    data-analytics-server="http://your-server:8080"
    data-analytics-key="your-api-key"
    data-analytics-debug
    data-analytics-auto-page-view></script>
```

## Usage

### Automatic PageView Tracking

When `data-analytics-auto-page-view` is enabled, the library will automatically send a page view event to dotCMS when the page loads. If this attribute is not present, you'll need to manually track page views and other events using the tracking API.

```html
<!-- Automatic page view tracking enabled -->
<script
    src="ca.min.js"
    data-analytics-server="http://localhost:8080"
    data-analytics-auto-page-view
    data-analytics-debug></script>

<!-- Without automatic tracking - events must be sent manually -->
<script src="ca.min.js" data-analytics-server="http://localhost:8080" data-analytics-debug></script>
```

### Manual Event Tracking (WIP)

> ⚠️ **Work in Progress**: This feature is currently under development.

To track events manually (coming soon):

```html
<script>
    dotAnalytics.ready().then(() => {
        // Track a simple event
        dotAnalytics.track('PAGE_REQUEST');

        // Track an event with additional properties
        dotAnalytics.track('USER_ACTION', {
            action: 'click',
            element: 'button',
            id: 'submit-form'
        });
    });
</script>
```

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
