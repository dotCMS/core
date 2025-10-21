# DotCMSBlockEditorRenderer Component

The `DotCMSBlockEditorRenderer` component is a React component for rendering DotCMS Block Editor content.

## Overview

The `DotCMSBlockEditorRenderer` is designed to display content created with DotCMS's Block Editor field type. It handles various block types out of the box and allows for custom rendering of blocks through a flexible renderer system.

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
| `blocks` | `BlockEditorNode` | The block editor content structure to render. |
| `customRenderers` | `CustomRenderer` | Optional custom renderers for specific block types. |
| `className` | `string` | Optional CSS class name to apply to the container. |
| `style` | `React.CSSProperties` | Optional inline styles to apply to the container. |


## Custom Renderers

You can customize how specific block types are rendered by providing a `customRenderers` prop. This is an object where keys are block types and values are rendering components:

```jsx
import { DotCMSBlockEditorRenderer } from '@dotcms/react/next';

// Custom renderer for a 'myCustomBlock' type
const MyCustomBlockRenderer = ({ node, children }) => (
  <div className="custom-block">
    <h2>{node.attrs.title}</h2>
    <p>{node.attrs.content}</p>
    {children}
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

## Inline Editing

The component supports inline editing for Block Editor fields when used in a DotCMS environment. This allows content editors to make changes directly on the page without going to the edit mode.

### Enabling Inline Editing

To enable inline editing for a Block Editor field:

```jsx
import { DotCMSBlockEditorRenderer } from '@dotcms/react/next';
import { enableBlockEditorInline } from '@dotcms/uve';

function DetailPage({ contentlet }) {
    const fieldName = "blogContent";

    const handleClick = () => {
        enableBlockEditorInline(contentlet, fieldName);
    };

    return (
        <div onClick={handleClick}>
            <DotCMSBlockEditorRenderer
                blocks={contentlet[fieldName]}
                fieldName={fieldName}
            />
        </div>
    );
}
export default DetailPage;
```

### Key Points for Inline Editing

1. Import the `enableBlockEditorInline` function from `@dotcms/uve`
2. Specify the `fieldName` prop on the renderer component to identify which field to edit
3. Create a click handler that calls `enableBlockEditorInline` with:
   - The contentlet object containing the field
   - The name of the field to edit
4. Attach the click handler to a parent element of the renderer

When a user clicks on the element, the Block Editor will switch to inline editing mode for the specified field.

## Error Handling

The component includes validation that:

1. Ensures the block structure is valid
2. Shows error messages in development mode if validation fails
3. Returns `null` in production mode if validation fails to prevent errors from propagating

## Performance Considerations

- The component uses React refs to avoid unnecessary re-renders
- Validation runs only when the blocks prop changes

## TypeScript Support

The component is fully typed with support for custom contentlet data types:

### Type Definitions

```typescript
import { CustomRendererProps, CustomRendererComponent, CustomRenderer } from '@dotcms/react';

// Base props for all custom renderers
interface CustomRendererProps<TData = any> {
    node: BlockEditorNode & {
        attrs?: {
            data?: TData;
            [key: string]: any;
        };
    };
    children?: React.ReactNode;
}

// Custom renderer component type
type CustomRendererComponent<TData = any> = React.FC<CustomRendererProps<TData>>;

// Map of block types to renderers
type CustomRenderer = Record<string, CustomRendererComponent<any>>;

// Main component props
interface BlockEditorRendererProps {
    blocks: BlockEditorNode;
    className?: string;
    style?: React.CSSProperties;
    customRenderers?: CustomRenderer;
}
```

### Usage Levels

#### Level 1: No Typing (Quickest)

```typescript
import { CustomRendererProps } from '@dotcms/react';

const customRenderers = {
    Activity: ({ node, children }: CustomRendererProps) => {
        // node.attrs.data is 'any' type
        const { title, description } = node.attrs.data;
        return <div>{title}: {description}</div>;
    }
};
```

#### Level 2: Inline Typing (Balanced)

```typescript
import { CustomRendererProps } from '@dotcms/react';

// Define your contentlet data interface
interface ActivityData {
    contentType: 'Activity';
    title: string;
    description: string;
    date: string;
    location?: string;
}

const customRenderers = {
    // ✅ TypeScript knows node.attrs.data is ActivityData
    Activity: ({ node, children }: CustomRendererProps<ActivityData>) => {
        const { title, description, date, location } = node.attrs.data;
        return (
            <article>
                <h2>{title}</h2>
                <p>{description}</p>
                <time>{date}</time>
                {location && <span>{location}</span>}
            </article>
        );
    },

    // Standard blocks don't need data typing
    heading: ({ node, children }: CustomRendererProps) => {
        const Heading = `h${node.attrs?.level || 1}` as keyof JSX.IntrinsicElements;
        return <Heading>{children}</Heading>;
    }
};
```

#### Level 3: Component-Level Typing (Best for Reusability)

```typescript
import { CustomRendererComponent, CustomRendererProps } from '@dotcms/react';

// Define your contentlet data interfaces
interface ActivityData {
    contentType: 'Activity';
    title: string;
    description: string;
    date: string;
    location?: string;
}

interface BlogData {
    contentType: 'Blog';
    title: string;
    body: string;
    author: string;
    publishDate: string;
}

// Create fully-typed reusable components
const Activity: CustomRendererComponent<ActivityData> = ({ node, children }) => {
    // ✅ Full autocomplete for node.attrs.data properties
    const { title, description, date, location } = node.attrs.data;

    return (
        <article className="activity">
            <h2>{title}</h2>
            <p>{description}</p>
            <time dateTime={date}>{new Date(date).toLocaleDateString()}</time>
            {location && <address>{location}</address>}
            {children}
        </article>
    );
};

const Blog: CustomRendererComponent<BlogData> = ({ node }) => {
    // ✅ Full type safety and autocomplete
    const { title, body, author, publishDate } = node.attrs.data;

    return (
        <article className="blog-post">
            <h1>{title}</h1>
            <p className="byline">By {author} on {publishDate}</p>
            <div dangerouslySetInnerHTML={{ __html: body }} />
        </article>
    );
};

// Use typed components
const customRenderers = {
    Activity,
    Blog,
    heading: ({ node, children }: CustomRendererProps) => {
        const Heading = `h${node.attrs?.level || 1}` as keyof JSX.IntrinsicElements;
        return <Heading>{children}</Heading>;
    }
};

<DotCMSBlockEditorRenderer
    blocks={content}
    customRenderers={customRenderers}
/>
```

### Accessing Node Properties

The `node` prop provides access to all block editor node properties:

```typescript
import { CustomRendererProps } from '@dotcms/react';

const MyRenderer: React.FC<CustomRendererProps<MyData>> = ({ node, children }) => {
    // Access node properties
    const type = node.type;                    // Block type name
    const data = node.attrs?.data;             // Contentlet data (typed as MyData)
    const level = node.attrs?.level;           // Other attributes (e.g., heading level)
    const style = node.attrs?.style;           // Inline styles
    const marks = node.marks;                  // Text formatting marks
    const content = node.content;              // Nested content nodes
    const text = node.text;                    // Text content

    return <div>{/* render based on node properties */}</div>;
};
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
const CalloutRenderer = ({ node, children }) => (
  <div className={`callout callout-${node.attrs.type}`}>
    <h3>{node.attrs.title}</h3>
    <p>{node.attrs.message}</p>
    {children}
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