import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { MarkdownComponent } from 'ngx-markdown';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormGroupDirective, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';

import { DotFieldRequiredDirective } from '@dotcms/ui';

import { DotAppsConfigurationDetailFormComponent } from './dot-apps-configuration-detail-form.component';

import { DotAppsConfigurationDetailGeneratedStringFieldComponent } from '../dot-apps-configuration-detail-generated-string-field/dot-apps-configuration-detail-generated-string-field.component';

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
    },
    {
        dynamic: false,
        name: 'generatedString',
        hidden: false,
        hint: 'This is a Generated String field!',
        label: 'Generated String:',
        required: false,
        type: 'GENERATED_STRING',
        value: 'generated-value',
        buttonLabel: 'Generate String',
        buttonEndpoint: '/api/generate-string',
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
    integration: secrets[4].value,
    generatedString: secrets[5].value
};

describe('DotAppsConfigurationDetailFormComponent', () => {
    let spectator: Spectator<DotAppsConfigurationDetailFormComponent>;
    const createComponent = createComponentFactory({
        component: DotAppsConfigurationDetailFormComponent,
        imports: [
            HttpClientTestingModule,
            ReactiveFormsModule,
            ButtonModule,
            CheckboxModule,
            SelectModule,
            InputTextModule,
            TextareaModule,
            DotFieldRequiredDirective,
            TooltipModule,
            MockComponent(DotAppsConfigurationDetailGeneratedStringFieldComponent),
            MockComponent(MarkdownComponent)
        ],
        providers: [FormGroupDirective],
        declarations: []
    });

    describe('Without warnings', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    formFields: secrets
                } as unknown
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

        it('should focus the first form field when form fields are available', async () => {
            // Create component with formFields
            const spectatorWithFields = createComponent({
                props: {
                    formFields: secrets
                } as unknown
            });
            spectatorWithFields.detectChanges();
            await spectatorWithFields.fixture.whenStable();

            const field = secrets[0];
            const firstFormField = spectatorWithFields.query<HTMLInputElement>(`#${field.name}`);
            expect(firstFormField).toBeTruthy();

            // Trigger focus by updating formFields (this will trigger the effect)
            spectatorWithFields.setInput('formFields', [...secrets]); // New array reference
            spectatorWithFields.detectChanges();
            await spectatorWithFields.fixture.whenStable();

            // Verify the element has focus
            expect(document.activeElement).toBe(firstFormField);
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

        it('should load Generated String Field component with right attributes', () => {
            const row = spectator.query(byTestId('generated-string-field'));
            expect(row).toBeTruthy();

            expect(
                spectator.query(DotAppsConfigurationDetailGeneratedStringFieldComponent)
            ).toBeTruthy();
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

            const openMock = jest.fn();
            window.open = openMock;
            const row = spectator.query(byTestId('integration'));
            const buttonElement = row.querySelector('button');

            buttonElement.click();
            expect(openMock).toHaveBeenCalledWith(field.value, '_blank');
            expect(openMock).toHaveBeenCalledTimes(1);
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
            const spyDataOutput = jest.spyOn(spectator.component.data, 'emit');
            const spyValidOutput = jest.spyOn(spectator.component.valid, 'emit');

            spectator.component.myFormGroup.get('name').setValue('Test2');
            spectator.component.myFormGroup.get('password').setValue('Password2');
            spectator.component.myFormGroup.get('enabled').setValue('false');

            expect(spyDataOutput).toHaveBeenCalledTimes(3);
            expect(spyValidOutput).toHaveBeenCalledTimes(3);
        });

        it('should emit form state disabled when required field empty', () => {
            const spyValidOutput = jest.spyOn(spectator.component.valid, 'emit');

            spectator.component.myFormGroup.get('name').setValue('');
            expect(spyValidOutput).toHaveBeenCalledWith(false);
            expect(spyValidOutput).toHaveBeenCalledTimes(1);
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
                } as unknown
            });
            spectator.detectChanges();
        });

        it('should have warning icons', () => {
            const warningIcons = spectator.queryAll('dot-icon');
            const formFields = spectator.component.$formFields();

            // Verify that we have warning icons
            expect(warningIcons.length).toBeGreaterThan(0);
            expect(warningIcons[0]).toBeTruthy();
            expect(warningIcons[1]).toBeTruthy();
            expect(warningIcons[2]).toBeTruthy();

            // Verify warning icon attributes
            expect(warningIcons[0].getAttribute('name')).toBe('warning');
            expect(warningIcons[0].getAttribute('size')).toBe('18');

            expect(warningIcons[1].getAttribute('name')).toBe('warning');
            expect(warningIcons[1].getAttribute('size')).toBe('18');

            expect(warningIcons[2].getAttribute('name')).toBe('warning');
            expect(warningIcons[2].getAttribute('size')).toBe('18');

            // Verify that the form fields have the expected warnings
            expect(formFields[0].warnings).toEqual(['error 0']);
            expect(formFields[1].warnings).toEqual(['error 1']);
            expect(formFields[2].warnings).toEqual(['error 2']);
        });
    });
});
