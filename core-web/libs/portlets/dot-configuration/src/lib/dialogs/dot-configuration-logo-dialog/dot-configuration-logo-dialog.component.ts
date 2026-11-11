import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileSelectEvent, FileUpload, FileUploadModule } from 'primeng/fileupload';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { DotCompanyConfigurationService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import { injectImageUpload } from '../../services/dot-configuration-image-upload';
import { assetFileName } from '../../store/dot-configuration.mappers';

export interface DotConfigurationLogoDialogData {
    /** Logo currently in the form, as a `/dA/...` path; empty when none is set. */
    current: string;
    /** Offer "Use dotCMS Logo". Only the login screen logo has a stock logo to go back to. */
    offerDotcmsLogo?: boolean;
}

// Uploaded file names keep their spaces (`/dA/<id>/asset/image (8).png`), so only the prefix is
// checked, as the server does.
const ASSET_PATH = /^\/dA\/.+$/;

/**
 * Image formats a logo can be uploaded in: the common web formats every browser renders, both as
 * the login page image and as the navbar's CSS background. SVG and PNG are recommended, for their
 * transparent background, but not required. Anything that is not an image is refused, since it
 * would show as a broken logo.
 */
export const LOGO_ACCEPT = [
    '.svg',
    '.png',
    '.jpg',
    '.jpeg',
    '.gif',
    '.webp',
    'image/svg+xml',
    'image/png',
    'image/jpeg',
    'image/gif',
    'image/webp'
].join(',');

/**
 * Picks a logo, for the login screen or the admin navbar: upload an image, type the path of
 * an existing asset or, for the login screen, go back to the stock dotCMS logo. Closes with the
 * `/dA/...` path on Apply and with nothing on Cancel; nothing is saved until Save Changes. A logo
 * cannot be left empty, as in the previous screen.
 */
@Component({
    selector: 'dot-configuration-logo-dialog',
    imports: [
        FormsModule,
        ButtonModule,
        FileUploadModule,
        InputTextModule,
        TooltipModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    templateUrl: './dot-configuration-logo-dialog.component.html'
})
export class DotConfigurationLogoDialogComponent implements OnInit {
    readonly #ref = inject(DynamicDialogRef);
    readonly #config = inject(DynamicDialogConfig<DotConfigurationLogoDialogData>);
    readonly #configurationService = inject(DotCompanyConfigurationService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #imageUpload = injectImageUpload();

    protected readonly fileUpload = viewChild<FileUpload>('fileUpload');
    protected readonly accept = LOGO_ACCEPT;
    protected readonly offerDotcmsLogo = !!this.#config.data?.offerDotcmsLogo;

    protected readonly $path = signal(this.#config.data?.current ?? '');
    protected readonly $uploading = this.#imageUpload.$uploading;
    protected readonly $pathInvalid = computed(() => !ASSET_PATH.test(this.$path().trim()));
    // An empty path only keeps Apply disabled: the navbar logo opens empty, and an error before
    // the user has typed or uploaded anything reads as if something already went wrong.
    protected readonly $showPathError = computed(
        () => !!this.$path().trim() && this.$pathInvalid()
    );
    protected readonly $fileName = computed(() => assetFileName(this.$path()));

    /** Path of the stock dotCMS logo; `null` while it loads or when the instance has none. */
    protected readonly $dotcmsLogo = signal<string | null>(null);
    /** The instance answered that it has no usable stock logo. */
    protected readonly $dotcmsLogoUnavailable = signal(false);

    ngOnInit(): void {
        if (!this.offerDotcmsLogo) {
            return;
        }

        this.#configurationService
            .getDotcmsLogo()
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: (path) => this.$dotcmsLogo.set(path),
                error: (error: HttpErrorResponse) => {
                    // A 404 is an expected state (the starter's stock logo is missing or not
                    // published), shown on the disabled button rather than as an error.
                    if (error.status !== HttpStatusCode.NotFound) {
                        this.#httpErrorManager.handle(error);
                    }

                    this.$dotcmsLogoUnavailable.set(true);
                }
            });
    }

    onPathChange(path: string): void {
        this.$path.set(path);
    }

    onFileSelect(event: FileSelectEvent): void {
        this.#imageUpload.upload(event, this.fileUpload(), (path) => this.$path.set(path));
    }

    useDotcmsLogo(): void {
        const path = this.$dotcmsLogo();

        if (path) {
            this.$path.set(path);
        }
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
