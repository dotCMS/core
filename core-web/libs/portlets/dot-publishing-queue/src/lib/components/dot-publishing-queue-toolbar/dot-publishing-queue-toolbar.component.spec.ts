import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueToolbarComponent } from './dot-publishing-queue-toolbar.component';

import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

describe('DotPublishingQueueToolbarComponent', () => {
    let spectator: Spectator<DotPublishingQueueToolbarComponent>;
    let store: InstanceType<typeof DotPublishingQueueStore>;

    const createComponent = createComponentFactory({
        component: DotPublishingQueueToolbarComponent,
        componentProviders: [
            mockProvider(DotPublishingQueueStore, {
                search: jest.fn().mockReturnValue(''),
                setSearch: jest.fn(),
                refresh: jest.fn()
            })
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'publishing-queue.search.placeholder': 'Search bundles',
                    'publishing-queue.refresh': 'Refresh',
                    'publishing-queue.upload-bundle': 'Upload Bundle',
                    'publishing-queue.upload-bundle.coming-soon': 'Coming soon',
                    'publishing-queue.site-selector.placeholder': 'Site',
                    'publishing-queue.site-selector.coming-soon': 'Coming soon'
                })
            }
        ]
    });

    beforeEach(() => {
        jest.useFakeTimers();
        spectator = createComponent();
        store = spectator.inject(DotPublishingQueueStore, true);
        jest.clearAllMocks();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    describe('layout', () => {
        it('renders search, refresh, upload (disabled), site selector (disabled)', () => {
            expect(spectator.query(byTestId('pq-search-input'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-refresh-btn'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-upload-btn'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-site-selector'))).toBeTruthy();
        });

        it('upload button is disabled', () => {
            const uploadBtn = spectator.query(byTestId('pq-upload-btn'))?.querySelector('button');
            expect(uploadBtn?.disabled).toBe(true);
        });
    });

    describe('search debounce', () => {
        it('calls store.setSearch only after 300ms', () => {
            spectator.component.onSearch('hello');
            jest.advanceTimersByTime(299);
            expect(store.setSearch).not.toHaveBeenCalled();

            jest.advanceTimersByTime(1);
            expect(store.setSearch).toHaveBeenCalledWith('hello');
        });

        it('coalesces rapid typing', () => {
            spectator.component.onSearch('a');
            jest.advanceTimersByTime(100);
            spectator.component.onSearch('ab');
            jest.advanceTimersByTime(100);
            spectator.component.onSearch('abc');
            jest.advanceTimersByTime(300);

            expect(store.setSearch).toHaveBeenCalledTimes(1);
            expect(store.setSearch).toHaveBeenCalledWith('abc');
        });

        it('skips duplicate values (distinctUntilChanged)', () => {
            spectator.component.onSearch('x');
            jest.advanceTimersByTime(300);
            spectator.component.onSearch('x');
            jest.advanceTimersByTime(300);

            expect(store.setSearch).toHaveBeenCalledTimes(1);
        });
    });

    describe('refresh', () => {
        it('clicking the refresh button calls store.refresh', () => {
            const refreshBtn = spectator.query(byTestId('pq-refresh-btn'))?.querySelector('button');
            expect(refreshBtn).toBeTruthy();
            spectator.click(refreshBtn as HTMLButtonElement);
            expect(store.refresh).toHaveBeenCalled();
        });
    });
});
