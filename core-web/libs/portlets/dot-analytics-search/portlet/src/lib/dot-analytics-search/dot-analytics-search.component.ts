import { JsonObject } from '@angular-devkit/core';
import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { ButtonDirective } from 'primeng/button';

import { DotAnalyticsSearchService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import {
    ANALYTICS_MONACO_EDITOR_OPTIONS,
    ANALYTICS_RESULTS_MONACO_EDITOR_OPTIONS,
    isValidJson
} from './utils';

import { DotAnalyticsSearchStore } from '../store/dot-analytics-search.store';

@Component({
    selector: 'lib-dot-analytics-search',
    standalone: true,
    imports: [CommonModule, DotMessagePipe, ButtonDirective, MonacoEditorModule, FormsModule],
    providers: [DotAnalyticsSearchStore, DotAnalyticsSearchService],
    templateUrl: './dot-analytics-search.component.html',
    styleUrl: './dot-analytics-search.component.scss'
})
export class DotAnalyticsSearchComponent {
    private readonly route = inject(ActivatedRoute);
    ANALYTICS_MONACO_EDITOR_OPTIONS = ANALYTICS_MONACO_EDITOR_OPTIONS;
    ANALYTICS__RESULTS_MONACO_EDITOR_OPTIONS = ANALYTICS_RESULTS_MONACO_EDITOR_OPTIONS;

    readonly store = inject(DotAnalyticsSearchStore);

    queryEditor = '';

    /**
     * Computed property to get the results from the store and format them as a JSON string.
     */
    results$ = computed(() => {
        const results = this.store.results();

        return results ? JSON.stringify(results, null, 2) : null;
    });

    ngOnInit() {
        const { isEnterprise } = this.route.snapshot.data;

        this.store.initLoad(isEnterprise);
    }

    /**
     * Handles the request to get results based on the query editor content.
     * Validates the JSON and calls the store's getResults method if valid.
     */
    handleRequest() {
        const value = isValidJson(this.queryEditor);
        if (value) {
            this.store.getResults(value as JsonObject);
        } else {
            //TODO: handle query error.
        }
    }
}
