import { Component, inject, signal, viewChild } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileSelectEvent, FileUpload, FileUploadModule } from 'primeng/fileupload';

import { DotMessagePipe } from '@dotcms/ui';

import { injectImageUpload } from '../../services/dot-configuration-image-upload';
import {
    BUNDLED_BACKGROUNDS,
    assetFileName,
    backgroundThumbnail
} from '../../store/dot-configuration.mappers';

export interface DotConfigurationBackgroundDialogData {
    /** Background currently in the form: a bundled path, a `/dA/...` path or empty for None. */
    current: string;
}

interface BackgroundTile {
    /** Value stored in the configuration; empty for None. */
    value: string;
    thumbnail: string;
    label: string;
}

/**
 * Picks the login background: one of the bundled images, None, or an uploaded image. Closes with
 * the chosen value on Apply and with nothing on Cancel; nothing is saved until Save Changes.
 */
@Component({
    selector: 'dot-configuration-background-dialog',
    imports: [ButtonModule, FileUploadModule, DotMessagePipe],
    templateUrl: './dot-configuration-background-dialog.component.html',
    styleUrls: ['./dot-configuration-background-dialog.component.scss']
})
export class DotConfigurationBackgroundDialogComponent {
    readonly #ref = inject(DynamicDialogRef);
    readonly #config = inject(DynamicDialogConfig<DotConfigurationBackgroundDialogData>);
    readonly #imageUpload = injectImageUpload();

    protected readonly fileUpload = viewChild<FileUpload>('fileUpload');

    protected readonly bundled: BackgroundTile[] = BUNDLED_BACKGROUNDS.map((path) => ({
        value: path,
        thumbnail: backgroundThumbnail(path),
        label: assetFileName(path)
    }));

    protected readonly $selected = signal(this.#config.data?.current ?? '');
    /** An uploaded or previously stored `/dA/...` background, shown as its own tile. */
    protected readonly $custom = signal(
        this.#isCustom(this.#config.data?.current ?? '') ? (this.#config.data?.current ?? '') : ''
    );
    protected readonly $uploading = this.#imageUpload.$uploading;

    protected readonly assetFileName = assetFileName;

    select(value: string): void {
        this.$selected.set(value);
    }

    onFileSelect(event: FileSelectEvent): void {
        this.#imageUpload.upload(event, this.fileUpload(), (path) => {
            this.$custom.set(path);
            this.$selected.set(path);
        });
    }

    apply(): void {
        this.#ref.close(this.$selected());
    }

    cancel(): void {
        this.#ref.close();
    }

    #isCustom(value: string): boolean {
        return value.startsWith('/dA/');
    }
}
