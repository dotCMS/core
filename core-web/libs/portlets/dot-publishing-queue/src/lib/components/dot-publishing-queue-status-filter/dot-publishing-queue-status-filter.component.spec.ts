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

interface StatusOption {
    value: string;
    label: string;
    codes: readonly PublishAuditStatus[];
}

/** Typed access to the component's protected members. */
interface StatusFilterInternals {
    $options: StatusOption[];
    $selected: { (): string[]; set: (value: string[]) => void };
    onChange: () => void;
    onRemoveAll: () => void;
}

describe('DotPublishingQueueStatusFilterComponent', () => {
    let spectator: Spectator<DotPublishingQueueStatusFilterComponent>;
    let internals: StatusFilterInternals;

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

    function select(values: string[]): void {
        internals.$selected.set(values);
        internals.onChange();
    }

    beforeEach(() => {
        statusFilter.set([]);
        spectator = createComponent();
        internals = spectator.component as unknown as StatusFilterInternals;
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
            expect(internals.$options.map((o) => o.label)).toEqual([
                'Scheduled',
                'Pending',
                'In progress',
                'Success',
                'Success with warnings',
                'Failed',
                'Partially failed'
            ]);
        });

        it('groups BUNDLE_SENT_SUCCESSFULLY with the in-progress statuses, not with Success', () => {
            expect(codesOf('in-progress')).toContain(PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY);
            expect(codesOf('success')).not.toContain(PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY);
        });
    });

    describe('selection wiring', () => {
        it('sends every in-flight status when "In progress" is selected', () => {
            select(['in-progress']);

            expect([...statusFilter()].sort()).toEqual(
                [
                    PublishAuditStatus.BUNDLING,
                    PublishAuditStatus.SENDING_TO_ENDPOINTS,
                    PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY,
                    PublishAuditStatus.WAITING_FOR_PUBLISHING,
                    PublishAuditStatus.PUBLISHING_BUNDLE,
                    PublishAuditStatus.RECEIVED_BUNDLE
                ].sort()
            );
        });

        it('sends the union of statuses when several groups are selected', () => {
            select(['failed', 'partially-failed']);

            expect([...statusFilter()].sort()).toEqual(
                [
                    PublishAuditStatus.FAILED_TO_BUNDLE,
                    PublishAuditStatus.FAILED_TO_SEND_TO_ALL_GROUPS,
                    PublishAuditStatus.FAILED_TO_PUBLISH,
                    PublishAuditStatus.FAILED_TO_SEND_TO_SOME_GROUPS
                ].sort()
            );
        });

        it('marks a group selected only when all of its statuses are in the store filter', () => {
            statusFilter.set([PublishAuditStatus.SUCCESS]);
            spectator.detectChanges();
            expect(internals.$selected()).not.toContain('success');

            statusFilter.set([
                PublishAuditStatus.SUCCESS,
                PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY
            ]);
            spectator.detectChanges();
            expect(internals.$selected()).toEqual(['success']);
        });

        it('empties the store filter when every selection is removed', () => {
            select(['scheduled', 'failed']);

            internals.onRemoveAll();

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
    });
});
