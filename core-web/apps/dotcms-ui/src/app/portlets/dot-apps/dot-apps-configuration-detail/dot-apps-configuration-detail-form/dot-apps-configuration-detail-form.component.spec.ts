import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';
import { MarkdownService } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { FormGroupDirective, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { Dropdown, DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { TooltipModule } from 'primeng/tooltip';

import { DotFieldRequiredDirective, DotIconModule } from '@dotcms/ui';

import { DotAppsConfigurationDetailFormComponent } from './dot-apps-configuration-detail-form.component';

const secrets = [
    {
        dynamic: false,
        name: 'name',
        hidden: false,
        hint: 'This is a Name',
        label: 'Name:',
        required: true,
        type: 'STRING',
        value: 'test',
        hasEnvVar: false,
        envShow: true,
        hasEnvVarValue: false
    },
    {
        dynamic: false,
        name: 'password',
        hidden: true,
        hint: 'This is a password',
        label: 'Password:',
        required: true,
        type: 'STRING',
        value: '****',
        hasEnvVar: false,
        envShow: true,
        hasEnvVarValue: false
    },
    {
        dynamic: false,
        name: 'enabled',
        hidden: false,
        hint: 'This is Enabled!',
        label: 'Enabled:',
        required: false,
        type: 'BOOL',
        value: 'true',
        hasEnvVar: false,
        envShow: true,
        hasEnvVarValue: false
    },
    {
        dynamic: false,
        name: 'select',
        hidden: false,
        hint: 'This is Select!',
        label: 'Select label:',
        options: [
            {
                label: 'uno',
                value: '1'
            },
            {
                label: 'dos',
                value: '2'
            }
        ],
        required: true,
        type: 'SELECT',
        value: '1',
        hasEnvVar: false,
        envShow: true,
        hasEnvVarValue: false
    },
    {
        dynamic: false,
        name: 'integration',
        hidden: false,
        hint: 'This is Integration!',
        label: 'Integration:',
        required: false,
        type: 'BUTTON',
        value: 'urlLink',
        hasEnvVar: false,
        envShow: true,
        hasEnvVarValue: false
    }
];

const formState = {
    name: secrets[0].value,
    password: secrets[1].value,
    enabled: JSON.parse(secrets[2].value),
    select: secrets[3].options[0].value,
    integration: secrets[4].value
};

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'markdown',
    template: `
        <ng-content></ng-content>
    `
})
class MockMarkdownComponent {}

describe('DotAppsConfigurationDetailFormComponent', () => {
    let spectator: Spectator<DotAppsConfigurationDetailFormComponent>;
    const createComponent = createComponentFactory({
        component: DotAppsConfigurationDetailFormComponent,
        imports: [
            HttpClientTestingModule,
            ButtonModule,
            CommonModule,
            CheckboxModule,
            DropdownModule,
            DotIconModule,
            InputTextareaModule,
            InputTextModule,
            ReactiveFormsModule,
            TooltipModule,
            DotFieldRequiredDirective
        ],
        providers: [MarkdownService, FormGroupDirective],
        declarations: [MockMarkdownComponent]
    });

    describe('Without warnings', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    formFields: secrets
                }
            });
            spectator.detectChanges();
        });

        it('should load form components', () => {
            const fields = spectator.queryAll('.field');
            expect(fields.length).toBe(secrets.length);
        });

        it('should not have warning icon', () => {
            const element = spectator.query('dot-icon');
            expect(element).toBeFalsy();
        });

        it('should focus the first form field after view init', async () => {
            spectator.detectComponentChanges();
            await spectator.fixture.whenStable();

            const field = spectator.component.formFields[0];
            const firstFormField = spectator.query<HTMLInputElement>(`#${field.name}`);
            spyOn(firstFormField, 'focus');
            spectator.component.ngAfterViewInit();
            expect(firstFormField.focus).toHaveBeenCalled();
            expect(document.activeElement).toEqual(firstFormField);
        });

        it('should load Label, Textarea & Hint with right attributes', () => {
            const row = spectator.query(byTestId('name'));

            const markdownElement = row.querySelector('markdown');
            expect(markdownElement).toBeTruthy();

            const field = secrets[0];

            const labelElement = row.querySelector('label');
            expect(labelElement.textContent.trim()).toBe(field.label);
            expect(labelElement.classList).toContain('p-label-input-required');

            const textareaElement = row.querySelector('textarea');
            expect(textareaElement.getAttribute('id')).toBe(field.name);
            expect(textareaElement.getAttribute('autoResize')).toBe('autoResize');
            expect(textareaElement.value).toBe(field.value);

            const hintElement = row.querySelector('.p-field-hint');
            expect(hintElement.textContent).toBe(field.hint);
        });

        it('should load Checkbox & Hint with right attributes', () => {
            const row = spectator.query(byTestId('enabled'));

            const markdownElement = row.querySelector('markdown');
            expect(markdownElement).toBeTruthy();

            const field = secrets[2];

            const checkboxElement = row.querySelector('p-checkbox');
            expect(checkboxElement.getAttribute('id')).toBe(field.name);

            const labelElement = checkboxElement.querySelector('label');
            expect(labelElement.textContent).toContain(field.label);

            const inputElement = row.querySelector('input');
            expect(inputElement.value).toBe(field.value);

            const hintElement = row.querySelector('.p-field-hint');
            expect(hintElement.textContent).toBe(field.hint);
        });

        it('should load Label, Select & Hint with right attributes', () => {
            const row = spectator.query(byTestId('select'));

            const markdownElement = row.querySelector('markdown');
            expect(markdownElement).toBeTruthy();

            const field = secrets[3];

            const labelElement = row.querySelector('label');
            expect(labelElement.textContent.trim()).toBe(field.label);

            const dropdownComponent = spectator.query(Dropdown);
            expect(dropdownComponent.id).toBe(field.name);
            expect(dropdownComponent.options).toBe(field.options);
            expect(dropdownComponent.value).toBe(field.value);

            const hintElement = row.querySelector('.p-field-hint');
            expect(hintElement.textContent).toBe(field.hint);
        });

        it('should load Label, Button & Hint with right attributes', () => {
            const row = spectator.query(byTestId('integration'));

            const field = secrets[4];

            const labelElement = row.querySelector('label');
            expect(labelElement.textContent.trim()).toBe(field.label);

            const buttonElement = row.querySelector('button');
            expect(buttonElement.id).toBe(field.name);

            const hintElement = row.querySelector('.form__group-hint');
            expect(hintElement.textContent).toBe(field.hint);
        });

        it('should Button be disabled when no configured app', () => {
            const row = spectator.query(byTestId('integration'));
            const buttonElement = row.querySelector('button');
            expect(buttonElement.disabled).toBe(true);
        });

        it('should Button open link on new tab when clicked on a configured app', () => {
            spectator.setInput('appConfigured', true);
            spectator.detectChanges();

            const field = secrets[4];

            const openMock = jasmine.createSpy();
            window.open = openMock;
            const row = spectator.query(byTestId('integration'));
            const buttonElement = row.querySelector('button');

            buttonElement.click();
            expect(openMock).toHaveBeenCalledWith(field.value, '_blank');
        });

        it('should emit form state when loaded', async () => {
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            spectator.output('data').subscribe((data) => {
                expect(data).toEqual(formState);
            });

            spectator.output('valid').subscribe((data) => {
                expect(data).toEqual(true);
            });
        });

        it('should emit form state when value changed', () => {
            const spyDataOutput = spyOn(spectator.component.data, 'emit');
            const spyValidOutput = spyOn(spectator.component.valid, 'emit');

            spectator.component.myFormGroup.get('name').setValue('Test2');
            spectator.component.myFormGroup.get('password').setValue('Password2');
            spectator.component.myFormGroup.get('enabled').setValue('false');

            expect(spyDataOutput).toHaveBeenCalledTimes(3);
            expect(spyValidOutput).toHaveBeenCalledTimes(3);
        });

        it('should emit form state disabled when required field empty', () => {
            const spyValidOutput = spyOn(spectator.component.valid, 'emit');

            spectator.component.myFormGroup.get('name').setValue('');
            expect(spyValidOutput).toHaveBeenCalledWith(false);
        });
    });

    describe('With warnings', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    formFields: secrets.map((item, i) => {
                        if (i < 3) {
                            return {
                                ...item,
                                warnings: [`error ${i}`]
                            };
                        }

                        return item;
                    })
                }
            });
            spectator.detectChanges();
        });

        it('should have warning icons', () => {
            const warningIcons = spectator.queryAll('dot-icon');
            const formFields = spectator.component.formFields;

            expect(warningIcons[0].getAttribute('name')).toBe('warning');
            expect(warningIcons[0].getAttribute('size')).toBe('18');
            expect(warningIcons[0].getAttribute('ng-reflect-content')).toBe(
                formFields[0].warnings[0]
            );

            expect(warningIcons[1].getAttribute('name')).toBe('warning');
            expect(warningIcons[1].getAttribute('size')).toBe('18');
            expect(warningIcons[1].getAttribute('ng-reflect-content')).toBe(
                formFields[1].warnings[0]
            );

            expect(warningIcons[2].getAttribute('name')).toBe('warning');
            expect(warningIcons[2].getAttribute('size')).toBe('18');
            expect(warningIcons[2].getAttribute('ng-reflect-content')).toBe(
                formFields[2].warnings[0]
            );
        });
    });
});
