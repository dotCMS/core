import { Component, computed, inject, signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotCopyButtonComponent, DotMessagePipe, DotSearchInputComponent } from '@dotcms/ui';

import { DotAiStore } from '../../store/dot-ai.store';
import { DOT_AI_CONFIG_SOURCE, toConfigRows } from '../../utils/dot-ai-config.utils';

/**
 * Config Values: every resolved dotAI setting, where it came from, and the raw provider
 * configuration behind it.
 *
 * A diagnostic screen, so it stays reachable and useful even when nothing else works —
 * which is exactly when it is needed (FR-048).
 */
@Component({
    selector: 'dot-ai-config-values',
    imports: [
        TableModule,
        TagModule,
        ButtonModule,
        DialogModule,
        DotSearchInputComponent,
        DotCopyButtonComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-ai-config-values.component.html',
    host: { class: 'block h-full' }
})
export default class DotAiConfigValuesComponent {
    protected readonly store = inject(DotAiStore);

    protected readonly sources = DOT_AI_CONFIG_SOURCE;
    protected readonly $filter = signal('');
    protected readonly $providerDialogOpen = signal(false);

    protected readonly $rows = computed(() => toConfigRows(this.store.resolvedConfig()));

    /** Client-side: the whole config arrives in one response, so there is nothing to fetch. */
    protected readonly $filteredRows = computed(() => {
        const needle = this.$filter().trim().toLowerCase();

        if (!needle) {
            return this.$rows();
        }

        return this.$rows().filter(
            (row) =>
                row.key.toLowerCase().includes(needle) || row.value.toLowerCase().includes(needle)
        );
    });

    /** A flat two-column table cannot represent nested JSON, so it gets its own view. */
    protected readonly $providerJson = computed(() =>
        JSON.stringify(this.store.resolvedConfig()?.providerConfig ?? {}, null, 2)
    );

    protected severityFor(source: string): 'info' | 'secondary' {
        return source === DOT_AI_CONFIG_SOURCE.APP_CONFIG ? 'info' : 'secondary';
    }
}
