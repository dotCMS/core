import { applicationConfig, Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { provideHttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotImageEditorComponent } from './dot-image-editor.component';

/**
 * Readable English labels for the `edit.content.image-editor.*` i18n keys so the
 * isolated Storybook UI shows text instead of raw keys. Any key not listed falls
 * back to the key itself via {@link MockDotMessageService}.
 */
const IMAGE_EDITOR_MESSAGES: Record<string, string> = {
    'edit.content.image-editor.title': 'Edit image',
    'edit.content.image-editor.close.aria': 'Close image editor',
    'edit.content.image-editor.undo.aria': 'Undo',
    'edit.content.image-editor.redo.aria': 'Redo',
    'edit.content.image-editor.tool.move': 'Move',
    'edit.content.image-editor.tool.crop': 'Crop',
    'edit.content.image-editor.tool.focal': 'Focal point',
    'edit.content.image-editor.zoom.in.aria': 'Zoom in',
    'edit.content.image-editor.zoom.out.aria': 'Zoom out',
    'edit.content.image-editor.zoom.fit.aria': 'Fit to screen',
    'edit.content.image-editor.canvas.error.message': 'The image could not be loaded.',
    'edit.content.image-editor.canvas.error.retry': 'Retry',
    'edit.content.image-editor.crop.apply': 'Apply',
    'edit.content.image-editor.crop.cancel': 'Cancel',
    'edit.content.image-editor.crop.box.aria': 'Crop selection',
    'edit.content.image-editor.focal.set': 'Set focal point',
    'edit.content.image-editor.focal.cancel': 'Cancel',
    'edit.content.image-editor.focal.marker.aria': 'Focal point marker',
    'edit.content.image-editor.address.copy.aria': 'Copy image URL',
    'edit.content.image-editor.address.copy.success': 'URL copied to clipboard',
    'edit.content.image-editor.address.copy.error': 'Could not copy the URL',
    'edit.content.image-editor.panel.adjust.title': 'Adjust',
    'edit.content.image-editor.panel.adjust.subtitle': 'Color and light',
    'edit.content.image-editor.panel.transform.title': 'Transform',
    'edit.content.image-editor.panel.transform.subtitle': 'Rotate, flip and resize',
    'edit.content.image-editor.panel.fileinfo.title': 'File info',
    'edit.content.image-editor.panel.fileinfo.subtitle': 'Size and compression',
    'edit.content.image-editor.panel.history.title': 'History',
    'edit.content.image-editor.panel.history.subtitle': 'Applied edits',
    'edit.content.image-editor.adjust.brightness': 'Brightness',
    'edit.content.image-editor.adjust.hue': 'Hue',
    'edit.content.image-editor.adjust.saturation': 'Saturation',
    'edit.content.image-editor.adjust.grayscale': 'Grayscale',
    'edit.content.image-editor.transform.rotate': 'Rotate',
    'edit.content.image-editor.transform.flip': 'Flip',
    'edit.content.image-editor.transform.flip.horizontal': 'Flip horizontal',
    'edit.content.image-editor.transform.flip.vertical': 'Flip vertical',
    'edit.content.image-editor.transform.scale': 'Scale',
    'edit.content.image-editor.transform.dimensions': 'Dimensions',
    'edit.content.image-editor.transform.dimensions.error.min': 'Value is too small',
    'edit.content.image-editor.fileinfo.compression': 'Compression',
    'edit.content.image-editor.fileinfo.compression.none': 'None',
    'edit.content.image-editor.fileinfo.compression.auto': 'Auto',
    'edit.content.image-editor.fileinfo.compression.jpeg': 'JPEG',
    'edit.content.image-editor.fileinfo.compression.webp': 'WebP',
    'edit.content.image-editor.fileinfo.quality': 'Quality',
    'edit.content.image-editor.fileinfo.filesize': 'File size',
    'edit.content.image-editor.history.empty': 'No edits applied yet',
    'edit.content.image-editor.history.reset': 'Reset',
    'edit.content.image-editor.history.remove.aria': 'Remove edit',
    'edit.content.image-editor.footer.cancel': 'Cancel',
    'edit.content.image-editor.footer.save': 'Save',
    'edit.content.image-editor.footer.save-as': 'Save as new',
    'edit.content.image-editor.footer.saving': 'Saving…',
    'edit.content.image-editor.footer.download': 'Download',
    'edit.content.image-editor.footer.download.aria': 'Download image',
    'edit.content.image-editor.discard.header': 'Discard changes?',
    'edit.content.image-editor.discard.message': 'Your unsaved edits will be lost.',
    'edit.content.image-editor.discard.confirm': 'Discard',
    'edit.content.image-editor.discard.reject': 'Keep editing'
};

/**
 * Story-only launcher that opens {@link DotImageEditorComponent} through PrimeNG's
 * `DialogService`. The editor injects `DynamicDialogConfig`/`DynamicDialogRef`, so
 * it can only be rendered from inside a dynamic dialog — clicking the button is the
 * only way to see it.
 */
@Component({
    selector: 'dot-image-editor-story-host',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, DynamicDialogModule],
    providers: [DialogService],
    template: `
        <div style="padding: 2rem">
            <button
                pButton
                type="button"
                label="Open Image Editor"
                data-testid="open-image-editor"
                (click)="open()"></button>
        </div>
    `
})
class DotImageEditorStoryHostComponent {
    /** Inode used to build the preview URL the editor requests on open. */
    inode = '00000000-0000-0000-0000-000000000000';

    readonly #dialogService = inject(DialogService);

    open(): void {
        this.#dialogService.open(DotImageEditorComponent, {
            data: {
                inode: this.inode,
                variable: 'fileAsset',
                fieldName: 'fileAsset',
                byInode: true,
                fileName: 'sample.jpg',
                mimeType: 'image/jpeg'
            },
            width: 'min(92vw, 75rem)',
            height: '90%',
            modal: true,
            closable: true,
            closeOnEscape: true,
            contentStyle: { height: '100%', overflow: 'hidden', padding: '0' }
        });
    }
}

const meta: Meta<DotImageEditorStoryHostComponent> = {
    title: 'Edit Content/Image Editor',
    component: DotImageEditorStoryHostComponent,
    decorators: [
        applicationConfig({
            // The store's effects use HttpClient; animations power the modal transitions.
            providers: [provideHttpClient(), provideAnimations()]
        }),
        moduleMetadata({
            providers: [
                DialogService,
                MessageService,
                ConfirmationService,
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService(IMAGE_EDITOR_MESSAGES)
                }
            ]
        })
    ],
    parameters: {
        docs: {
            description: {
                component: [
                    'Manual preview/launcher for the new image editor. The editor is opened',
                    "through PrimeNG's `DialogService` (DynamicDialog) — click **Open Image",
                    'Editor** to launch it.',
                    '',
                    'The live image preview requires a running dotCMS backend: in isolated',
                    'Storybook the `/contentAsset/image/...` URL has no server to answer it, so',
                    'the request 404s and the canvas shows the loading → error/retry state. That',
                    'is expected and intentionally exercises the error UI. Point the `inode` arg',
                    'at a real image asset on a connected instance to see a live preview.'
                ].join('\n')
            }
        }
    }
};

export default meta;

type Story = StoryObj<DotImageEditorStoryHostComponent>;

/**
 * Default launcher. Override `inode` to target a real image asset on a connected
 * dotCMS instance for a live preview.
 */
export const Default: Story = {
    args: {
        inode: '00000000-0000-0000-0000-000000000000'
    }
};
