import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileSelectEvent, FileUpload, FileUploadModule } from 'primeng/fileupload';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessagePipe } from '@dotcms/ui';

import { injectImageUpload } from '../../services/dot-configuration-image-upload';
import { assetFileName } from '../../store/dot-configuration.mappers';

export interface DotConfigurationLogoDialogData {
    /** Logo currently in the form, as a `/dA/...` path; empty when none is set. */
    current: string;
}

// Uploaded file names keep their spaces (`/dA/<id>/asset/image (8).png`), so only the prefix is
// checked, as the server does.
const ASSET_PATH = /^\/dA\/.+$/;

/** File types the login page renders well as a logo. */
export const LOGO_ACCEPT = '.svg,.png,image/svg+xml,image/png';

/**
 * Picks a logo, for the login screen or the admin navbar: upload an SVG or PNG, or type the path
 * of an existing asset. Closes with the `/dA/...` path on Apply and with nothing on Cancel;
 * nothing is saved until Save Changes. A logo cannot be left empty, as in the previous screen.
 */
@Component({
    selector: 'dot-configuration-logo-dialog',
    imports: [FormsModule, ButtonModule, FileUploadModule, InputTextModule, DotMessagePipe],
    templateUrl: './dot-configuration-logo-dialog.component.html',
    styleUrls: ['./dot-configuration-logo-dialog.component.scss']
})
export class DotConfigurationLogoDialogComponent {
    readonly #ref = inject(DynamicDialogRef);
    readonly #config = inject(DynamicDialogConfig<DotConfigurationLogoDialogData>);
    readonly #imageUpload = injectImageUpload();

    protected readonly fileUpload = viewChild<FileUpload>('fileUpload');
    protected readonly accept = LOGO_ACCEPT;

    protected readonly $path = signal(this.#config.data?.current ?? '');
    protected readonly $uploading = this.#imageUpload.$uploading;
    protected readonly $pathInvalid = computed(() => !ASSET_PATH.test(this.$path().trim()));
    // An empty path only keeps Apply disabled: the navbar logo opens empty, and an error before
    // the user has typed or uploaded anything reads as if something already went wrong.
    protected readonly $showPathError = computed(
        () => !!this.$path().trim() && this.$pathInvalid()
    );
    protected readonly $fileName = computed(() => assetFileName(this.$path()));

    onPathChange(path: string): void {
        this.$path.set(path);
    }

    onFileSelect(event: FileSelectEvent): void {
        this.#imageUpload.upload(event, this.fileUpload(), (path) => this.$path.set(path));
    }

    apply(): void {
        if (!this.$pathInvalid()) {
            this.#ref.close(this.$path().trim());
        }
    }

    cancel(): void {
        this.#ref.close();
    }
}
