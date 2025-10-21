# dotCMS Experiments SDK

The `@dotcms/experiments` SDK is the official dotCMS JavaScript library that helps add A/B testing to your web applications. It handles user assignments to different variants of a page and tracks their interactions.

## Overview

### When to Use It

- Adding A/B testing capabilities to your web application
- Running experiments to optimize user experience
- Testing different page variants with real users
- Tracking experiment performance with DotCMS Analytics

### Key Features

- **User Assignment to Experiments**: Automatically assigns users to different experimental variants, ensuring diverse user experiences and reliable test data
- **Link Verification for Redirection**: Checks links to ensure users are redirected to their assigned experiment variant, maintaining the integrity of the testing process
- **Automatic PageView Event Sending**: Automatically sends PageView events to DotCMS Analytics, enabling real-time tracking of user engagement and experiment effectiveness

## Table of Contents

- [Overview](#overview)
- [Installation](#installation)
- [Getting Started](#getting-started)
  - [Components](#components)
  - [How A/B Testing Works](#how-ab-testing-works-with-dotcmsexperiments)
- [Usage](#usage)
- [Support](#support)
- [Contributing](#contributing)
- [Licensing](#licensing)

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

### Components

### `DotExperimentsProvider`
This component utilizes React's Context API to provide DotExperiments instances to its descendants, facilitating access to A/B testing features throughout your webapps.

#### Props

-   **config**: Configuration object for DotCMS Analytics integration
    -   **apiKey**: Your API key from the DotCMS Analytics app
    -   **server**: The URL of your dotCMS instance
    -   **redirectFn**: The redirect function to use when assigning users to experiment variants

## Usage

```javascript
import { DotExperimentsProvider } from "@dotcms/experiments";
import { useRouter } from 'next/router';

const { replace } = useRouter();

const experimentConfig = {
  apiKey: 'your-api-key-from-dotcms-analytics-app',
  server: 'https://your-dotcms-instance.com',
  redirectFn: replace // Use replace from useRouter in Next.js
};

return (
  <DotExperimentsProvider config={experimentConfig}>
  
    <Header>
      <Navigation />
    </Header>
    <DotcmsLayout />
    <Footer />
    
  </DotExperimentsProvider>
);
```

### How A/B Testing Works with @dotcms/experiments

The A/B testing process with `@dotcms/experiments` is designed to be straightforward and automatic:

1. **Experiment Assignment**: When a user visits a page that includes an experiment, the library first checks if the user has been assigned to an experiment variant. If not, it queries DotCMS Analytics to determine if there are active experiments and assigns the user to the appropriate variant

2. **Page Redirection**: If the user's assigned variant differs from the current page, the library automatically redirects the user to the correct variant page. This ensures that the user experiences the variant they have been assigned to

3. **Tracking Pageviews**: After redirection or upon visiting the page, the library sends a pageview event to DotCMS Analytics. This data is used to determine the effectiveness of each variant, ultimately helping to identify which variant performs better in the A/B test

**Learn More**: For more detailed information on A/B testing features and capabilities, visit the [DotCMS A/B Testing Experiments](https://www.dotcms.com/product/ab-testing-experiments) page.

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
