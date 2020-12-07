import { DebugElement, forwardRef, Input } from '@angular/core';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
    ControlValueAccessor,
    FormBuilder,
    FormGroup,
    FormsModule,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule
} from '@angular/forms';
import { of } from 'rxjs';

import { SiteService } from 'dotcms-js';

import { DotThemeSelectorDropdownComponent } from './dot-theme-selector-dropdown.component';
import { DotThemesService } from '@services/dot-themes/dot-themes.service';
import { PaginatorService } from '@services/paginator';
import { By } from '@angular/platform-browser';
import { mockDotThemes } from '@tests/dot-themes.mock';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

const messageServiceMock = new MockDotMessageService({
    'dot.common.select.themes': 'Select Themes',
    'Last-Updated': 'Last updated'
});
@Component({
    selector: 'dot-searchable-dropdown',
    template: ``,
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TestSearchableComponent)
        }
    ]
})
class TestSearchableComponent implements ControlValueAccessor {
    @Input() placeholder;
    @Input() data;
    @Input() rows;
    @Input() externalItemListTemplate;
    @Input() totalRecords = [...mockDotThemes].length;

    toggleOverlayPanel = jasmine.createSpy();

    propagateChange = (_: any) => {};
    writeValue(): void {}
    registerOnChange(): void {}
    registerOnTouched(): void {}
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
    form: FormGroup;

    constructor(private fb: FormBuilder) {
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
    form: FormGroup;

    constructor(private fb: FormBuilder) {
        this.form = this.fb.group({
            theme: ''
        });
    }
}

describe('DotThemeSelectorDropdownComponent', () => {
    let paginationService: PaginatorService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                DotThemeSelectorDropdownComponent,
                TestSearchableComponent,
                TestHostFilledComponent,
                TestHostEmtpyComponent
            ],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: PaginatorService,
                    useValue: {
                        url: '',
                        paginationPerPage: '',
                        totalRecords: mockDotThemes.length,

                        setExtraParams() {},
                        getWithOffset() {
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
                        }
                    }
                },
                {
                    provide: DotThemesService,
                    useValue: {
                        get: jasmine.createSpy().and.returnValue(of(mockDotThemes[1]))
                    }
                }
            ],
            imports: [FormsModule, DotMessagePipeModule, ReactiveFormsModule]
        }).compileComponents();
    });

    describe('basic', () => {
        let component: DotThemeSelectorDropdownComponent;
        let fixture: ComponentFixture<DotThemeSelectorDropdownComponent>;

        beforeEach(() => {
            fixture = TestBed.createComponent(DotThemeSelectorDropdownComponent);
            paginationService = TestBed.inject(PaginatorService);
            component = fixture.componentInstance;
            spyOn(component, 'propagateChange');
            fixture.detectChanges();
        });

        describe('html', () => {
            it('should pass themes', () => {
                const searchable = fixture.debugElement.query(By.css('dot-searchable-dropdown'))
                    .componentInstance;
                expect(searchable.data).toEqual(mockDotThemes);
            });

            it('shoud set the right attributes', () => {
                const element = fixture.debugElement.query(By.css('dot-searchable-dropdown'));
                const instance = element.componentInstance;

                expect(instance.totalRecords).toBe(3);
                expect(instance.placeholder).toBe('Select Themes');
                expect(instance.rows).toBe(5);
                expect(instance.data).toEqual([...mockDotThemes]);
                expect(element.attributes.overlayWidth).toBe('350px');
                expect(element.attributes.labelPropertyName).toBe('name');
                expect(element.attributes.valuePropertyName).toBe('name');
            });
        });

        describe('events', () => {
            it('should call filterChange with right values', () => {
                paginationService.totalRecords = 5;
                const searchable = fixture.debugElement.query(By.css('dot-searchable-dropdown'));
                const arr = [mockDotThemes[1], mockDotThemes[4]];
                spyOn(paginationService, 'getWithOffset').and.returnValue(of([...arr]));
                searchable.triggerEventHandler('filterChange', 'test');

                expect(paginationService.getWithOffset).toHaveBeenCalledWith(0);
                expect(paginationService.searchParam).toEqual('test');
                expect(component.themes).toEqual(arr);
                expect(paginationService.totalRecords).toEqual(5);
            });

            it('should do something with change', () => {
                const searchable = fixture.debugElement.query(By.css('dot-searchable-dropdown'));
                const value = mockDotThemes[0];

                searchable.triggerEventHandler('change', { ...value });
                expect(component.value).toEqual(value);
                expect(component.propagateChange).toHaveBeenCalledWith(value.identifier);
                expect(searchable.componentInstance.toggleOverlayPanel).toHaveBeenCalledTimes(1);
            });
        });
    });

    describe('writeValue', () => {
        let fixture: ComponentFixture<TestHostFilledComponent | TestHostEmtpyComponent>;
        let dotThemesService: DotThemesService;
        let de: DebugElement;

        it('should get theme by id', () => {
            fixture = TestBed.createComponent(TestHostFilledComponent);
            de = fixture.debugElement;
            dotThemesService = TestBed.inject(DotThemesService);
            fixture.detectChanges();

            expect(dotThemesService.get).toHaveBeenCalledOnceWith('123');
            const selector = de.query(By.css('dot-theme-selector-dropdown')).componentInstance;
            expect(selector.value).toEqual(mockDotThemes[1]);
        });

        it('should not get theme when value is empty', () => {
            fixture = TestBed.createComponent(TestHostEmtpyComponent);
            de = fixture.debugElement;
            dotThemesService = TestBed.inject(DotThemesService);
            fixture.detectChanges();

            expect(dotThemesService.get).not.toHaveBeenCalled();
            const selector = de.query(By.css('dot-theme-selector-dropdown')).componentInstance;
            expect(selector.value).toBeNull();
        });
    });
});
