import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { DotMessageService, DotPublishingQueueService } from '@dotcms/data-access';
import {
    EndpointDetailView,
    PublishAuditStatus,
    PublishingJobDetailView
} from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueBundleDetailsDialogComponent } from './dot-publishing-queue-bundle-details-dialog.component';

import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

const detailFixture = (
    overrides: Partial<PublishingJobDetailView> = {}
): PublishingJobDetailView => ({
    bundleId: 'b-1',
    bundleName: 'My Bundle',
    status: PublishAuditStatus.SUCCESS,
    filterName: 'Default',
    filterKey: 'default.yml',
    assetCount: 2,
    environments: [
        {
            id: 'env-1',
            name: 'Prod',
            endpoints: [
                {
                    id: 'ep-1',
                    serverName: 'srv1',
                    address: '127.0.0.1',
                    port: '443',
                    protocol: 'https',
                    status: PublishAuditStatus.SUCCESS,
                    statusMessage: 'ok',
                    stackTrace: null
                }
            ]
        }
    ],
    timestamps: {
        bundleStart: '2026-06-08T10:00:00Z',
        bundleEnd: '2026-06-08T10:01:00Z',
        publishStart: '2026-06-08T10:01:00Z',
        publishEnd: '2026-06-08T10:02:00Z',
        createDate: '2026-06-08T10:00:00Z',
        statusUpdated: '2026-06-08T10:02:00Z'
    },
    numTries: 1,
    ...overrides
});

describe('DotPublishingQueueBundleDetailsDialogComponent', () => {
    let spectator: Spectator<DotPublishingQueueBundleDetailsDialogComponent>;

    const detail = signal<PublishingJobDetailView | null>(null);
    const detailStatus = signal<'init' | 'loading' | 'loaded' | 'error'>('loading');

    const createComponent = createComponentFactory({
        component: DotPublishingQueueBundleDetailsDialogComponent,
        providers: [
            mockProvider(DotPublishingQueueStore, { detail, detailStatus }),
            mockProvider(DotPublishingQueueService, {
                getBundleDownloadUrl: jest.fn((id: string) => `/api/bundle/_download/${id}`)
            }),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ]
    });

    beforeEach(() => {
        detail.set(null);
        detailStatus.set('loading');
        spectator = createComponent();
    });

    it('shows skeletons when loading', () => {
        expect(spectator.query(byTestId('pq-detail-meta'))).toBeFalsy();
    });

    it('renders metadata + endpoints once loaded', () => {
        detail.set(detailFixture());
        detailStatus.set('loaded');
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-detail-meta'))).toBeTruthy();
        expect(spectator.queryAll(byTestId('pq-detail-endpoint-row')).length).toBe(1);
    });

    it('shows download button only for completed bundles', () => {
        detail.set(detailFixture({ status: PublishAuditStatus.SUCCESS }));
        detailStatus.set('loaded');
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-detail-download-btn'))).toBeTruthy();
    });

    it('hides download button for failed bundles', () => {
        detail.set(detailFixture({ status: PublishAuditStatus.FAILED_TO_PUBLISH }));
        detailStatus.set('loaded');
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-detail-download-btn'))).toBeFalsy();
    });

    it('shows empty-endpoints message when environments is empty', () => {
        detail.set(detailFixture({ environments: [] }));
        detailStatus.set('loaded');
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-detail-endpoints-empty'))).toBeTruthy();
    });

    it('does NOT render the assets section (moved to View Contents dialog)', () => {
        detail.set(detailFixture());
        detailStatus.set('loaded');
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-detail-assets-shell'))).toBeFalsy();
        expect(spectator.query(byTestId('pq-detail-assets-table'))).toBeFalsy();
    });

    it('renders multiple environments inside a single table with the env name as a column', () => {
        detail.set(
            detailFixture({
                environments: [
                    {
                        id: 'env-A',
                        name: 'Prod',
                        endpoints: [
                            {
                                id: 'ep-A1',
                                serverName: 'srv-A1',
                                address: '10.0.0.1',
                                port: '443',
                                protocol: 'https',
                                status: PublishAuditStatus.SUCCESS,
                                statusMessage: 'ok',
                                stackTrace: null
                            }
                        ]
                    },
                    {
                        id: 'env-B',
                        name: 'Staging',
                        endpoints: [
                            {
                                id: 'ep-B1',
                                serverName: 'srv-B1',
                                address: '10.0.0.2',
                                port: '443',
                                protocol: 'https',
                                status: PublishAuditStatus.FAILED_TO_PUBLISH,
                                statusMessage: 'boom',
                                stackTrace: null
                            }
                        ]
                    }
                ]
            })
        );
        detailStatus.set('loaded');
        spectator.detectChanges();

        // One table for everything (not one-per-env), no subheader rows
        expect(spectator.queryAll(byTestId('pq-detail-endpoint-table')).length).toBe(1);
        expect(spectator.queryAll(byTestId('pq-detail-env-row')).length).toBe(0);

        // Env name appears in the row itself, once per endpoint
        const rows = spectator.queryAll(byTestId('pq-detail-endpoint-row'));
        expect(rows.length).toBe(2);
        expect(rows[0].textContent).toContain('Prod');
        expect(rows[0].textContent).toContain('srv-A1');
        expect(rows[1].textContent).toContain('Staging');
        expect(rows[1].textContent).toContain('srv-B1');
    });

    describe('endpointAddress', () => {
        function makeEndpoint(over: Partial<EndpointDetailView> = {}): EndpointDetailView {
            return {
                id: 'x',
                serverName: 's',
                address: '',
                port: '',
                protocol: '',
                status: null,
                statusMessage: null,
                stackTrace: null,
                ...over
            };
        }

        beforeEach(() => {
            detail.set(detailFixture());
            detailStatus.set('loaded');
            spectator.detectChanges();
        });

        it('returns null when the address is empty so the cell can show "—"', () => {
            expect(spectator.component.endpointAddress(makeEndpoint())).toBeNull();
            expect(
                spectator.component.endpointAddress(
                    makeEndpoint({ protocol: 'http', port: '8080' })
                )
            ).toBeNull();
        });

        it('builds the full URL when all parts are present', () => {
            expect(
                spectator.component.endpointAddress(
                    makeEndpoint({ protocol: 'https', address: '10.0.0.1', port: '443' })
                )
            ).toBe('https://10.0.0.1:443');
        });

        it('omits the protocol prefix when blank, and the :port when blank', () => {
            expect(
                spectator.component.endpointAddress(
                    makeEndpoint({ address: '10.0.0.1', port: '443' })
                )
            ).toBe('10.0.0.1:443');
            expect(
                spectator.component.endpointAddress(
                    makeEndpoint({ protocol: 'https', address: '10.0.0.1' })
                )
            ).toBe('https://10.0.0.1');
            expect(spectator.component.endpointAddress(makeEndpoint({ address: '10.0.0.1' }))).toBe(
                '10.0.0.1'
            );
        });
    });

    it('shows error state', () => {
        detail.set(null);
        detailStatus.set('error');
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-detail-error'))).toBeTruthy();
    });
});
