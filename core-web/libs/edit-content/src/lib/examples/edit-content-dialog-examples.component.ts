import { Component, inject } from '@angular/core';
import { JsonPipe, NgIf } from '@angular/common';

import { ButtonModule } from 'primeng/button';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotEditContentDialogService } from '../services/dot-edit-content-dialog.service';

/**
 * Example component demonstrating how to use the DotEditContentDialog functionality.
 * This component shows various ways to open edit content dialogs for both new and existing content.
 */
@Component({
    selector: 'dot-edit-content-dialog-examples',
    standalone: true,
    imports: [ButtonModule, JsonPipe, NgIf],
    template: `
        <div class="edit-content-dialog-examples">
            <h2>Edit Content Dialog Examples</h2>
            
            <div class="examples-section">
                <h3>Create New Content</h3>
                <p-button 
                    label="Create Blog Post" 
                    (onClick)="openNewBlogDialog()"
                    icon="pi pi-plus" />
                
                <p-button 
                    label="Create Product" 
                    (onClick)="openNewProductDialog()"
                    icon="pi pi-plus"
                    severity="secondary" />
            </div>

            <div class="examples-section">
                <h3>Edit Existing Content</h3>
                <p-button 
                    label="Edit Content (Example Inode)" 
                    (onClick)="openEditContentDialog()"
                    icon="pi pi-pencil"
                    severity="success" />
            </div>

            <div class="examples-section">
                <h3>Advanced Usage</h3>
                <p-button 
                    label="Create with Relationship" 
                    (onClick)="openNewContentWithRelationship()"
                    icon="pi pi-share-alt"
                    severity="info" />
                
                <p-button 
                    label="Custom Dialog Size" 
                    (onClick)="openCustomSizeDialog()"
                    icon="pi pi-window-maximize"
                    severity="warning" />
            </div>

            <div *ngIf="lastResult" class="result-section">
                <h3>Last Result</h3>
                <pre>{{ lastResult | json }}</pre>
            </div>
        </div>
    `,
    styles: [`
        .edit-content-dialog-examples {
            padding: 2rem;
            max-width: 800px;
            margin: 0 auto;
        }

        .examples-section {
            margin-bottom: 2rem;
            padding: 1rem;
            border: 1px solid #ddd;
            border-radius: 8px;
        }

        .examples-section h3 {
            margin-top: 0;
            color: #333;
        }

        .examples-section p-button {
            margin-right: 1rem;
            margin-bottom: 0.5rem;
        }

        .result-section {
            background: #f5f5f5;
            padding: 1rem;
            border-radius: 8px;
            margin-top: 2rem;
        }

        .result-section pre {
            background: white;
            padding: 1rem;
            border-radius: 4px;
            overflow-x: auto;
        }
    `]
})
export class DotEditContentDialogExamplesComponent {
    readonly #dialogService = inject(DotEditContentDialogService);
    
    lastResult: any = null;

    /**
     * Example: Create new blog post content
     */
    openNewBlogDialog(): void {
        this.#dialogService.openNewContentDialog('blog-post', {
            header: 'Create New Blog Post',
            onContentSaved: (contentlet) => {
                console.log('Blog post created:', contentlet);
                this.lastResult = { action: 'created', contentlet };
            },
            onCancel: () => {
                console.log('Blog post creation cancelled');
                this.lastResult = { action: 'cancelled' };
            }
        }).subscribe(result => {
            console.log('Dialog closed with result:', result);
        });
    }

    /**
     * Example: Create new product content
     */
    openNewProductDialog(): void {
        this.#dialogService.openNewContentDialog('product', {
            header: 'Create New Product',
            width: '90%',
            height: '90%',
            onContentSaved: (contentlet) => {
                console.log('Product created:', contentlet);
                this.lastResult = { action: 'created', contentlet };
            }
        }).subscribe(result => {
            console.log('Product dialog closed:', result);
        });
    }

    /**
     * Example: Edit existing content
     */
    openEditContentDialog(): void {
        // Using a placeholder inode - in real usage you'd get this from your data
        const exampleInode = 'your-content-inode-here';
        
        this.#dialogService.openEditContentDialog(exampleInode, {
            header: 'Edit Content',
            onContentSaved: (contentlet) => {
                console.log('Content updated:', contentlet);
                this.lastResult = { action: 'updated', contentlet };
            }
        }).subscribe(result => {
            console.log('Edit dialog closed:', result);
        });
    }

    /**
     * Example: Create content with relationship information
     */
    openNewContentWithRelationship(): void {
        this.#dialogService.openNewContentDialog('related-content', {
            header: 'Create Related Content',
            relationshipInfo: {
                parentContentletId: 'parent-inode',
                relationshipName: 'relatedContent',
                isParent: true
            },
            onContentSaved: (contentlet) => {
                console.log('Related content created:', contentlet);
                this.lastResult = { action: 'created', contentlet, hasRelationship: true };
                // Here you would typically update the parent's relationship
            }
        }).subscribe(result => {
            console.log('Relationship dialog closed:', result);
        });
    }

    /**
     * Example: Custom dialog size
     */
    openCustomSizeDialog(): void {
        this.#dialogService.openNewContentDialog('news', {
            header: 'Create News Article',
            width: '80%',
            height: '80%',
            onContentSaved: (contentlet) => {
                console.log('News article created:', contentlet);
                this.lastResult = { action: 'created', contentlet, customSize: true };
            }
        }).subscribe(result => {
            console.log('Custom size dialog closed:', result);
        });
    }

    /**
     * Example: Using the generic dialog method with full control
     */
    openAdvancedDialog(): void {
        this.#dialogService.openDialog(
            {
                mode: 'new',
                contentTypeId: 'advanced-content',
                onContentSaved: (contentlet) => {
                    console.log('Advanced content created:', contentlet);
                    this.lastResult = { action: 'created', contentlet, advanced: true };
                }
            },
            {
                header: 'Advanced Content Creation',
                width: '100%',
                height: '100%',
                maximizable: true,
                closable: true
            }
        ).subscribe(result => {
            console.log('Advanced dialog closed:', result);
        });
    }
} 