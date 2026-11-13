import { Component, computed, inject, input, output, viewChild, viewChildren } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';
import { Tooltip, TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DOT_SEARCH_SCOPE, DotSearchScope } from '@dotcms/dotcms-models';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import {
    CHIP_FILTER_LISTBOX_PT,
    CHIP_FILTER_POPOVER_PT,
    CHIP_FILTER_SCROLL_HEIGHT
} from '../dot-chip-filter/constants';
import { DotSearchInputComponent } from '../dot-search-input/dot-search-input.component';

/**
 * The search box with a Title / All Fields scope, shared by Content Drive and the AssetPicker.
 *
 * Purely presentational: the host owns the term and the scope, which flow in through `term` and
 * `scope` and come back through `search` (debounced) and `scopeChange`. It reads no store, no
 * Router and no shortcut registry, so it works in any host — the AssetPicker also runs in a legacy
 * host that has none of them — and each host keeps its own state and its own shortcuts, binding
 * them through {@link focus}.
 */
@Component({
    selector: 'dot-search-scope-input',
    templateUrl: './dot-search-scope-input.component.html',
    imports: [
        DotSearchInputComponent,
        ButtonModule,
        InputGroupModule,
        InputGroupAddonModule,
        ListboxModule,
        PopoverModule,
        TooltipModule,
        DotMessagePipe,
        FormsModule
    ],
    host: { class: 'w-full' }
})
export class DotSearchScopeInputComponent {
    readonly #messageService = inject(DotMessageService);

    /**
     * Current search term, owned by the host.
     * @alias term
     */
    readonly $term = input('', { alias: 'term' });

    /**
     * Active scope, owned by the host.
     * @alias scope
     */
    readonly $scope = input<DotSearchScope>(DOT_SEARCH_SCOPE.TITLE, { alias: 'scope' });

    /**
     * `data-testid` for the text box, so a host that renders more than one search box can tell them
     * apart. Defaults to the shared box's own id, so existing consumers and their tests are
     * unaffected.
     * @alias testId
     */
    readonly $testId = input('search-input-field', { alias: 'testId' });

    /** The debounced term, as the shared search box emits it. */
    readonly search = output<string>();

    /** A different scope was picked. Never emitted for the scope that is already active. */
    readonly scopeChange = output<DotSearchScope>();

    // NOTE: `private`, not `#`, despite TYPESCRIPT_STANDARDS.md:87. Angular's compiler rejects a
    // signal query on an ES-private field: "Cannot use 'viewChild' on a class member that is
    // declared as ES private." The standard cannot be followed here.
    private readonly $searchInput = viewChild.required(DotSearchInputComponent);

    /**
     * The explanation on each scope option. Found through a query even though the listbox renders
     * the options, because queries follow where a template is declared, not where it is stamped.
     * `private`, not `#`, for the same compiler reason as {@link $searchInput}.
     */
    private readonly $scopeTooltips = viewChildren(Tooltip);

    /**
     * The two options, in the order they read best: the narrow one first, because it is the choice
     * the user is here to make. The wide one is called "All Fields" rather than "All Content" — it
     * widens which FIELDS are read, not which content is searched.
     *
     * `label` is resolved here, eagerly, rather than left as a raw i18n key for the item template
     * to pipe through `| dm`. `p-listbox` puts its own `[attr.aria-label]` on each option element
     * from `getOptionLabel(option)`, which falls back to the raw `option.label` — it does not go
     * through this component's template, so a piped label left a screen reader announcing the key
     * itself (e.g. "content-drive.search.scope.title") instead of the translated text.
     */
    protected readonly scopeOptions = [
        {
            label: this.#messageService.get('content-drive.search.scope.title'),
            help: 'content-drive.search.scope.title.help',
            value: DOT_SEARCH_SCOPE.TITLE
        },
        {
            label: this.#messageService.get('content-drive.search.scope.all-fields'),
            help: 'content-drive.search.scope.all-fields.help',
            value: DOT_SEARCH_SCOPE.ALL_FIELDS
        }
    ];

    /**
     * The same popover/listbox pass-through every other filter dropdown already uses, so this panel
     * matches the rest of the toolbar instead of introducing its own styling.
     */
    protected readonly POPOVER_PT = CHIP_FILTER_POPOVER_PT;
    protected readonly LISTBOX_PT = CHIP_FILTER_LISTBOX_PT;
    protected readonly SCROLL_HEIGHT = CHIP_FILTER_SCROLL_HEIGHT;

    /**
     * The trigger shows the active scope. Already-translated text — see the comment on
     * `scopeOptions` for why the label is resolved there rather than piped through `| dm`.
     */
    protected readonly $activeScopeLabel = computed(
        () =>
            this.scopeOptions.find((option) => option.value === this.$scope())?.label ??
            this.#messageService.get('content-drive.search.scope.title')
    );

    /**
     * The trigger's own border removed and its background set to the field's white, both via PT
     * rather than Tailwind classes, and its content pinned to the edges so the active scope reads
     * from the same place no matter which scope is active.
     *
     * `pButtonPT`'s `root.style` is consumed through `[style]`/`[class]` HOST BINDINGS on the
     * directive's own host element (see `Bind`, the directive backing this), which Angular applies
     * the same way any `[style]` binding is — as a real inline style. That wins the cascade over
     * PrimeNG's own injected `.p-button-secondary` rule unconditionally, the same guarantee
     * `!important` gives, without reaching for it. A plain `bg-white` class cannot: PrimeNG's
     * styles are appended to the head at runtime, after the Tailwind stylesheet, so at equal
     * specificity the injected rule wins.
     *
     * The background value is the same design token the neighboring input paints with —
     * `inputtext.background`, read as the CSS variable PrimeNG emits for every token — so the two
     * halves of the field stay the same white in any theme, including dark mode. As a side effect
     * the hover recolor `.p-button-secondary` would apply is also beaten by the inline style, which
     * is what a dropdown trigger wants: the surface does not change under the pointer, the way
     * `p-select` behaves.
     *
     * The last entry exists because Lara centers a button's content (`justify-content: center`,
     * hardcoded in the injected stylesheet — no design token exposes it). Centered, the label and
     * the chevron are one group that recenters itself as its width changes. `space-between` — the
     * layout PrimeNG's own `p-select` trigger uses — parks the label on the left edge and the
     * chevron on the right edge of the fixed-width button, so switching scopes moves neither.
     */
    protected readonly TRIGGER_PT = {
        root: {
            style: {
                border: 'none',
                background: 'var(--p-inputtext-background)',
                justifyContent: 'space-between'
            }
        }
    };

    /**
     * Squares off the search input's connecting edge — the side that meets the scope addon. This
     * component's own host sits between `p-inputgroup` and the real `<input>`, breaking PrimeNG's
     * structural CSS, so the override rides the design token through `inputDt` instead of
     * competing for specificity (see the template comment). Hoisted like `TRIGGER_PT`: an inline
     * object literal would be recreated on every change detection cycle.
     */
    protected readonly INPUT_DT = {
        border: { radius: '{form.field.border.radius} 0 0 {form.field.border.radius}' }
    };

    /** Puts the caret in the text box, for hosts that bind their own search shortcut. */
    focus(): void {
        this.$searchInput().focus();
    }

    /**
     * Hides every scope option's explanation once the panel closes.
     */
    protected onScopePanelHide(): void {
        // A tooltip only hides on its own mouse-leave. Closing the panel by clicking outside it
        // removes the hovered option without one, so its explanation stayed floating over the page.
        this.$scopeTooltips().forEach((tooltip) => tooltip.hide());
    }

    /**
     * Reports a newly picked scope.
     *
     * Re-selecting the scope that is already active is ignored: the results cannot change, and the
     * host would reset the user to page 1 for nothing.
     *
     * The `null` guard is load-bearing, not defensive filler. `p-listbox` is single-select with
     * `metaKeySelection` at its default of `false`, which makes it a TOGGLE: re-clicking the
     * option that is already selected emits `null` instead of the option's value
     * (`onOptionSelectSingle` in `primeng/listbox`). Without the guard that `null` would reach the
     * host as a real scope change and silently flip the active search to All Fields.
     */
    protected onScopeChange(scope: DotSearchScope | null): void {
        if (scope == null || scope === this.$scope()) {
            return;
        }

        this.scopeChange.emit(scope);
    }
}
