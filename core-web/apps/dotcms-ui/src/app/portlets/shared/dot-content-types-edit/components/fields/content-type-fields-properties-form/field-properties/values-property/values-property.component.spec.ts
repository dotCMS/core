import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Component, forwardRef, Input } from '@angular/core';
import {
    ControlValueAccessor,
    NG_VALUE_ACCESSOR,
    NgControl,
    ReactiveFormsModule,
    UntypedFormControl,
    UntypedFormGroup
} from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSClazzes } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { ValuesPropertyComponent } from './index';

import { DotFieldHelperComponent } from '../../../../../../../../view/components/dot-field-helper/dot-field-helper.component';

@Component({
    selector: 'dot-field-validation-message',
    template: '',
    standalone: false
})
class TestFieldValidationMessageComponent {
    @Input() field: NgControl;
    @Input() message: string;
}

@Component({
    selector: 'dot-textarea-content',
    template: '',
    standalone: false,
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotTextareaContentMockComponent)
        }
    ]
})
class DotTextareaContentMockComponent implements ControlValueAccessor {
    @Input() show: string[];
    @Input() height: string;

    propagateChange = (_: unknown) => {
        //
    };

    registerOnChange(fn: () => void): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {
        //
    }

    writeValue(): void {
        // no-op
    }
}

describe('ValuesPropertyComponent', () => {
    let spectator: Spectator<ValuesPropertyComponent>;

    const messageServiceMock = new MockDotMessageService({
        'Validation-RegEx': 'Validation-RegEx'
    });

    const defaultGroup = new UntypedFormGroup({
        values: new UntypedFormControl('')
    });

    const defaultProperty = {
        name: 'values',
        value: 'value',
        field: { ...dotcmsContentTypeFieldBasicMock }
    };

    const createComponent = createComponentFactory({
        component: ValuesPropertyComponent,
        declarations: [TestFieldValidationMessageComponent, DotTextareaContentMockComponent],
        imports: [ReactiveFormsModule, DotFieldHelperComponent, DotSafeHtmlPipe, DotMessagePipe],
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        spectator.component.group = defaultGroup;
        spectator.component.property = defaultProperty;
        spectator.component.helpText = 'Helper Text';
        spectator.detectChanges();
    });

    it('should have a form', () => {
        const group = new UntypedFormGroup({
            values: new UntypedFormControl('')
        });
        spectator.component.group = group;
        spectator.detectChanges();
        const divForm = spectator.query('div.flex.flex-col');
        expect(divForm).toBeTruthy();
        expect(spectator.component.group).toEqual(group);
    });

    it('should have a field-message', () => {
        const fieldValidationMessage = spectator.debugElement.query(
            By.css('dot-field-validation-message')
        );
        expect(fieldValidationMessage).toBeTruthy();
        expect(fieldValidationMessage.componentInstance.field).toBe(
            spectator.component.group.controls['values']
        );
    });

    it('should have value field', () => {
        const valueField = spectator.query('dot-textarea-content');
        expect(valueField).toBeTruthy();
    });

    it('should have value component with the right options', () => {
        expect(spectator.component.value?.show).toEqual(['code']);
        expect(spectator.component.value?.height).toBe('15.7rem');
    });

    it('should show dot-helper for required clazz', () => {
        const helperSpectator = createComponent({ detectChanges: false });
        helperSpectator.component.group = defaultGroup;
        helperSpectator.component.property = {
            ...defaultProperty,
            field: {
                ...defaultProperty.field,
                clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField'
            }
        };
        helperSpectator.component.helpText = 'Helper Text';
        helperSpectator.detectChanges();

        const fieldHelper = helperSpectator.query('dot-field-helper');
        expect(fieldHelper).toBeTruthy();
    });

    it('should hide dot-helper except for required', () => {
        spectator.component.property = {
            ...defaultProperty,
            field: { ...defaultProperty.field, clazz: DotCMSClazzes.TEXT }
        };
        spectator.fixture.detectChanges(false);

        const fieldHelper = spectator.query('dot-field-helper');
        expect(fieldHelper).toBeNull();
    });
});
