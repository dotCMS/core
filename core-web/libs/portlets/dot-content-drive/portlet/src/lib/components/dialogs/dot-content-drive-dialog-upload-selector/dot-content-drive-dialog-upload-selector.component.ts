import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { DotFolderTreeNodeData } from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { UPLOAD_SELECTOR_OPTIONS } from '../../../shared/constants';
import {
    DotContentDriveUploadBaseType,
    DotContentDriveUploadSelection
} from '../../../shared/models';

/**
 * Content Drive upload selector: lets the user pick whether the upload is created as an Asset
 * (`DOTASSET`) or a File (`FILEASSET`). Presented as a compact menu in the Upload-button popover
 * and as spaced cards in the drag-and-drop modal (see {@link $variant}).
 *
 * Each option is a single click — choosing one emits the full
 * {@link DotContentDriveUploadSelection} (target folder + chosen base type + the files, when
 * already known) so the shell can trigger the upload directly. Carrying the folder forward also
 * feeds the per-folder upload preference set in folder settings (epic #35436).
 */
@Component({
    selector: 'dot-content-drive-dialog-upload-selector',
    imports: [DotMessagePipe],
    templateUrl: './dot-content-drive-dialog-upload-selector.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveDialogUploadSelectorComponent {
    /** Folder the upload targets; carried through to the emitted selection (root when undefined). */
    $targetFolder = input<DotFolderTreeNodeData | undefined>(undefined, { alias: 'targetFolder' });

    /** Files to upload — present for the drag-and-drop flow, absent for the Upload-button flow. */
    $files = input<FileList | undefined>(undefined, { alias: 'files' });

    /**
     * Presentation: `menu` (default) is the compact edge-to-edge list used in the button popover;
     * `cards` is the spaced, bordered layout used in the drag-and-drop modal.
     */
    $variant = input<'menu' | 'cards'>('menu', { alias: 'variant' });

    /** Emits the chosen base type plus the upload context when the user picks an option. */
    selectUploadType = output<DotContentDriveUploadSelection>();

    protected readonly options = UPLOAD_SELECTOR_OPTIONS;

    protected onSelect(baseType: DotContentDriveUploadBaseType): void {
        this.selectUploadType.emit({
            targetFolder: this.$targetFolder(),
            baseType,
            files: this.$files()
        });
    }
}
