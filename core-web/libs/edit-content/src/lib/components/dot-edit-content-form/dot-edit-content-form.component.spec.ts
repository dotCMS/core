import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditContentFormComponent } from './dot-edit-content-form.component';

import { FIELDS_MOCK, FIELD_MOCK, LAYOUT_MOCK } from '../../utils/mocks';
import { DotEditContentFieldComponent } from '../dot-edit-content-field/dot-edit-content-field.component';

describe('DotFormComponent', () => {
    let spectator: Spectator<DotEditContentFormComponent>;
    const createComponent = createComponentFactory({
        component: DotEditContentFormComponent,
        imports: [
            DotEditContentFieldComponent,
            CommonModule,
            ReactiveFormsModule,
            ButtonModule,
            DotMessagePipe
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    Save: 'Save'
                })
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                formData: LAYOUT_MOCK
            }
        });
    });

    describe('initilizeForm', () => {
        it('should initialize the form group with form controls for each field in the `formData` array', () => {
            const component = spectator.component;
            component.formData = LAYOUT_MOCK;
            spectator.detectChanges();

            expect(component.form.controls['name1']).toBeDefined();
            expect(component.form.controls['text2']).toBeDefined();
        });
    });

    describe('initializeFormControl', () => {
        it('should initialize a form control for a given DotCMSContentTypeField', () => {
            const formControl = spectator.component.initializeFormControl(FIELD_MOCK);

            expect(formControl).toBeDefined();
            expect(formControl.validator).toBeDefined();
        });

        it('should have a default value if is defined', () => {
            const formControl = spectator.component.initializeFormControl(FIELDS_MOCK[1]);
            expect(formControl.value).toEqual(FIELDS_MOCK[1].defaultValue);
        });
    });

    describe('saveContent', () => {
        it('should emit the form value through the `formSubmit` event', () => {
            const component = spectator.component;
            component.formData = LAYOUT_MOCK;
            component.initilizeForm();

            jest.spyOn(component.formSubmit, 'emit');
            const button = spectator.query(byTestId('button-save'));
            spectator.click(button);

            expect(component.formSubmit.emit).toHaveBeenCalledWith(component.form.value);
        });
    });
});
