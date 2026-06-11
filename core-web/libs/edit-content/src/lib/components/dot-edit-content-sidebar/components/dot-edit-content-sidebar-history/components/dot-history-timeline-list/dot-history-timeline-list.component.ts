import { NgTemplateOutlet } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    input,
    OnDestroy,
    output,
    TemplateRef,
    viewChild
} from '@angular/core';

import { TimelineModule } from 'primeng/timeline';

/**
 * Shared wrapper around PrimeNG's `<p-timeline>` used by both the Versions and
 * Push Publish sections in the History tab. Consumers project a marker template
 * and a content template; the wrapper handles layout, padding, test-ids, and
 * emits `reachedEnd` when its bottom sentinel scrolls into view so callers can
 * lazy-load the next page.
 */
@Component({
    selector: 'dot-history-timeline-list',
    imports: [NgTemplateOutlet, TimelineModule],
    templateUrl: './dot-history-timeline-list.component.html',
    styleUrls: ['./dot-history-timeline-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotHistoryTimelineListComponent<T> implements AfterViewInit, OnDestroy {
    /** Items rendered along the timeline. */
    readonly $items = input.required<T[]>({ alias: 'items' });

    /** Template projected for each marker. Receives the item via `$implicit`. */
    readonly markerTemplate = input.required<TemplateRef<{ $implicit: T }>>();

    /** Template projected for each event content. Receives the item via `$implicit`. */
    readonly contentTemplate = input.required<TemplateRef<{ $implicit: T }>>();

    /** Test id applied to the container. */
    readonly containerTestId = input<string>('timeline-container');

    /** Test id applied to the `<p-timeline>` element. */
    readonly timelineTestId = input<string>('timeline');

    /** Emitted when the bottom sentinel becomes visible (end of list reached). */
    readonly reachedEnd = output<void>();

    private readonly $sentinel = viewChild<ElementRef<HTMLElement>>('sentinel');

    private observer?: IntersectionObserver;

    ngAfterViewInit(): void {
        const sentinel = this.$sentinel()?.nativeElement;
        if (!sentinel) return;

        this.observer = new IntersectionObserver(
            (entries) => {
                if (entries[0]?.isIntersecting) {
                    this.reachedEnd.emit();
                }
            },
            { threshold: 0 }
        );
        this.observer.observe(sentinel);
    }

    ngOnDestroy(): void {
        this.observer?.disconnect();
    }
}
