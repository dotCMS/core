import { Component, computed, inject, signal } from '@angular/core';

import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotCopyButtonComponent,
    DotEmptyContainerComponent,
    DotMessagePipe,
    DotSearchInputComponent,
    PrincipalConfiguration
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

    readonly #noMatchesConfig = toEmptyStateConfig(this.#messageService, {
        title: 'dotai.config.empty',
        icon: 'filter_alt_off'
    });

    readonly #unavailableConfig = toEmptyStateConfig(this.#messageService, {
        title: 'dotai.config.unavailable.title',
        subtitle: 'dotai.config.unavailable.sub',
        icon: 'cloud_off'
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

    /**
     * Which empty state the pane owes the user, or null when it owes none.
     *
     * The rows being empty is not one fact but three, and they need different sentences.
     * `resolvedConfig` is a computed that always returns an object, so the rows are empty
     * during the initial async window and after a failed load as well as after a filter that
     * matched nothing -- and blaming the filter in the first two cases is a lie told by the
     * one screen that exists to be trusted when nothing else works (FR-048).
     *
     * Null while the config is still on its way: a brief blank pane says less than a wrong
     * sentence does.
     */
    protected readonly $emptyConfig = computed<PrincipalConfiguration | null>(() => {
        if (this.$filteredRows().length) {
            return null;
        }

        if (this.$filter().trim()) {
            return this.#noMatchesConfig;
        }

        return this.store.configUnavailable() ? this.#unavailableConfig : null;
    });

    protected severityFor(source: string): 'info' | 'secondary' {
        return source === DOT_AI_CONFIG_SOURCE.APP_CONFIG ? 'info' : 'secondary';
    }
}
