import { inject, Injectable } from '@angular/core';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Observable } from 'rxjs';


// TODO: Create this component
import { DotEditContentLayoutComponent } from '../components/dot-edit-content-layout/dot-edit-content.layout.component';

export interface EditContentConfig {
    /**
     * Content identifier - required for 'edit' mode
     * This is the unique identifier of the existing content to edit
     */
    inode?: string;

    /**
     * Content type variable - required for 'new' mode
     * This is the content type variable/name for creating new content
     */
    contentType: string;

    /**
     * Operation mode
     * - 'edit': Edit existing content (requires contentId)
     * - 'new': Create new content (requires contentType)
     */
    mode: 'new' | 'edit';



    /**
     * Size configuration for modal container
     */
    size?: 'sm' | 'md' | 'lg' | 'xl' | 'fullscreen';

    /**
     * Callback executed when content is saved
     */
    onSave: (content: any) => void;

    /**
     * Callback executed when editing is cancelled
     */
    onCancel: () => void;

}

export interface EditContentResult {
    action: 'save' | 'cancel' | 'close';
    content?: any;
}

@Injectable({
    providedIn: 'root'
})
export class DotEditContentOrchestratorService {
    readonly #dialogService = inject(DialogService);

    #ref: DynamicDialogRef | undefined;

    /**
     * Opens edit content in a modal dialog
     */
    openModal(config: EditContentConfig): Observable<EditContentResult> {
        // Validate configuration
        this.#validateConfig(config);

        this.#ref = this.#dialogService.open(DotEditContentLayoutComponent, {
            header: this.#getDialogTitle(config),
            width: this.#getDialogWidth(config.size || 'fullscreen'),
            height: this.#getDialogHeight(config.size || 'fullscreen'),
            maximizable: true,
            resizable: false,
            draggable: false,
            modal: true,
            closeOnEscape: false,
            dismissableMask: false,
            data: {
                config
            },
            styleClass: 'edit-content-modal'
        });


        return new Observable<EditContentResult>(subscriber => {
            this.#ref.onClose.subscribe((result: EditContentResult) => {
                if (result) {
                    subscriber.next(result);
                } else {
                    subscriber.next({ action: 'cancel' });
                }
                subscriber.complete();
            });
        });
    }






    #getDialogTitle(config: EditContentConfig): string {
        if (config.mode === 'new') {
            return `Create ${config.contentType || 'Content'}`;
        }
        return `Edit Content`;
    }

    #getDialogWidth(size?: string): string {
        const sizes = {
            'sm': '600px',
            'md': '800px',
            'lg': '1200px',
            'xl': '1400px',
            'fullscreen': '95vw'
        };
        return sizes[size as keyof typeof sizes] || sizes.lg;
    }

    #getDialogHeight(size?: string): string {
        const heights = {
            'sm': '400px',
            'md': '600px',
            'lg': '800px',
            'xl': '900px',
            'fullscreen': '95vh'
        };
        return heights[size as keyof typeof heights] || heights.lg;
    }







    /**
     * Validates the edit content configuration
     */
    #validateConfig(config: EditContentConfig): void {
        if (config.mode === 'edit' && !config.inode) {
            throw new Error('inode is required when mode is "edit"');
        }

        if (config.mode === 'new' && !config.contentType) {
            throw new Error('contentType is required when mode is "new"');
        }
    }
}
