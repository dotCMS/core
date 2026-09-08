import { Component, input } from '@angular/core';

/**
 * The portlet's empty / no-results / not-permitted state: a title with an optional line under
 * it, centred in whatever space it is given.
 *
 * Seven of these were written out by hand across the five tabs, each carrying its own
 * typography and spacing literals, so a change to the portlet's empty states meant editing
 * every tab and hoping none had drifted.
 *
 * Takes resolved strings rather than message keys, as `dot-site` does — the caller pipes its
 * own `dm`, which keeps the keys visible at the point of use.
 *
 * Not `dot-empty-container` from `@dotcms/ui`: that renders a smaller `text-lg` title, a
 * styled subtitle and an icon slot, so adopting it would restyle every empty state in the
 * portlet rather than just de-duplicate them.
 */
@Component({
    selector: 'dot-ai-empty-state',
    template: `
        <div class="p-8 text-center">
            <h2 class="text-2xl font-medium">{{ title() }}</h2>

            @if (subtitle()) {
                <p class="mt-1 text-muted-color">{{ subtitle() }}</p>
            }
        </div>
    `,
    // data-testid stays on the call site rather than an input: written there it lands on
    // this host element in the parent's own template, so it is present whether or not a test
    // renders this component for real.
    host: { class: 'block' }
})
export class DotAiEmptyStateComponent {
    readonly title = input.required<string>();

    readonly subtitle = input<string>('');
}
