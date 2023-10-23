import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditContentFormComponent } from './dot-edit-content-form.component';

import { EditContentFormData } from '../../models/dot-edit-content-form.interface';
import { FIELDS_MOCK, FIELD_MOCK, LAYOUT_MOCK } from '../../utils/mocks';
import { DotEditContentFieldComponent } from '../dot-edit-content-field/dot-edit-content-field.component';

export const VALUES_MOCK = {
    name1: 'Name1',
    text2: 'Text2'
};

export const CONTENT_FORM_DATA_MOCK: EditContentFormData = {
    layout: LAYOUT_MOCK
};

xdescribe('DotFormComponent', () => {
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
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should initialize the form group with form controls for each field in the `formData` array', () => {
        spectator.setInput('formData', CONTENT_FORM_DATA_MOCK);
        spectator.detectChanges();

        expect(spectator.component.form.controls['name1']).toBeDefined();
        expect(spectator.component.form.controls['text2']).toBeDefined();
    });

    it('should initialize a form control for a given DotCMSContentTypeField', () => {
        spectator.setInput('formData', CONTENT_FORM_DATA_MOCK);
        spectator.detectChanges();
        const formControl = spectator.component.initializeFormControl(FIELD_MOCK);

        expect(formControl).toBeDefined();
        expect(formControl.validator).toBeDefined();
    });

    it('should have a default value if is defined', () => {
        spectator.setInput('formData', CONTENT_FORM_DATA_MOCK);
        spectator.detectChanges();

        const formControl = spectator.component.initializeFormControl(FIELDS_MOCK[1]);
        expect(formControl.value).toEqual(FIELDS_MOCK[1].defaultValue);
    });

    it('should render a dot-edit-content-field and pass the field', () => {
        spectator.setInput('formData', CONTENT_FORM_DATA_MOCK);
        spectator.detectComponentChanges();

        expect(spectator.query(DotEditContentFieldComponent)).toBeDefined();
        expect(spectator.query(DotEditContentFieldComponent).field).toBeTruthy();
    });

    it('should emit the form value through the `formSubmit` event', () => {
        spectator.setInput('formData', CONTENT_FORM_DATA_MOCK);
        spectator.detectChanges();

        jest.spyOn(spectator.component.formSubmit, 'emit');
        const button = spectator.query(byTestId('button-save'));
        spectator.click(button);

        expect(spectator.component.formSubmit.emit).toHaveBeenCalledWith(
            spectator.component.form.value
        );
    });
});
