# DotCreateContentDialogComponent

A lightweight wrapper dialog component that embeds the full `DotEditContentLayoutComponent` for creating new content in dialog scenarios. This component provides the complete content editing experience including the sidebar functionality within a modal dialog.

## Features

- **Full Layout Experience**: Embeds the complete `DotEditContentLayoutComponent` with sidebar
- **Content Type Support**: Works with any content type that supports the new editor
- **Seamless Integration**: Uses the existing `DotEditContentStore` and infrastructure
- **Auto-Detection**: Automatically detects content type compatibility
- **Graceful Fallback**: Shows appropriate message for unsupported content types
- **Dialog Lifecycle**: Handles content creation completion and dialog closure

## Usage

### Basic Usage

```typescript
import { DotCreateContentDialogComponent, CreateContentDialogData } from '@dotcms/edit-content';

// In your component
openCreateContentDialog() {
  const dialogData: CreateContentDialogData = {
    contentTypeId: 'blog-post', // Content type variable name
    onContentCreated: (contentlet) => {
      console.log('Content created:', contentlet);
      // Handle the created content
    }
  };

  this.dialogService.open(DotCreateContentDialogComponent, {
    data: dialogData,
    header: 'Create Blog Post',
    width: '95%',
    height: '95%',
    style: { 'max-width': '1400px', 'max-height': '900px' }
  });
}
```

### Relationship Context Usage

```typescript
const dialogData: CreateContentDialogData = {
  contentTypeId: relatedContentTypeId,
  relationshipInfo: {
    parentContentletId: parentContentlet.inode,
    relationshipName: relationshipField.variable,
    isParent: true
  },
  onContentCreated: (contentlet) => {
    // Add to relationship
    this.addToRelationship(contentlet);
  }
};
```

## How It Works

The component is a simple wrapper that:

1. **Initializes Content Creation**: Uses `DotEditContentStore.initializeNewContent()` to set up a new content creation session
2. **Embeds Full Layout**: Renders the complete `DotEditContentLayoutComponent` including sidebar
3. **Detects Completion**: Watches for when content is successfully saved (contentlet has an inode)
4. **Handles Callback**: Calls the provided callback and closes the dialog

### Content Type Compatibility

The component automatically checks if a content type supports the new editor:

```typescript
// Content types with CONTENT_EDITOR2_ENABLED = true will show the full editor
// Content types without this flag will show a fallback message
```

## Configuration Interface

```typescript
interface CreateContentDialogData {
  contentTypeId: string;                              // Content type variable name
  relationshipInfo?: {                                // Optional relationship context
    parentContentletId: string;
    relationshipName: string;
    isParent: boolean;
  };
  onContentCreated?: (contentlet: DotCMSContentlet) => void; // Callback when content is saved
}
```

## State Management

The component leverages the existing `DotEditContentStore` which provides:

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
<div data-testid="create-content-dialog">
  <button data-testid="save-button">Save</button>
  <button data-testid="cancel-button">Cancel</button>
</div>
```

## Implementation Status

- ✅ Component architecture with full layout integration
- ✅ Content type initialization and compatibility detection
- ✅ Dialog lifecycle and content creation completion handling
- ✅ Integration with existing DotEditContentStore
- ✅ Sidebar functionality and complete editing experience
- ✅ Error handling and loading states
- ✅ Responsive dialog sizing and styling
- ✅ Accessibility with proper test IDs
- ⚠️ Legacy content type fallback (placeholder message)
- ⏳ Enhanced relationship-specific handling
- ⏳ Custom dialog actions and toolbar integration

## Next Steps

1. **Legacy Editor Support**: For content types without `CONTENT_EDITOR2_ENABLED`, implement iframe-based editor or redirect to standard creation flow
2. **Enhanced Relationship Integration**: Add specific handling for relationship context and validation
3. **Custom Dialog Actions**: Add custom save/cancel buttons in dialog footer if needed
4. **Performance Optimization**: Consider lazy loading of the layout component for better performance

## Dependencies

- `@ngrx/signals` - State management
- `primeng` - UI components
- `@dotcms/data-access` - API services
- `@dotcms/dotcms-models` - Type definitions
- `@dotcms/ui` - Shared UI components 