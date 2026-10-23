import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

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

    /**
     * What the host allows, already translated (e.g. `"images"`) — empty when it allows everything.
     *
     * Set, the options describe themselves in terms of it instead of promising types the host would
     * refuse. The option list itself is never filtered: an Asset and a File can each hold an image,
     * a video or an audio file, and a folder's pinned default can be either.
     *
     * Deliberately a translated string rather than a mode or a message key — this component stays
     * generic, and the host remains the one place that knows what its own restriction is called.
     */
    $restrictionLabel = input<string>('', { alias: 'restrictionLabel' });

    /**
     * The upload types to offer, in the default order; both when unset or empty. Content Drive narrows it to
     * the type a folder pins (#37896); the asset picker never sets it.
     */
    $allowedBaseTypes = input<DotUploadBaseType[] | undefined>(undefined, {
        alias: 'allowedBaseTypes'
    });

    /** Emits the chosen base type plus the upload context when the user picks an option. */
    selectUploadType = output<DotUploadSelection>();

    /**
     * The options to render. Filtered only by a non-empty `allowedBaseTypes`: an empty list
     * restricts nothing, so it can never leave the menu without options.
     */
    protected readonly $options = computed(() => {
        const allowed = this.$allowedBaseTypes();

        return allowed?.length
            ? UPLOAD_SELECTOR_OPTIONS.filter((option) => allowed.includes(option.baseType))
            : UPLOAD_SELECTOR_OPTIONS;
    });

    protected onSelect(baseType: DotUploadBaseType): void {
        this.selectUploadType.emit({
            targetFolder: this.$targetFolder(),
            baseType,
            files: this.$files()
        });
    }
}
