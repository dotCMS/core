import { Component, computed, inject, signal } from '@angular/core';

import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotCopyButtonComponent,
    DotEmptyContainerComponent,
    DotMessagePipe,
    DotSearchInputComponent
} from '@dotcms/ui';

import { DotAiStore } from '../../store/dot-ai.store';
import { DOT_AI_CONFIG_SOURCE, toConfigRows } from '../../utils/dot-ai-config.utils';
import { toEmptyStateConfig } from '../../utils/dot-ai-empty-state.utils';

/**
 * Config Values: every resolved dotAI setting and where it came from.
 *
 * A diagnostic screen, so it stays reachable and useful even when nothing else works —
 * which is exactly when it is needed (FR-048).
 */
@Component({
    selector: 'dot-ai-config-values',
    imports: [
        DotEmptyContainerComponent,
        TableModule,
        TagModule,
        DotSearchInputComponent,
        DotCopyButtonComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-ai-config-values.component.html',
    host: { class: 'block h-full' }
})
export default class DotAiConfigValuesComponent {
    protected readonly store = inject(DotAiStore);

    readonly #messageService = inject(DotMessageService);

    protected readonly sources = DOT_AI_CONFIG_SOURCE;
    protected readonly $filter = signal('');

    protected readonly redactionFailedConfig = toEmptyStateConfig(this.#messageService, {
        title: 'dotai.config.redaction-failed.title',
        subtitle: 'dotai.config.redaction-failed.sub',
        icon: 'visibility_off'
    });

    protected readonly noMatchesConfig = toEmptyStateConfig(this.#messageService, {
        title: 'dotai.config.empty',
        icon: 'filter_alt_off'
    });

    /**
     * Which site's configuration is on screen, and whether it is actually that site's.
     *
     * The server resolves dotAI settings per site and falls back to the System Host's when the
     * site has none of its own, so these are two different facts and the label has to say which
     * one applies. They arrive as separate fields precisely so this can be a message key rather
     * than an English sentence assembled on the server.
     */
    protected readonly $hostLabel = computed(() => {
        const config = this.store.resolvedConfig();

        if (!config?.configHost) {
            return '';
        }

        return this.#messageService.get(
            config.configHostInherited ? 'dotai.config.host.inherited' : 'dotai.config.host',
            config.configHost
        );
    });

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

    protected severityFor(source: string): 'info' | 'secondary' {
        return source === DOT_AI_CONFIG_SOURCE.APP_CONFIG ? 'info' : 'secondary';
    }
}
