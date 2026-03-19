import { Spectator, SpyObject, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { DotMessageService } from '@dotcms/data-access';
import {
    CUSTOM_FIELD_OPTIONS_KEY,
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotFieldVariable
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotRenderOptionsSettingsComponent } from './dot-render-options-settings.component';

import { DotFieldVariablesService } from '../../../fields/dot-content-type-fields-variables/services/dot-field-variables.service';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.field.properties.renderOptions.title': 'Render Options',
    'contenttypes.field.properties.renderOptions.showAsModal.label': 'Show as Modal',
    'contenttypes.field.properties.renderOptions.showAsModal.helper':
        'Display this field in an overlay',
    'contenttypes.field.properties.renderOptions.width': 'Width',
    'contenttypes.field.properties.renderOptions.height': 'Height',
    'contenttypes.field.properties.renderOptions.width.error': 'Width must be at least 1',
    'contenttypes.field.properties.renderOptions.height.error': 'Height must be at least 1'
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

const MOCK_FIELD_VARIABLE_OPTIONS: DotFieldVariable = {
    clazz: DotCMSClazzes.FIELD_VARIABLE,
    fieldId: 'field-id-456',
    id: 'var-id-789',
    key: CUSTOM_FIELD_OPTIONS_KEY,
    value: JSON.stringify({ showAsModal: true, width: '500px', height: '600px' })
};

const MOCK_SAVED_VARIABLE: DotFieldVariable = {
    clazz: DotCMSClazzes.FIELD_VARIABLE,
    fieldId: 'field-id-456',
    id: 'var-id-789',
    key: CUSTOM_FIELD_OPTIONS_KEY,
    value: JSON.stringify({ showAsModal: false, width: '398px', height: '400px' })
};

describe('DotRenderOptionsSettingsComponent', () => {
    let spectator: Spectator<DotRenderOptionsSettingsComponent>;
    let component: DotRenderOptionsSettingsComponent;
    let dotFieldVariablesService: SpyObject<DotFieldVariablesService>;

    describe('without existing field variable (defaults)', () => {
        const createComponent = createComponentFactory({
            component: DotRenderOptionsSettingsComponent,
            imports: [
                FormsModule,
                ReactiveFormsModule,
                InputTextModule,
                ToggleSwitchModule,
                DotMessagePipe
            ],
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
                props: {
                    field: MOCK_FIELD_BASE
                } as unknown
            });
            dotFieldVariablesService = spectator.inject(DotFieldVariablesService);
            component = spectator.component;
            spectator.detectChanges();
        });

        describe('isDirty', () => {
            it('should return false initially', () => {
                expect(component.isDirty).toBe(false);
            });

            it('should return true after the form is explicitly marked dirty', () => {
                component.form.markAsDirty();
                expect(component.isDirty).toBe(true);
            });
        });

        describe('isValid', () => {
            it('should return true initially (controls disabled, form valid)', () => {
                expect(component.isValid()).toBe(true);
            });

            it('should return true when showAsModal is false (width/height disabled)', () => {
                expect(component.form.controls['customFieldWidth'].disabled).toBe(true);
                expect(component.form.controls['customFieldHeight'].disabled).toBe(true);
                expect(component.isValid()).toBe(true);
            });

            it('should return true when showAsModal is true and values are valid', () => {
                component.form.controls['showAsModal'].setValue(true);

                expect(component.form.controls['customFieldWidth'].enabled).toBe(true);
                expect(component.form.controls['customFieldHeight'].enabled).toBe(true);
                expect(component.isValid()).toBe(true);
            });

            it('should return false when showAsModal is true and customFieldWidth is 0', () => {
                component.form.controls['showAsModal'].setValue(true);
                component.form.controls['customFieldWidth'].setValue(0);

                expect(component.isValid()).toBe(false);
            });

            it('should return false when showAsModal is true and customFieldHeight is 0', () => {
                component.form.controls['showAsModal'].setValue(true);
                component.form.controls['customFieldHeight'].setValue(0);

                expect(component.isValid()).toBe(false);
            });
        });

        describe('ngOnInit with no field variables', () => {
            it('should initialise showAsModal to false', () => {
                expect(component.form.controls['showAsModal'].value).toBe(false);
            });

            it('should initialise customFieldWidth to 398 (default)', () => {
                expect(component.form.getRawValue().customFieldWidth).toBe(398);
            });

            it('should initialise customFieldHeight to 400 (default)', () => {
                expect(component.form.getRawValue().customFieldHeight).toBe(400);
            });

            it('should disable width and height controls when showAsModal is false', () => {
                expect(component.form.controls['customFieldWidth'].disabled).toBe(true);
                expect(component.form.controls['customFieldHeight'].disabled).toBe(true);
            });
        });

        describe('width/height control enable/disable logic', () => {
            it('should enable width and height controls when showAsModal toggles to true', () => {
                component.form.controls['showAsModal'].setValue(true);

                expect(component.form.controls['customFieldWidth'].enabled).toBe(true);
                expect(component.form.controls['customFieldHeight'].enabled).toBe(true);
            });

            it('should disable width and height controls when showAsModal toggles back to false', () => {
                component.form.controls['showAsModal'].setValue(true);
                component.form.controls['showAsModal'].setValue(false);

                expect(component.form.controls['customFieldWidth'].disabled).toBe(true);
                expect(component.form.controls['customFieldHeight'].disabled).toBe(true);
            });

            it('should not show width/height inputs in DOM when showAsModal is false', () => {
                spectator.detectChanges();

                expect(spectator.query('[data-testid="render-options-width"]')).toBeNull();
                expect(spectator.query('[data-testid="render-options-height"]')).toBeNull();
            });

            it('should show width/height inputs in DOM when showAsModal is true', () => {
                component.form.controls['showAsModal'].setValue(true);
                spectator.detectChanges();

                expect(spectator.query('[data-testid="render-options-width"]')).not.toBeNull();
                expect(spectator.query('[data-testid="render-options-height"]')).not.toBeNull();
            });
        });

        describe('save()', () => {
            it('should call DotFieldVariablesService.save with correct payload', () => {
                component.save(MOCK_FIELD_BASE).subscribe();

                expect(dotFieldVariablesService.save).toHaveBeenCalledWith(
                    MOCK_FIELD_BASE,
                    expect.objectContaining({
                        clazz: DotCMSClazzes.FIELD_VARIABLE,
                        key: CUSTOM_FIELD_OPTIONS_KEY,
                        value: JSON.stringify({
                            showAsModal: false,
                            width: '398px',
                            height: '400px'
                        })
                    })
                );
            });

            it('should include disabled width/height values via getRawValue()', () => {
                // Controls are disabled but their raw values should still be sent
                expect(component.form.controls['customFieldWidth'].disabled).toBe(true);

                component.save(MOCK_FIELD_BASE).subscribe();

                const savedArg = (dotFieldVariablesService.save as jest.Mock).mock.calls[0][1];
                const parsed = JSON.parse(savedArg.value);
                expect(parsed.width).toBe('398px');
                expect(parsed.height).toBe('400px');
            });

            it('should update fieldVariableRef after successful save (POST becomes PUT)', () => {
                const saveSpy = jest.fn(() => of(MOCK_SAVED_VARIABLE));
                jest.spyOn(dotFieldVariablesService, 'save').mockImplementation(saveSpy);

                component.save(MOCK_FIELD_BASE).subscribe();

                // Call save again — it should now carry the id from the first save
                component.save(MOCK_FIELD_BASE).subscribe();

                const secondCallArg = saveSpy.mock.calls[1][1] as DotFieldVariable;
                expect(secondCallArg.id).toBe(MOCK_SAVED_VARIABLE.id);
            });

            it('should propagate errors from DotFieldVariablesService.save', () => {
                jest.spyOn(dotFieldVariablesService, 'save').mockReturnValue(
                    throwError(() => new Error('Save failed'))
                );

                let errorCaught = false;
                component.save(MOCK_FIELD_BASE).subscribe({
                    error: () => {
                        errorCaught = true;
                    }
                });

                expect(errorCaught).toBe(true);
            });
        });

        describe('error messages', () => {
            it('should not show width error when showAsModal is false', () => {
                spectator.detectChanges();
                expect(spectator.query('[data-testid="render-options-width-error"]')).toBeNull();
            });

            it('should not show height error when showAsModal is false', () => {
                spectator.detectChanges();
                expect(spectator.query('[data-testid="render-options-height-error"]')).toBeNull();
            });

            it('should show width error when control is touched and value is invalid', () => {
                component.form.controls['showAsModal'].setValue(true);
                spectator.detectChanges();

                const widthControl = component.form.controls['customFieldWidth'];
                widthControl.setValue(0);
                widthControl.markAsTouched();
                spectator.detectChanges();

                expect(
                    spectator.query('[data-testid="render-options-width-error"]')
                ).not.toBeNull();
            });

            it('should show height error when control is touched and value is invalid', () => {
                component.form.controls['showAsModal'].setValue(true);
                spectator.detectChanges();

                const heightControl = component.form.controls['customFieldHeight'];
                heightControl.setValue(0);
                heightControl.markAsTouched();
                spectator.detectChanges();

                expect(
                    spectator.query('[data-testid="render-options-height-error"]')
                ).not.toBeNull();
            });

            it('should not show width error when control is untouched even if invalid', () => {
                component.form.controls['showAsModal'].setValue(true);
                spectator.detectChanges();

                component.form.controls['customFieldWidth'].setValue(0);
                // Do NOT markAsTouched
                spectator.detectChanges();

                expect(spectator.query('[data-testid="render-options-width-error"]')).toBeNull();
            });
        });
    });

    describe('with existing field variable (CUSTOM_FIELD_OPTIONS_KEY)', () => {
        const fieldWithVariable: DotCMSContentTypeField = {
            ...MOCK_FIELD_BASE,
            fieldVariables: [MOCK_FIELD_VARIABLE_OPTIONS]
        };

        const createComponent = createComponentFactory({
            component: DotRenderOptionsSettingsComponent,
            imports: [
                FormsModule,
                ReactiveFormsModule,
                InputTextModule,
                ToggleSwitchModule,
                DotMessagePipe
            ],
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
                props: {
                    field: fieldWithVariable
                } as unknown
            });
            dotFieldVariablesService = spectator.inject(DotFieldVariablesService);
            component = spectator.component;
            spectator.detectChanges();
        });

        it('should restore saved showAsModal as true from existing variable', () => {
            expect(component.form.controls['showAsModal'].value).toBe(true);
        });

        it('should parse width (strips "px") from existing variable', () => {
            expect(component.form.getRawValue().customFieldWidth).toBe(500);
        });

        it('should parse height (strips "px") from existing variable', () => {
            expect(component.form.getRawValue().customFieldHeight).toBe(600);
        });

        it('should enable width and height controls since showAsModal is restored as true', () => {
            expect(component.form.controls['customFieldWidth'].enabled).toBe(true);
            expect(component.form.controls['customFieldHeight'].enabled).toBe(true);
        });

        it('should include existing variable id in save payload (PUT-style update)', () => {
            component.save(fieldWithVariable).subscribe();

            const savedArg = (dotFieldVariablesService.save as jest.Mock).mock
                .calls[0][1] as DotFieldVariable;
            expect(savedArg.id).toBe(MOCK_FIELD_VARIABLE_OPTIONS.id);
        });
    });

    describe('with malformed JSON in field variable', () => {
        const fieldWithBadJson: DotCMSContentTypeField = {
            ...MOCK_FIELD_BASE,
            fieldVariables: [
                {
                    clazz: DotCMSClazzes.FIELD_VARIABLE,
                    fieldId: 'field-id-456',
                    id: 'var-id-bad',
                    key: CUSTOM_FIELD_OPTIONS_KEY,
                    value: '{invalid json!!!}'
                }
            ]
        };

        const createComponent = createComponentFactory({
            component: DotRenderOptionsSettingsComponent,
            imports: [
                FormsModule,
                ReactiveFormsModule,
                InputTextModule,
                ToggleSwitchModule,
                DotMessagePipe
            ],
            providers: [
                FormBuilder,
                mockProvider(DotFieldVariablesService, {
                    save: jest.fn(() => of(MOCK_SAVED_VARIABLE))
                }),
                { provide: DotMessageService, useValue: messageServiceMock }
            ],
            detectChanges: false
        });

        it('should fall back to defaults without throwing', () => {
            const spectator = createComponent({
                props: { field: fieldWithBadJson } as unknown
            });
            spectator.detectChanges();
            const comp = spectator.component;

            expect(comp.form.controls['showAsModal'].value).toBe(false);
            expect(comp.form.getRawValue().customFieldWidth).toBe(398);
            expect(comp.form.getRawValue().customFieldHeight).toBe(400);
        });
    });
});
