# dotCMS Experiments SDK

The `@dotcms/experiments` SDK is the official dotCMS JavaScript library that helps add A/B testing to your web applications. It handles user assignments to different variants of a page and tracks their interactions.

## Overview

### When to Use It

-   Adding A/B testing capabilities to your web application
-   Running experiments to optimize user experience
-   Testing different page variants with real users
-   Tracking experiment performance with DotCMS Analytics

### Key Features

-   **User Assignment to Experiments**: Automatically assigns users to different experimental variants, ensuring diverse user experiences and reliable test data
-   **Link Verification for Redirection**: Checks links to ensure users are redirected to their assigned experiment variant, maintaining the integrity of the testing process
-   **Automatic PageView Event Sending**: Automatically sends PageView events to DotCMS Analytics, enabling real-time tracking of user engagement and experiment effectiveness

## Table of Contents

-   [Overview](#overview)
-   [Installation](#installation)
-   [Getting Started](#getting-started)
    -   [withExperiments HOC](#withexperiments-higher-order-component)
    -   [Configuration Object](#configuration-object)
-   [Usage](#usage)
    -   [With @dotcms/react](#with-dotcmsreact-recommended)
    -   [Configuration Best Practices](#configuration-best-practices)
    -   [How It Works](#how-it-works)
-   [Support](#support)
-   [Contributing](#contributing)
-   [Licensing](#licensing)

## Installation

Install the package via npm:

```bash
npm install @dotcms/experiments
```

Or using Yarn:

```bash
yarn add @dotcms/experiments
```

## Getting Started

### `withExperiments` Higher-Order Component

The SDK exports a single HOC (Higher-Order Component) called `withExperiments` that wraps your page component with experiment functionality. This component handles:

-   User assignment to experiment variants
-   Automatic redirection to the correct variant
-   Prevention of content flickering during variant loading
-   Automatic page view tracking to dotCMS Analytics
-   Click event handling to maintain variant consistency across navigation

### Configuration Object

The `config` object passed to `withExperiments` accepts the following properties:

-   **apiKey** (required): Your API key from the DotCMS Analytics app
-   **server** (required): The URL of your dotCMS instance (e.g., `https://your-dotcms-instance.com`)
-   **redirectFn** (optional): Custom redirect function for SPA navigation (default: `window.location.replace`)
-   **trackPageView** (optional): Enable/disable automatic page view tracking (default: `true`)
-   **debug** (optional): Enable debug logging in the browser console (default: `false`)

## Usage

### With @dotcms/react (Recommended)

The recommended approach is to wrap the `DotCMSLayoutBody` component with `withExperiments`. The pattern supports **conditional wrapping** - experiments are only enabled when an API key is configured:

```javascript
"use client";

import { withExperiments } from "@dotcms/experiments";
import { DotCMSLayoutBody, useEditableDotCMSPage } from "@dotcms/react";
import { useRouter } from 'next/navigation';

// Experiment configuration - only applied if apiKey is present
const experimentConfig = {
    apiKey: process.env.NEXT_PUBLIC_EXPERIMENTS_API_KEY,
    server: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    debug: true
};

export function Page({ pageContent }) {
    const { pageAsset } = useEditableDotCMSPage(pageContent);
    const { replace } = useRouter();

    // Conditionally wrap with experiments if apiKey is configured
    const DotCMSLayoutBodyComponent = experimentConfig.apiKey
        ? withExperiments(DotCMSLayoutBody, {
              ...experimentConfig,
              redirectFn: replace
          })
        : DotCMSLayoutBody;

    return (
        <main>
            <DotCMSLayoutBodyComponent
                page={pageAsset}
                components={pageComponents}
            />
        </main>
    );
}
```

> 📚 **Learn more about `DotCMSLayoutBody`**: See the [@dotcms/react SDK documentation](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/react/README.md#dotcmslayoutbody) for complete details on configuring the layout renderer, component mapping, and available props.

### Configuration Best Practices

-   **Use Environment Variables**: Store your API key and server URL in environment variables
-   **Conditional Wrapping**: Only enable experiments when an API key is configured using a ternary operator
-   **Framework Router**: Pass your framework's router function (e.g., `router.replace` for Next.js) to maintain SPA navigation
-   **Debug Mode**: Enable debug logging during development to troubleshoot experiment assignment issues

### How It Works

Once you wrap your component with `withExperiments`, the SDK automatically handles:

1. **User Assignment**: Assigns users to experiment variants when they visit a page with an active experiment
2. **Automatic Redirection**: Redirects users to their assigned variant URL (prevents seeing the wrong variant)
3. **Flicker Prevention**: Hides content during redirection to avoid showing the wrong variant momentarily
4. **Navigation Handling**: Maintains variant consistency when users click links
5. **Analytics Tracking**: Sends pageview events to DotCMS Analytics automatically

All of this happens behind the scenes - you just need to wrap your component and provide the configuration.

**Learn More**: For detailed information on creating and managing experiments in dotCMS, visit the [DotCMS A/B Testing Experiments](https://www.dotcms.com/product/ab-testing-experiments) page.

## Support

We offer multiple channels to get help with the dotCMS Experiments SDK:

-   **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository
-   **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions
-   **Stack Overflow**: Use the tag `dotcms-experiments` when posting questions
-   **Enterprise Support**: Enterprise customers can access premium support through the [dotCMS Support Portal](https://helpdesk.dotcms.com/support/)

When reporting issues, please include:

-   SDK version you're using
-   Framework/library version (if applicable)
-   Minimal reproduction steps
-   Expected vs. actual behavior

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the dotCMS Experiments SDK! If you'd like to contribute, please follow these steps:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

Before any pull requests can be accepted, an automated tool will ask you to agree to the [dotCMS Contributor's Agreement](https://gist.github.com/wezell/85ef45298c48494b90d92755b583acb3).

## Licensing

dotCMS comes in multiple editions and as such is dual-licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization, and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds several enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).

This SDK is part of dotCMS's dual-licensed platform (GPL 3.0 for Community, commercial license for Enterprise).
