# BlockEditorRenderer Component

The `BlockEditorRenderer` component is a React component for rendering DotCMS Block Editor content. It provides both read-only display capabilities and in-context editing functionality when used within the DotCMS edit interface.

## Overview

The BlockEditorRenderer is designed to render structured content created with DotCMS's Block Editor field. It can be used in two primary modes:

1. **Read-only mode** - For displaying content on the front-end
2. **Editable mode** - For enabling in-context editing within the DotCMS editor interface

## Installation

The BlockEditorRenderer is included in the `@dotcms/react` SDK package:

```bash
npm install @dotcms/react
# or
yarn add @dotcms/react
```

## Usage

### Basic Usage (Read-only)

```jsx
import { BlockEditorRenderer } from '@dotcms/react';

function MyComponent({ blockContent }) {
  return (
    <BlockEditorRenderer 
      blocks={blockContent} 
      className="my-content-section"
    />
  );
}
```

Where `blockContent` represents the Block Editor content type.
More info in the [Block Editor documentation](https://dev.dotcms.com/docs/block-editor#JSONObject)
### Editable Mode (Inside DotCMS Editor)

```jsx
import { BlockEditorRenderer } from '@dotcms/react';

function EditableComponent({ contentlet }) {
  return (
    <BlockEditorRenderer 
      blocks={contentlet.blockEditorField} 
      editable={true}
      contentlet={contentlet}
      fieldName="blockEditorField"
      className="my-editable-content"
    />
  );
}
```



## Props

The component accepts two sets of props depending on the mode of operation:

### Common Props

| Prop | Type | Description |
|------|------|-------------|
| `blocks` | `Block` | The block editor content structure to render. |
| `customRenderers` | `CustomRenderer` | Optional custom renderers for specific block types. |
| `className` | `string` | Optional CSS class name to apply to the container. |
| `style` | `React.CSSProperties` | Optional inline styles to apply to the container. |

### Editable Mode Props

| Prop | Type | Description |
|------|------|-------------|
| `editable` | `true` | Flag to enable inline editing. When true, contentlet and fieldName are required. |
| `contentlet` | `DotCMSContentlet` | Contentlet object containing the field to be edited. Required when editable is true. |
| `fieldName` | `string` | Name of the field in the contentlet that contains the block editor content. Required when editable is true. |

## Block Structure

The component expects blocks to follow a specific structure:

```typescript
interface Block {
  type: 'doc';
  content: BlockContent[];
}

interface BlockContent {
  type: string;
  content?: BlockContent[];
  [key: string]: any;
}
```

## Inline Editing

When used in editable mode, the BlockEditorRenderer allows content editors to make changes directly in the context of the page. This feature:

- Is only enabled in the DotCMS editor interface
- Requires the `editable`, `contentlet`, and `fieldName` props
- Initializes the Block Editor when the rendered content is clicked
- Ensures changes are saved back to the correct contentlet field

The component automatically detects if it's running inside the DotCMS editor using the `isInsideEditor()` function from `@dotcms/client`.

## Custom Renderers

You can customize how specific block types are rendered by providing a `customRenderers` prop. This is an object where keys are block types and values are rendering components:

```jsx
import { BlockEditorRenderer } from '@dotcms/react';

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
    <BlockEditorRenderer 
      blocks={blocks} 
      customRenderers={customRenderers}
    />
  );
}
```

## Error Handling

The component includes validation that:

1. Ensures the block structure is valid
2. Shows error messages in the editor if validation fails
3. Returns `null` in frontend mode if validation fails to prevent errors from propagating

## Performance Considerations

- The component uses React refs to avoid unnecessary re-renders
- Validation runs only when the blocks prop changes
- Inline editing setup only happens when all required props are available and the component is inside the editor

## TypeScript Support

The component is fully typed and will enforce the correct props based on the mode:

- When `editable` is `true`, TypeScript will require `contentlet` and `fieldName` props
- The `blocks` prop expects a valid Block structure


## Examples

### Basic Example

```jsx
import { BlockEditorRenderer } from '@dotcms/react';

function ArticleContent({ article }) {
  return (
    <BlockEditorRenderer 
      blocks={article.bodyContent} 
      className="article-body"
    />
  );
}
```

### Editable Example with Custom Styles

```jsx
import { BlockEditorRenderer } from '@dotcms/react';

function EditableArticle({ contentlet }) {
  const customStyles = {
    padding: '20px',
    maxWidth: '800px',
    margin: '0 auto'
  };

  return (
    <BlockEditorRenderer 
      blocks={contentlet.bodyContent}
      editable={true}
      contentlet={contentlet}
      fieldName="bodyContent"
      className="rich-text-content"
      style={customStyles}
    />
  );
}
```

### Custom Renderer Example

```jsx
import { BlockEditorRenderer } from '@dotcms/react';

// Custom renderer for a 'callout' block type
const CalloutRenderer = ({ node }) => (
  <div className={`callout callout-${node.attrs.type}`}>
    <h3>{node.attrs.title}</h3>
    <p>{node.attrs.message}</p>
    {node.content && node.content.map((child, i) => (
      <BlockEditorRenderer 
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
    <BlockEditorRenderer 
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

## Enterprise Features

Note that the inline editing functionality (`editable={true}`) is an Enterprise-only feature and requires a valid DotCMS Enterprise license.