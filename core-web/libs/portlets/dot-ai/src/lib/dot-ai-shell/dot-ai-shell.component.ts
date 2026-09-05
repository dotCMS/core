import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { TabsModule } from 'primeng/tabs';

import { filter, map, startWith } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { DOT_AI_TABS, DotAiTabId } from '../models/dot-ai-portlet.models';

/**
 * Shell for the dotAI portlet: the tab bar plus the outlet the five tabs render into.
 *
 * Tabs are routed rather than value-driven, so each one is bookmarkable (FR-005) and only the
 * active tab's component is ever instantiated. That is why `<p-tabs>` wraps the `<p-tablist>`
 * alone and the `<router-outlet>` sits outside it — with routed tabs there are no
 * `<p-tabpanel>` children to hold content. Same shape as `dot-analytics-dashboard`.
 *
 * `[value]` is still bound, even though nothing reads it for navigation: PrimeNG derives
 * `aria-selected` from it, and without it every tab reports `aria-selected="true"` to a
 * screen reader. Navigation comes from `routerLink`; `[value]` exists purely so the
 * accessibility state is truthful (FR-056).
 */
@Component({
    selector: 'dot-ai-shell',
    imports: [RouterOutlet, RouterLink, RouterLinkActive, TabsModule, DotMessagePipe],
    templateUrl: './dot-ai-shell.component.html',
    host: { class: 'flex flex-1 min-h-0 flex-col' }
})
export default class DotAiShellComponent {
    readonly #router = inject(Router);

    protected readonly tabs = DOT_AI_TABS;

    /** Active tab id, derived from the URL so a deep link and a click agree. */
    protected readonly $activeTab = toSignal(
        this.#router.events.pipe(
            filter((event) => event instanceof NavigationEnd),
            map(() => this.#activeTabFromUrl()),
            startWith(this.#activeTabFromUrl())
        ),
        { initialValue: DOT_AI_TABS[0].id }
    );

    #activeTabFromUrl(): DotAiTabId {
        return (
            DOT_AI_TABS.find((tab) => this.#router.url.includes(`/dotai/${tab.id}`))?.id ??
            DOT_AI_TABS[0].id
        );
    }
}
