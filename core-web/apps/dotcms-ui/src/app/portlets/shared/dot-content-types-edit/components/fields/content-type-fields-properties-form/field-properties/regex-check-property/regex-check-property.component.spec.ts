import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ReactiveFormsModule, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';

import { DotMessageService } from '@dotcms/data-access';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { RegexCheckPropertyComponent } from './index';

import { FieldProperty } from '../field-properties.model';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.field.properties.validation_regex.label': 'Validation-RegEx',
    'contenttypes.field.properties.validation_regex.values.select': 'Select',
    'contenttypes.field.properties.validation_regex.values.email': 'Email',
    'contenttypes.field.properties.validation_regex.values.numbers_only': 'Numbers only',
    'contenttypes.field.properties.validation_regex.values.letters_only': 'Letters only',
    'contenttypes.field.properties.validation_regex.values.alphanumeric': 'Alphanumeric',
    'contenttypes.field.properties.validation_regex.values.us_zip_code': 'US Zip Code',
    'contenttypes.field.properties.validation_regex.values.us_phone': 'US Phone',
    'contenttypes.field.properties.validation_regex.values.url_pattern': 'URL Pattern',
    'contenttypes.field.properties.validation_regex.values.no_html': 'No HTML'
});

describe('RegexCheckPropertyComponent', () => {
    let spectator: Spectator<RegexCheckPropertyComponent>;

    const createComponent = createComponentFactory({
        component: RegexCheckPropertyComponent,
        imports: [
            ReactiveFormsModule,
            NoopAnimationsModule,
            InputTextModule,
            SelectModule,
            DotMessagePipe,
            DotFieldRequiredDirective
        ],
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
    });

    const defaultGroup = new UntypedFormGroup({
        regexCheck: new UntypedFormControl('')
    });
    const defaultProperty: FieldProperty = {
        name: 'regexCheck',
        value: 'value',
        field: { ...dotcmsContentTypeFieldBasicMock }
    };

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        spectator.component.group = defaultGroup;
        spectator.component.property = defaultProperty;
        spectator.detectChanges();
    });

    it('should have a form', () => {
        const group = new UntypedFormGroup({ regexCheck: new UntypedFormControl(null) });
        spectator.component.group = group;
        spectator.component.property = defaultProperty;
        spectator.detectChanges();

        const divForm = spectator.query('div');
        expect(divForm).toBeTruthy();
        expect(spectator.component.group).toEqual(group);
    });

    it('should have a input', () => {
        const pInput = spectator.query('input[type="text"]');
        expect(pInput).toBeTruthy();
    });

    it('should have a dropDown', () => {
        const pSelect = spectator.debugElement.query(By.css('p-select'));
        expect(pSelect).toBeTruthy();
        expect(spectator.component.regexCheckTemplates).toBe(pSelect?.componentInstance?.options);
    });

    it('should change the input value', () => {
        const pSelect = spectator.debugElement.query(By.css('p-select'));
        expect(pSelect).toBeTruthy();

        pSelect.triggerEventHandler('onChange', {
            value: '^([a-zA-Z0-9]+[a-zA-Z0-9._%+-]*@(?:[a-zA-Z0-9-]+.)+[a-zA-Z]{2,4})$'
        });

        expect(spectator.component.group.get('regexCheck').value).toBe(
            '^([a-zA-Z0-9]+[a-zA-Z0-9._%+-]*@(?:[a-zA-Z0-9-]+.)+[a-zA-Z]{2,4})$'
        );
    });
});
