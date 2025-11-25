import { byTestId, createComponentFactory, Spectator, mockProvider } from '@ngneat/spectator/jest';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Validators } from '@angular/forms';

import { TabView } from 'primeng/tabview';

import { DotMessageService, DotFormatDateService } from '@dotcms/data-access';
import { DotFormatDateServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditContentFormComponent } from './dot-edit-content-form.component';

import { EditContentPayload } from '../../models/dot-edit-content-form.interface';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import {
    CONSTANT_FIELD_MOCK,
    CONTENT_FORM_DATA_MOCK,
    HIDDEN_FIELD_MOCK,
    JUST_FIELDS_MOCKS,
    LAYOUT_FIELDS_VALUES_MOCK,
    LAYOUT_MOCK,
    MOCK_DATE,
    MockResizeObserver,
    TAB_DIVIDER_MOCK
} from '../../utils/mocks';
import { DotEditContentFieldComponent } from '../dot-edit-content-field/dot-edit-content-field.component';

describe('DotFormComponent', () => {
    let spectator: Spectator<DotEditContentFormComponent>;
    let dotMessageService: DotMessageService;
    const createComponent = createComponentFactory({
        component: DotEditContentFormComponent,
        imports: [HttpClientTestingModule],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    Save: 'Save',
                    Content: 'content'
                })
            },
            { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
            mockProvider(DotEditContentService)
        ]
    });

    describe('with data', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    formData: CONTENT_FORM_DATA_MOCK
                }
            });
        });

        it('should initialize the form controls', () => {
            expect(spectator.component.form.value).toEqual({
                name1: 'Placeholder',
                text2: null,
                text3: null,
                someTag: ['some', 'tags', 'separated', 'by', 'comma'],
                date: new Date(MOCK_DATE)
            });
        });

        it('should initialize the form validators', () => {
            expect(
                spectator.component.form.controls['name1'].hasValidator(Validators.required)
            ).toBe(true);
            expect(
                spectator.component.form.controls['text2'].hasValidator(Validators.required)
            ).toBe(true);
            expect(
                spectator.component.form.controls['text3'].hasValidator(Validators.required)
            ).toBe(false);
        });

        it('should validate regex', () => {
            expect(spectator.component.form.controls['text2'].valid).toBeFalsy();

            spectator.component.form.controls['text2'].setValue('dot@gmail.com');
            expect(spectator.component.form.controls['text2'].valid).toBeTruthy();
        });

        it('should have 1 row, 2 columns and 3 fields', () => {
            expect(spectator.queryAll(byTestId('row'))).toHaveLength(1);
            expect(spectator.queryAll(byTestId('column'))).toHaveLength(2);
            expect(spectator.queryAll(byTestId('field'))).toHaveLength(5);
        });

        it('should pass field to attr to dot-edit-content-field', () => {
            const fields = spectator.queryAll(DotEditContentFieldComponent);
            JUST_FIELDS_MOCKS.forEach((field, index) => {
                expect(fields[index].field).toEqual(field);
                expect(fields[index].contentType).toEqual(
                    CONTENT_FORM_DATA_MOCK.contentType.variable
                );
                expect(fields[index].contentlet).toEqual(CONTENT_FORM_DATA_MOCK.contentlet);
            });
        });

        it('should emit the form value through the `formSubmit` event', () => {
            jest.spyOn(spectator.component.changeValue, 'emit');

            spectator.component.form.controls['name1'].setValue('New Value');

            expect(spectator.component.changeValue.emit).toHaveBeenCalledWith({
                ...LAYOUT_FIELDS_VALUES_MOCK,
                name1: 'New Value'
            });
        });

        it('should not have multiple tabs', () => {
            const tabViewComponent = spectator.query(TabView);
            const formRow = spectator.query(byTestId('row'));
            expect(tabViewComponent).toBeNull();
            expect(formRow).toExist();
        });

        it('should initialize the form controls', () => {
            expect(spectator.component.form.value).toEqual({
                name1: 'Placeholder',
                text2: null,
                text3: null,
                someTag: ['some', 'tags', 'separated', 'by', 'comma'],
                date: new Date(MOCK_DATE)
            });
        });

        it('should initialize the form validators', () => {
            expect(
                spectator.component.form.controls['name1'].hasValidator(Validators.required)
            ).toBe(true);
            expect(
                spectator.component.form.controls['text2'].hasValidator(Validators.required)
            ).toBe(true);
            expect(
                spectator.component.form.controls['text3'].hasValidator(Validators.required)
            ).toBe(false);
        });

        it('should validate regex', () => {
            expect(spectator.component.form.controls['text2'].valid).toBeFalsy();

            spectator.component.form.controls['text2'].setValue('dot@gmail.com');
            expect(spectator.component.form.controls['text2'].valid).toBeTruthy();
        });

        it('should have 1 row, 2 columns and 3 fields', () => {
            expect(spectator.queryAll(byTestId('row'))).toHaveLength(1);
            expect(spectator.queryAll(byTestId('column'))).toHaveLength(2);
            expect(spectator.queryAll(byTestId('field'))).toHaveLength(5);
        });

        it('should pass field to attr to dot-edit-content-field', () => {
            const fields = spectator.queryAll(DotEditContentFieldComponent);
            JUST_FIELDS_MOCKS.forEach((field, index) => {
                expect(fields[index].field).toEqual(field);
            });
        });
    });

    describe('with  Constant field', () => {
        const data: EditContentPayload = { ...CONTENT_FORM_DATA_MOCK };

        data.contentType.fields = [
            ...data.contentType.fields,
            CONSTANT_FIELD_MOCK,
            HIDDEN_FIELD_MOCK
        ];
        data.contentlet = {
            ...data.contentlet,
            constant: 'constant-value',
            hidden: 'hidden-value'
        };

        data.contentType.layout[0].columns[0].fields.push(CONSTANT_FIELD_MOCK);
        data.contentType.layout[0].columns[0].fields.push(HIDDEN_FIELD_MOCK);

        beforeEach(() => {
            spectator = createComponent({
                props: {
                    formData: data
                }
            });
        });

        it('should not include constant / hidden fields in the form or render it', () => {
            expect(spectator.component.form.value).toEqual({
                name1: 'Placeholder',
                text2: null,
                text3: null,
                someTag: ['some', 'tags', 'separated', 'by', 'comma'],
                date: new Date(MOCK_DATE)
            });

            expect(spectator.queryAll(DotEditContentFieldComponent).length).toEqual(5);
        });
    });

    describe('with data and multiple tabs', () => {
        const originalResizeObserver = window.ResizeObserver;

        beforeEach(() => {
            spectator = createComponent({
                detectChanges: false,
                props: {
                    formData: {
                        ...CONTENT_FORM_DATA_MOCK,
                        contentType: {
                            ...CONTENT_FORM_DATA_MOCK.contentType,
                            layout: [...LAYOUT_MOCK, TAB_DIVIDER_MOCK]
                        }
                    }
                }
            });
            dotMessageService = spectator.inject(DotMessageService, true);

            window.ResizeObserver = MockResizeObserver;
        });

        it('should have a p-tabView', () => {
            jest.spyOn(dotMessageService, 'get');
            spectator.detectChanges();
            const tabViewComponent = spectator.query(TabView);
            expect(tabViewComponent.scrollable).toBeTruthy();
            expect(tabViewComponent).toExist();
            expect(dotMessageService.get).toHaveBeenCalled();
        });

        afterEach(() => {
            window.ResizeObserver = originalResizeObserver;
        });
    });
});
