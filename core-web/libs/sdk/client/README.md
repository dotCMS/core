# @dotcms/client

`@dotcms/client` is the official dotCMS JavaScript library designed to simplify interactions with the DotCMS REST APIs.

This client library provides a streamlined, promise-based interface to fetch pages and navigation API.

## Features

-   Easy-to-use methods to interact with the [DotCMS Page](https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas) and [Navigation APIs](https://www.dotcms.com/docs/latest/navigation-rest-api).
-   Support for custom actions to communicate with the DotCMS page editor.
-   Comprehensive TypeScript typings for better development experience.

## Installation

Install the package via npm:

```bash
npm install @dotcms/client
```

Or using Yarn:

```bash
yarn add @dotcms/client
```

## Usage

`@dotcms/client` supports both ES modules and CommonJS. You can import it using either syntax:

### ES Modules

```javascript
import { DotCmsClient } from '@dotcms/client';
```

### CommonJS

```javascript
const { DotCmsClient } = require('@dotcms/client');
```

First, initialize the client with your DotCMS instance details.

```javascript
const client = DotCmsClient.init({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token',
    siteId: 'your-site-id'
});
```

### Fetching a Page

Retrieve the elements of any page in your DotCMS system in JSON format.

```javascript
const pageData = await client.page.get({
    path: '/your-page-path',
    language_id: 1,
    personaId: 'optional-persona-id'
});

console.log(pageData);
```

### Fetching Navigation

Retrieve information about the DotCMS file and folder tree.

```javascript
const navData = await client.nav.get({
    path: '/',
    depth: 2,
    languageId: 1
});

console.log(navData);
```

## API Reference

Detailed documentation of the `@dotcms/client` methods, parameters, and types can be found below:

### `DotCmsClient.init(config: ClientConfig): DotCmsClient`

Initializes the DotCMS client with the specified configuration.

### `DotCmsClient.page.get(options: PageApiOptions): Promise<unknown>`

Retrieves the specified page's elements from your DotCMS system in JSON format.

### `DotCmsClient.nav.get(options: NavApiOptions): Promise<unknown>`

Retrieves information about the DotCMS file and folder tree.

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