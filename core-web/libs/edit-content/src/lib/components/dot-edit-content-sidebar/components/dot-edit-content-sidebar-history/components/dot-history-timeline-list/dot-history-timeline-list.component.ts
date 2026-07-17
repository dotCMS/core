import { NgTemplateOutlet } from '@angular/common';
import {
    afterNextRender,
    ChangeDetectionStrategy,
    Component,
    computed,
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
 * emits `reachedEnd` when its bottom sentinel enters the viewport so callers can
 * load the next page. The initial IntersectionObserver callback is ignored, so a
 * list shorter than the viewport does not auto-load before the user scrolls.
 */
@Component({
    selector: 'dot-history-timeline-list',
    imports: [NgTemplateOutlet, TimelineModule],
    templateUrl: './dot-history-timeline-list.component.html',
    styleUrls: ['./dot-history-timeline-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotHistoryTimelineListComponent<T> implements OnDestroy {
    /** Items rendered along the timeline. */
    readonly $items = input.required<T[]>({ alias: 'items' });

    /** Spread copy so PrimeNG Timeline re-renders when items change (OnPush + ngFor). */
    readonly $timelineValue = computed(() => [...this.$items()]);

    /** Forces p-timeline recreation when the item set changes. */
    readonly $timelineKey = computed(() =>
        this.$items()
            .map((item) => String((item as { id?: string }).id ?? item))
            .join('|')
    );

    /** Template projected for each marker. Receives the item via `$implicit`. */
    readonly markerTemplate = input.required<TemplateRef<{ $implicit: T }>>();

    /** Template projected for each event content. Receives the item via `$implicit`. */
    readonly contentTemplate = input.required<TemplateRef<{ $implicit: T }>>();

    /** Test id applied to the container. */
    readonly containerTestId = input<string>('timeline-container');

    /** Test id applied to the `<p-timeline>` element. */
    readonly timelineTestId = input<string>('timeline');

    /** Emitted when the bottom sentinel enters the viewport (end of list reached). */
    readonly reachedEnd = output<void>();

    // NOTE: viewChild (and all Angular signal queries) cannot be declared on an
    // ES-private (#) member — the Angular compiler rejects it (NG1053). So the
    // internal state here stays on TypeScript `private` for consistency.
    private readonly $sentinel = viewChild<ElementRef<HTMLElement>>('sentinel');

    private observer?: IntersectionObserver;

    /**
     * IntersectionObserver delivers an initial callback right after `observe()`.
     * We skip it so a list shorter than the viewport (sentinel already on screen)
     * does not emit `reachedEnd` and auto-load the next page before any scroll.
     */
    private initialObservation = true;

    constructor() {
        // afterNextRender runs once after the first render, browser-only — the
        // sentinel ViewChild is resolved by then and SSR never touches the DOM API.
        afterNextRender(() => {
            const sentinel = this.$sentinel()?.nativeElement;
            if (!sentinel) return;

            this.observer = new IntersectionObserver(
                (entries) => {
                    if (this.initialObservation) {
                        this.initialObservation = false;
                        return;
                    }

                    if (entries[0]?.isIntersecting) {
                        this.reachedEnd.emit();
                    }
                },
                { threshold: 0 }
            );
            this.observer.observe(sentinel);
        });
    }

    ngOnDestroy(): void {
        this.observer?.disconnect();
    }
}
