import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    inject,
    OnInit
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { DotChipFilterComponent } from '@dotcms/ui';

import { DotHostFolderFieldComponent } from '../../../../../dot-edit-content-host-folder-field/components/host-folder-field/host-folder-field.component';
import { AddRelationshipsStore } from '../../store/add-relationships.store';

/**
 * The site / folder scope the dialog searches in.
 *
 * **It reuses `dot-host-folder-field` rather than browsing folders itself.** That component is the
 * product's site-and-folder browser — the sites list, the lazy folder tree, the in-site search and
 * the Cancel/Select footer — and it is a `ControlValueAccessor`, so a `FormControl` is the whole
 * integration. The first version of this chip hand-rolled a `p-treeSelect`, which meant a second,
 * worse folder browser to keep in step with the real one.
 *
 * **It looks like the chips beside it** because it projects `dot-chip-filter` — the very component
 * they render — into that component's `[hostFolderTrigger]` slot. The alternative — rendering the browser's input-styled trigger and
 * hiding its parts one input at a time — would have put a form field in a row of filter chips.
 *
 * **Scope is not a filter**, which is why this writes to the picker store instead of
 * `DOT_FILTER_FACADE`: Content Drive keeps its browsed folder outside the filter bag for the same
 * reason — "Clear all" returns the *filters* to their defaults and leaves the editor where they
 * were browsing.
 */
@Component({
    selector: 'dot-add-relationships-site-chip',
    imports: [ReactiveFormsModule, DotChipFilterComponent, DotHostFolderFieldComponent],
    templateUrl: './add-relationships-site-chip.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { 'data-filter-chip': 'site' }
})
export class AddRelationshipsSiteChipComponent implements OnInit {
    readonly #picker = inject(AddRelationshipsStore);
    readonly #destroyRef = inject(DestroyRef);

    /** Bound to the shared browser. Its value is `hostname:/path/`. */
    protected readonly scopeControl = new FormControl<string | null>(null);

    /** The site actually being searched, as the chip's applied selection. */
    protected readonly $selections = computed(() => [this.#picker.scopeLabel()]);

    ngOnInit(): void {
        // Seeded in the browser's own `hostname:/path/` vocabulary, not in the chip's display text.
        // `writeValue` feeds that straight into `loadSites`, so a value in any other shape leaves
        // the overlay with no sites to render — it opens empty.
        this.scopeControl.setValue(toBrowserValue(this.#picker.assetPath()), { emitEvent: false });

        this.scopeControl.valueChanges
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe((value) => {
                const scope = parseScope(value);

                if (scope && this.#isNewScope(scope)) {
                    this.#picker.setScope(scope);
                }
            });
    }

    /**
     * Whether this value actually moves the scope.
     *
     * The seeding above is silent, but `dot-host-folder-field` is a `ControlValueAccessor`: once it
     * has resolved the current site — about 90ms in, visibly longer on a throttled connection — it
     * echoes the value back through `onChange`, which **does** emit. Without this guard that echo
     * calls `setScope`, which resets paging and reloads, so opening the dialog fired the same
     * search twice with a byte-identical body. On a slow connection the first result set renders
     * and is then replaced by an identical one.
     *
     * This is obligation O9 — "a chip re-emitting its current selection must not reset paging" —
     * applied to scope. It lives here rather than in `setScope` for the same reason O9's guard
     * lives in the filter facade and not in `patchFilters`: the store method is unconditional on
     * purpose, so its other callers keep their semantics.
     *
     * @param scope The scope parsed from the control's new value.
     * @return Whether it differs from the scope already being searched.
     */
    #isNewScope(scope: { hostname: string; path?: string }): boolean {
        const current = parseScope(toBrowserValue(this.#picker.assetPath()));

        return current?.hostname !== scope.hostname || current?.path !== scope.path;
    }
}

/**
 * Converts a drive-search `//host/path/` into the browser's `host:/path/` value.
 *
 * @param assetPath The scope the picker is searching, or '' when none resolved.
 * @return The value the shared browser writes and reads, or null when there is no scope.
 */
function toBrowserValue(assetPath: string): string | null {
    if (!assetPath) {
        return null;
    }

    const withoutScheme = assetPath.replace(/^\/\//, '');
    const slash = withoutScheme.indexOf('/');

    if (slash === -1) {
        return `${withoutScheme}:/`;
    }

    return `${withoutScheme.slice(0, slash)}:${withoutScheme.slice(slash) || '/'}`;
}

/**
 * Splits the shared browser's `hostname:/path/` value into the parts the scope needs.
 *
 * @param value The control value, or null before anything is chosen.
 * @return The hostname and folder path, or null when the value carries no host.
 */
function parseScope(value: string | null): { hostname: string; path?: string } | null {
    if (!value) {
        return null;
    }

    const [hostname, path] = value.split(':');

    if (!hostname) {
        return null;
    }

    return { hostname, path: path && path !== '/' ? path : undefined };
}
