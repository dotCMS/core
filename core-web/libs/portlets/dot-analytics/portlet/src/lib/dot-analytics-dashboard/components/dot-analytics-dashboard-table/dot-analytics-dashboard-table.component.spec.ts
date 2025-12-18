import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { MockModule } from 'ng-mocks';

import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import {
    RequestState,
    TopPerformanceTableEntity
} from '@dotcms/portlets/dot-analytics/data-access';

import { DotAnalyticsDashboardTableComponent } from './dot-analytics-dashboard-table.component';

describe('DotAnalyticsDashboardTableComponent', () => {
    let spectator: Spectator<DotAnalyticsDashboardTableComponent>;

    const mockTableData: TopPerformanceTableEntity[] = [
        {
            'EventSummary.title': 'Home Page',
            'EventSummary.identifier': '/home',
            'EventSummary.totalEvents': '1250'
        },
        {
            'EventSummary.title': 'About Us',
            'EventSummary.identifier': '/about',
            'EventSummary.totalEvents': '890'
        },
        {
            'EventSummary.title': 'Contact',
            'EventSummary.identifier': '/contact',
            'EventSummary.totalEvents': '567'
        }
    ];

    const createMockTableState = (
        data: TopPerformanceTableEntity[] | null = mockTableData,
        status: ComponentStatus = ComponentStatus.LOADED
    ): RequestState<TopPerformanceTableEntity[]> => ({
        data,
        status,
        error: null
    });

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
                tableState: createMockTableState()
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
        it('should initialize with required tableState input', () => {
            expect(spectator.component.$tableState()).toEqual(createMockTableState());
        });

        it('should transform and provide table data', () => {
            expect(spectator.component['$data']()).toBeDefined();
            expect(Array.isArray(spectator.component['$data']())).toBe(true);
        });
    });

    describe('Data Handling', () => {
        it('should handle data changes', () => {
            const newData: TopPerformanceTableEntity[] = [
                {
                    'EventSummary.title': 'New Page',
                    'EventSummary.identifier': '/new',
                    'EventSummary.totalEvents': '100'
                }
            ];

            // Create new component with different data
            spectator = createComponent({
                props: {
                    tableState: createMockTableState(newData, ComponentStatus.LOADED)
                } as unknown,
                detectChanges: false
            });

            expect(spectator.component.$tableState().data).toEqual(newData);
            expect(spectator.component['$data']()).toHaveLength(1);
        });

        it('should handle empty data array', () => {
            // Create new component with empty data
            spectator = createComponent({
                props: {
                    tableState: createMockTableState([], ComponentStatus.LOADED)
                } as unknown,
                detectChanges: false
            });

            expect(spectator.component.$tableState().data).toEqual([]);
            expect(spectator.component['$data']()).toHaveLength(0);
        });
    });

    describe('Status Changes', () => {
        it('should handle different status values', () => {
            // Test LOADING status
            spectator = createComponent({
                props: {
                    tableState: createMockTableState(mockTableData, ComponentStatus.LOADING)
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component.$tableState().status).toBe(ComponentStatus.LOADING);

            // Test ERROR status
            spectator = createComponent({
                props: {
                    tableState: createMockTableState(mockTableData, ComponentStatus.ERROR)
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component.$tableState().status).toBe(ComponentStatus.ERROR);

            // Test INIT status
            spectator = createComponent({
                props: {
                    tableState: createMockTableState(mockTableData, ComponentStatus.INIT)
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component.$tableState().status).toBe(ComponentStatus.INIT);
        });
    });

    describe('Computed Properties', () => {
        it('should correctly identify loading state', () => {
            // Test LOADING state
            spectator = createComponent({
                props: {
                    tableState: createMockTableState(mockTableData, ComponentStatus.LOADING)
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component['$isLoading']()).toBe(true);

            // Test INIT state
            spectator = createComponent({
                props: {
                    tableState: createMockTableState(mockTableData, ComponentStatus.INIT)
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component['$isLoading']()).toBe(true);

            // Test LOADED state
            spectator = createComponent({
                props: {
                    tableState: createMockTableState(mockTableData, ComponentStatus.LOADED)
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component['$isLoading']()).toBe(false);
        });

        it('should correctly identify empty state', () => {
            // Test with empty array
            spectator = createComponent({
                props: {
                    tableState: createMockTableState([], ComponentStatus.LOADED)
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component['$isEmpty']()).toBe(true);

            // Test with null data
            spectator = createComponent({
                props: {
                    tableState: createMockTableState(null, ComponentStatus.LOADED)
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component['$isEmpty']()).toBe(true);

            // Test with valid data
            spectator = createComponent({
                props: {
                    tableState: createMockTableState(mockTableData, ComponentStatus.LOADED)
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component['$isEmpty']()).toBe(false);
        });

        it('should correctly identify error state', () => {
            // Test ERROR state
            spectator = createComponent({
                props: {
                    tableState: createMockTableState(mockTableData, ComponentStatus.ERROR)
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component['$isError']()).toBe(true);

            // Test non-error states
            spectator = createComponent({
                props: {
                    tableState: createMockTableState(mockTableData, ComponentStatus.LOADED)
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component['$isError']()).toBe(false);

            spectator = createComponent({
                props: {
                    tableState: createMockTableState(mockTableData, ComponentStatus.LOADING)
                } as unknown,
                detectChanges: false
            });
            expect(spectator.component['$isError']()).toBe(false);
        });
    });

    describe('Empty State', () => {
        it('should show empty state when data is empty', () => {
            spectator = createComponent({
                props: {
                    tableState: createMockTableState([], ComponentStatus.LOADED)
                } as unknown
            });

            const emptyState = spectator.query('[data-testid="empty-table-state"]');
            expect(emptyState).toExist();
        });

        it('should show empty state icon and messages', () => {
            spectator = createComponent({
                props: {
                    tableState: createMockTableState([], ComponentStatus.LOADED)
                } as unknown
            });

            const stateMessage = spectator.query('dot-analytics-state-message');
            expect(stateMessage).toExist();
        });

        it('should not show empty state when data is available', () => {
            spectator = createComponent({
                props: {
                    tableState: createMockTableState(mockTableData, ComponentStatus.LOADED)
                } as unknown
            });

            const emptyState = spectator.query('[data-testid="empty-table-state"]');
            expect(emptyState).not.toExist();
        });
    });

    describe('Error State', () => {
        it('should show error state when status is ERROR', () => {
            spectator = createComponent({
                props: {
                    tableState: createMockTableState(mockTableData, ComponentStatus.ERROR)
                } as unknown
            });

            const errorState = spectator.query('.table-error-state');
            expect(errorState).toExist();
        });

        it('should not show error state when status is not ERROR', () => {
            spectator = createComponent({
                props: {
                    tableState: createMockTableState(mockTableData, ComponentStatus.LOADED)
                } as unknown
            });

            const errorState = spectator.query('.table-error-state');
            expect(errorState).not.toExist();
        });

        it('should show error state message component', () => {
            spectator = createComponent({
                props: {
                    tableState: createMockTableState(mockTableData, ComponentStatus.ERROR)
                } as unknown
            });

            const stateMessage = spectator.query('dot-analytics-state-message');
            expect(stateMessage).toExist();
        });
    });
});
