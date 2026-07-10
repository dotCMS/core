import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { Component, TemplateRef, viewChild } from '@angular/core';

import { DotHistoryTimelineListComponent } from './dot-history-timeline-list.component';

interface TestItem {
    id: string;
    label: string;
}

/**
 * Host wraps the timeline-list with concrete templates and a typed items
 * array so we can validate input wiring, template projection, reached-end
 * emission, and test-id propagation from a real consumer's perspective.
 */
@Component({
    selector: 'dot-test-host',
    imports: [DotHistoryTimelineListComponent],
    template: `
        <ng-template #marker let-item>
            <span [attr.data-testid]="'marker-' + item.id">{{ item.label }}-marker</span>
        </ng-template>
        <ng-template #content let-item>
            <div [attr.data-testid]="'content-' + item.id">{{ item.label }}-content</div>
        </ng-template>
        <dot-history-timeline-list
            [items]="items"
            [markerTemplate]="$markerTpl()!"
            [contentTemplate]="$contentTpl()!"
            [containerTestId]="containerTestId"
            [timelineTestId]="timelineTestId"
            (reachedEnd)="onReachedEnd()" />
    `
})
class TestHostComponent {
    readonly $markerTpl = viewChild<TemplateRef<{ $implicit: TestItem }>>('marker');
    readonly $contentTpl = viewChild<TemplateRef<{ $implicit: TestItem }>>('content');

    items: TestItem[] = [
        { id: '1', label: 'first' },
        { id: '2', label: 'second' }
    ];
    containerTestId = 'host-container';
    timelineTestId = 'host-timeline';

    reachedEndCount = 0;
    onReachedEnd(): void {
        this.reachedEndCount++;
    }
}

describe('DotHistoryTimelineListComponent', () => {
    let spectator: Spectator<TestHostComponent>;
    let intersectionCallback: IntersectionObserverCallback;
    let originalIntersectionObserver: typeof IntersectionObserver;

    const createHost = createComponentFactory({
        component: TestHostComponent,
        detectChanges: false
    });

    beforeEach(() => {
        // jsdom lacks IntersectionObserver — stub it and capture the callback so
        // we can simulate the sentinel intersecting the viewport. Keep a reference
        // to the original so afterEach can restore it and the stub never leaks into
        // other specs sharing this Jest worker.
        originalIntersectionObserver = global.IntersectionObserver;
        global.IntersectionObserver = jest
            .fn()
            .mockImplementation((callback: IntersectionObserverCallback) => {
                intersectionCallback = callback;
                return {
                    observe: jest.fn(),
                    unobserve: jest.fn(),
                    disconnect: jest.fn()
                };
            }) as unknown as typeof IntersectionObserver;

        spectator = createHost();
        spectator.detectChanges();
    });

    afterEach(() => {
        global.IntersectionObserver = originalIntersectionObserver;
    });

    describe('test ids', () => {
        it('should apply containerTestId on the wrapper', () => {
            expect(spectator.query(byTestId('host-container'))).toBeTruthy();
        });

        it('should apply timelineTestId on the p-timeline element', () => {
            expect(spectator.query(byTestId('host-timeline'))).toBeTruthy();
        });

        it('should render the load-more sentinel', () => {
            expect(spectator.query(byTestId('timeline-sentinel'))).toBeTruthy();
        });
    });

    describe('template projection', () => {
        it('should render the marker template for every item', () => {
            const first = spectator.query(byTestId('marker-1'));
            const second = spectator.query(byTestId('marker-2'));
            expect(first?.textContent?.trim()).toBe('first-marker');
            expect(second?.textContent?.trim()).toBe('second-marker');
        });

        it('should render the content template for every item', () => {
            const first = spectator.query(byTestId('content-1'));
            const second = spectator.query(byTestId('content-2'));
            expect(first?.textContent?.trim()).toBe('first-content');
            expect(second?.textContent?.trim()).toBe('second-content');
        });

        it('should re-render when items input changes', () => {
            spectator.component.items = [{ id: '3', label: 'third' }];
            spectator.detectChanges();
            expect(spectator.query(byTestId('content-3'))?.textContent?.trim()).toBe(
                'third-content'
            );
            expect(spectator.query(byTestId('content-1'))).toBeNull();
            expect(spectator.query(byTestId('content-2'))).toBeNull();
        });
    });

    describe('reachedEnd output', () => {
        it('should ignore the initial observer callback so a short list does not auto-load', () => {
            // The first callback after observe() is the initial observation and
            // must be skipped even if the sentinel is already intersecting.
            intersectionCallback(
                [{ isIntersecting: true } as IntersectionObserverEntry],
                {} as IntersectionObserver
            );
            expect(spectator.component.reachedEndCount).toBe(0);
        });

        it('should emit reachedEnd when the sentinel intersects after the initial observation', () => {
            // Initial observation (skipped)
            intersectionCallback(
                [{ isIntersecting: false } as IntersectionObserverEntry],
                {} as IntersectionObserver
            );
            // Real intersection triggered by a user scroll
            intersectionCallback(
                [{ isIntersecting: true } as IntersectionObserverEntry],
                {} as IntersectionObserver
            );
            expect(spectator.component.reachedEndCount).toBe(1);
        });

        it('should not emit reachedEnd when the sentinel is not intersecting', () => {
            // Initial observation (skipped)
            intersectionCallback(
                [{ isIntersecting: false } as IntersectionObserverEntry],
                {} as IntersectionObserver
            );
            intersectionCallback(
                [{ isIntersecting: false } as IntersectionObserverEntry],
                {} as IntersectionObserver
            );
            expect(spectator.component.reachedEndCount).toBe(0);
        });
    });
});
