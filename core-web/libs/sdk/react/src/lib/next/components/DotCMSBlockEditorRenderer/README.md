# DotCMSBlockEditorRenderer Component

The `DotCMSBlockEditorRenderer` component is a React component for rendering DotCMS Block Editor content.

## Overview

The DotCMSBlockEditorRenderer displays structured content created with DotCMS's Block Editor field. The component handles validation of the block structure and renders the content appropriately.

## Installation

The DotCMSBlockEditorRenderer is included in the `@dotcms/react` SDK package:

```bash
npm install @dotcms/react
# or
yarn add @dotcms/react
```

## Usage

### Basic Usage

```jsx
import { DotCMSBlockEditorRenderer } from '@dotcms/react/next';

function MyComponent({ blockContent }) {
  return (
    <DotCMSBlockEditorRenderer 
      blocks={blockContent} 
      className="my-content-section"
    />
  );
}
```

Where `blockContent` represents the Block Editor content structure.
More info in the [Block Editor documentation](https://dev.dotcms.com/docs/block-editor#JSONObject)

## Props

| Prop | Type | Description |
|------|------|-------------|
| `blocks` | `BlockEditorContent` | The block editor content structure to render. |
| `customRenderers` | `CustomRenderer` | Optional custom renderers for specific block types. |
| `className` | `string` | Optional CSS class name to apply to the container. |
| `style` | `React.CSSProperties` | Optional inline styles to apply to the container. |

## Block Structure

The component expects blocks to follow a specific structure:

```typescript
interface BlockEditorContent {
  type: 'doc';
  content: BlockContent[];
}

interface BlockContent {
  type: string;
  content?: BlockContent[];
  [key: string]: any;
}
```

## Custom Renderers

You can customize how specific block types are rendered by providing a `customRenderers` prop. This is an object where keys are block types and values are rendering components:

```jsx
import { DotCMSBlockEditorRenderer } from '@dotcms/react/next';

// Custom renderer for a 'myCustomBlock' type
const MyCustomBlockRenderer = ({ node }) => (
  <div className="custom-block">
    <h2>{node.attrs.title}</h2>
    <p>{node.attrs.content}</p>
  </div>
);

function ContentDisplay({ blocks }) {
  const customRenderers = {
    myCustomBlock: MyCustomBlockRenderer
  };

  return (
    <DotCMSBlockEditorRenderer 
      blocks={blocks} 
      customRenderers={customRenderers}
    />
  );
}
```

## Error Handling

The component includes validation that:

1. Ensures the block structure is valid
2. Shows error messages in development mode if validation fails
3. Returns `null` in production mode if validation fails to prevent errors from propagating

## Performance Considerations

- The component uses React refs to avoid unnecessary re-renders
- Validation runs only when the blocks prop changes

## TypeScript Support

The component is fully typed:

```typescript
type CustomRenderer<T = any> = Record<string, React.FC<T>>;

interface BlockEditorRendererProps {
    blocks: BlockEditorContent;
    className?: string;
    style?: React.CSSProperties;
    customRenderers?: CustomRenderer;
}
```

## Examples

### Basic Example

```jsx
import { DotCMSBlockEditorRenderer } from '@dotcms/react/next';

function ArticleContent({ article }) {
  return (
    <DotCMSBlockEditorRenderer 
      blocks={article.bodyContent} 
      className="article-body"
    />
  );
}
```

### Example with Custom Styles

```jsx
import { DotCMSBlockEditorRenderer } from '@dotcms/react/next';

function StyledArticle({ contentlet }) {
  const customStyles = {
    padding: '20px',
    maxWidth: '800px',
    margin: '0 auto'
  };

  return (
    <DotCMSBlockEditorRenderer 
      blocks={contentlet.bodyContent}
      className="rich-text-content"
      style={customStyles}
    />
  );
}
```

### Custom Renderer Example

```jsx
import { DotCMSBlockEditorRenderer } from '@dotcms/react/next';

// Custom renderer for a 'callout' block type
const CalloutRenderer = ({ node }) => (
  <div className={`callout callout-${node.attrs.type}`}>
    <h3>{node.attrs.title}</h3>
    <p>{node.attrs.message}</p>
    {node.content && node.content.map((child, i) => (
      <DotCMSBlockEditorRenderer 
        key={i} 
        blocks={{ type: 'doc', content: [child] }}
      />
    ))}
  </div>
);

function EnhancedContent({ blocks }) {
  const customRenderers = {
    callout: CalloutRenderer
  };

  return (
    <DotCMSBlockEditorRenderer 
      blocks={blocks} 
      customRenderers={customRenderers}
    />
  );
}
```

## Best Practices

1. **Validation**: Always validate the blocks structure before passing it to the component
2. **Styling**: Use the `className` prop for styling rather than direct style manipulation
3. **Error Handling**: Implement error boundaries around the component to catch any rendering issues
4. **Performance**: For large block structures, consider implementing virtualization or pagination
5. **Accessibility**: Ensure custom renderers maintain proper accessibility standards