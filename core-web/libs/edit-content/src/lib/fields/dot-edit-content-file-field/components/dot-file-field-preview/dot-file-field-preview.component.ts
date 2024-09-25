import {
    CUSTOM_ELEMENTS_SCHEMA,
    ChangeDetectionStrategy,
    Component,
    computed,
    input,
    output,
    signal
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotTempFileThumbnailComponent, DotFileSizeFormatPipe, DotMessagePipe } from '@dotcms/ui';

import { PreviewFile } from '../../models';
import { getFileMetadata } from '../../utils';

@Component({
    selector: 'dot-file-field-preview',
    standalone: true,
    imports: [
        DotTempFileThumbnailComponent,
        DotFileSizeFormatPipe,
        DotMessagePipe,
        ButtonModule,
        DialogModule
    ],
    providers: [],
    templateUrl: './dot-file-field-preview.component.html',
    styleUrls: ['./dot-file-field-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotFileFieldPreviewComponent {
    $previewFile = input.required<PreviewFile>({ alias: 'previewFile' });
    removeFile = output();
    $showDialog = signal(false);

    $metadata = computed(() => {
        const previewFile = this.$previewFile();
        if (previewFile.source === 'temp') {
            return previewFile.file.metadata;
        }

        return getFileMetadata(previewFile.file);
    });

    toggleShowDialog() {
        this.$showDialog.set(!this.$showDialog());
    }
}
