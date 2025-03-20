# dotCMS API Client - `@dotcms/client`

The `@dotcms/client` is a JavaScript/TypeScript library for interacting with a dotCMS instance. It allows you to easily fetch pages, content, and navigation information in JSON format, as well as to make complex queries on content collections.

This client library provides a streamlined, promise-based interface to fetch pages and navigation API.

## Features

-   Easy-to-use methods to interact with [dotCMS pages](https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas) and the [Navigation API](https://www.dotcms.com/docs/latest/navigation-rest-api).
-   Support for custom actions to communicate with the dotCMS page editor.
-   Comprehensive TypeScript typings for better development experience.

# dotCMS API Client

## Installation

To get started, install the client via npm or yarn:

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

### Initialization

First, initialize the client with your dotCMS instance details.

```javascript
const client = DotCmsClient.init({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token',
    siteId: 'your-site-id'
});
```

### Fetching a Page

You can retrieve the elements of any page in your dotCMS system in JSON format using the `client.page.get()` method.

```javascript
const pageData = await client.page.get({
    path: '/your-page-path',
    language_id: 1,
    personaId: 'optional-persona-id'
});
```

### Fetching Navigation

Retrieve the dotCMS file and folder tree to get information about the navigation structure.

```javascript
const navData = await client.nav.get({
    path: '/',
    depth: 2,
    languageId: 1
});
```

### Fetching a Collection of Content

The `getCollection` method allows you to fetch a collection of content items (sometimes called "contentlets") using a builder pattern for complex queries.

#### Basic Usage

Here’s a simple example to fetch content from a collection:

```typescript
import { DotCmsClient } from '@dotcms/client';

const client = DotCmsClient.init({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token'
});

const collectionResponse = await client.content
    .getCollection('Blog') // Collection name
    .limit(10) // Limit results to 10 items
    .page(1) // Fetch the first page
    .fetch(); // Execute the query

console.log(collectionResponse.contentlets);
```

#### Sorting Content

You can sort the content by any field in ascending or descending order:

```typescript
const sortedResponse = await client.content
    .getCollection('Blog')
    .sortBy([{ field: 'title', order: 'asc' }]) // Sort by title in ascending order
    .fetch();
```

#### Filtering by Language

If you need to filter content by language, you can specify the `language` parameter:

```typescript
const languageFilteredResponse = await client.content
    .getCollection('Blog')
    .language(2) // Filter by language ID (e.g., 2)
    .fetch();
```

#### Using Complex Queries

You can build more complex queries using the query builder. For example, filter by `author` and `title`:

```typescript
const complexQueryResponse = await client.content
    .getCollection('Blog')
    .query((qb) => qb.field('author').equals('John Doe').and().field('title').equals('Hello World'))
    .fetch();
```

#### Fetching Draft Content

To only fetch draft content, use the `draft()` method:

```typescript
const draftContentResponse = await client.content
    .getCollection('Blog')
    .draft() // Fetch only drafts content
    .fetch();
```

#### Setting Depth for Relationships

To fetch content with a specific relationship depth, use the `depth()` method:

```typescript
const depthResponse = await client.content
    .getCollection('Blog')
    .depth(2) // Fetch related content up to depth 2
    .fetch();
```

#### Combining Multiple Methods

You can combine multiple methods to build more complex queries. For example, limit results, sort them, and filter by author:

```typescript
const combinedResponse = await client.content
    .getCollection('Blog')
    .limit(5)
    .page(2)
    .sortBy([{ field: 'title', order: 'asc' }])
    .query((qb) => qb.field('author').equals('John Doe'))
    .depth(1)
    .fetch();
```

## Error Handling Example

To handle errors gracefully, you can use a `try-catch` block around your API calls. Here’s an example:

```typescript
try {
    const pageData = await client.page.get({
        path: '/your-page-path',
        languageId: 1
    });
} catch (error) {
    console.error('Failed to fetch page data:', error);
}
```

This ensures that any errors that occur during the fetch (e.g., network issues, invalid paths, etc.) are caught and logged properly.

## Pagination

When fetching large collections of content, pagination is key to managing the number of results returned:

```typescript
const paginatedResponse = await client.content
    .getCollection('Blog')
    .limit(10) // Limit to 10 items per page
    .page(2) // Get the second page of results
    .fetch();
```

## API Reference

Detailed documentation of the `@dotcms/client` methods, parameters, and types can be found below:

### `DotCmsClient.init(config: ClientConfig): DotCmsClient`

Initializes the dotCMS client with the specified configuration.

### `DotCmsClient.page.get(options: PageApiOptions): Promise<unknown>`

Retrieves the specified page's elements from your dotCMS system in JSON format.

### `DotCmsClient.nav.get(options: NavApiOptions): Promise<unknown>`

Retrieves information about the dotCMS file and folder tree.

### `DotCmsClient.content.getCollection(contentType: string): CollectionBuilder<T>`

Creates a builder to filter and fetches a collection of content items for a specific content type.

#### Parameters

`contentType` (string): The content type to retrieve.

#### Returns

`CollectionBuilder<T>`: A builder instance for chaining filters and executing the query.

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. Before any pull requests can be accepted, an automated tool will ask you to agree to the [dotCMS Contributor's Agreement](https://gist.github.com/wezell/85ef45298c48494b90d92755b583acb3).

## Licensing

dotCMS comes in multiple editions and as such is dual licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds a number of enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://dotcms.com/cms-platform/features).

## Support

If you need help or have any questions, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.

## Documentation

Always refer to the official [dotCMS documentation](https://www.dotcms.com/docs/latest/) for comprehensive guides and API references.

## Getting Help

| Source          | Location                                                            |
| --------------- | ------------------------------------------------------------------- |
| Installation    | [Installation](https://dotcms.com/docs/latest/installation)         |
| Documentation   | [Documentation](https://dotcms.com/docs/latest/table-of-contents)   |
| Videos          | [Helpful Videos](http://dotcms.com/videos/)                         |
| Forums/Listserv | [via Google Groups](https://groups.google.com/forum/#!forum/dotCMS) |
| Twitter         | @dotCMS                                                             |
| Main Site       | [dotCMS.com](https://dotcms.com/)                                   |
