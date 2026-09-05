import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { DotMessagePipe } from '@dotcms/ui';

import { DotAiTab } from '../models/dot-ai-portlet.models';

/**
 * Temporary stand-in for the five dotAI tabs.
 *
 * It exists so the portlet registration and routing can be verified in a real browser before
 * any logic is written — the `portlet.xml` swap is the one part of this feature with no
 * automated coverage, so it ships first against a shell this small.
 *
 * Reads its tab from route `data` rather than `input()`, because the app does not enable
 * `withComponentInputBinding()`.
 *
 * Every route is replaced by its real component as the tabs land. Delete this file when the
 * last one does.
 */
@Component({
    selector: 'dot-ai-tab-placeholder',
    imports: [DotMessagePipe],
    template: `
        <div
            class="flex h-full flex-col items-center justify-center gap-2 p-8 text-center"
            [attr.data-testid]="'dotai-placeholder-' + tab.id">
            <span class="material-symbols-outlined text-4xl! text-surface-400" aria-hidden="true">
                {{ tab.icon }}
            </span>
            <h2 class="text-2xl font-medium">{{ tab.labelKey | dm }}</h2>
            <p class="text-muted-color">{{ 'dotai.placeholder.coming-soon' | dm }}</p>
        </div>
    `,
    host: { class: 'block h-full' }
})
export default class DotAiTabPlaceholderComponent {
    protected readonly tab = inject(ActivatedRoute).snapshot.data['tab'] as DotAiTab;
}
