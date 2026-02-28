# Changelog

All notable changes to the DotCMS Angular SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## v1.2.0

### Fixed

#### Contentlet Component Input Binding

- **Fixed**: `DotContentletBlock` component now correctly passes `node` instead of `contentlet` to custom renderers
  - Ensures consistency with the documented API where custom renderers receive the complete `BlockEditorNode`
  - Custom renderers should access contentlet data via `node.attrs?.['data']`

  ```typescript
  // Example: Custom renderer accessing contentlet data
  @Component({
    selector: 'activity-renderer',
    template: '<div>{{ contentlet().title }}</div>'
  })
  export class ActivityRendererComponent {
    @Input() node!: BlockEditorNode;

    contentlet = computed(() => {
      return this.node.attrs?.['data'] as Activity;
    });
  }
  ```
### Changed

#### Block Editor Renderer - Breaking Changes

- **BREAKING**: `DotCMSBlockEditorRenderer` now uses unified `BlockEditorNode` interface instead of `BlockEditorContent`
  - The `blocks` input parameter now expects `BlockEditorNode` type
  - Custom renderers must now use `@Input() node: BlockEditorNode` instead of `@Input() content: ContentNode`
  - All custom renderers receive the complete node structure with type, attrs, content, marks, and text properties

- **BREAKING**: Custom renderer component signature changed
  ```typescript
  // Before
  export class MyCustomComponent {
    @Input() content: ContentNode;
  }

  // After
  export class MyCustomComponent {
    @Input() node!: BlockEditorNode;
  }
  ```

- **BREAKING**: Component outlet inputs parameter changed
  ```typescript
  // Before
  *ngComponentOutlet="customRender | async; inputs: { content: node }"

  // After
  *ngComponentOutlet="customRender | async; inputs: { node: node }"
  ```

- **Important**: When creating a custom renderer for BlockEditorRenderer, the component must have an `@Input()` property named `node` of type `BlockEditorNode`
  - The `node` input contains all node information including type, attrs, content, marks and text properties
  - Custom renderers access node data via `@Input() node: BlockEditorNode`

  ```typescript
  // Example: Custom renderer component structure
  @Component({
    selector: 'my-custom-renderer',
    template: '<div>{{ node.text }}</div>' 
  })
  export class MyCustomRendererComponent {
    @Input() node!: BlockEditorNode; // Required input name and type
  }
  ```

### Removed

#### Type Definitions

- **BREAKING**: Removed redundant `BlockEditorContent` interface from `@dotcms/types`
  - Use `BlockEditorNode` for all block editor content structures
  - `BlockEditorNode` serves as the unified interface for both root content and individual nodes

### Migration Guide

#### For Custom Renderer Components

Update your custom renderer components to use the new node-based signature:

```typescript
// Before
import { Component, Input } from '@angular/core';

@Component({
  selector: 'my-custom-paragraph',
  template: `
    <div class="custom-paragraph">
      {{ content.text }}
    </div>
  `
})
export class MyCustomParagraphComponent {
  @Input() content: ContentNode;
}

// After
import { Component, Input } from '@angular/core';
import { BlockEditorNode } from '@dotcms/types';

@Component({
  selector: 'my-custom-paragraph',
  template: `
    <div class="custom-paragraph" [style]="node.attrs">
      {{ node.text }}
    </div>
  `
})
export class MyCustomParagraphComponent {
  @Input() node: BlockEditorNode;
}
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

Update your contentlet custom renderers to use the new input property:

```typescript
// Before
@Component({
  template: '<div>{{ contentlet.title }}</div>'
})
export class MyContentletComponent {
  @Input() contentlet: DotCMSBasicContentlet;
}

// After
@Component({
  template: '<div>{{ node.attrs.data.title }}</div>'
})
export class MyContentletComponent {
  @Input() node: BlockEditorNode;

  // Access contentlet data via node.attrs.data
  get contentlet() {
    return this.node.attrs?.['data'];
  }
}
```

### Benefits

- **Consistency**: Single unified interface (`BlockEditorNode`) for all block editor structures
- **Flexibility**: Custom renderers now have access to the complete node structure including type, attributes, marks, and nested content
- **Type Safety**: Improved TypeScript type inference and IDE autocomplete support
- **Maintainability**: Reduced type redundancy and clearer API surface
