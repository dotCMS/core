import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { TabsModule } from 'primeng/tabs';

import { DotMessagePipe } from '@dotcms/ui';

import { DOT_AI_TABS } from '../models/dot-ai-portlet.models';

/**
 * Shell for the dotAI portlet: the tab bar plus the outlet the five tabs render into.
 *
 * Tabs are routed rather than value-driven, so each one is bookmarkable (FR-005) and only the
 * active tab's component is ever instantiated. That is why `<p-tabs>` wraps the `<p-tablist>`
 * alone and the `<router-outlet>` sits outside it: with routed tabs there are no
 * `<p-tabpanel>` children to hold content, and active state comes from `routerLinkActive`
 * rather than a `[value]` binding. Same shape as `dot-analytics-dashboard`.
 */
@Component({
    selector: 'dot-ai-shell',
    imports: [RouterOutlet, RouterLink, RouterLinkActive, TabsModule, DotMessagePipe],
    templateUrl: './dot-ai-shell.component.html',
    host: { class: 'flex flex-1 min-h-0 flex-col' }
})
export default class DotAiShellComponent {
    protected readonly tabs = DOT_AI_TABS;
}
