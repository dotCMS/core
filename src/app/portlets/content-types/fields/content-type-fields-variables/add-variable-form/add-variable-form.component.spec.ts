import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { AddVariableFormComponent } from './add-variable-form.component';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';

describe('AddVariableFormComponent', () => {
    let comp: AddVariableFormComponent;
    let fixture: ComponentFixture<AddVariableFormComponent>;
    let de: DebugElement;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'contenttypes.field.variables.add_button.label': 'Add',
            'contenttypes.field.variables.value_header.label': 'Value',
            'contenttypes.field.variables.key_header.label': 'Key'
        });

        DOTTestBed.configureTestingModule({
            declarations: [AddVariableFormComponent],
            imports: [],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
            ]
        });

        fixture = DOTTestBed.createComponent(AddVariableFormComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        fixture.detectChanges();
    });

    it('should load the component', () => {
        const submitButton = de.query(By.css('button'));
        const labels = fixture.debugElement.queryAll(By.css('label'));

        expect(submitButton.nativeElement.innerText).toBe('Add');
        expect(submitButton.nativeElement.disabled).toBe(true);
        expect(labels[0].nativeElement.innerText).toBe('Key');
        expect(labels[1].nativeElement.innerText).toBe('Value');
    });

    it('should submit form, reset and focus on key field', () => {
        spyOn(comp.keyField.nativeElement, 'focus');
        spyOn(comp.saveVariable, 'emit');

        const param = {
            key: 'key1',
            value: 'value1'
        };
        const submitButton = de.query(By.css('button'));
        comp.form.controls.key.setValue(param.key);
        comp.form.controls.value.setValue(param.value);

        fixture.detectChanges();

        expect(submitButton.nativeElement.disabled).toBe(false);

        submitButton.nativeElement.click();

        expect(comp.saveVariable.emit).toHaveBeenCalledWith(param);
        expect(comp.form.value).toEqual({
            key: '',
            value: ''
        });
        expect(comp.form.pristine).toBe(true);
        expect(comp.keyField.nativeElement.focus).toHaveBeenCalledTimes(1);
    });
});
