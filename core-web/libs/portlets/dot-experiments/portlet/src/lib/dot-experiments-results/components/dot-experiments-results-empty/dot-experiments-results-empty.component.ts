import { Component, input } from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';

import { EMPTY_CHART_BACKGROUND_IMAGE } from '../../../shared/constants';

/**
 * What the Results screen shows in place of a report it cannot draw yet.
 *
 * The silhouette of the real report is drawn behind the message — the chart's own skeleton, then
 * rows where the summary will be — so an Experiment that has simply not collected enough sessions
 * reads as a report on its way rather than as an absence. It also keeps the screen the shape it
 * will have once the data lands, so nothing jumps when it does.
 */
@Component({
    selector: 'dot-experiments-results-empty',
    imports: [SkeletonModule],
    templateUrl: './dot-experiments-results-empty.component.html',
    host: {
        class: 'block w-full'
    }
})
export class DotExperimentsResultsEmptyComponent {
    readonly $title = input.required<string>({ alias: 'title' });
    readonly $subtitle = input.required<string>({ alias: 'subtitle' });

    protected readonly backgroundImage = EMPTY_CHART_BACKGROUND_IMAGE;
    protected readonly ghostRows = [0, 1, 2];
}
