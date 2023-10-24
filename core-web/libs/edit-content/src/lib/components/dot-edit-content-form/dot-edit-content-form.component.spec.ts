import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { Validators } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditContentFormComponent } from './dot-edit-content-form.component';

import { EditContentFormData } from '../../models/dot-edit-content-form.interface';
import { JUST_FIELDS_MOCKS, LAYOUT_MOCK } from '../../utils/mocks';
import { DotEditContentFieldComponent } from '../dot-edit-content-field/dot-edit-content-field.component';

export const VALUES_MOCK = {
    name1: 'Name1',
    text2: 'Text2'
};

export const CONTENT_FORM_DATA_MOCK: EditContentFormData = {
    layout: LAYOUT_MOCK
};

describe('DotFormComponent', () => {
    let spectator: Spectator<DotEditContentFormComponent>;
    const createComponent = createComponentFactory({
        component: DotEditContentFormComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    Save: 'Save'
                })
            }
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
                text2: undefined,
                text3: undefined
            });
        });

        it('should initialize the form validators', () => {
            expect(
                spectator.component.form.controls['name1'].hasValidator(Validators.required)
            ).toBe(true);
            expect(
                spectator.component.form.controls['text2'].hasValidator(Validators.required)
            ).toBe(true);
            // const regex = new RegExp('^([a-zA-Z0-9]+[a-zA-Z0-9._%+-]*@(?:[a-zA-Z0-9-]+.)+[a-zA-Z]{2,4})$')
            // expect(spectator.component.form.controls['text2'].hasValidator(Validators.pattern(regex))).toBe(true);
            expect(
                spectator.component.form.controls['text3'].hasValidator(Validators.required)
            ).toBe(false);
        });

        it('should have 1 row, 2 columns and 3 fields', () => {
            expect(spectator.queryAll(byTestId('row'))).toHaveLength(1);
            expect(spectator.queryAll(byTestId('column'))).toHaveLength(2);
            expect(spectator.queryAll(byTestId('field'))).toHaveLength(3);
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

            expect(spectator.component.formSubmit.emit).toHaveBeenCalledWith(
                spectator.component.form.value
            );
        });
    });

    describe('no data', () => {
        beforeEach(() => {
            spectator = createComponent({});
        });

        it('should have form undefined', () => {
            jest.spyOn(spectator.component, 'initilizeForm');
            expect(spectator.component.form).toEqual(undefined);
            expect(spectator.component.initilizeForm).not.toHaveBeenCalled();
        });
    });
});
