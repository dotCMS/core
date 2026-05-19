import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output, TemplateRef } from '@angular/core';

import { TimelineModule } from 'primeng/timeline';

/**
 * Shared scrollable wrapper around PrimeNG's `<p-timeline>` used by both the
 * Versions and Push Publish accordions in the History tab. Consumers project
 * a marker template and a content template; the wrapper handles layout,
 * padding, scroll plumbing, and test-ids.
 */
@Component({
    selector: 'dot-history-timeline-list',
    imports: [NgTemplateOutlet, TimelineModule],
    templateUrl: './dot-history-timeline-list.component.html',
    styleUrls: ['./dot-history-timeline-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotHistoryTimelineListComponent<T> {
    /** Items rendered along the timeline. */
    readonly $items = input.required<T[]>({ alias: 'items' });

    /** Template projected for each marker. Receives the item via `$implicit`. */
    readonly markerTemplate = input.required<TemplateRef<{ $implicit: T }>>();

    /** Template projected for each event content. Receives the item via `$implicit`. */
    readonly contentTemplate = input.required<TemplateRef<{ $implicit: T }>>();

    /** Test id applied to the scrollable container. */
    readonly containerTestId = input<string>('timeline-container');

    /** Test id applied to the `<p-timeline>` element. */
    readonly timelineTestId = input<string>('timeline');

    /** Emitted when the container scrolls. */
    readonly scrolled = output<Event>();
}
