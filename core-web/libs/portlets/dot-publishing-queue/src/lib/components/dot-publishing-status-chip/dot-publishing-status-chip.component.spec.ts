import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { PublishAuditStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotPublishingStatusChipComponent,
    publishingStatusBucket
} from './dot-publishing-status-chip.component';

describe('publishingStatusBucket (pure fn)', () => {
    const cases: Array<[PublishAuditStatus, 'success' | 'danger' | 'warn' | 'info']> = [
        [PublishAuditStatus.SUCCESS, 'success'],
        [PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY, 'success'],
        [PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY, 'success'],
        [PublishAuditStatus.SUCCESS_WITH_WARNINGS, 'warn'],
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
        [PublishAuditStatus.BUNDLING, 'warn'],
        [PublishAuditStatus.SENDING_TO_ENDPOINTS, 'warn'],
        [PublishAuditStatus.PUBLISHING_BUNDLE, 'warn'],
        [PublishAuditStatus.RECEIVED_BUNDLE, 'warn']
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
                    'publishing-queue.status.SUCCESS': 'Success',
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

    it('exposes success severity for SUCCESS status', () => {
        spectator = createComponent({ props: { status: PublishAuditStatus.SUCCESS } });
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-status-chip'))?.textContent?.trim()).toContain(
            'Success'
        );
        expect(spectator.component.$bucket()).toBe('success');
    });

    it('exposes danger severity for FAILED_TO_PUBLISH status', () => {
        spectator = createComponent({
            props: { status: PublishAuditStatus.FAILED_TO_PUBLISH }
        });
        spectator.detectChanges();
        expect(spectator.component.$bucket()).toBe('danger');
    });

    it('exposes warn severity for BUNDLING status (in-flight)', () => {
        spectator = createComponent({ props: { status: PublishAuditStatus.BUNDLING } });
        spectator.detectChanges();
        expect(spectator.component.$bucket()).toBe('warn');
    });

    it('only ever emits severities PrimeNG renders — an unknown one falls back to the solid primary fill', () => {
        // `p-tag` derives `p-tag-{severity}`; a value outside this set produces no
        // class at all and the tag renders like a primary button.
        const valid = new Set(['success', 'secondary', 'info', 'warn', 'danger', 'contrast']);
        for (const status of Object.values(PublishAuditStatus)) {
            expect(valid).toContain(publishingStatusBucket(status as PublishAuditStatus));
        }
    });

    it('exposes info severity for WAITING_FOR_PUBLISHING status', () => {
        spectator = createComponent({
            props: { status: PublishAuditStatus.WAITING_FOR_PUBLISHING }
        });
        spectator.detectChanges();
        expect(spectator.component.$bucket()).toBe('info');
    });
});
