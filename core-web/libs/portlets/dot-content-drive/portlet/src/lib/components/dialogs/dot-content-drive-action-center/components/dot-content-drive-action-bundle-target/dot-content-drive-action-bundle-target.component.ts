import { catchError, of, take } from 'rxjs';

import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SelectModule } from 'primeng/select';

import { AddToBundleService, DotCurrentUserService } from '@dotcms/data-access';
import { DotBundle } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

/**
 * `sessionStorage` key holding the last bundle used.
 *
 * Deliberately the same key `DotAddToBundleComponent` writes, so the single-item dialog and this bulk
 * step agree on "the bundle I was just working with" instead of each remembering its own.
 */
const LAST_BUNDLE_USED = 'lastSelectedBundle';

/**
 * Normalises the editable select's value into a {@link DotBundle}.
 *
 * A typed string becomes a bundle whose `id` *is* its name: the endpoint resolves by id, falls back to
 * name, and creates a bundle when neither matches — which is how bundles have always been created from
 * this control. Whitespace-only input is nothing, not a bundle named `" "`.
 */
export const toBundle = (value: DotBundle | string | null | undefined): DotBundle | null => {
    if (!value) {
        return null;
    }

    if (typeof value === 'string') {
        const name = value.trim();

        return name ? { id: name, name } : null;
    }

    return value.name ? value : null;
};

/**
 * The configuration step for **Add to Bundle**: pick an existing bundle or name a new one.
 *
 * A rewrite of `DotAddToBundleComponent`'s inner form rather than a reuse of that component. It owns its
 * own `p-dialog`, its own accept/cancel footer, and calls `AddToBundleService.addToBundle` itself on
 * submit — a self-contained dialog rather than a form, and it never exposes its value. The service is
 * the genuinely reusable part, and that is what this shares.
 *
 * Presentational: the parent owns the chosen bundle and fires the request.
 */
@Component({
    selector: 'dot-content-drive-action-bundle-target',
    imports: [SelectModule, FormsModule, DotMessagePipe],
    templateUrl: './dot-content-drive-action-bundle-target.component.html',
    // Neither is `providedIn: 'root'`, and `AddToBundleService` needs the current user to look up
    // that user's unsent bundles. `DotAddToBundleComponent` provides the same pair.
    providers: [AddToBundleService, DotCurrentUserService],
    // A plain block: the host owns the scrolling column these sections sit in.
    host: { class: 'block' }
})
export class DotContentDriveActionBundleTargetComponent implements OnInit {
    readonly #addToBundleService = inject(AddToBundleService);

    /**
     * Distinct assets the bundle will receive.
     *
     * Not the number of selected rows: a bundle holds one entry per *identifier*, so several language
     * versions of one contentlet collapse into a single asset. The parent does that counting.
     */
    readonly assetCount = input.required<number>();
    /** Selected rows dropped by that collapse, surfaced so the smaller number is not a surprise. */
    readonly collapsedCount = input<number>(0);
    /** Freezes the picker while an action is in flight. */
    readonly disabled = input<boolean>(false);

    /** The chosen bundle, or `null` while nothing is chosen. */
    readonly bundleChange = output<DotBundle | null>();

    /** The user's unsent bundles. */
    protected readonly $bundles = signal<DotBundle[]>([]);
    /** True while the bundle list is in flight; the select stays disabled until it settles. */
    protected readonly $loading = signal<boolean>(true);

    /**
     * The raw `p-select` value: a `DotBundle` when picked from the list, a `string` when typed.
     *
     * `editable` is what makes "type a name to create a bundle" work, and it means the control's value
     * changes type depending on how the user answered.
     */
    protected readonly $value = signal<DotBundle | string | null>(null);

    /** Placeholder reflects whether there is anything to select, matching the legacy dialog. */
    protected readonly $placeholder = computed(() =>
        this.$bundles().length
            ? 'contenttypes.content.add_to_bundle.select'
            : 'contenttypes.content.add_to_bundle.type'
    );

    ngOnInit(): void {
        this.#addToBundleService
            .getBundles()
            .pipe(
                take(1),
                // An unreachable bundle list still leaves the step usable: typing a name creates a
                // bundle regardless of what loaded, so this degrades to "new bundle only" rather
                // than blocking the action behind an error.
                catchError(() => of([] as DotBundle[]))
            )
            .subscribe((bundles) => {
                this.$bundles.set(bundles);
                this.$loading.set(false);
                this.preselectLastUsed(bundles);
            });
    }

    protected onValueChange(value: DotBundle | string | null): void {
        this.$value.set(value);
        this.bundleChange.emit(toBundle(value));
    }

    /**
     * Arms the bundle this user last added to, when it is still unsent.
     *
     * Matched by name rather than id, as the legacy dialog does: a bundle created by name in a previous
     * visit was stored with its name as its id, so the id never matches what the server returns.
     */
    private preselectLastUsed(bundles: DotBundle[]): void {
        const stored = readLastBundleUsed();

        if (!stored) {
            return;
        }

        const match = bundles.find((bundle) => bundle.name === stored.name);

        if (match) {
            this.onValueChange(match);
        }
    }
}

/** Reads the remembered bundle, tolerating the absent and the corrupt. */
const readLastBundleUsed = (): DotBundle | null => {
    try {
        // `?? 'null'` for the absent key: `getItem` returns `string | null`, and `JSON.parse`
        // of the literal `null` yields null — the same value this returned before.
        return JSON.parse(sessionStorage.getItem(LAST_BUNDLE_USED) ?? 'null') as DotBundle | null;
    } catch {
        // Hand-edited or half-written storage must not take the step down with it.
        return null;
    }
};

/** Remembers the bundle just used, so the next visit opens on it. Shared with the legacy dialog. */
export const rememberLastBundleUsed = (bundle: DotBundle): void => {
    try {
        sessionStorage.setItem(LAST_BUNDLE_USED, JSON.stringify(bundle));
    } catch {
        // Private browsing or a full quota; remembering is a convenience, never a requirement.
    }
};
