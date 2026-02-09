# Changelog

All notable changes to the DotCMS React SDK will be documented in this file.


## v1.2.0

### Fixed

#### Optional Custom Renderers

- **Fixed**: `customRenderers` prop is now optional in `DotContent` component
  - Changed `customRenderers: CustomRenderer` to `customRenderers?: CustomRenderer`
  - Added optional chaining when accessing custom renderers: `customRenderers?.[contentType]`
  - Prevents runtime errors when `customRenderers` is not provided
  - Allows using the component without custom renderers and relying on default rendering

  ```typescript
  // Now works without custom renderers
  <DotCMSBlockEditorRenderer blocks={blogContent} />

  // Also works with custom renderers
  <DotCMSBlockEditorRenderer
    blocks={blogContent}
    customRenderers={customRenderers}
  />
  ```

### Added

#### TypeScript Support for Custom Renderers

- **NEW**: Generic type support for contentlet data in custom renderers
  - Added `CustomRendererProps<TData>` interface for typing custom renderer props
  - Added `CustomRendererComponent<TData>` type for creating typed custom renderer components
  - Enables full TypeScript IntelliSense and autocomplete for `node.attrs.data` properties
  - Three levels of type safety: no typing (any), inline typing, and component-level typing

  ```typescript
  // Define your contentlet data interface
  interface ActivityData {
      title: string;
      description: string;
      date: string;
  }

  // Option 1: Inline typing
  const customRenderers = {
      Activity: ({ node }: CustomRendererProps<ActivityData>) => {
          const { title, description } = node.attrs.data; // ✅ Fully typed!
          return <div>{title}: {description}</div>;
      }
  };

  // Option 2: Component-level typing (reusable)
  const Activity: CustomRendererComponent<ActivityData> = ({ node, children }) => {
      const { title, description, date } = node.attrs.data; // ✅ Autocomplete works!
      return <article><h2>{title}</h2><p>{description}</p></article>;
  };
  ```

### Changed

#### Block Editor Renderer - Breaking Changes

- **BREAKING**: `DotCMSBlockEditorRenderer` now uses unified `BlockEditorNode` interface instead of `BlockEditorContent`
  - The `blocks` prop now expects `BlockEditorNode` type
  - Custom renderers must now use `node` prop instead of `content` prop
  - All custom renderers receive the complete node structure with type, attrs, content, marks, and text properties

- **BREAKING**: Custom renderer component signature changed
  ```typescript
  // Before
  const MyCustomBlock: React.FC<{ content: any }> = ({ content }) => {
    const [{ text }] = content;
    return <div>{text}</div>;
  };

  // After
  const MyCustomBlock: React.FC<CustomRendererProps> = ({ node }) => {
    const text = node.content?.[0]?.text;
    return <div>{text}</div>;
  };
  ```

- **BREAKING**: `CustomRenderer` type definition updated
  ```typescript
  // Before
  type CustomRenderer<T = any> = Record<string, React.FC<T>>;

  // After
  interface CustomRendererProps<TData = any> {
      node: BlockEditorNode & {
          attrs?: {
              data?: TData;
              [key: string]: any;
          };
      };
      children?: React.ReactNode;
  }
  type CustomRendererComponent<TData = any> = React.FC<CustomRendererProps<TData>>;
  type CustomRenderer = Record<string, CustomRendererComponent<any>>;
  ```

#### Component Props

- **Changed**: `BlockEditorRendererProps.blocks` prop type
  ```typescript
  // Before
  interface BlockEditorRendererProps {
    blocks: BlockEditorContent;
    // ...
  }

  // After
  interface BlockEditorRendererProps {
    blocks: BlockEditorNode;
    // ...
  }
  ```

#### Internal Components

- **Changed**: `BlockEditorBlock` component now passes complete `node` object to custom renderers
  ```tsx
  // Before
  <CustomRendererComponent content={node.content}>

  // After
  <CustomRendererComponent node={node}>
  ```

- **Changed**: `DotContent` component now passes complete `node` to contentlet renderers
  ```tsx
  // Before
  <Component {...node} />

  // After
  <Component node={node} />
  ```

### Removed

#### Type Definitions

- **BREAKING**: Removed redundant `BlockEditorContent` interface from `@dotcms/types`
  - Use `BlockEditorNode` for all block editor content structures
  - `BlockEditorNode` serves as the unified interface for both root content and individual nodes

### Migration Guide

#### For Custom Renderer Components

Update your custom renderer components to use the new node-based signature:

```tsx
// Before
import { DotCMSBlockEditorRenderer } from '@dotcms/react';

const ParagraphRenderer = ({ content }) => {
  const [{ text }] = content;
  return <p>{text}</p>;
};

const customRenderers = {
  paragraph: ParagraphRenderer
};

// After
import { DotCMSBlockEditorRenderer, CustomRendererProps } from '@dotcms/reactt';
import { BlockEditorNode } from '@dotcms/types';

const ParagraphRenderer = ({ node }: CustomRendererProps) => {
  const text = node.content?.[0]?.text;
  return <p style={node.attrs}>{text}</p>;
};

const customRenderers = {
  paragraph: ParagraphRenderer
};
```

#### For Block Editor Content Type Declarations

Update type declarations in your components:

```typescript
// Before
import { BlockEditorContent } from '@dotcms/types';

interface BlogPost {
  content: BlockEditorContent;
}

// After
import { BlockEditorNode } from '@dotcms/types';

interface BlogPost {
  content: BlockEditorNode;
}
```


#### For Contentlet Renderers

Update your contentlet custom renderers to use the new prop structure:

```tsx
// Before
const ProductRenderer = ({ contentType, identifier, title, data }) => (
  <div>
    <h2>{title}</h2>
    <p>{data.description}</p>
  </div>
);

// After
const ProductRenderer = ({ node }) => {
  const { data } = node.attrs;
  return (
    <div>
      <h2>{data.title}</h2>
      <p>{data.description}</p>
    </div>
  );
};
```
### Benefits

- **Consistency**: Single unified interface (`BlockEditorNode`) for all block editor structures
- **Flexibility**: Custom renderers now have access to the complete node structure including type, attributes, marks, and nested content
- **Simplicity**: Automatic nested content rendering via `children` prop eliminates manual recursion
- **Type Safety**: Strongly-typed custom renderer signature with explicit prop types
- **Maintainability**: Reduced type redundancy and clearer API surface
- **Better DX**: Improved TypeScript IntelliSense and autocomplete support
