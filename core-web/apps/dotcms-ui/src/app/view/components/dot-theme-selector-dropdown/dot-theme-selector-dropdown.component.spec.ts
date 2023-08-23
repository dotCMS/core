/* eslint-disable @typescript-eslint/no-empty-function */

import { of } from 'rxjs';

import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import {
    FormsModule,
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormGroup
} from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import {
    PaginationEvent,
    SearchableDropdownComponent
} from '@components/_common/searchable-dropdown/component/searchable-dropdown.component';
import { DotMessageService, DotThemesService, PaginatorService } from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService, mockDotThemes } from '@dotcms/utils-testing';

import { DotThemeSelectorDropdownComponent } from './dot-theme-selector-dropdown.component';

const messageServiceMock = new MockDotMessageService({
    'dot.common.select.themes': 'Select Themes',
    'Last-Updated': 'Last updated'
});

@Component({
    selector: 'dot-site-selector',
    template: `<select>
        <option>Fake site selector</option>
    </select>`
})
class MockDotSiteSelectorComponent {
    @Input() system;
    searchableDropdown = {
        handleClick: () => {
            //
        }
    };

    updateCurrentSite = jasmine.createSpy('updateCurrentSite');
}

@Component({
    selector: 'dot-fake-form',
    template: `
        <form [formGroup]="form">
            <dot-theme-selector-dropdown formControlName="theme"></dot-theme-selector-dropdown>
        </form>
    `
})
class TestHostFilledComponent {
    form: UntypedFormGroup;

    constructor(private fb: UntypedFormBuilder) {
        this.form = this.fb.group({
            theme: '123'
        });
    }
}

@Component({
    selector: 'dot-fake-form',
    template: `
        <form [formGroup]="form">
            <dot-theme-selector-dropdown formControlName="theme"></dot-theme-selector-dropdown>
        </form>
    `
})
class TestHostEmtpyComponent {
    form: UntypedFormGroup;

    constructor(private fb: UntypedFormBuilder) {
        this.form = this.fb.group({
            theme: ''
        });
    }
}

describe('DotThemeSelectorDropdownComponent', () => {
    let paginationService: PaginatorService;
    let de: DebugElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                DotThemeSelectorDropdownComponent,
                SearchableDropdownComponent,
                TestHostFilledComponent,
                TestHostEmtpyComponent,
                MockDotSiteSelectorComponent
            ],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: PaginatorService,
                    useValue: {
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
                    }
                },
                {
                    provide: SiteService,
                    useValue: {
                        getCurrentSite() {
                            return of({
                                identifier: '123'
                            });
                        },
                        getSiteById() {
                            return of({
                                identifier: '123'
                            });
                        },
                        currentSite: { identifier: '123' }
                    }
                },
                {
                    provide: DotThemesService,
                    useValue: {
                        get: jasmine.createSpy().and.returnValue(of(mockDotThemes[1]))
                    }
                }
            ],
            imports: [
                FormsModule,
                DotMessagePipe,
                ReactiveFormsModule,
                SearchableDropDownModule,
                DotIconModule,
                BrowserAnimationsModule
            ]
        }).compileComponents();
    });

    describe('basic', () => {
        let component: DotThemeSelectorDropdownComponent;
        let fixture: ComponentFixture<DotThemeSelectorDropdownComponent>;

        beforeEach(() => {
            fixture = TestBed.createComponent(DotThemeSelectorDropdownComponent);

            de = fixture.debugElement;
            paginationService = TestBed.inject(PaginatorService);
            component = fixture.componentInstance;
            spyOn(component, 'propagateChange');
            spyOn(paginationService, 'get').and.callThrough();
            fixture.detectChanges();
        });

        describe('html', () => {
            it('should set themes if theme selector is open', fakeAsync(() => {
                component.searchableDropdown.display.emit();
                tick();
                expect(component.totalRecords).toEqual(3);
                expect(component.themes).toEqual(mockDotThemes);
            }));

            it('should call paginatorService get method to System Host on init ', () => {
                expect(paginationService.url).toEqual('v1/themes');
                expect(paginationService.extraParams.get('hostId')).toEqual('SYSTEM_HOST');
                expect(paginationService.get).toHaveBeenCalled();
            });

            it('should not call pagination service if the url is not set', () => {
                //Paginator service is now called at least once at the beginning, that's why it has an url at the very beginning
                component.currentSiteIdentifier = '123';
                paginationService.url = '';
                spyOn(paginationService, 'getWithOffset');
                component.searchableDropdown.pageChange.emit({ first: 0 } as PaginationEvent);
                expect(paginationService.getWithOffset).not.toHaveBeenCalledTimes(1);
            });

            it('should call pagination service if the url is set', () => {
                component.currentSiteIdentifier = '123';
                component.paginatorService.url = 'v1/test';
                spyOn(paginationService, 'getWithOffset').and.callThrough();
                component.searchableDropdown.pageChange.emit({ first: 10 } as PaginationEvent);
                expect(paginationService.getWithOffset).toHaveBeenCalledWith(10);
            });

            it('should set the right attributes', () => {
                const element = de.query(By.css('dot-searchable-dropdown'));

                const instance = element.componentInstance;
                expect(instance.placeholder).toBe('Select Themes');
                expect(element.attributes.overlayWidth).toBe('490px');
                expect(element.attributes.labelPropertyName).toBe('name');
                expect(element.attributes.valuePropertyName).toBe('name');

                component.onShow();
                fixture.detectChanges();
                expect(instance.rows).toBe(5);
            });
        });

        describe('events', () => {
            it('should set value propagate change and toggle the overlay', () => {
                const searchable = de.query(By.css('dot-searchable-dropdown'));
                spyOn(searchable.componentInstance, 'toggleOverlayPanel');
                const value = mockDotThemes[0];

                searchable.triggerEventHandler('switch', { ...value });
                expect(component.value).toEqual(value);
                expect(component.propagateChange).toHaveBeenCalledWith(value.identifier);
                expect(searchable.componentInstance.toggleOverlayPanel).toHaveBeenCalledTimes(1);
            });
        });

        describe('filters', () => {
            beforeEach(() => {
                spyOn(paginationService, 'setExtraParams');
                spyOn(paginationService, 'getWithOffset').and.returnValue(of(mockDotThemes));
                spyOnProperty(paginationService, 'totalRecords').and.returnValue(3);

                const searchableButton = de.query(By.css('dot-searchable-dropdown button'));
                searchableButton.nativeElement.click();
            });

            it('should system to true', () => {
                fixture.detectChanges();
                const siteSelector = de.query(By.css('[data-testId="siteSelector"]'));
                expect(siteSelector.componentInstance.system).toEqual(true);
            });

            it('should update themes, totalRecords and call setExtraParams when site selector change', fakeAsync(() => {
                fixture.detectChanges();
                const siteSelector = de.query(By.css('[data-testId="siteSelector"]'));
                siteSelector.triggerEventHandler('switch', {
                    identifier: '123'
                });
                tick();
                expect(paginationService.setExtraParams).toHaveBeenCalledWith('hostId', '123');
                expect(component.themes).toEqual(mockDotThemes);
                expect(component.totalRecords).toBe(3);
            }));

            it('should update themes, totalRecords and call setExtraParams when search input change', async () => {
                fixture.detectChanges();
                await fixture.whenStable();
                const input = de.query(By.css('[data-testId="searchInput"]')).nativeElement;
                input.value = 'hello';
                const event = new KeyboardEvent('keyup');
                input.dispatchEvent(event);
                await fixture.whenStable();
                expect(paginationService.searchParam).toBe('hello');
                expect(component.themes).toEqual(mockDotThemes);
                expect(component.totalRecords).toBe(3);
            });

            it('should allow keyboad nav on filter Input - ArrowDown', async () => {
                fixture.detectChanges();
                await fixture.whenStable();
                const input = de.query(By.css('[data-testId="searchInput"]')).nativeElement;
                const event = new KeyboardEvent('keyup', { key: 'ArrowDown' });
                input.dispatchEvent(event);
                await fixture.whenStable();
                expect(component.selectedOptionIndex).toBe(1);
                expect(component.selectedOptionValue).toBe(mockDotThemes[1].name);
            });

            it('should allow keyboad nav on filter Input - ArrowUp', async () => {
                fixture.detectChanges();
                await fixture.whenStable();
                const input = de.query(By.css('[data-testId="searchInput"]')).nativeElement;
                const event = new KeyboardEvent('keyup', { key: 'ArrowUp' });
                input.dispatchEvent(event);
                await fixture.whenStable();
                expect(component.selectedOptionIndex).toBe(0);
                expect(component.selectedOptionValue).toBe(mockDotThemes[0].name);
            });

            it('should allow keyboad nav on filter Input - Enter', async () => {
                spyOn(component, 'onChange');
                fixture.detectChanges();
                await fixture.whenStable();
                const input = de.query(By.css('[data-testId="searchInput"]')).nativeElement;
                const event = new KeyboardEvent('keyup', { key: 'Enter' });
                input.dispatchEvent(event);
                await fixture.whenStable();
                expect(component.onChange).toHaveBeenCalledWith(mockDotThemes[0]);
            });
        });
    });

    describe('writeValue', () => {
        let fixture: ComponentFixture<TestHostFilledComponent | TestHostEmtpyComponent>;
        let dotThemesService: DotThemesService;
        let siteService: SiteService;
        let de: DebugElement;

        it('should get theme by id', () => {
            fixture = TestBed.createComponent(TestHostFilledComponent);
            de = fixture.debugElement;
            dotThemesService = TestBed.inject(DotThemesService);
            siteService = TestBed.inject(SiteService);
            spyOn(siteService, 'getSiteById').and.callThrough();
            fixture.detectChanges();

            expect(dotThemesService.get).toHaveBeenCalledOnceWith('123');
            expect(siteService.getSiteById).toHaveBeenCalledWith('test');
            const selector = de.query(By.css('dot-theme-selector-dropdown')).componentInstance;
            expect(selector.value).toEqual(mockDotThemes[1]);
        });

        it('should not get theme when value is empty', () => {
            fixture = TestBed.createComponent(TestHostEmtpyComponent);
            de = fixture.debugElement;
            dotThemesService = TestBed.inject(DotThemesService);
            fixture.detectChanges();
            const selector = de.query(By.css('dot-theme-selector-dropdown')).componentInstance;
            selector.value = null; // Paginator service is called once on init and it sets a default value that we need to clean to test this
            fixture.detectChanges();

            expect(dotThemesService.get).not.toHaveBeenCalled();
            expect(selector.value).toBeNull();
        });
    });
});
