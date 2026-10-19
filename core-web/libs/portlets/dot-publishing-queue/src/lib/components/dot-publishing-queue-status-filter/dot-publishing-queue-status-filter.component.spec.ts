import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { CUSTOM_ELEMENTS_SCHEMA, signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { ENDPOINT_ONLY_STATUSES, PublishAuditStatus } from '@dotcms/dotcms-models';
import { DotChipFilterComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueStatusFilterComponent } from './dot-publishing-queue-status-filter.component';

import { DotPublishingQueueStore } from '../../store/dot-publishing-queue.store';

const GROUP_LABELS: Record<string, string> = {
    'publishing-queue.filter.status.scheduled': 'Scheduled',
    'publishing-queue.filter.status.pending': 'Pending',
    'publishing-queue.filter.status.in-progress': 'In progress',
    'publishing-queue.filter.status.success': 'Success',
    'publishing-queue.filter.status.success-with-warnings': 'Success with warnings',
    'publishing-queue.filter.status.failed': 'Failed',
    'publishing-queue.filter.status.partially-failed': 'Partially failed'
};

/** What each option must send to the API, written out so the test pins the mapping itself. */
const EXPECTED_CODES: Array<[string, PublishAuditStatus[]]> = [
    ['scheduled', [PublishAuditStatus.SCHEDULED]],
    ['pending', [PublishAuditStatus.BUNDLE_REQUESTED]],
    [
        'in-progress',
        [
            PublishAuditStatus.BUNDLING,
            PublishAuditStatus.SENDING_TO_ENDPOINTS,
            PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY,
            PublishAuditStatus.WAITING_FOR_PUBLISHING,
            PublishAuditStatus.PUBLISHING_BUNDLE,
            PublishAuditStatus.RECEIVED_BUNDLE
        ]
    ],
    ['success', [PublishAuditStatus.SUCCESS, PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY]],
    ['success-with-warnings', [PublishAuditStatus.SUCCESS_WITH_WARNINGS]],
    [
        'failed',
        [
            PublishAuditStatus.FAILED_TO_BUNDLE,
            PublishAuditStatus.FAILED_TO_SEND_TO_ALL_GROUPS,
            PublishAuditStatus.FAILED_TO_PUBLISH
        ]
    ],
    ['partially-failed', [PublishAuditStatus.FAILED_TO_SEND_TO_SOME_GROUPS]]
];

describe('DotPublishingQueueStatusFilterComponent', () => {
    let spectator: Spectator<DotPublishingQueueStatusFilterComponent>;

    const statusFilter = signal<PublishAuditStatus[]>([]);
    const groups = DotPublishingQueueStatusFilterComponent.STATUS_FILTER_GROUPS;

    const createComponent = createComponentFactory({
        component: DotPublishingQueueStatusFilterComponent,
        componentProviders: [
            mockProvider(DotPublishingQueueStore, {
                statusFilter,
                setStatusFilter: vi.fn((codes: PublishAuditStatus[]) => statusFilter.set(codes))
            })
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'publishing-queue.filter.status': 'Status',
                    search: 'Search',
                    ...GROUP_LABELS
                })
            }
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA]
    });

    function codesOf(value: string): PublishAuditStatus[] {
        return [...(groups.find((g) => g.value === value)?.codes ?? [])];
    }

    function openPanel(): void {
        spectator.triggerEventHandler(DotChipFilterComponent, 'clicked', new Event('click'));
        spectator.detectChanges();
    }

    /** Waits for `ngModel` to write the new selection back into the listbox, as the browser does between clicks. */
    async function clickOption(value: string): Promise<void> {
        spectator.click(byTestId(`pq-status-filter-item-${value}`));
        spectator.detectChanges();
        await spectator.fixture.whenStable();
    }

    function renderedOptionLabels(): string[] {
        return spectator
            .queryAll('[data-testid^="pq-status-filter-item-"]')
            .map((el) => el.textContent?.trim() ?? '');
    }

    beforeEach(() => {
        statusFilter.set([]);
        spectator = createComponent();
    });

    describe('source-of-truth invariant', () => {
        it('places every bundle-level status in exactly one group (catches future enum drift)', () => {
            const endpointOnly = new Set<PublishAuditStatus>(ENDPOINT_ONLY_STATUSES);
            const bundleLevel = Object.values(PublishAuditStatus).filter(
                (status) => !endpointOnly.has(status)
            );

            for (const status of bundleLevel) {
                const owners = groups.filter((g) => g.codes.includes(status));
                expect(owners.length, `${status} must belong to exactly one group`).toBe(1);
            }
        });

        it('keeps endpoint-only statuses out of every group', () => {
            const grouped = groups.flatMap((g) => [...g.codes]);

            for (const status of ENDPOINT_ONLY_STATUSES) {
                expect(grouped).not.toContain(status);
            }
        });
    });

    describe('options', () => {
        it('renders the 7 groups in lifecycle order with translated labels', () => {
            openPanel();

            expect(renderedOptionLabels()).toEqual([
                'Scheduled',
                'Pending',
                'In progress',
                'Success',
                'Success with warnings',
                'Failed',
                'Partially failed'
            ]);
        });
    });

    describe('selection', () => {
        it.each(EXPECTED_CODES)(
            'sends exactly its statuses when "%s" is picked',
            async (value, codes) => {
                openPanel();

                await clickOption(value);

                expect([...statusFilter()].sort()).toEqual([...codes].sort());
            }
        );

        it('sends the union of statuses when several options are picked', async () => {
            openPanel();

            await clickOption('failed');
            await clickOption('partially-failed');

            expect([...statusFilter()].sort()).toEqual(
                [
                    PublishAuditStatus.FAILED_TO_BUNDLE,
                    PublishAuditStatus.FAILED_TO_SEND_TO_ALL_GROUPS,
                    PublishAuditStatus.FAILED_TO_PUBLISH,
                    PublishAuditStatus.FAILED_TO_SEND_TO_SOME_GROUPS
                ].sort()
            );
        });

        it("drops an option's statuses when it is picked again", async () => {
            openPanel();
            await clickOption('scheduled');
            await clickOption('failed');

            await clickOption('scheduled');

            expect([...statusFilter()].sort()).toEqual(codesOf('failed').sort());
        });

        it('empties the store filter when the chip is cleared', async () => {
            openPanel();
            await clickOption('scheduled');
            await clickOption('failed');

            spectator.click(byTestId('chip-remove'));

            expect(statusFilter()).toEqual([]);
        });
    });

    describe('chip', () => {
        it('shows the translated labels of the selected groups', () => {
            statusFilter.set([...codesOf('scheduled'), ...codesOf('partially-failed')]);
            spectator.detectChanges();

            expect(spectator.query(byTestId('chip-values'))).toHaveText(
                'Scheduled, Partially failed'
            );
        });

        it('shows a group only when all of its statuses are in the store filter', () => {
            statusFilter.set([PublishAuditStatus.SUCCESS]);
            spectator.detectChanges();
            expect(spectator.query(byTestId('chip-values'))).not.toExist();

            statusFilter.set([
                PublishAuditStatus.SUCCESS,
                PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY
            ]);
            spectator.detectChanges();
            expect(spectator.query(byTestId('chip-values'))).toHaveText('Success');
        });
    });
});
