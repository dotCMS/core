import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { PublishAuditStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotPublishingStatusChipComponent,
    publishingStatusBucket
} from './dot-publishing-status-chip.component';

describe('publishingStatusBucket (pure fn)', () => {
    const cases: Array<[PublishAuditStatus, 'success' | 'danger' | 'warning' | 'info']> = [
        [PublishAuditStatus.SUCCESS, 'success'],
        [PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY, 'success'],
        [PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY, 'success'],
        [PublishAuditStatus.SUCCESS_WITH_WARNINGS, 'warning'],
        [PublishAuditStatus.FAILED_TO_SEND_TO_ALL_GROUPS, 'danger'],
        [PublishAuditStatus.FAILED_TO_SEND_TO_SOME_GROUPS, 'danger'],
        [PublishAuditStatus.FAILED_TO_BUNDLE, 'danger'],
        [PublishAuditStatus.FAILED_TO_SENT, 'danger'],
        [PublishAuditStatus.FAILED_TO_PUBLISH, 'danger'],
        [PublishAuditStatus.FAILED_INTEGRITY_CHECK, 'danger'],
        [PublishAuditStatus.INVALID_TOKEN, 'danger'],
        [PublishAuditStatus.LICENSE_REQUIRED, 'danger'],
        [PublishAuditStatus.WAITING_FOR_PUBLISHING, 'info'],
        [PublishAuditStatus.BUNDLE_REQUESTED, 'info'],
        [PublishAuditStatus.SCHEDULED, 'info'],
        [PublishAuditStatus.BUNDLING, 'warning'],
        [PublishAuditStatus.SENDING_TO_ENDPOINTS, 'warning'],
        [PublishAuditStatus.PUBLISHING_BUNDLE, 'warning'],
        [PublishAuditStatus.RECEIVED_BUNDLE, 'warning']
    ];

    it('covers every value of PublishAuditStatus', () => {
        const allValues = Object.values(PublishAuditStatus);
        const mapped = new Set(cases.map(([s]) => s));
        for (const v of allValues) {
            expect(mapped.has(v as PublishAuditStatus)).toBe(true);
        }
    });

    it.each(cases)('maps %s → %s', (status, bucket) => {
        expect(publishingStatusBucket(status)).toBe(bucket);
    });
});

describe('DotPublishingStatusChipComponent', () => {
    let spectator: Spectator<DotPublishingStatusChipComponent>;

    const createComponent = createComponentFactory({
        component: DotPublishingStatusChipComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'publishing-queue.status.SUCCESS': 'Sent',
                    'publishing-queue.status.FAILED_TO_PUBLISH': 'Publish error',
                    'publishing-queue.status.BUNDLING': 'Bundling',
                    'publishing-queue.status.WAITING_FOR_PUBLISHING': 'Waiting'
                })
            }
        ],
        detectChanges: false
    });

    it('renders nothing when status is null', () => {
        spectator = createComponent({ props: { status: null } });
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-status-chip'))).toBeFalsy();
    });

    it('renders green classes for success bucket', () => {
        spectator = createComponent({ props: { status: PublishAuditStatus.SUCCESS } });
        spectator.detectChanges();
        const chip = spectator.query(byTestId('pq-status-chip'));
        expect(chip?.classList.contains('bg-green-100!')).toBe(true);
        expect(chip?.classList.contains('text-green-700!')).toBe(true);
        expect(chip?.textContent?.trim()).toContain('Sent');
    });

    it('renders red classes for danger bucket', () => {
        spectator = createComponent({
            props: { status: PublishAuditStatus.FAILED_TO_PUBLISH }
        });
        spectator.detectChanges();
        const chip = spectator.query(byTestId('pq-status-chip'));
        expect(chip?.classList.contains('bg-red-100!')).toBe(true);
    });

    it('renders yellow classes for warning bucket (in-flight)', () => {
        spectator = createComponent({ props: { status: PublishAuditStatus.BUNDLING } });
        spectator.detectChanges();
        const chip = spectator.query(byTestId('pq-status-chip'));
        expect(chip?.classList.contains('bg-yellow-100!')).toBe(true);
    });

    it('renders blue classes for info bucket (waiting)', () => {
        spectator = createComponent({
            props: { status: PublishAuditStatus.WAITING_FOR_PUBLISHING }
        });
        spectator.detectChanges();
        const chip = spectator.query(byTestId('pq-status-chip'));
        expect(chip?.classList.contains('bg-blue-100!')).toBe(true);
    });
});
