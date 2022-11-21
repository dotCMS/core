/* eslint-disable @typescript-eslint/no-explicit-any */

import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ControlValueAccessor, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@pipes/dot-message/dot-message.pipe';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotCategoriesPropertiesComponent } from '@dotcms/app/portlets/dot-categories/dot-categories-create-edit/dot-categories-properties/dot-categories-properties.component';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotEventsService } from '@dotcms/app/api/services/dot-events/dot-events.service';
import { DotCategoriesUtillService } from '@dotcms/app/api/services/dot-categories/dot-categories-utill.service';
import { CoreWebService } from '@dotcms/dotcms-js';
// import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
@Component({
    selector: 'dot-container-properties',
    template: '<ng-content></ng-content>',
    styleUrls: []
})
export class DotThemeSelectorDropdownMockComponent implements ControlValueAccessor {
    propagateChange = (_: any) => {
        //
    };
    registerOnChange(fn: any): void {
        this.propagateChange = fn;
    }
    registerOnTouched(): void {
        //
    }
    writeValue(): void {
        //
    }
}

const messageServiceMock = new MockDotMessageService({
    'message.categories.create.name': 'Title',
    'message.categories.create.variable': 'Variable',
    'message.categories.create.categoryUniqueKey': 'CategoryUniqueKey',
    'message.categories.create.keywords': 'keywords'
});

fdescribe('DotCategoryPropsComponent', () => {
    let fixture: ComponentFixture<DotCategoriesPropertiesComponent>;
    let component: DotCategoriesPropertiesComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                DotCategoriesPropertiesComponent,
                DotMessagePipe,
                DotThemeSelectorDropdownMockComponent
            ],
            imports: [FormsModule, ReactiveFormsModule, DotFieldValidationMessageModule],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DynamicDialogRef,
                    useValue: {
                        close: jasmine.createSpy()
                    }
                },
                {
                    provide: DynamicDialogConfig,
                    useValue: {
                        data: {
                            template: {
                                title: '',
                                friendlyName: '',
                                theme: '',
                                image: ''
                            },
                            onSave: jasmine.createSpy(),
                            onCancel: jasmine.createSpy()
                        }
                    }
                },
                DotCategoriesUtillService,
                {
                    provide: CoreWebService,
                    useClass: CoreWebServiceMock
                },
                DotGlobalMessageService,
                DotEventsService
            ]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotCategoriesPropertiesComponent);
        // de = fixture.debugElement;
        component = fixture.componentInstance;
        // dialogConfig = TestBed.inject(DynamicDialogConfig);
        // dialogRef = TestBed.inject(DynamicDialogRef);

        fixture.detectChanges();
    });

    describe('form', () => {
        it('should get value from config', () => {
            expect(component.form.value).toEqual({
                title: '',
                variable: '',
                categoryUniqueKey: '',
                keywords: ''
            });
        });

        it('should be invalid by default', () => {
            expect(component.form.valid).toBe(false);
        });

        it('should be valid when required fields are set', () => {
            component.form.get('title').setValue('Hello World');

            expect(component.form.valid).toBe(true);
            expect(component.form.value).toEqual({
                title: 'Hello World',
                variable: '',
                categoryUniqueKey: '',
                keywords: ''
            });
        });
    });
});
