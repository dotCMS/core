# dotCMS Client SDK

The `@dotcms/client` is a JavaScript/TypeScript library for interacting with a dotCMS instance. It allows you to easily fetch pages, content, and navigation information in JSON format, as well as to make complex queries on content collections.

This client library provides a streamlined, promise-based interface to fetch pages and navigation API.


## What is it?

The `@dotcms/client` library serves as a specialized connector between your applications and dotCMS. This SDK:

- **Simplifies API interactions** with dotCMS by providing intuitive methods and builders
- **Handles authentication and requests** securely across all API endpoints
- **Works universally** in both browser-based applications and Node.js environments
- **Abstracts complexity** of the underlying REST APIs into developer-friendly interfaces
- **Provides type definitions** for enhanced developer experience and code completion

Whether you're building a headless frontend that consumes dotCMS content or a server-side application that needs to interact with your content repository, this client SDK eliminates boilerplate code and standardizes how you work with the dotCMS APIs.

## Module Formats
`@dotcms/client` supports both ES modules and CommonJS. You can import it using either syntax:

### ES Modules

```javascript
import { createDotCMSClient } from '@dotcms/client/next';
```

### CommonJS

```javascript
const { createDotCMSClient } = require('@dotcms/client/next');
```

## How to Install

Install the client SDK in your project using your preferred package manager:

```bash
# Using npm
npm install @dotcms/client@next

# Using yarn
yarn add @dotcms/client@next

# Using pnpm
pnpm add @dotcms/client@next
```

## Dev Dependencies

This package has the following dev dependencies that you'll need to install in your project:

| Dependency | Version | Description |
|------------|---------|-------------|
| `@dotcms/types` | latest | Required for type definitions |

Install dev dependencies:

```bash
# Using npm
npm install @dotcms/types --save-dev

# Using yarn
yarn add @dotcms/types --dev

# Using pnpm
pnpm add @dotcms/types --dev
```

## Browser Compatibility

The `@dotcms/client` package is compatible with the following browsers:

| Browser | Minimum Version | TLS Version |
|---------|----------------|-------------|
| Chrome  | Latest 2 versions | TLS 1.2+ |
| Edge    | Latest 2 versions | TLS 1.2+ |
| Firefox | Latest 2 versions | TLS 1.2+ |
