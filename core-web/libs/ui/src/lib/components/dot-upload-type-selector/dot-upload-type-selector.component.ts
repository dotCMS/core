import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { TreeNodeData } from '@dotcms/dotcms-models';

import { UPLOAD_SELECTOR_OPTIONS } from './constants';
import { DotUploadBaseType, DotUploadSelection } from './models';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

/**
 * Upload type prompt: lets the user pick whether an upload is created as an Asset (`DOTASSET`) or a
 * File (`FILEASSET`). Shared by Content Drive (Upload-button popover and drag-and-drop modal) and
 * the AssetPicker.
 *
 * Each option is a single click — choosing one emits the full {@link DotUploadSelection} (target
 * folder + chosen base type + the files, when already known) so the host can trigger the upload
 * directly. Carrying the folder forward also feeds the per-folder upload preference set in folder
 * settings (epic #35436).
 */
@Component({
    selector: 'dot-upload-type-selector',
    imports: [DotMessagePipe],
    templateUrl: './dot-upload-type-selector.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUploadTypeSelectorComponent {
    /** Folder the upload targets; carried through to the emitted selection (root when undefined). */
    $targetFolder = input<TreeNodeData | undefined>(undefined, { alias: 'targetFolder' });

    /** Files to upload — present for the drag-and-drop flow, absent for the Upload-button flow. */
    $files = input<FileList | undefined>(undefined, { alias: 'files' });

    /** Emits the chosen base type plus the upload context when the user picks an option. */
    selectUploadType = output<DotUploadSelection>();

    protected readonly options = UPLOAD_SELECTOR_OPTIONS;

    protected onSelect(baseType: DotUploadBaseType): void {
        this.selectUploadType.emit({
            targetFolder: this.$targetFolder(),
            baseType,
            files: this.$files()
        });
    }
}
