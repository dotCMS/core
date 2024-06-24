# @dotcms/experiments

`@dotcms/experiments` is the official dotCMS JavaScript library that helps add A/B testing to your webapps. It handle user assignments to different variants of a page and tracks their interactions.

## Features

- **User Assignment to Experiments**: Automatically assigns users to different experimental variants, ensuring diverse user experiences and reliable test data.
- **Link Verification for Redirection**: Checks links to ensure users are redirected to their assigned experiment variant, maintaining the integrity of the testing process.
- **Automatic PageView Event Sending**: Automatically sends PageView events to DotCMS Analytics, enabling real-time tracking of user engagement and experiment effectiveness.



## Installation
You can install the package via npm or Yarn:

```bash
npm install @dotcms/experiments
```
Or using Yarn:

```bash
yarn add @dotcms/experiments
```


## Components

### `DotExperimentsProvider`
This component utilizes React's Context API to provide DotExperiments instances to its descendants, facilitating access to A/B testing features throughout your webapps.

#### Props
-   **config**: Configuration object for DotCMS Analytics integration.
    -   **apiKey**: Your API key from the DotCMS Analytics app.
    -   **server**: The URL of your DotCMS instance.
    -   **redirectFn**: The redirect function to use when assigning users to experiment variants.

#### Usage

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

## How A/B Testing Works with @dotcms/experiments

The A/B testing process with `@dotcms/experiments` is designed to be straightforward and automatic:

1. **Experiment Assignment**: When a user visits a page that includes an experiment, the library first checks if the user has been assigned to an experiment variant. If not, it queries DotCMS Analytics to determine if there are active experiments and assigns the user to the appropriate variant.

2. **Page Redirection**: If the user's assigned variant differs from the current page, the library automatically redirects the user to the correct variant page. This ensures that the user experiences the variant they have been assigned to.

3. **Tracking Pageviews**: After redirection or upon visiting the page, the library sends a pageview event to DotCMS Analytics. This data is used to determine the effectiveness of each variant, ultimately helping to identify which variant performs better in the A/B test.


## Learn More About A/B Testing with DotCMS

For more detailed information on A/B testing features and capabilities, visit the DotCMS A/B testing and experiments page: [DotCMS A/B Testing Experiments](https://www.dotcms.com/product/ab-testing-experiments).


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
