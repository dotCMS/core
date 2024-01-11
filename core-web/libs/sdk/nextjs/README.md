# @dotcms/nextjs

`@dotcms/nextjs` is the official Next.js library designed to work seamlessly with DotCMS and React, providing a set of components and utilities optimized for Next.js applications.

## Installation

Install the package via npm:

```bash
npm install @dotcms/nextjs
```

Or using Yarn:

```bash
yarn add @dotcms/nextjs
```

## Components

### `DotcmsLayout`

A functional component that renders a layout for a DotCMS page using Next.js navigation and the `@dotcms/react` package. This component should be used as a client component because it utilizes hooks from `@dotcms/react`.

#### Props

-   **props**: `DotcmsPageProps` - The properties for the DotCMS page.

#### Usage

This component needs to be used as a client component due to its reliance on Next.js and `@dotcms/react` hooks.

```tsx
'use client';

import { DotcmsLayout } from '@dotcms/nextjs';
import { Header, Footer, Navigation } from '../components';

export function MyPage({ data, nav }) {
    return (
        <div className="flex flex-col min-h-screen gap-6">
            {data.layout.header && (
                <Header>
                    <Navigation items={nav} />
                </Header>
            )}
            <main className="container flex flex-col gap-8 m-auto">
                <DotcmsLayout
                    entity={{
                        components: {
                            webPageContent: WebPageContent,
                            Banner: Banner,
                            Activity: Activity,
                            Product: Product,
                            Image: ImageComponent
                        },
                        ...data
                    }}
                />
            </main>
            {data.layout.footer && <Footer />}
        </div>
    );
}
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
| Code Examples   | [Codeshare](https://dotcms.com/codeshare/)                          |
| Forums/Listserv | [via Google Groups](https://groups.google.com/forum/#!forum/dotCMS) |
| Twitter         | @dotCMS                                                             |
| Main Site       | [dotCMS.com](https://dotcms.com/)                                   |
