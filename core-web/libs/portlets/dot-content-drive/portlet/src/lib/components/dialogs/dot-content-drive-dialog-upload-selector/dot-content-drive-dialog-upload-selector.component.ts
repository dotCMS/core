import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    output,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { RadioButtonModule } from 'primeng/radiobutton';

import { DotFolderTreeNodeData } from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { UPLOAD_SELECTOR_OPTIONS } from '../../../shared/constants';
import {
    DotContentDriveUploadBaseType,
    DotContentDriveUploadSelection
} from '../../../shared/models';
import { DotContentDriveStore } from '../../../store/dot-content-drive.store';

/**
 * Content Drive upload dialog body: lets the user choose whether the upload is created as an
 * Asset (`DOTASSET`) or a File (`FILEASSET`) before the upload runs.
 *
 * On confirm it emits the full {@link DotContentDriveUploadSelection} (target folder + chosen
 * base type + the files, when already known) so the shell can trigger the upload directly.
 * Carrying the folder forward also feeds the future "remember preference per folder" feature
 * (epic #35436) without reshaping this contract.
 */
@Component({
    selector: 'dot-content-drive-dialog-upload-selector',
    imports: [FormsModule, ButtonModule, RadioButtonModule, DotMessagePipe],
    templateUrl: './dot-content-drive-dialog-upload-selector.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveDialogUploadSelectorComponent {
    readonly #store = inject(DotContentDriveStore);

    /** Folder the upload targets; carried through to the emitted selection (root when undefined). */
    $targetFolder = input<DotFolderTreeNodeData | undefined>(undefined, { alias: 'targetFolder' });

    /** Files to upload — present for the drag-and-drop flow, absent for the Upload-button flow. */
    $files = input<FileList | undefined>(undefined, { alias: 'files' });

    /** Emits the chosen base type plus the upload context when the user confirms. */
    selectUploadType = output<DotContentDriveUploadSelection>();

    protected readonly options = UPLOAD_SELECTOR_OPTIONS;

    /** Currently selected base type. Defaults to the first (recommended) option. */
    protected readonly $selectedType = signal<DotContentDriveUploadBaseType>(
        UPLOAD_SELECTOR_OPTIONS[0].baseType
    );
    protected readonly $canContinue = computed(() => !!this.$selectedType());

    protected onContinue(): void {
        const baseType = this.$selectedType();
        if (!baseType) {
            return;
        }

        this.selectUploadType.emit({
            targetFolder: this.$targetFolder(),
            baseType,
            files: this.$files()
        });
    }

    protected onCancel(): void {
        this.#store.closeDialog();
    }
}
