# Migration Guide: Alpha to 1.0.X

If you're upgrading from the `alpha.xx` version of `@dotcms/client`, this guide will help you migrate to the 1.0.X release. The 1.0.X version introduces several breaking changes and improvements for better developer experience, type safety, and performance.

### Breaking Changes Summary

| Change | Alpha Version | 1.0.X Version |
|--------|---------------|----------------|
| Client Initialization | `DotCmsClient.init()` | `createDotCMSClient()` |
| Import Statement | `import { DotCmsClient }` | `import { createDotCMSClient }` |
| Navigation API | `client.nav.get()` | `client.navigation.get()` |
| Content Collection | `.fetch()` method required | Direct await on collection |
| Parameter Names | `language_id` | `languageId` |
| Response Structure | Different format | Standardized response format |

## Step-by-Step Migration Guide

### 1. Update Dependencies

First, update your package.json to use the latest 1.0.X version:

```bash
# Remove the alpha version
npm uninstall @dotcms/client

# Install the 1.0.X version with types
npm install @dotcms/client@latest @dotcms/types@latest
```

### 2. Update Import Statements

**Before (Alpha):**
```javascript
import { DotCmsClient } from '@dotcms/client';
```

**After (1.0.X):**
```javascript
import { createDotCMSClient } from '@dotcms/client';
```

### 3. Update Client Initialization

**Before (Alpha):**
```javascript
const client = DotCmsClient.init({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token',
    siteId: 'your-site-id'
});
```

**After (1.0.X):**
```javascript
const client = createDotCMSClient({
    dotcmsUrl: 'https://your-dotcms-instance.com',
    authToken: 'your-auth-token',
    siteId: 'your-site-id'
});
```

### 4. Update Navigation API Calls

**Before (Alpha):**
```javascript
const navData = await client.nav.get({
    path: '/',
    depth: 2,
    languageId: 1
});
```

**After (1.0.X):**
```javascript
const navData = await client.navigation.get('/', {
    depth: 2,
    languageId: 1
});
```

### 5. Update Content Collection Queries

**Before (Alpha):**
```javascript
const collectionResponse = await client.content
    .getCollection('Blog')
    .limit(10)
    .page(1)
    .fetch(); // .fetch() was required

console.log(collectionResponse.contentlets);
```

**After (1.0.X):**
```javascript
const blogs = await client.content
    .getCollection('Blog')
    .limit(10)
    .page(1); // Direct await, no .fetch() needed

console.log(blogs.contentlets);
```

### 6. Update Parameter Names

**Before (Alpha):**
```javascript
const pageData = await client.page.get({
    path: '/your-page-path',
    language_id: 1, // underscore naming
    personaId: 'optional-persona-id'
});
```

**After (1.0.X):**
```javascript
const { pageAsset } = await client.page.get('/your-page-path', {
    languageId: 1, // camelCase naming
    personaId: 'optional-persona-id'
});
```

🚨 Also notice that the url path is now the first param of the `get` method and is not longer in the object.

### 7. Update Response Handling

**Before (Alpha):**
```javascript
const pageData = await client.page.get({ path: '/about-us' });
// Direct access to page data
console.log(pageData.page.title);
```

**After (1.0.X):**
```javascript
const { pageAsset } = await client.page.get('/about-us');
// Destructured response
console.log(pageAsset.page.title);
```

### 8. Update Query Builder Syntax

**Before (Alpha):**
```javascript
const complexQueryResponse = await client.content
    .getCollection('Blog')
    .query((qb) => qb.field('author').equals('John Doe').and().field('title').equals('Hello World'))
    .fetch();
```

**After (1.0.X):**
```javascript
const blogs = await client.content
    .getCollection('Blog')
    .query((qb) => qb.field('author').equals('John Doe').and().field('title').equals('Hello World'));
```

### 9. Update Sorting Syntax

**Before (Alpha):**
```javascript
const sortedResponse = await client.content
    .getCollection('Blog')
    .sortBy([{ field: 'title', order: 'asc' }])
    .fetch();
```

**After (1.0.X):**
```javascript
const blogs = await client.content
    .getCollection('Blog')
    .sortBy([{ field: 'title', order: 'asc' }]);
```

## Troubleshooting

### Common Migration Issues

**Issue 1: `DotCmsClient is not a function` Error**

**Problem:** Using the old initialization method.

**Solution:**
```javascript
// ❌ This will fail
const client = DotCmsClient.init({...});

// ✅ Use this instead
const client = createDotCMSClient({...});
```

**Issue 2: `client.nav is not a function` Error**

**Problem:** The navigation API method name changed.

**Solution:**
```javascript
// ❌ This will fail
const navData = await client.nav.get({...});

// ✅ Use this instead
const navData = await client.navigation.get('/', {...});
```

**Issue 3: `Cannot read property 'contentlets' of undefined` Error**

**Problem:** Response structure changed, especially for page requests.

**Solution:**
```javascript
// ❌ This might fail
const pageData = await client.page.get('/path');
console.log(pageData.page.title);

// ✅ Use destructuring
const { pageAsset } = await client.page.get('/path');
console.log(pageAsset.page.title);
```

**Issue 4: Content Collection `.fetch()` Method Not Found**

**Problem:** The `.fetch()` method is no longer required.

**Solution:**
```javascript
// ❌ This will fail
const result = await client.content.getCollection('Blog').fetch();

// ✅ Direct await
const result = await client.content.getCollection('Blog');
```

**Issue 5: TypeScript Errors After Migration**

**Problem:** Missing type definitions or changed interfaces.

**Solution:**
```bash
# Install the types package
npm install @dotcms/types@latest

# Update your TypeScript imports
import type { DotCMSClient } from '@dotcms/client';
import type { DotCMSPageAsset } from '@dotcms/types';
```

**Issue 6: Build or Runtime Errors with Module Resolution**

**Problem:** Module resolution issues after updating packages.

**Solution:**
```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Or if using Yarn
rm -rf node_modules yarn.lock
yarn install
```

**Issue 7: Unexpected Response Format**

**Problem:** Trying to access properties that don't exist in the new response format.

**Solution:**
```javascript
// ❌ This might fail
const response = await client.page.get('/path');
console.log(response.containers); // This property structure changed

// ✅ Use the new structure
const { pageAsset } = await client.page.get('/path');
console.log(pageAsset.containers);
```

### Migration Checklist

Use this checklist to ensure you've completed all necessary migration steps:

- [ ] **Dependencies**: Update package.json dependencies
- [ ] **Imports**: Change import statements from `DotCmsClient` to `createDotCMSClient`
- [ ] **Initialization**: Update client initialization method
- [ ] **Navigation API**: Replace `client.nav.get()` with `client.navigation.get()`
- [ ] **Content Collections**: Remove `.fetch()` calls from content collection queries
- [ ] **Parameter Names**: Update parameter names from snake_case to camelCase
- [ ] **Response Handling**: Update response destructuring (especially for page requests)
- [ ] **TypeScript**: Install and configure TypeScript types if using TypeScript
- [ ] **Testing**: Test all API calls to ensure they work correctly
- [ ] **Documentation**: Update any internal documentation or comments

### Testing Your Migration

After completing the migration, test these key areas:

1. **Client Initialization**: Verify the client initializes without errors
2. **Page Fetching**: Test fetching different pages and accessing their properties
3. **Content Collections**: Test querying different content types with various filters
4. **Navigation**: Test navigation API calls with different parameters
5. **Error Handling**: Verify error handling works as expected
6. **TypeScript**: If using TypeScript, ensure there are no type errors

### Performance Considerations

The 1.0.X version includes several performance improvements:

- **Optimized Requests**: Reduced request overhead and improved caching
- **Better Error Handling**: More efficient error processing
- **Type Safety**: Compile-time checks prevent runtime errors
- **GraphQL Integration**: More efficient data fetching with GraphQL

### Need Help?

If you encounter issues during migration that aren't covered here:

1. **Check the Full Documentation**: Review the [README.md](./README.md) for updated API documentation
2. **GitHub Issues**: [Open an issue](https://github.com/dotCMS/core/issues/new/choose) on GitHub with your specific problem
3. **Community Support**: Visit our [community forum](https://community.dotcms.com/) for community support

### Additional Resources

- [dotCMS Client SDK Documentation](./README.md)
- [dotCMS API Documentation](https://dev.dotcms.com/docs/rest-api)
- [dotCMS GraphQL Documentation](https://dev.dotcms.com/docs/graphql)
- [dotCMS Community Forum](https://community.dotcms.com/)

---

**Note**: This migration guide is specific to upgrading from the alpha version to the 1.0.X release. For other version migrations, please refer to the appropriate documentation or release notes.
