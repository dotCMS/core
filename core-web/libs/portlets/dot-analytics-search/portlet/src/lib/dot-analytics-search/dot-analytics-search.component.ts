import { JsonObject } from '@angular-devkit/core';
import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { addStylePlugin } from '@nxext/stencil/src/stencil-core-utils';

import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { ButtonDirective } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { SplitterModule } from 'primeng/splitter';
import { TooltipModule } from 'primeng/tooltip';

import { DotAnalyticsSearchService, DotMessageService } from '@dotcms/data-access';
import { DotEmptyContainerComponent, DotMessagePipe, PrincipalConfiguration } from '@dotcms/ui';

import {
    ANALYTICS_MONACO_EDITOR_OPTIONS,
    ANALYTICS_RESULTS_MONACO_EDITOR_OPTIONS,
    isValidJson
} from './utils';

import { DotAnalyticsSearchStore } from '../store/dot-analytics-search.store';

@Component({
    selector: 'lib-dot-analytics-search',
    standalone: true,
    imports: [
        CommonModule,
        DotMessagePipe,
        ButtonDirective,
        MonacoEditorModule,
        FormsModule,
        SplitterModule,
        DropdownModule,
        DotEmptyContainerComponent,
        TooltipModule
    ],
    providers: [DotAnalyticsSearchStore, DotAnalyticsSearchService],
    templateUrl: './dot-analytics-search.component.html',
    styleUrl: './dot-analytics-search.component.scss'
})
export class DotAnalyticsSearchComponent {
    private readonly route = inject(ActivatedRoute);
    ANALYTICS_MONACO_EDITOR_OPTIONS = ANALYTICS_MONACO_EDITOR_OPTIONS;
    ANALYTICS__RESULTS_MONACO_EDITOR_OPTIONS = ANALYTICS_RESULTS_MONACO_EDITOR_OPTIONS;

    readonly store = inject(DotAnalyticsSearchStore);

    /**
     * Represents the DotMessageService instance.
     */
    readonly #dotMessageService = inject(DotMessageService);

    /**
     * The content of the query editor.
     */
    queryEditor = '';

    /**
     * Signal representing the empty state configuration.
     */
    $emptyState = signal<PrincipalConfiguration>({
        title: this.#dotMessageService.get('analytics.search.no.results'),
        icon: 'pi-search',
        subtitle: this.#dotMessageService.get('analytics.search.execute.results')
    });

    /**
     * Signal representing whether the query editor content is valid JSON.
     */
    $isValidJson = signal<boolean>(false);

    /**
     * Computed property to get the results from the store and format them as a JSON string.
     */
    $results = computed(() => {
        const results = this.store.results();

        return results ? JSON.stringify(results, null, 2) : null;
    });

    ngOnInit() {
        const { isEnterprise, healthCheck } = this.route.snapshot.data;

        this.store.initLoad(isEnterprise, healthCheck);
    }

    /**
     * Handles the request to get results based on the query editor content.
     * Validates the JSON and calls the store's getResults method if valid.
     */
    handleRequest() {
        const value = isValidJson(this.queryEditor);
        if (value) {
            this.store.getResults(value as JsonObject);
        }
    }

    /**
     * Handles changes to the query editor content.
     * Updates the $isValidJson signal based on the validity of the JSON.
     *
     * @param value - The new content of the query editor.
     */
    handleQueryChange(value: string) {
        this.$isValidJson.set(!!isValidJson(value));
    }

    protected readonly addStylePlugin = addStylePlugin;
}
