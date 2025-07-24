import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { MockModule } from 'ng-mocks';

import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotAnalyticsDashboardTableComponent } from './dot-analytics-dashboard-table.component';

describe('DotAnalyticsDashboardTableComponent', () => {
    let spectator: Spectator<DotAnalyticsDashboardTableComponent>;

    const mockTableData = [
        {
            pageTitle: 'Home Page',
            path: '/home',
            views: 1250
        },
        {
            pageTitle: 'About Us',
            path: '/about',
            views: 890
        },
        {
            pageTitle: 'Contact',
            path: '/contact',
            views: 567
        }
    ];

    const createComponent = createComponentFactory({
        component: DotAnalyticsDashboardTableComponent,
        overrideComponents: [
            [
                DotAnalyticsDashboardTableComponent,
                {
                    remove: { imports: [CardModule, SkeletonModule, TableModule] },
                    add: {
                        imports: [
                            MockModule(CardModule),
                            MockModule(SkeletonModule),
                            MockModule(TableModule)
                        ]
                    }
                }
            ]
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: {
                    get: jest.fn().mockReturnValue('Translated message')
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                data: mockTableData,
                status: ComponentStatus.LOADED
            } as unknown,
            detectChanges: false // Prevent automatic change detection
        });
    });

    describe('Component Initialization', () => {
        it('should create component successfully', () => {
            expect(spectator.component).toBeTruthy();
        });
    });

    describe('Required Inputs', () => {
        it('should initialize with required data input', () => {
            expect(spectator.component.$data()).toEqual(mockTableData);
        });

        it('should initialize with required status input', () => {
            expect(spectator.component.$status()).toBe(ComponentStatus.LOADED);
        });
    });

    describe('Data Handling', () => {
        it('should handle data changes', () => {
            const newData = [{ pageTitle: 'New Page', path: '/new', views: 100 }];

            // Create new component with different data
            spectator = createComponent({
                props: {
                    data: newData,
                    status: ComponentStatus.LOADED
                } as unknown,
                detectChanges: false
            });

            expect(spectator.component.$data()).toEqual(newData);
            expect(spectator.component.$data()).toHaveLength(1);
        });

        it('should handle empty data array', () => {
            // Create new component with empty data
            spectator = createComponent({
                props: {
                    data: [],
                    status: ComponentStatus.LOADED
                } as unknown,
                detectChanges: false
            });

            expect(spectator.component.$data()).toEqual([]);
            expect(spectator.component.$data()).toHaveLength(0);
        });
    });

    describe('Status Changes', () => {
        it('should handle different status values', () => {
            // Test LOADING status
            spectator = createComponent({
                props: {
                    data: mockTableData,
                    status: ComponentStatus.LOADING
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component.$status()).toBe(ComponentStatus.LOADING);

            // Test ERROR status
            spectator = createComponent({
                props: {
                    data: mockTableData,
                    status: ComponentStatus.ERROR
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component.$status()).toBe(ComponentStatus.ERROR);

            // Test INIT status
            spectator = createComponent({
                props: {
                    data: mockTableData,
                    status: ComponentStatus.INIT
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component.$status()).toBe(ComponentStatus.INIT);
        });
    });

    describe('Computed Properties', () => {
        it('should correctly identify loading state', () => {
            // Test LOADING state
            spectator = createComponent({
                props: {
                    data: mockTableData,
                    status: ComponentStatus.LOADING
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component['$isLoading']()).toBe(true);

            // Test INIT state
            spectator = createComponent({
                props: {
                    data: mockTableData,
                    status: ComponentStatus.INIT
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component['$isLoading']()).toBe(true);

            // Test LOADED state
            spectator = createComponent({
                props: {
                    data: mockTableData,
                    status: ComponentStatus.LOADED
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component['$isLoading']()).toBe(false);
        });

        it('should correctly identify empty state', () => {
            // Test with empty array
            spectator = createComponent({
                props: {
                    data: [],
                    status: ComponentStatus.LOADED
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component['$isEmpty']()).toBe(true);

            // Test with null data
            spectator = createComponent({
                props: {
                    data: null,
                    status: ComponentStatus.LOADED
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component['$isEmpty']()).toBe(true);

            // Test with valid data
            spectator = createComponent({
                props: {
                    data: mockTableData,
                    status: ComponentStatus.LOADED
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component['$isEmpty']()).toBe(false);
        });
    });

    describe('Empty State', () => {
        it('should show empty state when data is empty', () => {
            spectator = createComponent({
                props: {
                    data: [],
                    status: ComponentStatus.LOADED
                } as unknown
            });

            const emptyState = spectator.query('[data-testid="empty-table-state"]');
            expect(emptyState).toExist();
        });

        it('should show empty state icon and messages', () => {
            spectator = createComponent({
                props: {
                    data: [],
                    status: ComponentStatus.LOADED
                } as unknown
            });

            const emptyIcon = spectator.query('.pi-table');
            const emptyMessage = spectator.query('.empty-message');
            const emptyDescription = spectator.query('.empty-description');

            expect(emptyIcon).toExist();
            expect(emptyMessage).toExist();
            expect(emptyDescription).toExist();
        });

        it('should not show empty state when data is available', () => {
            spectator = createComponent({
                props: {
                    data: mockTableData,
                    status: ComponentStatus.LOADED
                } as unknown
            });

            const emptyState = spectator.query('[data-testid="empty-table-state"]');
            expect(emptyState).not.toExist();
        });

        it('should show table title in empty state', () => {
            spectator = createComponent({
                props: {
                    data: [],
                    status: ComponentStatus.LOADED
                } as unknown
            });

            const title = spectator.query('.table-title');
            expect(title).toExist();
            expect(title).toHaveText('Translated message');
        });
    });
});
