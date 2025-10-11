# Changelog

All notable changes to the DotCMS React SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## v1.2.0

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
  const MyCustomBlock: React.FC<CustomRendererProps> = ({ node, children }) => {
    const text = node.content?.[0]?.text;
    return <div>{text}{children}</div>;
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
import { DotCMSBlockEditorRenderer } from '@dotcms/react/next';

const ParagraphRenderer = ({ content }) => {
  const [{ text }] = content;
  return <p>{text}</p>;
};

const customRenderers = {
  paragraph: ParagraphRenderer
};

// After
import { DotCMSBlockEditorRenderer } from '@dotcms/react/next';
import { BlockEditorNode } from '@dotcms/types';

const ParagraphRenderer = ({ node, children }) => {
  const text = node.content?.[0]?.text;
  return <p style={node.attrs}>{text}{children}</p>;
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

#### For Nested Content Rendering

The `children` prop now handles nested content rendering automatically:

```tsx
// Before - Manual nested rendering
const CalloutRenderer = ({ node }) => (
  <div className="callout">
    <h3>{node.attrs.title}</h3>
    {node.content && node.content.map((child, i) => (
      <DotCMSBlockEditorRenderer
        key={i}
        blocks={{ type: 'doc', content: [child] }}
      />
    ))}
  </div>
);

// After - Automatic nested rendering via children
const CalloutRenderer = ({ node, children }) => (
  <div className="callout">
    <h3>{node.attrs.title}</h3>
    {children}
  </div>
);
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

#### For Test Files

Update test assertions to use the new prop structure:

```tsx
// Before
const customRenderers = {
  paragraph: ({ content }) => {
    const [{ text }] = content;
    return <p data-testid="custom-paragraph">{text}</p>;
  }
};

// After
const customRenderers = {
  paragraph: ({ node }) => {
    const text = node.content?.[0]?.text;
    return <p data-testid="custom-paragraph">{text}</p>;
  }
};
```

### Benefits

- **Consistency**: Single unified interface (`BlockEditorNode`) for all block editor structures
- **Flexibility**: Custom renderers now have access to the complete node structure including type, attributes, marks, and nested content
- **Simplicity**: Automatic nested content rendering via `children` prop eliminates manual recursion
- **Type Safety**: Strongly-typed custom renderer signature with explicit prop types
- **Maintainability**: Reduced type redundancy and clearer API surface
- **Better DX**: Improved TypeScript IntelliSense and autocomplete support
