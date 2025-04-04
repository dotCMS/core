# DotCMS Block Editor Renderer

A standalone Angular component for rendering DotCMS Block Editor content with support for custom block renderers.

## Overview

The Block Editor Renderer is designed to display content created with DotCMS's Block Editor field type. It handles various block types out of the box and allows for custom rendering of blocks through a flexible renderer system.

## Basic Usage

```typescript
@Component({
    selector: 'my-component',
    standalone: true,
    imports: [DotCMSBlockEditorRendererComponent],
    template: `
        <dotcms-block-editor-renderer
            [blocks]="blockEditorContent"
            [customRenderers]="myCustomRenderers">
        </dotcms-block-editor-renderer>
    `
})
export class MyComponent {
    blockEditorContent: Block = {...};
}
```

## Default Block Types

The renderer supports the following block types out of the box:

### Text Blocks
- `PARAGRAPH` - Basic paragraph block
- `TEXT` - Inline text with optional marks
- `HEADING` - Headings (h1-h6)
- `HARDBREAK` - Line break

### Lists
- `BULLET_LIST` - Unordered list
- `ORDERED_LIST` - Ordered list
- `LIST_ITEM` - List item

### Media
- `DOT_IMAGE` - Image block
- `DOT_VIDEO` - Video block

### Other
- `BLOCK_QUOTE` - Blockquote
- `CODE_BLOCK` - Code block
- `HORIZONTAL_RULE` - Horizontal line
- `TABLE` - Table structure
- `DOT_CONTENT` - DotCMS contentlet

### Text Marks
Text blocks can include the following marks:
- `bold`
- `italic`
- `underline`
- `strike`
- `superscript`
- `subscript`
- `link`

## Custom Renderers

You can provide custom renderers for any block type. This allows you to override the default rendering or add support for custom blocks.

### Creating a Custom Renderer

```typescript
@Component({
    selector: 'my-custom-paragraph',
    standalone: true,
    template: `
        <div class="custom-paragraph">
            {{ content.text }}
        </div>
    `
})
export class MyCustomParagraphComponent {
    @Input() content: ContentNode;
}
```

### Registering Custom Renderers

```typescript
const customRenderers: CustomRenderer = {
    'paragraph': Promise.resolve(MyCustomParagraphComponent),
    'custom-block': Promise.resolve(MyCustomBlockComponent)
};

@Component({
    template: `
        <dotcms-block-editor-renderer
            [blocks]="blocks"
            [customRenderers]="customRenderers">
        </dotcms-block-editor-renderer>
    `
})
export class MyComponent {
    customRenderers = customRenderers;
}
```

## Block Structure

Blocks follow this general structure:

```typescript
interface Block {
    type: string;
    content?: ContentNode[];
    attrs?: Record<string, any>;
    marks?: Mark[];
    text?: string;
}

interface Mark {
    type: string;
    attrs?: Record<string, any>;
}
```

## Error Handling

The component includes built-in validation and error handling:

- Validates block structure
- Shows error messages in edit mode
- Gracefully handles invalid blocks
- Console warnings for development

## Examples

### Basic Text Block

```typescript
const blocks = {
    type: 'doc',
    content: [
        {
            type: 'paragraph',
            content: [
                {
                    type: 'text',
                    text: 'Hello World',
                    marks: [{ type: 'bold' }]
                }
            ]
        }
    ]
};
```

### Custom Block with Attributes

```typescript
const blocks = {
    type: 'doc',
    content: [
        {
            type: 'custom-block',
            attrs: {
                backgroundColor: 'blue',
                fontSize: '16px'
            },
            content: [
                {
                    type: 'text',
                    text: 'Custom styled text'
                }
            ]
        }
    ]
};
```

## Best Practices

1. **Custom Renderers**
   - Keep them lightweight
   - Use standalone components
   - Handle all possible block attributes
   - Include proper error boundaries

2. **Performance**
   - Use `OnPush` change detection
   - Implement trackBy functions for lists
   - Lazy load complex custom renderers

3. **Accessibility**
   - Maintain proper heading hierarchy
   - Include ARIA attributes where needed
   - Ensure keyboard navigation works

## Related Documentation

- [DotCMS Block Editor Documentation](https://www.dotcms.com/docs/latest/block-editor)
- [Angular Standalone Components](https://angular.dev/guide/components)