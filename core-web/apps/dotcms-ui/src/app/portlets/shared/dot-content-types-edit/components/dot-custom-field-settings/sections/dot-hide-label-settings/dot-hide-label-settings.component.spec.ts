import { Spectator, SpyObject, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotFieldVariable,
    HIDE_LABEL_VARIABLE_KEY
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotHideLabelSettingsComponent } from './dot-hide-label-settings.component';

import { DotFieldVariablesService } from '../../../fields/dot-content-type-fields-variables/services/dot-field-variables.service';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.field.properties.hideLabel.label': 'Hide Label'
});

const MOCK_FIELD_BASE: DotCMSContentTypeField = {
    contentTypeId: 'content-type-id-123',
    id: 'field-id-456',
    clazz: DotCMSClazzes.CUSTOM_FIELD,
    name: 'My Custom Field',
    dataType: null,
    fieldType: '',
    fieldTypeLabel: '',
    fieldVariables: [],
    fixed: null,
    iDate: null,
    indexed: null,
    listed: null,
    modDate: null,
    readOnly: null,
    required: null,
    searchable: null,
    sortOrder: null,
    unique: null,
    variable: null,
    defaultValue: null,
    hint: null,
    regexCheck: undefined,
    values: null
};

const MOCK_SAVED_VARIABLE: DotFieldVariable = {
    clazz: DotCMSClazzes.FIELD_VARIABLE,
    fieldId: 'field-id-456',
    id: 'var-id-789',
    key: HIDE_LABEL_VARIABLE_KEY,
    value: 'true'
};

describe('DotHideLabelSettingsComponent', () => {
    let spectator: Spectator<DotHideLabelSettingsComponent>;
    let component: DotHideLabelSettingsComponent;
    let dotFieldVariablesService: SpyObject<DotFieldVariablesService>;

    describe('without existing field variable (defaults)', () => {
        const createComponent = createComponentFactory({
            component: DotHideLabelSettingsComponent,
            imports: [ReactiveFormsModule, CheckboxModule, DotMessagePipe],
            providers: [
                FormBuilder,
                mockProvider(DotFieldVariablesService, {
                    save: jest.fn(() => of(MOCK_SAVED_VARIABLE))
                }),
                { provide: DotMessageService, useValue: messageServiceMock }
            ],
            detectChanges: false
        });

        beforeEach(() => {
            spectator = createComponent({
                props: { field: MOCK_FIELD_BASE } as unknown
            });
            dotFieldVariablesService = spectator.inject(DotFieldVariablesService);
            component = spectator.component;
            spectator.detectChanges();
        });

        describe('isDirty', () => {
            it('should return false initially', () => {
                expect(component.isDirty).toBe(false);
            });

            it('should return true after the form is marked dirty', () => {
                component.form.markAsDirty();
                expect(component.isDirty).toBe(true);
            });
        });

        describe('isValid', () => {
            it('should always return true', () => {
                expect(component.isValid()).toBe(true);
            });
        });

        describe('ngOnInit with no field variables', () => {
            it('should initialise hideLabel to false', () => {
                expect(component.form.controls['hideLabel'].value).toBe(false);
            });

            it('should render the checkbox', () => {
                expect(spectator.query('[data-testid="hide-label-checkbox"]')).not.toBeNull();
            });
        });

        describe('save()', () => {
            it('should call DotFieldVariablesService.save with value "false" when unchecked', () => {
                component.save(MOCK_FIELD_BASE).subscribe();

                expect(dotFieldVariablesService.save).toHaveBeenCalledWith(
                    MOCK_FIELD_BASE,
                    expect.objectContaining({
                        clazz: DotCMSClazzes.FIELD_VARIABLE,
                        key: HIDE_LABEL_VARIABLE_KEY,
                        value: 'false'
                    })
                );
            });

            it('should call DotFieldVariablesService.save with value "true" when checked', () => {
                component.form.controls['hideLabel'].setValue(true);
                component.save(MOCK_FIELD_BASE).subscribe();

                expect(dotFieldVariablesService.save).toHaveBeenCalledWith(
                    MOCK_FIELD_BASE,
                    expect.objectContaining({
                        key: HIDE_LABEL_VARIABLE_KEY,
                        value: 'true'
                    })
                );
            });

            it('should update fieldVariableRef after successful save (POST becomes PUT)', () => {
                const saveSpy = jest.fn(() => of(MOCK_SAVED_VARIABLE));
                jest.spyOn(dotFieldVariablesService, 'save').mockImplementation(saveSpy);

                component.save(MOCK_FIELD_BASE).subscribe();
                component.save(MOCK_FIELD_BASE).subscribe();

                const secondCallArg = saveSpy.mock.calls[1][1] as DotFieldVariable;
                expect(secondCallArg.id).toBe(MOCK_SAVED_VARIABLE.id);
            });

            it('should propagate errors from DotFieldVariablesService.save', () => {
                jest.spyOn(dotFieldVariablesService, 'save').mockReturnValue(
                    throwError(() => new Error('Save failed'))
                );

                let errorCaught = false;
                component.save(MOCK_FIELD_BASE).subscribe({ error: () => (errorCaught = true) });

                expect(errorCaught).toBe(true);
            });
        });
    });

    describe('with existing field variable (HIDE_LABEL_VARIABLE_KEY = "true")', () => {
        const fieldWithVariable: DotCMSContentTypeField = {
            ...MOCK_FIELD_BASE,
            fieldVariables: [
                {
                    clazz: DotCMSClazzes.FIELD_VARIABLE,
                    fieldId: 'field-id-456',
                    id: 'var-id-789',
                    key: HIDE_LABEL_VARIABLE_KEY,
                    value: 'true'
                }
            ]
        };

        const createComponent = createComponentFactory({
            component: DotHideLabelSettingsComponent,
            imports: [ReactiveFormsModule, CheckboxModule, DotMessagePipe],
            providers: [
                FormBuilder,
                mockProvider(DotFieldVariablesService, {
                    save: jest.fn(() => of(MOCK_SAVED_VARIABLE))
                }),
                { provide: DotMessageService, useValue: messageServiceMock }
            ],
            detectChanges: false
        });

        beforeEach(() => {
            spectator = createComponent({
                props: { field: fieldWithVariable } as unknown
            });
            dotFieldVariablesService = spectator.inject(DotFieldVariablesService);
            component = spectator.component;
            spectator.detectChanges();
        });

        it('should parse hideLabel as true from existing variable', () => {
            expect(component.form.controls['hideLabel'].value).toBe(true);
        });

        it('should include existing variable id in save payload (PUT-style update)', () => {
            component.save(fieldWithVariable).subscribe();

            const savedArg = (dotFieldVariablesService.save as jest.Mock).mock
                .calls[0][1] as DotFieldVariable;
            expect(savedArg.id).toBe('var-id-789');
        });
    });

    describe('with existing field variable (HIDE_LABEL_VARIABLE_KEY = "false")', () => {
        const fieldWithFalseVariable: DotCMSContentTypeField = {
            ...MOCK_FIELD_BASE,
            fieldVariables: [
                {
                    clazz: DotCMSClazzes.FIELD_VARIABLE,
                    fieldId: 'field-id-456',
                    id: 'var-id-000',
                    key: HIDE_LABEL_VARIABLE_KEY,
                    value: 'false'
                }
            ]
        };

        const createComponent = createComponentFactory({
            component: DotHideLabelSettingsComponent,
            imports: [ReactiveFormsModule, CheckboxModule, DotMessagePipe],
            providers: [
                FormBuilder,
                mockProvider(DotFieldVariablesService, {
                    save: jest.fn(() => of(MOCK_SAVED_VARIABLE))
                }),
                { provide: DotMessageService, useValue: messageServiceMock }
            ],
            detectChanges: false
        });

        beforeEach(() => {
            spectator = createComponent({
                props: { field: fieldWithFalseVariable } as unknown
            });
            component = spectator.component;
            spectator.detectChanges();
        });

        it('should parse hideLabel as false from existing variable with value "false"', () => {
            expect(component.form.controls['hideLabel'].value).toBe(false);
        });
    });
});
