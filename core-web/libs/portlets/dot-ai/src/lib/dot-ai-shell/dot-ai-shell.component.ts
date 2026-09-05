import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { MessageModule } from 'primeng/message';
import { TabsModule } from 'primeng/tabs';

import { filter, map, startWith } from 'rxjs/operators';

import { DotAiCompletionsStreamService, DotMessageService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe } from '@dotcms/ui';

import { DOT_AI_TABS, DotAiTabId } from '../models/dot-ai-portlet.models';
import { DotAiStore } from '../store/dot-ai.store';

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
    imports: [
        RouterOutlet,
        RouterLink,
        RouterLinkActive,
        TabsModule,
        MessageModule,
        DotMessagePipe
    ],
    // Store lives here, above the tabs, so the shared retrieval settings survive tab
    // navigation and the index list has a single owner with two readers.
    providers: [DotAiStore, DotAiCompletionsStreamService],
    templateUrl: './dot-ai-shell.component.html',
    host: { class: 'flex flex-1 min-h-0 flex-col' }
})
export default class DotAiShellComponent {
    readonly #router = inject(Router);
    readonly #globalStore = inject(GlobalStore);
    readonly #messageService = inject(DotMessageService);

    protected readonly store = inject(DotAiStore);
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

    constructor() {
        // Without this the shell header keeps whatever trail the previous portlet left —
        // observed live as "Home > Getting Started / Welcome" while sitting on dotAI.
        this.#globalStore.setBreadcrumbs([
            {
                id: 'dotai',
                label: this.#messageService.get('com.dotcms.repackage.javax.portlet.title.dotai'),
                url: '/dotAdmin/#/dotai',
                target: '_self'
            }
        ]);
    }

    #activeTabFromUrl(): DotAiTabId {
        return (
            DOT_AI_TABS.find((tab) => this.#router.url.includes(`/dotai/${tab.id}`))?.id ??
            DOT_AI_TABS[0].id
        );
    }
}
