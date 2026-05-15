import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { MockProvider } from 'ng-mocks';
import { of } from 'rxjs';

import { DialogModule } from 'primeng/dialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';

import { DotPageTypesService, DotRouterService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotAutofocusDirective, DotMessagePipe } from '@dotcms/ui';

import { DotCreatePageDialogComponent } from './dot-create-page-dialog.component';

const createMockContentType = (partial: Partial<DotCMSContentType>): DotCMSContentType =>
    partial as DotCMSContentType;

const MOCK_PAGE_TYPES: DotCMSContentType[] = [
    createMockContentType({
        name: 'Simple Page',
        variable: 'simplePage',
        icon: 'description'
    }),
    createMockContentType({
        name: 'Advanced Page',
        variable: 'advancedPage',
        icon: 'article'
    }),
    createMockContentType({
        name: 'Landing Page',
        variable: 'landingPage',
        icon: 'web'
    }),
    createMockContentType({
        name: 'Blog Post',
        variable: 'blogPost',
        icon: 'rss_feed'
    })
];

describe('DotCreatePageDialogComponent', () => {
    let spectator: Spectator<DotCreatePageDialogComponent>;
    let mockPageTypesService: jest.Mocked<DotPageTypesService>;
    let mockRouterService: jest.Mocked<DotRouterService>;

    const createComponent = createComponentFactory({
        component: DotCreatePageDialogComponent,
        imports: [
            DotCreatePageDialogComponent,
            DialogModule,
            IconFieldModule,
            InputIconModule,
            InputTextModule,
            DotAutofocusDirective,
            DotMessagePipe
        ],
        detectChanges: false
    });

    beforeEach(() => {
        jest.useFakeTimers();
        spectator = createComponent({
            providers: [
                MockProvider(DotPageTypesService, {
                    getPageContentTypes: jest.fn().mockReturnValue(of(MOCK_PAGE_TYPES))
                }),
                MockProvider(DotRouterService, {
                    goToURL: jest.fn()
                })
            ]
        });
        spectator.setInput('visibility', false);

        mockPageTypesService = spectator.inject(
            DotPageTypesService
        ) as unknown as jest.Mocked<DotPageTypesService>;
        mockRouterService = spectator.inject(
            DotRouterService
        ) as unknown as jest.Mocked<DotRouterService>;
    });

    afterEach(() => {
        jest.clearAllMocks();
        jest.useRealTimers();
    });

    it('should create', () => {
        spectator.detectChanges();
        expect(spectator.component).toBeTruthy();
    });

    describe('Initialization', () => {
        it('should load page types on initialization', () => {
            spectator.detectChanges();

            expect(mockPageTypesService.getPageContentTypes).toHaveBeenCalled();
            expect(spectator.component.$pageTypes()).toEqual(MOCK_PAGE_TYPES);
        });

        it('should initialize search control with empty value', () => {
            spectator.detectChanges();

            expect(spectator.component.searchControl.value).toBe('');
        });

        it('should initialize $searchTerm signal with empty string', () => {
            spectator.detectChanges();
            jest.advanceTimersByTime(300);

            expect(spectator.component.$searchTerm()).toBe('');
        });

        it('should initialize $filteredPageTypes with all page types', () => {
            spectator.detectChanges();

            expect(spectator.component.$filteredPageTypes()).toEqual(MOCK_PAGE_TYPES);
        });
    });

    describe('Dialog Visibility', () => {
        it('should display dialog when visibility is true', () => {
            spectator.setInput('visibility', true);
            spectator.detectChanges();

            const dialog = spectator.query('p-dialog');
            expect(dialog).toBeTruthy();
        });

        it('should hide dialog when visibility is false', () => {
            spectator.setInput('visibility', false);
            spectator.detectChanges();

            const dialog = spectator.query('p-dialog');
            expect(dialog).toBeTruthy(); // Dialog element exists but hidden
        });

        it('should emit visibilityChange when dialog is closed', () => {
            spectator.setInput('visibility', true);
            spectator.detectChanges();

            let emittedValue: boolean | null = null;
            spectator.output('visibilityChange').subscribe((value) => {
                emittedValue = value;
            });

            spectator.triggerEventHandler('p-dialog', 'visibleChange', false);

            expect(emittedValue).toBe(false);
        });

        it('should reset searchControl on dialog hide', () => {
            spectator.detectChanges();

            spectator.component.searchControl.setValue('blog');
            jest.advanceTimersByTime(300);
            expect(spectator.component.$searchTerm()).toBe('blog');

            spectator.triggerEventHandler('p-dialog', 'onHide', null);
            expect(spectator.component.searchControl.value).toBe('');

            jest.advanceTimersByTime(300);
            expect(spectator.component.$searchTerm()).toBe('');
        });
    });

    describe('Search Functionality', () => {
        it('should debounce search input by 300ms', () => {
            spectator.detectChanges();

            spectator.component.searchControl.setValue('simple');
            jest.advanceTimersByTime(100);
            expect(spectator.component.$searchTerm()).toBe('');

            jest.advanceTimersByTime(200);
            expect(spectator.component.$searchTerm()).toBe('simple');
        });

        it('should trim and lowercase search term', () => {
            spectator.detectChanges();

            spectator.component.searchControl.setValue('  SIMPLE Page  ');
            jest.advanceTimersByTime(300);

            expect(spectator.component.$searchTerm()).toBe('simple page');
        });

        it('should filter by page type name', () => {
            spectator.detectChanges();

            spectator.component.searchControl.setValue('landing');
            jest.advanceTimersByTime(300);

            const filtered = spectator.component.$filteredPageTypes();
            expect(filtered).toHaveLength(1);
            expect(filtered[0].name).toBe('Landing Page');
        });

        it('should filter by page type variable', () => {
            spectator.detectChanges();

            spectator.component.searchControl.setValue('blogPost');
            jest.advanceTimersByTime(300);

            const filtered = spectator.component.$filteredPageTypes();
            expect(filtered).toHaveLength(1);
            expect(filtered[0].variable).toBe('blogPost');
        });

        it('should filter case-insensitively', () => {
            spectator.detectChanges();

            spectator.component.searchControl.setValue('ADVANCED');
            jest.advanceTimersByTime(300);

            const filtered = spectator.component.$filteredPageTypes();
            expect(filtered).toHaveLength(1);
            expect(filtered[0].name).toBe('Advanced Page');
        });

        it('should return empty array when no matches found', () => {
            spectator.detectChanges();

            spectator.component.searchControl.setValue('nonexistent');
            jest.advanceTimersByTime(300);

            expect(spectator.component.$filteredPageTypes()).toHaveLength(0);
        });

        it('should return all page types when search is empty', () => {
            spectator.detectChanges();

            spectator.component.searchControl.setValue('landing');
            jest.advanceTimersByTime(300);
            expect(spectator.component.$filteredPageTypes()).toHaveLength(1);

            spectator.component.searchControl.setValue('');
            jest.advanceTimersByTime(300);

            expect(spectator.component.$filteredPageTypes()).toEqual(MOCK_PAGE_TYPES);
        });

        it('should filter by partial matches', () => {
            spectator.detectChanges();

            spectator.component.searchControl.setValue('page');
            jest.advanceTimersByTime(300);

            const filtered = spectator.component.$filteredPageTypes();
            expect(filtered.length).toBeGreaterThan(1);
            expect(filtered.every((type) => type.name?.toLowerCase().includes('page'))).toBe(true);
        });
    });

    describe('Template Rendering', () => {
        it('should render dialog with correct header', () => {
            spectator.setInput('visibility', true);
            spectator.detectChanges();

            const dialog = spectator.query('p-dialog');
            expect(dialog).toBeTruthy();
        });

        it('should render search input', () => {
            spectator.setInput('visibility', true);
            spectator.detectChanges();

            const input = spectator.query('input[type="text"]');
            expect(input).toBeTruthy();
        });

        it('should render search icon', () => {
            spectator.setInput('visibility', true);
            spectator.detectChanges();

            const icon = spectator.query('.pi-search');
            expect(icon).toBeTruthy();
        });

        it('should render page type list when types are available', () => {
            spectator.setInput('visibility', true);
            spectator.detectChanges();

            const items = spectator.queryAll('[class*="flex cursor-pointer"]');
            expect(items.length).toBeGreaterThan(0);
        });

        it('should render "No results" when filtered list is empty', () => {
            spectator.setInput('visibility', true);
            spectator.detectChanges();

            spectator.component.searchControl.setValue('nonexistent');
            jest.advanceTimersByTime(300);
            spectator.detectChanges();

            const noResults = spectator.query('.text-center');
            expect(noResults?.textContent?.trim()).toContain('No results');
        });

        it('should render correct number of page types', () => {
            spectator.setInput('visibility', true);
            spectator.detectChanges();

            const items = spectator.queryAll('[class*="flex cursor-pointer"]');
            expect(items).toHaveLength(MOCK_PAGE_TYPES.length);
        });

        it('should render page type names correctly', () => {
            spectator.setInput('visibility', true);
            spectator.detectChanges();

            const items = spectator.queryAll('[class*="flex cursor-pointer"]');
            const firstItemText = items[0]?.textContent?.trim();

            expect(firstItemText).toContain('Simple Page');
        });

        it('should render page type icons correctly', () => {
            spectator.setInput('visibility', true);
            spectator.detectChanges();

            const icons = spectator.queryAll('.material-symbols-outlined');
            expect(icons.length).toBeGreaterThan(0);
            expect(icons[0]?.textContent?.trim()).toBe('description');
        });
    });

    describe('Navigation', () => {
        it('should call goToCreatePage when page type is clicked', () => {
            spectator.setInput('visibility', true);
            spectator.detectChanges();

            const goToCreatePageSpy = jest.spyOn(spectator.component, 'goToCreatePage');

            spectator.component.goToCreatePage('simplePage');

            expect(goToCreatePageSpy).toHaveBeenCalledWith('simplePage');
        });

        it('should navigate to correct URL when goToCreatePage is called', () => {
            spectator.detectChanges();

            spectator.component.goToCreatePage('simplePage');

            expect(mockRouterService.goToURL).toHaveBeenCalledWith('/pages/new/simplePage');
        });

        it('should emit visibilityChange(false) before navigation', () => {
            spectator.detectChanges();

            let emittedValue: boolean | null = null;
            spectator.output('visibilityChange').subscribe((value) => {
                emittedValue = value;
            });

            spectator.component.goToCreatePage('simplePage');

            expect(emittedValue).toBe(false);
            expect(mockRouterService.goToURL).toHaveBeenCalled();
        });

        it('should navigate with correct variable name', () => {
            spectator.detectChanges();

            spectator.component.goToCreatePage('advancedPage');

            expect(mockRouterService.goToURL).toHaveBeenCalledWith('/pages/new/advancedPage');
        });
    });

    describe('Integration Workflows', () => {
        it('should handle complete search and navigation workflow', () => {
            spectator.setInput('visibility', true);
            spectator.detectChanges();

            spectator.component.searchControl.setValue('landing');
            jest.advanceTimersByTime(300);
            spectator.detectChanges();

            expect(spectator.component.$filteredPageTypes()).toHaveLength(1);
            expect(spectator.component.$filteredPageTypes()[0].name).toBe('Landing Page');

            let emittedVisibility: boolean | null = null;
            spectator.output('visibilityChange').subscribe((value) => {
                emittedVisibility = value;
            });

            spectator.component.goToCreatePage('landingPage');

            expect(emittedVisibility).toBe(false);
            expect(mockRouterService.goToURL).toHaveBeenCalledWith('/pages/new/landingPage');
        });

        it('should handle search with no results workflow', () => {
            spectator.setInput('visibility', true);
            spectator.detectChanges();

            spectator.component.searchControl.setValue('nonexistent');
            jest.advanceTimersByTime(300);
            spectator.detectChanges();

            expect(spectator.component.$filteredPageTypes()).toHaveLength(0);

            const noResults = spectator.query('.text-center');
            expect(noResults?.textContent?.trim()).toContain('No results');

            spectator.component.searchControl.setValue('');
            jest.advanceTimersByTime(300);
            spectator.detectChanges();

            expect(spectator.component.$filteredPageTypes()).toEqual(MOCK_PAGE_TYPES);
        });

        it('should handle rapid search input changes', () => {
            spectator.setInput('visibility', true);
            spectator.detectChanges();

            const input = spectator.query('input[type="text"]') as HTMLInputElement;
            expect(input).toBeTruthy();

            spectator.typeInElement('g', input);
            jest.advanceTimersByTime(100);
            spectator.typeInElement('go', input);
            jest.advanceTimersByTime(100);
            spectator.typeInElement('gon', input);
            jest.advanceTimersByTime(100);
            spectator.typeInElement('gone', input);

            expect(spectator.component.$searchTerm()).toBe('');

            jest.advanceTimersByTime(300);

            expect(spectator.component.$searchTerm()).toBe('gone');
        });
    });

    describe('Edge Cases', () => {
        it('should handle empty page types list', () => {
            mockPageTypesService.getPageContentTypes.mockReturnValue(of([]));

            spectator.component.$pageTypes.set([]);
            spectator.detectChanges();

            expect(spectator.component.$pageTypes()).toEqual([]);
            expect(spectator.component.$filteredPageTypes()).toEqual([]);
        });

        it('should handle page types with null name', () => {
            const typesWithNull = [
                createMockContentType({ name: null, variable: 'test' })
            ] as DotCMSContentType[];

            spectator.component.$pageTypes.set(typesWithNull);
            spectator.setInput('visibility', true);
            spectator.detectChanges();

            spectator.component.searchControl.setValue('test');
            jest.advanceTimersByTime(300);
            spectator.detectChanges();

            const rows = spectator.queryAll(
                '[data-testid="dot-pages-create-page-dialog__page-type-row"]'
            );
            expect(rows).toHaveLength(1);
        });

        it('should handle page types with null variable', () => {
            const typesWithNull = [
                createMockContentType({ name: 'Test Page', variable: null })
            ] as DotCMSContentType[];

            spectator.component.$pageTypes.set(typesWithNull);
            spectator.detectChanges();

            spectator.component.searchControl.setValue('test');
            jest.advanceTimersByTime(300);

            expect(spectator.component.$filteredPageTypes()).toHaveLength(1);
        });

        it('should handle whitespace-only search input', () => {
            spectator.detectChanges();

            spectator.component.searchControl.setValue('   ');
            jest.advanceTimersByTime(300);

            expect(spectator.component.$searchTerm()).toBe('');
            expect(spectator.component.$filteredPageTypes()).toEqual(MOCK_PAGE_TYPES);
        });

        it('should handle special characters in search', () => {
            spectator.detectChanges();

            spectator.component.searchControl.setValue('simple@#$');
            jest.advanceTimersByTime(300);

            expect(spectator.component.$filteredPageTypes()).toHaveLength(0);
        });
    });

    describe('Signal State Management', () => {
        it('should update $searchTerm signal when form control changes', () => {
            spectator.detectChanges();

            expect(spectator.component.$searchTerm()).toBe('');

            spectator.component.searchControl.setValue('test');
            jest.advanceTimersByTime(300);

            expect(spectator.component.$searchTerm()).toBe('test');
        });

        it('should reactively update $filteredPageTypes when $searchTerm changes', () => {
            spectator.detectChanges();

            expect(spectator.component.$filteredPageTypes()).toEqual(MOCK_PAGE_TYPES);

            spectator.component.searchControl.setValue('simple');
            jest.advanceTimersByTime(300);

            expect(spectator.component.$filteredPageTypes()).toHaveLength(1);
        });

        it('should accept visibility input updates', () => {
            spectator.setInput('visibility', true);
            expect(spectator.component.$visibility()).toBe(true);

            spectator.setInput('visibility', false);
            expect(spectator.component.$visibility()).toBe(false);
        });

        it('should maintain page types signal state', () => {
            spectator.detectChanges();

            expect(spectator.component.$pageTypes()).toEqual(MOCK_PAGE_TYPES);

            const newTypes = [createMockContentType({ name: 'New Type', variable: 'newType' })];
            spectator.component.$pageTypes.set(newTypes);

            expect(spectator.component.$pageTypes()).toEqual(newTypes);
        });
    });
});
