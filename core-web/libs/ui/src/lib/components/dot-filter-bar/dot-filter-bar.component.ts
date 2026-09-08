import { Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DOT_FILTER_FACADE } from './filter-facade.token';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

/**
 * The filter chip row, shared by every surface that offers filters.
 *
 * It owns exactly two things — the wrapping layout, and the "Clear all" button — and deliberately
 * knows nothing about any individual chip. Chips arrive through `<ng-content>` and reach their
 * surface's state through {@link DOT_FILTER_FACADE} themselves.
 *
 * **Why projection rather than a chip registry.** A registry living here could not render Content
 * Drive's Workflow chip: that one stays in the portlet, and `@dotcms/ui` must not import portlet
 * code. Projection lets a surface mix shared chips with its own and keeps this component free of
 * per-chip knowledge — which is also what makes "add a chip" cost one component plus one element
 * per toolbar instead of an entry in a registry every consumer has to know about.
 *
 * Chip **order** is therefore the surface's template order, anchored by `DOT_CANONICAL_FILTER_ORDER`
 * and asserted per toolbar rather than enforced here.
 *
 * **Why a shared component reads `content-drive.*` keys.** It does look odd now that the
 * AssetPicker renders this row too, and it is deliberate: the keys were moved verbatim with the
 * chips. Renaming them means editing `Language.properties` and every translation that ships with
 * it, for no user-visible gain, and the risk is orphaning a key in a language nobody notices until
 * a customer sees the raw id. The contract lists them as frozen for exactly this reason
 * (`contracts/filter-facade.contract.md` §6, research R8). Recorded as debt, not as an oversight.
 *
 * One named slot exists beyond the chips: anything marked `dotFilterBarTrailing` renders after
 * "Clear all", still inside the wrapping row. It is there because this component is a full-width
 * flex item, so a surface that puts something *next to* the bar gets it on a line of its own —
 * which is how Content Drive's action-execution indicator lost its right-aligned spot on the filter
 * row.
 */
@Component({
    selector: 'dot-filter-bar',
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-filter-bar.component.html',
    host: { class: 'block w-full' }
})
export class DotFilterBarComponent {
    /**
     * Protected rather than private: the template reads `$hasNonDefaultFilters` straight off it,
     * and wrapping that in a local computed would add a layer that only forwards.
     */
    protected readonly filters = inject(DOT_FILTER_FACADE);

    protected onClearAll(): void {
        this.filters.clearFilters();
    }
}
