import { EMPTY } from 'rxjs';

import { Component, DestroyRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileSelectEvent, FileUpload, FileUploadModule } from 'primeng/fileupload';
import { InputTextModule } from 'primeng/inputtext';

import { catchError, finalize } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotConfigurationAssetService } from '../../services/dot-configuration-asset.service';
import { assetFileName } from '../../store/dot-configuration.mappers';

export interface DotConfigurationLogoDialogData {
    /** Logo currently in the form, as a `/dA/...` path; empty when none is set. */
    current: string;
}

const ASSET_PATH = /^\/dA\/\S+$/;

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
    readonly #assetService = inject(DotConfigurationAssetService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #destroyRef = inject(DestroyRef);

    protected readonly fileUpload = viewChild<FileUpload>('fileUpload');
    protected readonly accept = LOGO_ACCEPT;

    protected readonly $path = signal(this.#config.data?.current ?? '');
    protected readonly $uploading = signal(false);
    protected readonly $pathInvalid = computed(() => !ASSET_PATH.test(this.$path().trim()));
    protected readonly $fileName = computed(() => assetFileName(this.$path()));

    onPathChange(path: string): void {
        this.$path.set(path);
    }

    onFileSelect(event: FileSelectEvent): void {
        const [file] = event.currentFiles;
        this.fileUpload()?.clear();

        if (!file) {
            return;
        }

        this.$uploading.set(true);
        this.#assetService
            .uploadImage(file)
            .pipe(
                catchError((error) => {
                    this.#httpErrorManager.handle(error);

                    return EMPTY;
                }),
                finalize(() => this.$uploading.set(false)),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((path) => this.$path.set(path));
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
