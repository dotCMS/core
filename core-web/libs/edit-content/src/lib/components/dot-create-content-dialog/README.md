# DotEditContentDialogComponent

A comprehensive dialog wrapper component that embeds the full `DotEditContentLayoutComponent` for both creating new content and editing existing content in modal scenarios. This component provides the complete content editing experience including sidebar functionality within a modal dialog.

## Features

- **Dual Mode Support**: Works for both creating new content and editing existing content
- **Full Layout Experience**: Embeds the complete `DotEditContentLayoutComponent` with sidebar
- **Content Type Support**: Works with any content type that supports the new editor
- **Store Isolation**: Each dialog instance gets its own isolated `DotEditContentStore`
- **Seamless Integration**: Uses the existing content editing infrastructure
- **Auto-Detection**: Automatically detects content type compatibility
- **Graceful Fallback**: Shows appropriate message for unsupported content types
- **Dialog Lifecycle**: Handles content save completion and dialog closure
- **Callback System**: Provides callbacks for save and cancel events

## Usage

### Basic Usage with Service

The recommended approach is to use the `DotEditContentDialogService`:

```typescript
import { DotEditContentDialogService } from '@dotcms/edit-content';

// In your component
constructor(private editContentDialogService: DotEditContentDialogService) {}

// Create new content
openCreateDialog() {
  this.editContentDialogService.openNewContentDialog('blog-post', {
    header: 'Create Blog Post',
    onContentSaved: (contentlet) => {
      console.log('Content created:', contentlet);
      this.refreshContentList();
    }
  }).subscribe(result => {
    if (result) {
      console.log('Dialog completed with:', result);
    }
  });
}

// Edit existing content
openEditDialog(contentletInode: string) {
  this.editContentDialogService.openEditContentDialog(contentletInode, {
    header: 'Edit Content',
    onContentSaved: (contentlet) => {
      console.log('Content updated:', contentlet);
      this.refreshContentList();
    }
  }).subscribe(result => {
    if (result) {
      console.log('Dialog completed with:', result);
    }
  });
}
```

### Direct Component Usage

For more control, you can use the component directly:

```typescript
import { DotEditContentDialogComponent, EditContentDialogData } from '@dotcms/edit-content';

// Create new content
openCreateContentDialog() {
  const dialogData: EditContentDialogData = {
    mode: 'new',
    contentTypeId: 'blog-post',
    onContentSaved: (contentlet) => {
      console.log('Content created:', contentlet);
    }
  };

  this.dialogService.open(DotEditContentDialogComponent, {
    data: dialogData,
    header: 'Create Blog Post',
    width: '95%',
    height: '95%'
  });
}

// Edit existing content
openEditContentDialog(inode: string) {
  const dialogData: EditContentDialogData = {
    mode: 'edit',
    contentletInode: inode,
    onContentSaved: (contentlet) => {
      console.log('Content updated:', contentlet);
    }
  };

  this.dialogService.open(DotEditContentDialogComponent, {
    data: dialogData,
    header: 'Edit Content',
    width: '95%',
    height: '95%'
  });
}
```

### Relationship Context Usage

When creating content within relationship contexts:

```typescript
const dialogData: EditContentDialogData = {
  mode: 'new',
  contentTypeId: relatedContentTypeId,
  relationshipInfo: {
    parentContentletId: parentContentlet.inode,
    relationshipName: relationshipField.variable,
    isParent: true
  },
  onContentSaved: (contentlet) => {
    // Add to relationship
    this.addToRelationship(contentlet);
  }
};
```

## Configuration Interfaces

### EditContentDialogData

```typescript
interface EditContentDialogData {
  mode: 'new' | 'edit';                           // Operation mode
  contentTypeId?: string;                         // For new content: content type ID
  contentletInode?: string;                       // For edit: contentlet inode
  depth?: number;                                 // Loading depth for existing content
  relationshipInfo?: {                            // Optional relationship context
    parentContentletId: string;
    relationshipName: string;
    isParent: boolean;
  };
  onContentSaved?: (contentlet: DotCMSContentlet) => void; // Success callback
  onCancel?: () => void;                          // Cancel callback
}
```

### Service Methods

The `DotEditContentDialogService` provides three main methods:

```typescript
// Create new content
openNewContentDialog(
  contentTypeId: string,
  options?: {
    header?: string;
    width?: string;
    height?: string;
    relationshipInfo?: EditContentDialogData['relationshipInfo'];
    onContentSaved?: (contentlet: DotCMSContentlet) => void;
    onCancel?: () => void;
  }
): Observable<DotCMSContentlet | null>

// Edit existing content
openEditContentDialog(
  contentletInode: string,
  options?: {
    header?: string;
    width?: string;
    height?: string;
    depth?: DotContentletDepths;
    onContentSaved?: (contentlet: DotCMSContentlet) => void;
    onCancel?: () => void;
  }
): Observable<DotCMSContentlet | null>

// Generic dialog with full control
openDialog(
  data: EditContentDialogData,
  dialogOptions?: { [key: string]: any }
): Observable<DotCMSContentlet | null>
```

## How It Works

### Store Isolation

Each dialog instance gets its own `DotEditContentStore` by providing it in the component's providers array:

```typescript
@Component({
  providers: [DotEditContentStore], // Component-scoped instance
  // ...
})
```

This ensures:
- Multiple dialogs can be open simultaneously without interference
- Form state doesn't leak between dialog instances
- Each dialog maintains its own workflow state
- Store state is automatically cleaned up when the dialog closes

### Initialization Flow

1. **Dialog Mode Enabled**: `store.enableDialogMode()` prevents route-based initialization
2. **Content Initialization**: Based on mode:
   - `mode: 'new'` → `store.initializeNewContent(contentTypeId)`
   - `mode: 'edit'` → `store.initializeExistingContent({ inode, depth })`
3. **Form Rendering**: The complete `DotEditContentLayoutComponent` renders with sidebar
4. **Save Detection**: Effects monitor for successful save operations
5. **Callback Execution**: Appropriate callbacks are triggered
6. **Dialog Closure**: Dialog closes with the result contentlet

### Content Type Compatibility

The component automatically checks if a content type supports the new editor:

```typescript
// Content types with CONTENT_EDITOR2_ENABLED = true will show the full editor
// Content types without this flag will show a fallback message
```

## State Management

The dialog leverages the existing `DotEditContentStore` which provides:

- Complete content editing state management
- Workflow and sidebar functionality
- Form validation and submission
- Error handling and loading states
- All the same features as the standard content editor

## Styling

The component follows the DotCMS design system:

- Uses global SCSS variables for consistency
- BEM methodology for CSS class naming
- Responsive design patterns
- Proper spacing and typography
- Accessible color contrast

## Testing

All interactive elements include `data-testid` attributes following Testing Library best practices:

```html
<div data-testid="edit-content-dialog">
  <dot-edit-content-form-layout data-testid="edit-content-layout" />
</div>
```

## Migration from Legacy Dialog

If you were using the old `DotCreateContentDialogComponent`:

```typescript
// Old way
const dialogData: CreateContentDialogData = {
  contentTypeId: 'blog-post',
  onContentCreated: (contentlet) => { /* ... */ }
};

// New way
const dialogData: EditContentDialogData = {
  mode: 'new',
  contentTypeId: 'blog-post',
  onContentSaved: (contentlet) => { /* ... */ }
};
```

## Examples

For comprehensive examples, see `edit-content-dialog-examples.component.ts` which demonstrates:

- Creating new content
- Editing existing content
- Using relationship information
- Custom dialog sizes
- Advanced configurations
- Error handling
- Callback usage

## Integration with Existing Code

The dialog integrates seamlessly with existing DotCMS patterns:

```typescript
// In relationship fields
private openContentDialog(contentType: DotCMSContentType): void {
  this.editContentDialogService.openNewContentDialog(contentType.id, {
    relationshipInfo: {
      parentContentletId: this.parentInode,
      relationshipName: this.fieldVariable,
      isParent: true
    },
    onContentSaved: (contentlet) => {
      this.addToRelationshipData(contentlet);
    }
  }).subscribe();
}

// In content listing components
editContent(contentlet: DotCMSContentlet): void {
  this.editContentDialogService.openEditContentDialog(contentlet.inode, {
    header: `Edit ${contentlet.title}`,
    onContentSaved: (updated) => {
      this.updateContentInList(updated);
    }
  }).subscribe();
}
``` 