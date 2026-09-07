import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { Component, TemplateRef, signal, viewChild } from '@angular/core';

import { DotCMSContentletVersion } from '@dotcms/dotcms-models';

import { DotHistoryTimelineListComponent } from './dot-history-timeline-list.component';

import { DotPushPublishHistoryItem } from '../../../../../../models/dot-edit-content.model';

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
            [items]="$items()"
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

    readonly $items = signal<TestItem[]>([
        { id: '1', label: 'first' },
        { id: '2', label: 'second' }
    ]);
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

    describe('timeline key', () => {
        it('should key version items by inode', () => {
            const timelineList = spectator.query(DotHistoryTimelineListComponent);
            expect(timelineList!.$timelineKey()).toBe('1|2');
        });

        it('should change the key when swapping equally-sized version lists', () => {
            const timelineList = spectator.query(DotHistoryTimelineListComponent);
            const initialKey = timelineList!.$timelineKey();

            spectator.component.$items.set([
                { id: '3', label: 'third' },
                { id: '4', label: 'fourth' }
            ]);
            spectator.detectChanges();

            expect(timelineList!.$timelineKey()).not.toBe(initialKey);
            expect(timelineList!.$timelineKey()).toBe('3|4');
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
            spectator.component.$items.set([{ id: '3', label: 'third' }]);
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

const mockVersionA: DotCMSContentletVersion = {
    archived: false,
    country: 'US',
    countryCode: 'US',
    experimentVariant: false,
    inode: 'inode-a',
    isoCode: 'en-US',
    language: 'English',
    languageCode: 'en',
    languageFlag: 'us',
    languageId: 1,
    live: true,
    modDate: 1701428400000,
    modUser: 'dotcms.org.1',
    modUserName: 'Admin User',
    title: 'Version A',
    working: true
};

const mockVersionB: DotCMSContentletVersion = {
    ...mockVersionA,
    inode: 'inode-b',
    title: 'Version B',
    working: false
};

const mockVersionC: DotCMSContentletVersion = {
    ...mockVersionA,
    inode: 'inode-c',
    title: 'Version C',
    working: false
};

const mockVersionD: DotCMSContentletVersion = {
    ...mockVersionA,
    inode: 'inode-d',
    title: 'Version D',
    working: false
};

@Component({
    selector: 'dot-version-test-host',
    imports: [DotHistoryTimelineListComponent],
    template: `
        <ng-template #marker let-item>
            <span [attr.data-testid]="'marker-' + item.inode">{{ item.title }}-marker</span>
        </ng-template>
        <ng-template #content let-item>
            <div [attr.data-testid]="'content-' + item.inode">{{ item.title }}-content</div>
        </ng-template>
        <dot-history-timeline-list
            [items]="$items()"
            [markerTemplate]="$markerTpl()!"
            [contentTemplate]="$contentTpl()!" />
    `
})
class VersionTestHostComponent {
    readonly $markerTpl = viewChild<TemplateRef<{ $implicit: DotCMSContentletVersion }>>('marker');
    readonly $contentTpl =
        viewChild<TemplateRef<{ $implicit: DotCMSContentletVersion }>>('content');

    readonly $items = signal<DotCMSContentletVersion[]>([mockVersionA, mockVersionB]);
}

describe('DotHistoryTimelineListComponent with version items', () => {
    let spectator: Spectator<VersionTestHostComponent>;

    const createHost = createComponentFactory({
        component: VersionTestHostComponent,
        detectChanges: false
    });

    beforeEach(() => {
        global.IntersectionObserver = jest.fn().mockImplementation(() => ({
            observe: jest.fn(),
            unobserve: jest.fn(),
            disconnect: jest.fn()
        })) as unknown as typeof IntersectionObserver;

        spectator = createHost();
        spectator.detectChanges();
    });

    it('should key items by inode', () => {
        const timelineList = spectator.query(DotHistoryTimelineListComponent);
        expect(timelineList!.$timelineKey()).toBe('inode-a|inode-b');
    });

    it('should re-render when swapping equally-sized version lists', () => {
        spectator.component.$items.set([mockVersionC, mockVersionD]);
        spectator.detectChanges();

        expect(spectator.query(byTestId('content-inode-c'))?.textContent?.trim()).toBe(
            'Version C-content'
        );
        expect(spectator.query(byTestId('content-inode-a'))).toBeNull();
        expect(spectator.query(byTestId('content-inode-b'))).toBeNull();
    });
});

const mockPushPublishA: DotPushPublishHistoryItem = {
    bundleId: 'bundle-a',
    environment: 'Production',
    pushDate: 1701428400000,
    pushedBy: 'admin'
};

const mockPushPublishB: DotPushPublishHistoryItem = {
    bundleId: 'bundle-b',
    environment: 'Staging',
    pushDate: 1701514800000,
    pushedBy: 'admin'
};

const mockPushPublishC: DotPushPublishHistoryItem = {
    bundleId: 'bundle-c',
    environment: 'Production',
    pushDate: 1701601200000,
    pushedBy: 'editor'
};

const mockPushPublishD: DotPushPublishHistoryItem = {
    bundleId: 'bundle-d',
    environment: 'Staging',
    pushDate: 1701687600000,
    pushedBy: 'editor'
};

@Component({
    selector: 'dot-pushpublish-test-host',
    imports: [DotHistoryTimelineListComponent],
    template: `
        <ng-template #marker let-item>
            <span [attr.data-testid]="'marker-' + item.bundleId">
                {{ item.environment }}-marker
            </span>
        </ng-template>
        <ng-template #content let-item>
            <div [attr.data-testid]="'content-' + item.bundleId">
                {{ item.environment }}-content
            </div>
        </ng-template>
        <dot-history-timeline-list
            [items]="$items()"
            [markerTemplate]="$markerTpl()!"
            [contentTemplate]="$contentTpl()!" />
    `
})
class PushPublishTestHostComponent {
    readonly $markerTpl =
        viewChild<TemplateRef<{ $implicit: DotPushPublishHistoryItem }>>('marker');
    readonly $contentTpl =
        viewChild<TemplateRef<{ $implicit: DotPushPublishHistoryItem }>>('content');

    readonly $items = signal<DotPushPublishHistoryItem[]>([mockPushPublishA, mockPushPublishB]);
}

describe('DotHistoryTimelineListComponent with push publish items', () => {
    let spectator: Spectator<PushPublishTestHostComponent>;

    const createHost = createComponentFactory({
        component: PushPublishTestHostComponent,
        detectChanges: false
    });

    beforeEach(() => {
        global.IntersectionObserver = jest.fn().mockImplementation(() => ({
            observe: jest.fn(),
            unobserve: jest.fn(),
            disconnect: jest.fn()
        })) as unknown as typeof IntersectionObserver;

        spectator = createHost();
        spectator.detectChanges();
    });

    it('should key items by bundleId', () => {
        const timelineList = spectator.query(DotHistoryTimelineListComponent);
        expect(timelineList!.$timelineKey()).toBe('bundle-a|bundle-b');
    });

    it('should re-render when swapping equally-sized push publish lists', () => {
        spectator.component.$items.set([mockPushPublishC, mockPushPublishD]);
        spectator.detectChanges();

        expect(spectator.query(byTestId('content-bundle-c'))?.textContent?.trim()).toBe(
            'Production-content'
        );
        expect(spectator.query(byTestId('content-bundle-a'))).toBeNull();
        expect(spectator.query(byTestId('content-bundle-b'))).toBeNull();
    });
});
