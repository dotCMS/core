/* eslint-disable @typescript-eslint/no-explicit-any */

import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { Component } from '@angular/core';
import { ReactiveFormsModule, UntypedFormControl, Validators } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotFieldValidationMessageComponent } from './dot-field-validation-message.component';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

const messageServiceMock = new MockDotMessageService({
    'contentType.errors.input.maxlength': 'Value must be no more than {0} characters',
    'error.form.validator.maxlength': 'Max length error',
    'error.form.validator.required': 'Required error',
    'error.form.validator.pattern': 'Pattern error',
    'contentType.form.variable.placeholder': 'Will be auto-generated if left empty'
});

@Component({ selector: 'dot-custom-host', template: '', standalone: false })
class CustomHostComponent {
    defaultMessage = 'Required';
    control = new UntypedFormControl('', [Validators.required, Validators.pattern(/^.+\..+$/)]);
}

describe('FieldValidationComponent', () => {
    let spectator: SpectatorHost<DotFieldValidationMessageComponent, CustomHostComponent>;
    const createHost = createHostFactory({
        component: DotFieldValidationMessageComponent,
        host: CustomHostComponent,
        imports: [ReactiveFormsModule, DotMessagePipe],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    describe('Using validators messages', () => {
        beforeEach(() => {
            spectator = createHost(
                `
        <input [formControl]="control" />
        <dot-field-validation-message [field]="control"></dot-field-validation-message>`
            );
        });

        it('should hide the message by default', () => {
            expect(spectator.queryHost(byTestId('error-msg'))).not.toExist();
        });

        it('should hide the message when field it is valid', () => {
            expect(spectator.hostComponent.control.valid).toBe(false);
            spectator.hostComponent.control.setValue('match-pattern.js');

            expect(spectator.hostComponent.control.valid).toBe(true);
            expect(spectator.queryHost(byTestId('error-msg'))).not.toExist();
        });

        it('should show the default message when field it is dirty and invalid', () => {
            expect(spectator.hostComponent.control.valid).toBe(false);
            expect(spectator.hostComponent.control.dirty).toBe(false);

            spectator.hostComponent.control.setValue('');
            spectator.hostComponent.control.markAsDirty();

            spectator.detectComponentChanges();

            expect(spectator.hostComponent.control.valid).toBe(false);
            expect(spectator.hostComponent.control.dirty).toBe(true);

            expect(spectator.queryHost(byTestId('error-msg'))).toExist();
        });

        it('should show the message when field has not valid pattern', () => {
            expect(spectator.hostComponent.control.valid).toBe(false);
            expect(spectator.hostComponent.control.dirty).toBe(false);

            spectator.hostComponent.control.setValue('not match pattern');
            spectator.hostComponent.control.markAsDirty();

            spectator.detectComponentChanges();

            expect(spectator.hostComponent.control.valid).toBe(false);
            expect(spectator.hostComponent.control.dirty).toBe(true);

            expect(spectator.queryHost(byTestId('error-msg'))).toExist();
            expect(spectator.queryHost(byTestId('error-msg'))).toContainText('Pattern error');
        });
    });

    describe('Using default message', () => {
        beforeEach(() => {
            spectator = createHost(
                `
        <input [formControl]="control" />
        <dot-field-validation-message [field]="control" [message]="defaultMessage"></dot-field-validation-message>`
            );
        });

        it('should show the message Input() when field it is dirty and invalid', () => {
            expect(spectator.hostComponent.control.valid).toBe(false);
            expect(spectator.hostComponent.control.dirty).toBe(false);

            spectator.hostComponent.control.setValue('');
            spectator.hostComponent.control.markAsDirty();

            spectator.detectComponentChanges();

            expect(spectator.hostComponent.control.valid).toBe(false);
            expect(spectator.hostComponent.control.dirty).toBe(true);

            expect(spectator.queryHost(byTestId('error-msg'))).toExist();
            expect(spectator.queryHost(byTestId('error-msg'))).toContainText('Required');
        });
    });
});
