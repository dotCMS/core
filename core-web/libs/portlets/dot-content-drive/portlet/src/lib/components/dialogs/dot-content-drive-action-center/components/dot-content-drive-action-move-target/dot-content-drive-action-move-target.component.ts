import { Component, computed, input, linkedSignal, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DotHostFolderFieldComponent } from '@dotcms/edit-content';
import { DotMessagePipe } from '@dotcms/ui';

import { toHostFolderValue, toPathToMove } from '../../../../../utils/action-center';

/**
 * The configuration step for a bulk **Move**: pick the destination folder.
 *
 * Wraps `DotHostFolderFieldComponent` (the Site-or-Folder content field's inner control) rather than
 * reimplementing a picker. That component already carries the whole interaction this step needs — a
 * sites list, a lazily-paginated folder tree, folder search, and a staged selection that only commits
 * on its own "Select" button — and it is a plain `ControlValueAccessor` with no dialog chrome of its
 * own, which is what makes it embeddable in the shell's single dialog.
 *
 * Presentational: the parent owns the chosen path and decides what happens next, so this holds only
 * the picker's raw value and emits the translated one.
 *
 * **Two contracts meet here.** The picker speaks the content field's `hostname:/path`; the bulk
 * endpoint's `_path_to_move` wants `//hostname/path`. {@link toPathToMove} and
 * {@link toHostFolderValue} own the conversion in each direction — inbound to seed the picker at the
 * folder being browsed, outbound to emit what will actually be sent — so no caller outside this
 * component has to know the picker's format.
 */
@Component({
    selector: 'dot-content-drive-action-move-target',
    // `ngModel` is how the picker's value is read: it is a `ControlValueAccessor` with no output of
    // its own, so a form binding is the only channel it exposes.
    imports: [DotHostFolderFieldComponent, DotMessagePipe, FormsModule],
    templateUrl: './dot-content-drive-action-move-target.component.html',
    // A plain block: the host owns the scrolling column these sections sit in.
    host: { class: 'block' }
})
export class DotContentDriveActionMoveTargetComponent {
    /** How many contentlets the move will run on, shown so the step keeps the count in view. */
    readonly itemCount = input.required<number>();
    /** Freezes the picker while an action is in flight. */
    readonly disabled = input<boolean>(false);
    /**
     * Where the picker opens, as `//hostname/path` — the folder Content Drive is currently browsing.
     *
     * Seeds the tree at that folder instead of the bare site list, which is the point: a move is
     * usually to somewhere near where the items already are. Empty leaves the picker unseeded.
     */
    readonly startingPath = input<string>('');

    /**
     * The destination as `//hostname/path`, or an empty string while nothing is chosen.
     *
     * Emitted already converted, so the parent stores exactly what it will send.
     */
    readonly pathToMoveChange = output<string>();

    /**
     * The picker's own value (`hostname:/path`).
     *
     * `linkedSignal` rather than a plain one so it starts at {@link startingPath} and still accepts the
     * user's own writes: seeding through the value is the only way to tell this picker where to open,
     * since `writeValue` is what drives its initial site/folder load.
     */
    protected readonly $hostFolderValue = linkedSignal<string | null>(
        () =>
            // Read through the util so an unparseable starting path leaves the picker unseeded rather
            // than feeding it something it cannot resolve.
            toHostFolderValue(this.startingPath()) || null
    );

    /** True once a destination is chosen, so the parent's Continue can gate on something truthful. */
    protected readonly $hasPath = computed(() => !!toPathToMove(this.$hostFolderValue()));

    /**
     * Whether the user has touched the picker yet.
     *
     * Gates the invalid styling. Without it the field renders with a red border the instant the step
     * opens — including through the sites/folders load, where there is nothing for the user to have
     * done wrong yet. "Not filled in yet" is what the disabled Continue and the footer hint are for;
     * red is for a field the user has actually emptied.
     */
    protected readonly $touched = signal(false);

    /** Marks the field invalid only once the user has cleared a destination they had chosen. */
    protected readonly $hasError = computed(() => this.$touched() && !this.$hasPath());

    protected onHostFolderValueChange(value: string | null): void {
        this.$touched.set(true);
        this.$hostFolderValue.set(value);
        this.pathToMoveChange.emit(toPathToMove(value));
    }
}
