import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { CommonModule } from '@angular/common';
import { Component, inject, model } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonDirective } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { SplitterModule } from 'primeng/splitter';
import { TooltipModule } from 'primeng/tooltip';

import { DotAnalyticsSearchService } from '@dotcms/data-access';
import { DotEmptyContainerComponent, DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsSearchStore } from './store/dot-analytics-search.store';
import { ANALYTICS_MONACO_EDITOR_OPTIONS, ANALYTICS_RESULTS_MONACO_EDITOR_OPTIONS } from './utils';

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
        TooltipModule,
        DialogModule
    ],
    providers: [DotAnalyticsSearchStore, DotAnalyticsSearchService],
    templateUrl: './dot-analytics-search.component.html',
    styleUrl: './dot-analytics-search.component.scss'
})
export default class DotAnalyticsSearchComponent {
    ANALYTICS_MONACO_EDITOR_OPTIONS = ANALYTICS_MONACO_EDITOR_OPTIONS;
    ANALYTICS__RESULTS_MONACO_EDITOR_OPTIONS = ANALYTICS_RESULTS_MONACO_EDITOR_OPTIONS;

    readonly store = inject(DotAnalyticsSearchStore);

    /**
     * Boolean model to control the visibility of the dialog.
     */
    $showDialog = model<boolean>(false);

    /**
     * Adds an example query to the store and hides the dialog.
     *
     * @param query - The example query to be added.
     */
    addExampleQuery(query: string): void {
        this.store.setQuery(query);
        this.$showDialog.set(false);
    }
}
