import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA, signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { PublishAuditStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueStatusFilterComponent } from './dot-publishing-queue-status-filter.component';

import { DotPublishingQueueStore } from '../../store/dot-publishing-queue.store';

// Mirrors the actual labels in Language.properties — the filter dedupes by
// translated label, so the test must give it the same overlaps.
const STATUS_LABELS: Record<PublishAuditStatus, string> = {
    [PublishAuditStatus.SCHEDULED]: 'Scheduled',
    [PublishAuditStatus.BUNDLE_REQUESTED]: 'Pending',
    [PublishAuditStatus.WAITING_FOR_PUBLISHING]: 'Waiting',
    [PublishAuditStatus.BUNDLING]: 'Bundling',
    [PublishAuditStatus.SENDING_TO_ENDPOINTS]: 'Sending',
    [PublishAuditStatus.PUBLISHING_BUNDLE]: 'Publishing',
    [PublishAuditStatus.RECEIVED_BUNDLE]: 'Received',
    [PublishAuditStatus.SUCCESS]: 'Success',
    [PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY]: 'Success',
    [PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY]: 'Saved',
    [PublishAuditStatus.SUCCESS_WITH_WARNINGS]: 'Success (warn)',
    [PublishAuditStatus.FAILED_TO_SEND_TO_ALL_GROUPS]: 'Failed (all)',
    [PublishAuditStatus.FAILED_TO_SEND_TO_SOME_GROUPS]: 'Failed (some)',
    [PublishAuditStatus.FAILED_TO_BUNDLE]: 'Build error',
    [PublishAuditStatus.FAILED_TO_SENT]: 'Send error',
    [PublishAuditStatus.FAILED_TO_PUBLISH]: 'Publish error',
    [PublishAuditStatus.FAILED_INTEGRITY_CHECK]: 'Integrity',
    [PublishAuditStatus.INVALID_TOKEN]: 'Auth error',
    [PublishAuditStatus.LICENSE_REQUIRED]: 'No license'
};

describe('DotPublishingQueueStatusFilterComponent', () => {
    let spectator: Spectator<DotPublishingQueueStatusFilterComponent>;

    const statusFilter = signal<PublishAuditStatus[]>([]);

    const createComponent = createComponentFactory({
        component: DotPublishingQueueStatusFilterComponent,
        componentProviders: [
            mockProvider(DotPublishingQueueStore, {
                statusFilter,
                setStatusFilter: jest.fn((codes: PublishAuditStatus[]) => statusFilter.set(codes))
            })
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'publishing-queue.filter.status': 'Status',
                    search: 'Search',
                    ...Object.fromEntries(
                        Object.entries(STATUS_LABELS).map(([code, label]) => [
                            `publishing-queue.status.${code}`,
                            label
                        ])
                    )
                })
            }
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA]
    });

    beforeEach(() => {
        statusFilter.set([]);
        spectator = createComponent();
    });

    describe('source-of-truth invariant', () => {
        it('STATUS_ORDER covers every value of PublishAuditStatus (catches future enum drift)', () => {
            const ordered = new Set<string>(DotPublishingQueueStatusFilterComponent.STATUS_ORDER);
            for (const value of Object.values(PublishAuditStatus)) {
                expect(ordered.has(value)).toBe(true);
            }
        });

        it('exposes SCHEDULED as a filterable status', () => {
            // The bug this fix closed: SCHEDULED is in the enum but used to be
            // missing from the filter, so users couldn't filter for it.
            expect(DotPublishingQueueStatusFilterComponent.STATUS_ORDER).toContain(
                PublishAuditStatus.SCHEDULED
            );
        });
    });

    describe('option list (dedupe by label)', () => {
        it('renders one option per unique translated label', () => {
            const options = (spectator.component as unknown as { $options: { label: string }[] })
                .$options;
            const labels = options.map((o) => o.label);
            // SUCCESS + BUNDLE_SENT_SUCCESSFULLY both render as "Success" → one option
            expect(labels.filter((l) => l === 'Success').length).toBe(1);
            // No duplicates across the full list
            expect(new Set(labels).size).toBe(labels.length);
        });

        it('the "Success" option groups SUCCESS and BUNDLE_SENT_SUCCESSFULLY', () => {
            const options = (
                spectator.component as unknown as {
                    $options: { label: string; codes: PublishAuditStatus[] }[];
                }
            ).$options;
            const success = options.find((o) => o.label === 'Success');
            expect(success).toBeDefined();
            expect([...(success?.codes ?? [])].sort()).toEqual(
                [PublishAuditStatus.SUCCESS, PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY].sort()
            );
        });

        it('Scheduled appears first in the rendered order', () => {
            const options = (spectator.component as unknown as { $options: { label: string }[] })
                .$options;
            expect(options[0].label).toBe('Scheduled');
        });
    });

    describe('selection wiring', () => {
        function getOptions() {
            return (
                spectator.component as unknown as {
                    $options: { value: string; label: string; codes: PublishAuditStatus[] }[];
                }
            ).$options;
        }

        function getSelected(): string[] {
            return (spectator.component as unknown as { $selected: () => string[] }).$selected();
        }

        function setSelected(labels: string[]): void {
            (
                spectator.component as unknown as { $selected: { set: (v: string[]) => void } }
            ).$selected.set(labels);
        }

        function callOnChange(): void {
            (spectator.component as unknown as { onChange: () => void }).onChange();
        }

        it('picking "Success" flattens to SUCCESS + BUNDLE_SENT_SUCCESSFULLY in the store', () => {
            setSelected(['Success']);
            callOnChange();
            const stored = [...statusFilter()].sort();
            expect(stored).toEqual(
                [PublishAuditStatus.SUCCESS, PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY].sort()
            );
        });

        it('shows "Success" selected only when both grouped codes are present in the store filter', () => {
            // Only one of the two grouped codes → option is NOT considered selected.
            statusFilter.set([PublishAuditStatus.SUCCESS]);
            spectator.detectChanges();
            expect(getSelected()).not.toContain('Success');

            // Both codes → option IS considered selected.
            statusFilter.set([
                PublishAuditStatus.SUCCESS,
                PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY
            ]);
            spectator.detectChanges();
            expect(getSelected()).toContain('Success');
        });

        it('clearing all empties the store filter', () => {
            statusFilter.set([
                PublishAuditStatus.SUCCESS,
                PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY
            ]);
            spectator.detectChanges();
            (spectator.component as unknown as { onRemoveAll: () => void }).onRemoveAll();
            expect(statusFilter()).toEqual([]);
        });

        it('exposes a Scheduled option that maps to the SCHEDULED code', () => {
            const scheduled = getOptions().find((o) => o.label === 'Scheduled');
            expect(scheduled).toBeDefined();
            expect(scheduled?.codes).toEqual([PublishAuditStatus.SCHEDULED]);
        });
    });
});
