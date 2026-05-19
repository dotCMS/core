import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Component, TemplateRef, viewChild } from '@angular/core';

import { DotHistoryTimelineListComponent } from './dot-history-timeline-list.component';

interface TestItem {
    id: string;
    label: string;
}

/**
 * Host wraps the timeline-list with concrete templates and a typed items
 * array so we can validate input wiring, template projection, scroll
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
            (scrolled)="onScrolled($event)" />
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

    lastScrollEvent: Event | null = null;
    onScrolled(event: Event): void {
        this.lastScrollEvent = event;
    }
}

describe('DotHistoryTimelineListComponent', () => {
    let spectator: Spectator<TestHostComponent>;

    const createHost = createComponentFactory({
        component: TestHostComponent,
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createHost();
        spectator.detectChanges();
    });

    describe('test ids', () => {
        it('should apply containerTestId on the scrollable wrapper', () => {
            expect(spectator.query(byTestId('host-container'))).toBeTruthy();
        });

        it('should apply timelineTestId on the p-timeline element', () => {
            expect(spectator.query(byTestId('host-timeline'))).toBeTruthy();
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

    describe('scroll output', () => {
        it('should emit scrolled when the container scrolls', () => {
            const container = spectator.query(byTestId('host-container'));
            expect(container).toBeTruthy();
            spectator.dispatchFakeEvent(container as Element, 'scroll');
            expect(spectator.component.lastScrollEvent).toBeInstanceOf(Event);
            expect(spectator.component.lastScrollEvent?.type).toBe('scroll');
        });
    });
});
