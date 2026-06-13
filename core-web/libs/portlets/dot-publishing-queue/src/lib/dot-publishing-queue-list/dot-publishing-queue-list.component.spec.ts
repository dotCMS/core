import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { PublishAuditStatus, PublishingJobView } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueListComponent } from './dot-publishing-queue-list.component';

const job = (overrides: Partial<PublishingJobView> = {}): PublishingJobView => ({
    bundleId: 'bundle-1',
    bundleName: 'Bundle One',
    status: PublishAuditStatus.WAITING_FOR_PUBLISHING,
    filterName: null,
    filterKey: null,
    assetCount: 3,
    assetPreview: [],
    environmentCount: 2,
    createDate: '2026-06-08T10:00:00Z',
    statusUpdated: null,
    numTries: 0,
    ...overrides
});

describe('DotPublishingQueueListComponent', () => {
    let spectator: Spectator<DotPublishingQueueListComponent>;

    const createComponent = createComponentFactory({
        component: DotPublishingQueueListComponent,
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService({}) }],
        detectChanges: false
    });

    const defaultInputs = {
        mode: 'ready' as const,
        rows: [job()],
        status: 'loaded' as const,
        total: 1,
        page: 1,
        rowsPerPage: 10,
        headerKey: 'publishing-queue.ready.title',
        emptyConfig: {
            icon: 'pi-folder-open',
            title: "Your bundle's empty",
            subtitle: 'Add content here'
        }
    };

    beforeEach(() => {
        spectator = createComponent({ props: defaultInputs });
        spectator.detectChanges();
    });

    it('renders the header with the count', () => {
        const header = spectator.query(byTestId('pq-list-header'));
        expect(header?.textContent).toContain('(1)');
    });

    it('renders a row per job', () => {
        const rows = spectator.queryAll(byTestId('pq-list-row'));
        expect(rows.length).toBe(1);
    });

    it('renders the Send button in ready mode (disabled)', () => {
        const sendBtn = spectator.query(byTestId('pq-row-send-btn'))?.querySelector('button');
        expect(sendBtn).toBeTruthy();
        expect(sendBtn?.disabled).toBe(false);
        expect(spectator.query(byTestId('pq-row-kebab-btn'))).toBeTruthy();
    });

    it('emits sendClick when Send is clicked', () => {
        let emitted: PublishingJobView | undefined;
        spectator.output('sendClick').subscribe((j) => (emitted = j as PublishingJobView));
        const sendBtn = spectator.query(byTestId('pq-row-send-btn'))?.querySelector('button');
        spectator.click(sendBtn as HTMLButtonElement);
        expect(emitted?.bundleId).toBe('bundle-1');
    });

    it('hides Send + kebab in progress mode', () => {
        spectator.setInput('mode', 'progress');
        spectator.setInput('rows', [job({ status: PublishAuditStatus.BUNDLING })]);
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-row-send-btn'))).toBeFalsy();
        expect(spectator.query(byTestId('pq-row-kebab-btn'))).toBeFalsy();
    });

    it('shows Retry button on failed progress rows + emits retryClick', () => {
        spectator.setInput('mode', 'progress');
        spectator.setInput('rows', [job({ status: PublishAuditStatus.FAILED_TO_PUBLISH })]);
        spectator.detectChanges();
        const retry = spectator.query(byTestId('pq-row-retry-btn'));
        expect(retry).toBeTruthy();

        let emitted: PublishingJobView | undefined;
        spectator.output('retryClick').subscribe((j) => (emitted = j as PublishingJobView));
        spectator.click(retry?.querySelector('button') as HTMLButtonElement);
        expect(emitted?.bundleId).toBe('bundle-1');
    });

    it('shows skeletons while loading and no rows yet', () => {
        spectator.setInput('rows', []);
        spectator.setInput('status', 'loading');
        spectator.detectChanges();
        expect(spectator.queryAll(byTestId('pq-list-skeleton')).length).toBeGreaterThan(0);
    });

    it('shows empty state when not loading and zero rows', () => {
        spectator.setInput('rows', []);
        spectator.setInput('status', 'loaded');
        spectator.setInput('total', 0);
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-list-empty'))).toBeTruthy();
    });

    it('emits rowClick on row click', () => {
        let emitted: PublishingJobView | undefined;
        spectator.output('rowClick').subscribe((j) => (emitted = j as PublishingJobView));

        const row = spectator.query(byTestId('pq-list-row'));
        spectator.click(row as HTMLElement);

        expect(emitted?.bundleId).toBe('bundle-1');
    });

    it('emits rowClick on Enter keydown', () => {
        let emitted: PublishingJobView | undefined;
        spectator.output('rowClick').subscribe((j) => (emitted = j as PublishingJobView));

        const row = spectator.query(byTestId('pq-list-row'));
        spectator.dispatchKeyboardEvent(row as HTMLElement, 'keydown', 'Enter');

        expect(emitted?.bundleId).toBe('bundle-1');
    });

    describe('status chip', () => {
        it('renders the chip when row.status is set', () => {
            spectator.setInput('mode', 'progress');
            spectator.setInput('rows', [job({ status: PublishAuditStatus.BUNDLING })]);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-row-status'))).toBeTruthy();
        });

        it('skips the chip when row.status is null (drafts)', () => {
            spectator.setInput('rows', [job({ status: null })]);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-row-status'))).toBeFalsy();
        });
    });

    describe('pagination', () => {
        it('shows paginator only when total exceeds page size', () => {
            spectator.setInput('total', 5);
            spectator.setInput('rowsPerPage', 10);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-list-paginator'))).toBeFalsy();

            spectator.setInput('total', 30);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-list-paginator'))).toBeTruthy();
        });

        it('emits pageChange with 1-based page when paginator fires', () => {
            spectator.setInput('total', 100);
            spectator.detectChanges();
            let emitted = 0;
            spectator.output('pageChange').subscribe((p) => (emitted = p as number));

            spectator.component.onPaginate({ first: 20, rows: 10, page: 2, pageCount: 10 });

            expect(emitted).toBe(3);
        });
    });
});
