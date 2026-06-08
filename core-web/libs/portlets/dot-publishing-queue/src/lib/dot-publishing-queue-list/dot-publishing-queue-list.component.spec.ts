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
        emptyKey: 'publishing-queue.empty.ready'
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
        expect(sendBtn?.disabled).toBe(true);
    });

    it('hides the Send button in progress mode', () => {
        spectator.setInput('mode', 'progress');
        spectator.setInput('rows', [job({ status: PublishAuditStatus.BUNDLING })]);
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-row-send-btn'))).toBeFalsy();
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

    describe('statusSeverity', () => {
        it('returns success for SUCCESS', () => {
            expect(spectator.component.statusSeverity(PublishAuditStatus.SUCCESS)).toBe('success');
        });

        it('returns danger for FAILED_TO_PUBLISH', () => {
            expect(spectator.component.statusSeverity(PublishAuditStatus.FAILED_TO_PUBLISH)).toBe(
                'danger'
            );
        });

        it('returns info for WAITING_FOR_PUBLISHING', () => {
            expect(
                spectator.component.statusSeverity(PublishAuditStatus.WAITING_FOR_PUBLISHING)
            ).toBe('info');
        });

        it('returns warn for in-progress like BUNDLING', () => {
            expect(spectator.component.statusSeverity(PublishAuditStatus.BUNDLING)).toBe('warn');
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
