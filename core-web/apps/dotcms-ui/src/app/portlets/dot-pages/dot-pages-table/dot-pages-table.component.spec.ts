import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';
import { MockProvider } from 'ng-mocks';

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { fakeAsync, flush, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';

import type { FilterMetadata, LazyLoadEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';

import { DotFormatDateService, DotMessageService } from '@dotcms/data-access';
import { DotcmsEventsService } from '@dotcms/dotcms-js';
import { DotCMSContentlet, DotSystemLanguage } from '@dotcms/dotcms-models';
import {
    DotAutofocusDirective,
    DotContentletStatusChipComponent,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';
import { DotcmsEventsServiceMock } from '@dotcms/utils-testing';

import { DotPagesTableComponent } from './dot-pages-table.component';

type LazyLoadArg = Parameters<DotPagesTableComponent['loadPagesLazy']>[0];
type RowSelectArg = Parameters<DotPagesTableComponent['onRowSelect']>[0];
type OpenMenuArg = Parameters<DotPagesTableComponent['openMenu']['emit']>[0];

const mockContentlet = (partial: Partial<DotCMSContentlet>): DotCMSContentlet => {
    return partial as DotCMSContentlet;
};

const rowSelectEvent = (data: DotCMSContentlet): RowSelectArg => ({ data }) as RowSelectArg;

describe('DotPagesTableComponent', () => {
    let spectator: SpectatorHost<DotPagesTableComponent>;
    let mockDotMessageService: jest.Mocked<Pick<DotMessageService, 'get'>>;

    interface HostComponent {
        pages: DotCMSContentlet[];
        languages: DotSystemLanguage[];
        totalRecords: number;
        isLoading?: boolean;
    }

    const host = () => spectator.hostComponent as HostComponent;

    const setSearchValue = (value: string) => {
        const input = spectator.query('input[type="text"]') as HTMLInputElement | null;
        if (!input) {
            throw new Error('Search input not found');
        }

        input.value = value;
        input.dispatchEvent(new Event('input'));
        spectator.fixture.changeDetectorRef.detectChanges();
    };

    const MOCK_LANGUAGES: DotSystemLanguage[] = [
        {
            id: 1,
            language: 'English',
            languageCode: 'en',
            countryCode: 'US',
            country: 'United States',
            isoCode: 'en-US'
        },
        {
            id: 2,
            language: 'Spanish',
            languageCode: 'es',
            countryCode: 'ES',
            country: 'Spain',
            isoCode: 'es-ES'
        },
        {
            id: 3,
            language: 'French',
            languageCode: 'fr',
            countryCode: '',
            country: '',
            isoCode: 'fr-FR'
        }
    ];

    const MOCK_PAGES: DotCMSContentlet[] = [
        {
            identifier: 'page-1',
            title: 'Home Page',
            urlMap: '/home',
            contentType: 'htmlpageasset',
            languageId: 1,
            modUserName: 'Admin User',
            modDate: '2024-01-15T10:30:00',
            locked: false,
            working: true,
            live: true
        } as unknown as DotCMSContentlet,
        {
            identifier: 'page-2',
            title: 'About Page',
            url: '/about',
            contentType: 'htmlpageasset',
            languageId: 2,
            modUserName: 'Editor User',
            modDate: '2024-01-16T14:20:00',
            locked: true,
            working: true,
            live: false
        } as unknown as DotCMSContentlet,
        {
            identifier: 'page-3',
            title: 'Contact Page',
            urlMap: '/contact',
            contentType: 'htmlpageasset',
            languageId: 1,
            modUserName: 'Content User',
            modDate: '2024-01-17T09:15:00',
            locked: false,
            working: true,
            live: true
        } as unknown as DotCMSContentlet
    ];

    const createHost = createHostFactory({
        component: DotPagesTableComponent,
        imports: [
            ButtonModule,
            CheckboxModule,
            SelectModule,
            TableModule,
            TooltipModule,
            DotAutofocusDirective,
            DotContentletStatusChipComponent,
            DotMessagePipe,
            DotRelativeDatePipe,
            ReactiveFormsModule
        ],
        schemas: [NO_ERRORS_SCHEMA],
        providers: [
            MockProvider(DotFormatDateService),
            {
                provide: DotcmsEventsService,
                useClass: DotcmsEventsServiceMock
            }
        ]
    });

    beforeEach(() => {
        mockDotMessageService = {
            get: jest.fn((key: string) => key)
        };

        spectator = createHost(
            `<dot-pages-table
                [pages]="pages"
                [languages]="languages"
                [totalRecords]="totalRecords"
                [isLoading]="isLoading"
            />`,
            {
                providers: [{ provide: DotMessageService, useValue: mockDotMessageService }],
                hostProps: {
                    pages: MOCK_PAGES,
                    languages: MOCK_LANGUAGES,
                    totalRecords: 3,
                    isLoading: false
                }
            }
        );
    });

    describe('Initialization', () => {
        it('should create component', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should initialize with required inputs', () => {
            expect(spectator.component.$pages()).toEqual(MOCK_PAGES);
            expect(spectator.component.$languages()).toEqual(MOCK_LANGUAGES);
            expect(spectator.component.$totalRecords()).toBe(3);
        });

        it('should initialize form controls with default values', () => {
            expect(spectator.component.searchControl.value).toBe('');
            expect(spectator.component.languageControl.value).toBeNull();
            expect(spectator.component.archivedControl.value).toBe(false);
        });

        it('should compute language options with "All" option first', () => {
            const languageOptions = spectator.component.$languageOptions();

            expect(languageOptions).toHaveLength(4);
            expect(languageOptions[0]).toEqual({ label: 'All', value: null });
            expect(languageOptions[1]).toEqual({ label: 'English (US)', value: 1 });
            expect(languageOptions[2]).toEqual({ label: 'Spanish (ES)', value: 2 });
            expect(languageOptions[3]).toEqual({ label: 'French', value: 3 });
        });

        it('should compute language ISO code mapping', () => {
            const languageIsoCodeById = spectator.component.$languageIsoCodeById();

            expect(languageIsoCodeById).toEqual({
                1: 'en-US',
                2: 'es-ES',
                3: 'fr-FR'
            });
        });

        it('should initialize dot state labels', () => {
            expect(mockDotMessageService.get).toHaveBeenCalledWith('Archived');
            expect(mockDotMessageService.get).toHaveBeenCalledWith('Published');
            expect(mockDotMessageService.get).toHaveBeenCalledWith('Revision');
            expect(mockDotMessageService.get).toHaveBeenCalledWith('Draft');
        });
    });

    describe('Search Control', () => {
        it('should debounce search input by 300ms', fakeAsync(() => {
            const searchSpy = jest.fn();
            spectator.component.search.subscribe(searchSpy);

            setSearchValue('test');

            // Should not emit immediately
            tick(100);
            expect(searchSpy).not.toHaveBeenCalled();

            // Should emit after 300ms
            tick(200);
            expect(searchSpy).toHaveBeenCalledWith('test');
            expect(searchSpy).toHaveBeenCalledTimes(1);

            flush();
        }));

        it('should emit distinct values only', fakeAsync(() => {
            const searchSpy = jest.fn();
            spectator.component.search.subscribe(searchSpy);

            // Set same value twice
            setSearchValue('test');
            tick(300);
            setSearchValue('test');
            tick(300);

            expect(searchSpy).toHaveBeenCalledTimes(1);
            expect(searchSpy).toHaveBeenCalledWith('test');

            flush();
        }));

        it('should emit new distinct value after debounce', fakeAsync(() => {
            const searchSpy = jest.fn();
            spectator.component.search.subscribe(searchSpy);

            setSearchValue('test1');
            tick(300);
            setSearchValue('test2');
            tick(300);

            expect(searchSpy).toHaveBeenCalledTimes(2);
            expect(searchSpy).toHaveBeenNthCalledWith(1, 'test1');
            expect(searchSpy).toHaveBeenNthCalledWith(2, 'test2');

            flush();
        }));

        it('should handle rapid typing correctly', fakeAsync(() => {
            const searchSpy = jest.fn();
            spectator.component.search.subscribe(searchSpy);

            // Simulate rapid typing
            setSearchValue('t');
            tick(50);
            setSearchValue('te');
            tick(50);
            setSearchValue('tes');
            tick(50);
            setSearchValue('test');
            tick(300);

            // Should only emit the final value after 300ms
            expect(searchSpy).toHaveBeenCalledTimes(1);
            expect(searchSpy).toHaveBeenCalledWith('test');

            flush();
        }));

        it('should handle empty search string', fakeAsync(() => {
            const searchSpy = jest.fn();
            spectator.component.search.subscribe(searchSpy);

            setSearchValue('');
            tick(300);

            expect(searchSpy).toHaveBeenCalledWith('');

            flush();
        }));
    });

    describe('Language Control', () => {
        it('should emit language change immediately without debounce', () => {
            const languageChangeSpy = jest.fn();
            spectator.component.languageChange.subscribe(languageChangeSpy);

            spectator.component.languageControl.setValue(1);

            expect(languageChangeSpy).toHaveBeenCalledWith(1);
            expect(languageChangeSpy).toHaveBeenCalledTimes(1);
        });

        it('should emit null for "All" languages option', () => {
            const languageChangeSpy = jest.fn();
            spectator.component.languageChange.subscribe(languageChangeSpy);

            spectator.component.languageControl.setValue(null);

            expect(languageChangeSpy).toHaveBeenCalledWith(null);
        });

        it('should emit distinct language values only', () => {
            const languageChangeSpy = jest.fn();
            spectator.component.languageChange.subscribe(languageChangeSpy);

            spectator.component.languageControl.setValue(1);
            spectator.component.languageControl.setValue(1);

            expect(languageChangeSpy).toHaveBeenCalledTimes(1);
        });

        it('should emit when changing between different languages', () => {
            const languageChangeSpy = jest.fn();
            spectator.component.languageChange.subscribe(languageChangeSpy);

            spectator.component.languageControl.setValue(1);
            spectator.component.languageControl.setValue(2);
            spectator.component.languageControl.setValue(null);

            expect(languageChangeSpy).toHaveBeenCalledTimes(3);
            expect(languageChangeSpy).toHaveBeenNthCalledWith(1, 1);
            expect(languageChangeSpy).toHaveBeenNthCalledWith(2, 2);
            expect(languageChangeSpy).toHaveBeenNthCalledWith(3, null);
        });
    });

    describe('Archived Control', () => {
        it('should emit archived change immediately without debounce', () => {
            const archivedChangeSpy = jest.fn();
            spectator.component.archivedChange.subscribe(archivedChangeSpy);

            spectator.component.archivedControl.setValue(true);

            expect(archivedChangeSpy).toHaveBeenCalledWith(true);
            expect(archivedChangeSpy).toHaveBeenCalledTimes(1);
        });

        it('should emit false for unchecked state', () => {
            const archivedChangeSpy = jest.fn();
            spectator.component.archivedChange.subscribe(archivedChangeSpy);

            spectator.component.archivedControl.setValue(false);

            expect(archivedChangeSpy).toHaveBeenCalledWith(false);
        });

        it('should emit distinct archived values only', () => {
            const archivedChangeSpy = jest.fn();
            spectator.component.archivedChange.subscribe(archivedChangeSpy);

            spectator.component.archivedControl.setValue(true);
            spectator.component.archivedControl.setValue(true);

            expect(archivedChangeSpy).toHaveBeenCalledTimes(1);
        });

        it('should emit when toggling archived checkbox', () => {
            const archivedChangeSpy = jest.fn();
            spectator.component.archivedChange.subscribe(archivedChangeSpy);

            spectator.component.archivedControl.setValue(true);
            spectator.component.archivedControl.setValue(false);
            spectator.component.archivedControl.setValue(true);

            expect(archivedChangeSpy).toHaveBeenCalledTimes(3);
            expect(archivedChangeSpy).toHaveBeenNthCalledWith(1, true);
            expect(archivedChangeSpy).toHaveBeenNthCalledWith(2, false);
            expect(archivedChangeSpy).toHaveBeenNthCalledWith(3, true);
        });
    });

    describe('Lazy Load Handling', () => {
        it('should emit lazy load events after initialization', () => {
            const lazyLoadSpy = jest.fn();
            spectator.component.lazyLoad.subscribe(lazyLoadSpy);

            const mockLazyLoadEvent = {
                first: 40,
                rows: 40,
                sortField: 'modDate',
                sortOrder: -1
            };

            // Component may have already skipped first load in previous tests
            // This tests that subsequent loads are properly emitted
            spectator.triggerEventHandler('p-table', 'onLazyLoad', mockLazyLoadEvent);

            expect(lazyLoadSpy).toHaveBeenCalledWith(mockLazyLoadEvent);
        });

        it('should emit multiple lazy load events', () => {
            const lazyLoadSpy = jest.fn();
            spectator.component.lazyLoad.subscribe(lazyLoadSpy);

            const events = [
                { first: 40, rows: 40, sortField: 'modDate', sortOrder: -1 },
                { first: 80, rows: 40, sortField: 'title', sortOrder: 1 },
                { first: 120, rows: 40, sortField: 'urlMap', sortOrder: 1 }
            ];

            // Emit multiple events
            events.forEach((event) =>
                spectator.triggerEventHandler('p-table', 'onLazyLoad', event)
            );

            expect(lazyLoadSpy).toHaveBeenCalledTimes(3);
            expect(lazyLoadSpy).toHaveBeenNthCalledWith(1, events[0]);
            expect(lazyLoadSpy).toHaveBeenNthCalledWith(2, events[1]);
            expect(lazyLoadSpy).toHaveBeenNthCalledWith(3, events[2]);
        });

        it('should pass through lazy load event data correctly', () => {
            const lazyLoadSpy = jest.fn();
            spectator.component.lazyLoad.subscribe(lazyLoadSpy);

            const complexEvent: LazyLoadEvent = {
                first: 120,
                rows: 40,
                sortField: 'contentType',
                sortOrder: 1,
                filters: {
                    languageId: { value: 1 } as FilterMetadata
                }
            };

            spectator.triggerEventHandler(
                'p-table',
                'onLazyLoad',
                complexEvent as unknown as LazyLoadArg
            );

            expect(lazyLoadSpy).toHaveBeenCalledWith(complexEvent);
        });
    });

    describe('Row Selection', () => {
        it('should emit navigation URL with urlMap and languageId', () => {
            const navigateToPageSpy = jest.fn();
            spectator.component.navigateToPage.subscribe(navigateToPageSpy);

            spectator.triggerEventHandler('p-table', 'onRowSelect', rowSelectEvent(MOCK_PAGES[0]));

            expect(navigateToPageSpy).toHaveBeenCalledWith('/home?language_id=1&device_inode=');
        });

        it('should use url property when urlMap is not available', () => {
            const navigateToPageSpy = jest.fn();
            spectator.component.navigateToPage.subscribe(navigateToPageSpy);

            spectator.triggerEventHandler('p-table', 'onRowSelect', rowSelectEvent(MOCK_PAGES[1]));

            expect(navigateToPageSpy).toHaveBeenCalledWith('/about?language_id=2&device_inode=');
        });

        it('should prefer urlMap over url when both are present', () => {
            const navigateToPageSpy = jest.fn();
            spectator.component.navigateToPage.subscribe(navigateToPageSpy);

            spectator.triggerEventHandler(
                'p-table',
                'onRowSelect',
                rowSelectEvent(
                    mockContentlet({ urlMap: '/contact', url: '/fallback', languageId: 1 })
                )
            );

            expect(navigateToPageSpy).toHaveBeenCalledWith('/contact?language_id=1&device_inode=');
        });

        it('should handle empty URL gracefully', () => {
            const navigateToPageSpy = jest.fn();
            spectator.component.navigateToPage.subscribe(navigateToPageSpy);

            spectator.triggerEventHandler(
                'p-table',
                'onRowSelect',
                rowSelectEvent(mockContentlet({ languageId: 1 }))
            );

            expect(navigateToPageSpy).toHaveBeenCalledWith('?language_id=1&device_inode=');
        });

        it('should handle missing languageId', () => {
            const navigateToPageSpy = jest.fn();
            spectator.component.navigateToPage.subscribe(navigateToPageSpy);

            spectator.triggerEventHandler(
                'p-table',
                'onRowSelect',
                rowSelectEvent(mockContentlet({ urlMap: '/home' }))
            );

            expect(navigateToPageSpy).toHaveBeenCalledWith('/home?language_id=&device_inode=');
        });

        it('should handle various languageId types', () => {
            const navigateToPageSpy = jest.fn();
            spectator.component.navigateToPage.subscribe(navigateToPageSpy);

            // Number languageId
            spectator.triggerEventHandler(
                'p-table',
                'onRowSelect',
                rowSelectEvent(mockContentlet({ urlMap: '/page1', languageId: 1 }))
            );
            expect(navigateToPageSpy).toHaveBeenCalledWith('/page1?language_id=1&device_inode=');

            // String languageId
            spectator.triggerEventHandler(
                'p-table',
                'onRowSelect',
                rowSelectEvent(
                    mockContentlet({ urlMap: '/page2', languageId: '2' as unknown as number })
                )
            );
            expect(navigateToPageSpy).toHaveBeenCalledWith('/page2?language_id=2&device_inode=');
        });
    });

    describe('Open Menu Action', () => {
        beforeEach(() => spectator.detectChanges());

        it('should stop event propagation', () => {
            const mockEvent = {
                stopPropagation: jest.fn()
            } as unknown as MouseEvent;

            spectator.triggerEventHandler('#pageActionButton-0', 'onClick', mockEvent);

            expect(mockEvent.stopPropagation).toHaveBeenCalled();
        });

        it('should emit openMenu event with originalEvent and data', () => {
            const openMenuSpy = jest.fn();
            spectator.component.openMenu.subscribe(openMenuSpy);

            const mockEvent = {
                stopPropagation: jest.fn()
            } as unknown as MouseEvent;

            spectator.triggerEventHandler('#pageActionButton-0', 'onClick', mockEvent);

            expect(openMenuSpy).toHaveBeenCalledWith({
                originalEvent: mockEvent,
                data: MOCK_PAGES[0]
            });
        });

        it('should handle menu action for different pages', () => {
            const openMenuSpy = jest.fn();
            spectator.component.openMenu.subscribe(openMenuSpy);

            MOCK_PAGES.forEach((page, index) => {
                const mockEvent = {
                    stopPropagation: jest.fn()
                } as unknown as MouseEvent;

                spectator.triggerEventHandler(`#pageActionButton-${index}`, 'onClick', mockEvent);

                expect(mockEvent.stopPropagation).toHaveBeenCalled();
                expect(openMenuSpy).toHaveBeenCalledWith({
                    originalEvent: mockEvent,
                    data: page
                });
            });

            expect(openMenuSpy).toHaveBeenCalledTimes(MOCK_PAGES.length);
        });
    });

    describe('Page Events', () => {
        it('should emit createPage event when the create button is clicked', () => {
            const createPageSpy = jest.fn();
            spectator.component.createPage.subscribe(createPageSpy);

            // Ensure the caption create button is the only p-button (avoid row action buttons)
            host().pages = [];
            spectator.fixture.changeDetectorRef.detectChanges();

            spectator.triggerEventHandler('p-button', 'onClick', new MouseEvent('click'));

            expect(createPageSpy).toHaveBeenCalled();
        });

        it('should emit pageChange event when p-table emits onPage', () => {
            const pageChangeSpy = jest.fn();
            spectator.component.pageChange.subscribe(pageChangeSpy);

            spectator.triggerEventHandler('p-table', 'onPage', {});

            expect(pageChangeSpy).toHaveBeenCalled();
        });
    });

    describe('Edge Cases', () => {
        beforeEach(() => spectator.detectChanges());

        it('should handle empty pages array', () => {
            host().pages = [];
            spectator.fixture.changeDetectorRef.detectChanges();

            expect(spectator.component.$pages()).toEqual([]);
        });

        it('should handle empty languages array', () => {
            host().languages = [];
            spectator.fixture.changeDetectorRef.detectChanges();

            const languageOptions = spectator.component.$languageOptions();
            expect(languageOptions).toEqual([{ label: 'All', value: null }]);

            const languageIsoCodeById = spectator.component.$languageIsoCodeById();
            expect(languageIsoCodeById).toEqual({});
        });

        it('should handle zero total records', () => {
            host().totalRecords = 0;
            spectator.fixture.changeDetectorRef.detectChanges();

            expect(spectator.component.$totalRecords()).toBe(0);
        });

        it('should handle languages without countryCode', () => {
            const languagesWithoutCountryCode: DotSystemLanguage[] = [
                {
                    id: 1,
                    language: 'English',
                    languageCode: 'en',
                    countryCode: '',
                    country: '',
                    isoCode: 'en'
                }
            ];

            host().languages = languagesWithoutCountryCode;
            spectator.fixture.changeDetectorRef.detectChanges();

            const languageOptions = spectator.component.$languageOptions();
            expect(languageOptions[1]).toEqual({ label: 'English', value: 1 });
        });

        it('should handle languages without isoCode', () => {
            const languagesWithoutIsoCode: DotSystemLanguage[] = [
                {
                    id: 1,
                    language: 'English',
                    languageCode: 'en',
                    countryCode: 'US',
                    country: 'United States',
                    isoCode: undefined
                } as unknown as DotSystemLanguage
            ];

            host().languages = languagesWithoutIsoCode;
            spectator.fixture.changeDetectorRef.detectChanges();

            const languageIsoCodeById = spectator.component.$languageIsoCodeById();
            expect(languageIsoCodeById[1]).toBe('');
        });

        it('should handle page with null urlMap and url', () => {
            const navigateToPageSpy = jest.fn();
            spectator.component.navigateToPage.subscribe(navigateToPageSpy);

            spectator.triggerEventHandler(
                'p-table',
                'onRowSelect',
                rowSelectEvent(
                    mockContentlet({
                        urlMap: null as unknown as string,
                        url: null as unknown as string,
                        languageId: 1
                    })
                )
            );

            expect(navigateToPageSpy).toHaveBeenCalledWith('?language_id=1&device_inode=');
        });
    });

    describe('Integration Workflows', () => {
        beforeEach(() => spectator.detectChanges());

        it('should handle complete search workflow', fakeAsync(() => {
            const searchSpy = jest.fn();
            const lazyLoadSpy = jest.fn();

            spectator.component.search.subscribe(searchSpy);
            spectator.component.lazyLoad.subscribe(lazyLoadSpy);

            // User types search query
            setSearchValue('home');
            tick(300);

            expect(searchSpy).toHaveBeenCalledWith('home');

            // Lazy load triggered - component is already initialized so this will emit
            spectator.triggerEventHandler('p-table', 'onLazyLoad', { first: 0, rows: 40 });

            expect(lazyLoadSpy).toHaveBeenCalled();

            flush();
        }));

        it('should handle filter combination workflow', fakeAsync(() => {
            const searchSpy = jest.fn();
            const languageChangeSpy = jest.fn();
            const archivedChangeSpy = jest.fn();

            spectator.component.search.subscribe(searchSpy);
            spectator.component.languageChange.subscribe(languageChangeSpy);
            spectator.component.archivedChange.subscribe(archivedChangeSpy);

            // User applies multiple filters
            setSearchValue('test');
            spectator.component.languageControl.setValue(1);
            spectator.component.archivedControl.setValue(true);

            // Search is debounced
            expect(searchSpy).not.toHaveBeenCalled();
            expect(languageChangeSpy).toHaveBeenCalledWith(1);
            expect(archivedChangeSpy).toHaveBeenCalledWith(true);

            tick(300);
            expect(searchSpy).toHaveBeenCalledWith('test');

            flush();
        }));

        it('should handle row selection and navigation workflow', () => {
            const navigateToPageSpy = jest.fn();
            spectator.component.navigateToPage.subscribe(navigateToPageSpy);

            // User selects a page row
            spectator.triggerEventHandler('p-table', 'onRowSelect', rowSelectEvent(MOCK_PAGES[0]));

            // Should navigate with correct URL
            expect(navigateToPageSpy).toHaveBeenCalledWith('/home?language_id=1&device_inode=');
        });

        it('should handle menu action workflow', () => {
            const openMenuSpy = jest.fn();
            spectator.component.openMenu.subscribe(openMenuSpy);

            const mockEvent = {
                stopPropagation: jest.fn()
            } as unknown as MouseEvent;

            const mockPage = MOCK_PAGES[0];

            // User clicks menu button
            spectator.triggerEventHandler('#pageActionButton-0', 'onClick', mockEvent);

            // Should stop propagation and emit menu event
            expect(mockEvent.stopPropagation).toHaveBeenCalled();
            expect(openMenuSpy).toHaveBeenCalledWith({
                originalEvent: mockEvent,
                data: mockPage
            } as OpenMenuArg);
        });

        it('should handle pagination workflow', () => {
            const lazyLoadSpy = jest.fn();
            const pageChangeSpy = jest.fn();

            spectator.component.lazyLoad.subscribe(lazyLoadSpy);
            spectator.component.pageChange.subscribe(pageChangeSpy);

            // User changes page
            spectator.triggerEventHandler('p-table', 'onPage', {});
            spectator.triggerEventHandler('p-table', 'onLazyLoad', { first: 40, rows: 40 });

            expect(pageChangeSpy).toHaveBeenCalled();
            expect(lazyLoadSpy).toHaveBeenCalledWith({ first: 40, rows: 40 });
        });

        it('should handle sorting workflow', () => {
            const lazyLoadSpy = jest.fn();
            spectator.component.lazyLoad.subscribe(lazyLoadSpy);

            // User changes sort
            spectator.triggerEventHandler('p-table', 'onLazyLoad', {
                first: 0,
                rows: 40,
                sortField: 'title'
            });

            expect(lazyLoadSpy).toHaveBeenCalledWith({
                first: 0,
                rows: 40,
                sortField: 'title'
            });
        });

        it('should handle create page workflow', () => {
            const createPageSpy = jest.fn();
            spectator.component.createPage.subscribe(createPageSpy);

            // User clicks create button
            host().pages = [];
            spectator.fixture.changeDetectorRef.detectChanges();
            spectator.triggerEventHandler('p-button', 'onClick', new MouseEvent('click'));

            expect(createPageSpy).toHaveBeenCalled();
        });

        it('should handle rapid filter changes workflow', fakeAsync(() => {
            const searchSpy = jest.fn();
            const languageChangeSpy = jest.fn();
            const archivedChangeSpy = jest.fn();

            spectator.component.search.subscribe(searchSpy);
            spectator.component.languageChange.subscribe(languageChangeSpy);
            spectator.component.archivedChange.subscribe(archivedChangeSpy);

            // User rapidly changes filters
            setSearchValue('test1');
            tick(100);
            setSearchValue('test2');
            tick(100);
            setSearchValue('test3');
            tick(300);

            spectator.component.languageControl.setValue(1);
            spectator.component.languageControl.setValue(2);
            spectator.component.languageControl.setValue(3);

            spectator.component.archivedControl.setValue(true);
            spectator.component.archivedControl.setValue(false);

            // Search should only emit final value
            expect(searchSpy).toHaveBeenCalledTimes(1);
            expect(searchSpy).toHaveBeenCalledWith('test3');

            // Language and archived should emit each distinct value
            expect(languageChangeSpy).toHaveBeenCalledTimes(3);
            expect(archivedChangeSpy).toHaveBeenCalledTimes(2);

            flush();
        }));
    });
});
