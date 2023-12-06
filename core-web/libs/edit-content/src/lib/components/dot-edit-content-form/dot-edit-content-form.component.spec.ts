import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { Validators } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';
import { DotFormatDateService } from '@dotcms/ui';
import { DotFormatDateServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditContentFormComponent } from './dot-edit-content-form.component';

import {
    CONTENT_FORM_DATA_MOCK,
    JUST_FIELDS_MOCKS,
    LAYOUT_FIELDS_VALUES_MOCK,
    MOCK_DATE
} from '../../utils/mocks';
import { DotEditContentFieldComponent } from '../dot-edit-content-field/dot-edit-content-field.component';

describe('DotFormComponent', () => {
    let spectator: Spectator<DotEditContentFormComponent>;
    const createComponent = createComponentFactory({
        component: DotEditContentFormComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    Save: 'Save',
                    Copy: 'Copy'
                })
            },
            { provide: DotFormatDateService, useClass: DotFormatDateServiceMock }
        ]
    });

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
        expect(spectator.component.form.controls['name1'].hasValidator(Validators.required)).toBe(
            true
        );
        expect(spectator.component.form.controls['text2'].hasValidator(Validators.required)).toBe(
            true
        );
        expect(spectator.component.form.controls['text3'].hasValidator(Validators.required)).toBe(
            false
        );
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

    it('should emit the form value through the `formSubmit` event', () => {
        jest.spyOn(spectator.component.formSubmit, 'emit');
        const button = spectator.query(byTestId('button-save'));
        spectator.click(button);

        expect(spectator.component.formSubmit.emit).toHaveBeenCalledWith(LAYOUT_FIELDS_VALUES_MOCK);
    });
});
