import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

/** Folder-aware upload labels. Anything else falls back to the generic "Upload". */
const UPLOAD_LABEL_KEY_BY_BASE_TYPE: Record<string, string> = {
    [DotCMSBaseTypesContentTypes.DOTASSET]: 'content-drive.upload-asset',
    [DotCMSBaseTypesContentTypes.FILEASSET]: 'content-drive.upload-file'
};

const DEFAULT_UPLOAD_LABEL_KEY = 'content-drive.upload';

/**
 * Upload trigger shared across Content Drive and AssetPicker.
 *
 * Emit-only by design: it owns the folder-aware label and nothing else. The host runs the upload
 * orchestration (type popover, file input, drag-and-drop handoff) off the emitted click.
 */
@Component({
    selector: 'dot-upload-button',
    imports: [ButtonModule, DotMessagePipe],
    template: `
        <p-button
            [label]="$labelKey() | dm"
            [disabled]="$disabled()"
            icon="pi pi-upload"
            [outlined]="true"
            data-testid="upload-button"
            (click)="upload.emit($event)" />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'inline-flex' }
})
export class DotUploadButtonComponent {
    /**
     * Base type the target folder pins uploads to, when it pins one. Drives the button label.
     * @type {string | null}
     * @alias defaultBaseType
     */
    readonly $defaultBaseType = input<string | null>(null, { alias: 'defaultBaseType' });

    /**
     * @type {boolean}
     * @alias disabled
     */
    readonly $disabled = input(false, { alias: 'disabled' });

    /**
     * Emits the originating click so the host can anchor an overlay to the button.
     * Consume it synchronously — `currentTarget` is cleared once dispatch settles.
     */
    readonly upload = output<MouseEvent>();

    protected readonly $labelKey = computed(
        () =>
            UPLOAD_LABEL_KEY_BY_BASE_TYPE[this.$defaultBaseType()?.toUpperCase() ?? ''] ??
            DEFAULT_UPLOAD_LABEL_KEY
    );
}
