/* eslint-disable @typescript-eslint/no-empty-function */

import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, ElementRef, inject as inject_1, Input } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import {
    FormsModule,
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormGroup
} from '@angular/forms';
import { provideAnimations } from '@angular/platform-browser/animations';

import {
    DotEventsService,
    DotMessageService,
    DotSystemConfigService,
    DotThemesService,
    PaginatorService
} from '@dotcms/data-access';
import { CoreWebService, Site, SiteService } from '@dotcms/dotcms-js';
import { DotIconComponent, DotMessagePipe } from '@dotcms/ui';
import { CoreWebServiceMock, MockDotMessageService, mockDotThemes } from '@dotcms/utils-testing';

import { DotThemeSelectorDropdownComponent } from './dot-theme-selector-dropdown.component';

import { MockDotSystemConfigService } from '../../../test/dot-test-bed';
import {
    PaginationEvent,
    SearchableDropdownComponent
} from '../_common/searchable-dropdown/component/searchable-dropdown.component';

const messageServiceMock = new MockDotMessageService({
    'dot.common.select.themes': 'Select Themes',
    'Last-Updated': 'Last updated'
});

@Component({
    selector: 'dot-site-selector',
    template: `
        <select>
            <option>Fake site selector</option>
        </select>
    `,
    standalone: false
})
class MockDotSiteSelectorComponent {
    @Input() system;
    searchableDropdown = {
        handleClick: () => {
            //
        }
    };

    updateCurrentSite = jest.fn();
}

@Component({
    selector: 'dot-fake-form',
    template: `
        <form [formGroup]="form">
            <dot-theme-selector-dropdown formControlName="theme"></dot-theme-selector-dropdown>
        </form>
    `,
    standalone: false
})
class TestHostFilledComponent {
    private fb = inject_1(UntypedFormBuilder);

    form: UntypedFormGroup;

    constructor() {
        this.form = this.fb.group({
            theme: '123'
        });
    }
}

@Component({
    selector: 'dot-fake-form-empty',
    template: `
        <form [formGroup]="form">
            <dot-theme-selector-dropdown formControlName="theme"></dot-theme-selector-dropdown>
        </form>
    `,
    standalone: false
})
class TestHostEmtpyComponent {
    private fb = inject_1(UntypedFormBuilder);

    form: UntypedFormGroup;

    constructor() {
        this.form = this.fb.group({
            theme: ''
        });
    }
}

describe('DotThemeSelectorDropdownComponent', () => {
    let paginationService: PaginatorService;

    const mockPaginatorService = {
        param: '',
        url: '',
        paginationPerPage: '',
        total: '',
        extraParams: new Map(),

        set searchParam(value) {
            this.param = value;
        },

        get searchParam() {
            return this.param;
        },

        set totalRecords(value) {
            this.total = value;
        },

        get totalRecords() {
            return this.total || mockDotThemes.length;
        },
        setExtraParams(key: string, value: string) {
            this.extraParams.set(key, value);
        },
        getWithOffset() {
            return of([...mockDotThemes]);
        },
        get() {
            return of([...mockDotThemes]);
        }
    };

    const mockSiteService = {
        getCurrentSite() {
            return of({
                identifier: '123'
            });
        },
        getSiteById() {
            return of({
                identifier: '123',
                hostname: 'test'
            });
        },
        currentSite: { identifier: '123' },
        refreshSites$: of(null),
        currentSite$: of({ identifier: '123' })
    };

    const mockDotThemesService = {
        get: jest.fn().mockReturnValue(of(mockDotThemes[1]))
    };

    const createComponent = createComponentFactory({
        component: DotThemeSelectorDropdownComponent,
        componentProviders: [
            { provide: PaginatorService, useValue: mockPaginatorService },
            { provide: DotThemesService, useValue: mockDotThemesService }
        ],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            provideAnimations(),
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: SiteService, useValue: mockSiteService },
            { provide: DotSystemConfigService, useClass: MockDotSystemConfigService },
            DotEventsService
        ],
        imports: [
            SearchableDropdownComponent,
            FormsModule,
            DotMessagePipe,
            ReactiveFormsModule,
            DotIconComponent
        ],
        declarations: [
            TestHostFilledComponent,
            TestHostEmtpyComponent,
            MockDotSiteSelectorComponent
        ],
        detectChanges: false
    });

    describe('basic', () => {
        let spectator: Spectator<DotThemeSelectorDropdownComponent>;
        let component: DotThemeSelectorDropdownComponent;

        beforeEach(() => {
            spectator = createComponent();
            component = spectator.component;
            // Initialize searchInput manually to avoid null reference in ngAfterViewInit
            component.searchInput = {
                nativeElement: { value: '', focus: jest.fn() }
            } as ElementRef;
            paginationService = component.paginatorService;
            jest.spyOn(component, 'propagateChange');
            jest.spyOn(paginationService, 'get');
            // Don't call detectChanges here to avoid ngOnInit calling propagateChange
        });

        afterEach(() => {
            jest.clearAllMocks();
        });

        describe('html', () => {
            it('should set themes if theme selector is open', fakeAsync(() => {
                spectator.detectChanges();
                component.searchableDropdown.display.emit();
                tick();
                expect(component.totalRecords).toEqual(3);
                expect(component.themes).toEqual(mockDotThemes);
            }));

            it('should set paginatorService configuration on init ', () => {
                spectator.detectChanges();
                expect(paginationService.url).toEqual('v1/themes');
                expect(paginationService.paginationPerPage).toEqual(5);
            });

            it('should not call pagination service if the url is not set', () => {
                spectator.detectChanges();
                //Paginator service is now called at least once at the beginning, that's why it has an url at the very beginning
                component.currentSiteIdentifier = '123';
                paginationService.url = '';
                jest.spyOn(paginationService, 'getWithOffset');
                component.searchableDropdown.pageChange.emit({ first: 0 } as PaginationEvent);
                expect(paginationService.getWithOffset).not.toHaveBeenCalledTimes(1);
            });

            it('should call pagination service if the url is set', () => {
                spectator.detectChanges();
                component.currentSiteIdentifier = '123';
                component.paginatorService.url = 'v1/test';
                jest.spyOn(paginationService, 'getWithOffset');
                component.searchableDropdown.pageChange.emit({ first: 10 } as PaginationEvent);
                expect(paginationService.getWithOffset).toHaveBeenCalledWith(10);
                expect(paginationService.getWithOffset).toHaveBeenCalledTimes(1);
            });

            it('should set the right attributes', () => {
                spectator.detectChanges();
                const element = spectator.query('dot-searchable-dropdown');

                const instance = component.searchableDropdown;
                expect(instance.placeholder).toBe('Select Themes');
                expect(element.getAttribute('overlayWidth')).toBe('490px');
                expect(element.getAttribute('labelPropertyName')).toBe('name');
                expect(element.getAttribute('valuePropertyName')).toBe('name');

                component.onShow();
                spectator.detectChanges();
                expect(instance.rows).toBe(5);
            });
        });

        describe('events', () => {
            it('should set value propagate change and toggle the overlay', () => {
                spectator.detectChanges();
                jest.spyOn(component.searchableDropdown, 'toggleOverlayPanel');
                const value = mockDotThemes[0];

                component.onChange(value);
                expect(component.value).toEqual(value);
                expect(component.propagateChange).toHaveBeenCalledWith(value.identifier);
                expect(component.propagateChange).toHaveBeenCalledTimes(1); // Only called once in onChange
                expect(component.searchableDropdown.toggleOverlayPanel).toHaveBeenCalledTimes(1);
            });
        });

        describe('filters', () => {
            beforeEach(() => {
                jest.spyOn(paginationService, 'setExtraParams');
                jest.spyOn(paginationService, 'getWithOffset').mockReturnValue(of(mockDotThemes));
                spectator.detectChanges();
                Object.defineProperty(paginationService, 'totalRecords', {
                    value: 3,
                    writable: true
                });

                // Open the dropdown to make filter elements available
                const searchableButton = spectator.query('dot-searchable-dropdown button');
                if (searchableButton) {
                    spectator.click(searchableButton);
                    spectator.detectChanges();
                }
            });

            it('should system to true', () => {
                const siteSelector = spectator.query('[data-testId="siteSelector"]');
                expect(siteSelector.getAttribute('ng-reflect-system')).toBe('true');
            });

            it('should update themes, totalRecords and call setExtraParams when site selector change', fakeAsync(() => {
                component.siteChange({
                    identifier: '123',
                    hostname: 'test',
                    archived: false
                } as Site);
                tick();
                expect(paginationService.setExtraParams).toHaveBeenCalledWith('hostId', '123'); // Call from dropdown open (onShow)
                expect(paginationService.setExtraParams).toHaveBeenCalledTimes(2); // Called twice: once when dropdown opens (onShow) and once on siteChange
                expect(component.themes).toEqual(mockDotThemes);
                expect(component.totalRecords).toBe(3);
            }));

            it('should update themes, totalRecords and call setExtraParams when search input change', async () => {
                await spectator.fixture.whenStable();
                const input = spectator.query('[data-testId="searchInput"]') as HTMLInputElement;
                input.value = 'hello';
                const event = new KeyboardEvent('keyup');
                input.dispatchEvent(event);
                await spectator.fixture.whenStable();
                expect(paginationService.searchParam).toBe('hello');
                expect(component.themes).toEqual(mockDotThemes);
                expect(component.totalRecords).toBe(3);
            });

            it('should allow keyboad nav on filter Input - ArrowDown', async () => {
                await spectator.fixture.whenStable();
                const input = spectator.query('[data-testId="searchInput"]') as HTMLInputElement;
                const event = new KeyboardEvent('keyup', { key: 'ArrowDown' });
                input.dispatchEvent(event);
                await spectator.fixture.whenStable();
                expect(component.selectedOptionIndex).toBe(1);
                expect(component.selectedOptionValue).toBe(mockDotThemes[1].name);
            });

            it('should allow keyboad nav on filter Input - ArrowUp', async () => {
                await spectator.fixture.whenStable();
                const input = spectator.query('[data-testId="searchInput"]') as HTMLInputElement;
                const event = new KeyboardEvent('keyup', { key: 'ArrowUp' });
                input.dispatchEvent(event);
                await spectator.fixture.whenStable();
                expect(component.selectedOptionIndex).toBe(0);
                expect(component.selectedOptionValue).toBe(mockDotThemes[0].name);
            });

            it('should allow keyboad nav on filter Input - Enter', async () => {
                jest.spyOn(component, 'onChange');
                await spectator.fixture.whenStable();
                const input = spectator.query('[data-testId="searchInput"]') as HTMLInputElement;
                const event = new KeyboardEvent('keyup', { key: 'Enter' });
                input.dispatchEvent(event);
                await spectator.fixture.whenStable();
                expect(component.onChange).toHaveBeenCalledWith(mockDotThemes[0]);
                expect(component.onChange).toHaveBeenCalledTimes(1);
            });
        });
    });

    describe('writeValue', () => {
        let testSpectator: Spectator<DotThemeSelectorDropdownComponent>;
        let testComponent: DotThemeSelectorDropdownComponent;

        beforeEach(() => {
            // Clear mock calls and create fresh component instance
            jest.clearAllMocks();
            testSpectator = createComponent();
            testComponent = testSpectator.component;
        });

        it('should get theme by id', fakeAsync(() => {
            testSpectator.detectChanges();

            // Call writeValue with an existing theme ID
            testComponent.writeValue('123');
            tick(500); // Wait for all async operations

            // Verify the theme service was called with the provided ID
            expect(mockDotThemesService.get).toHaveBeenCalledWith('123');
            // Verify that component state reflects the loaded theme (mockDotThemesService.get returns mockDotThemes[1])
            expect(testComponent.value).toBe(mockDotThemes[1]);
        }));

        it('should load default system theme when no identifier is provided', fakeAsync(() => {
            testSpectator.detectChanges();
            jest.spyOn(mockPaginatorService, 'setExtraParams');
            jest.spyOn(mockPaginatorService, 'get');

            // Call writeValue with empty or null value
            testComponent.writeValue('');
            tick(500); // Wait for all async operations

            // Verify the paginator was called to get system themes
            expect(mockPaginatorService.setExtraParams).toHaveBeenCalledWith(
                'hostId',
                'SYSTEM_HOST'
            );
            expect(mockPaginatorService.get).toHaveBeenCalled();
        }));
    });
});
