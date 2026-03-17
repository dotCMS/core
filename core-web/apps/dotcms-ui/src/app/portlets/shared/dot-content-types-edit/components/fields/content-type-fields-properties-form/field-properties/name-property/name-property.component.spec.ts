import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ReactiveFormsModule, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { InputTextModule } from 'primeng/inputtext';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotAutofocusDirective,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { NamePropertyComponent } from './index';

import { DotCopyLinkComponent } from '../../../../../../../../view/components/dot-copy-link/dot-copy-link.component';
import { FieldProperty } from '../field-properties.model';

const messageServiceMock = new MockDotMessageService({
    'Default-Value': 'Default-Value',
    'contenttypes.field.properties.name.label': 'Name',
    'contenttypes.field.properties.name.error.required': 'Required',
    'contenttypes.field.properties.name.variable': 'Variable'
});

describe('NamePropertyComponent', () => {
    let spectator: Spectator<NamePropertyComponent>;

    const createComponent = createComponentFactory({
        component: NamePropertyComponent,
        imports: [
            ReactiveFormsModule,
            NoopAnimationsModule,
            InputTextModule,
            DotMessagePipe,
            DotSafeHtmlPipe,
            DotFieldRequiredDirective,
            DotAutofocusDirective,
            DotFieldValidationMessageComponent,
            DotCopyLinkComponent
        ],
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
    });

    const defaultProperty: FieldProperty = {
        name: 'name',
        value: 'value',
        field: { ...dotcmsContentTypeFieldBasicMock }
    };

    const defaultGroup = new UntypedFormGroup({
        name: new UntypedFormControl('')
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        spectator.component.group = defaultGroup;
        spectator.component.property = defaultProperty;
        spectator.detectChanges();
    });

    it('should have a form', () => {
        const group = new UntypedFormGroup({
            name: new UntypedFormControl('')
        });
        spectator.component.group = group;
        spectator.detectChanges();
        const divForm = spectator.query('div.field');
        expect(divForm).toBeTruthy();
        expect(spectator.component.group).toEqual(group);
    });

    it('should have a input', () => {
        expect(spectator.query('input[type="text"]')).toBeTruthy();
    });

    it('should have a field-message', () => {
        const fieldValidationMessage = spectator.debugElement.query(
            By.css('dot-field-validation-message')
        );
        expect(fieldValidationMessage).toBeTruthy();
        const nameControl = spectator.component.group.controls['name'];
        expect((fieldValidationMessage.componentInstance as { _field: unknown })._field).toBe(
            nameControl
        );
    });

    it('should focus on input on load using the directive', () => {
        const input = spectator.query('input.name__input');
        expect(input).toBeTruthy();
        expect(input.getAttribute('dotautofocus')).toBeDefined();
    });

    it('should have copy variable button', () => {
        const copySpectator = createComponent({ detectChanges: false });
        copySpectator.component.group = defaultGroup;
        copySpectator.component.property = {
            name: 'name',
            value: 'value',
            field: {
                ...dotcmsContentTypeFieldBasicMock,
                variable: 'thisIsAVar'
            }
        };
        copySpectator.detectChanges();

        const copyEl = copySpectator.debugElement.query(By.css('dot-copy-link'));
        expect(copyEl).toBeTruthy();
        const copyComp = copyEl.componentInstance as { copy: string; label: string };
        expect(copyComp.copy).toBe('thisIsAVar');
        expect(copyComp.label).toBe('thisIsAVar');
    });
});
