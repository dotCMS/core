import { Spectator, SpyObject, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotFieldVariable,
    DotRenderModes,
    NEW_RENDER_MODE_VARIABLE_KEY
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotCustomFieldSettingsComponent } from './dot-custom-field-settings.component';
import { DotHideLabelSettingsComponent } from './sections/dot-hide-label-settings';
import { DotRenderOptionsSettingsComponent } from './sections/dot-render-options-settings';

import { DotFieldVariablesService } from '../fields/dot-content-type-fields-variables/services/dot-field-variables.service';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.dropzone.action.save': 'Save',
    'contenttypes.dropzone.action.cancel': 'Cancel',
    'contenttypes.field.properties.renderOptions.title': 'Render Options',
    'contenttypes.field.properties.renderOptions.showAsModal.label': 'Show as Modal',
    'contenttypes.field.properties.renderOptions.showAsModal.helper': 'Display in overlay',
    'contenttypes.field.properties.renderOptions.width': 'Width',
    'contenttypes.field.properties.renderOptions.height': 'Height',
    'contenttypes.field.properties.renderOptions.width.error': 'Width must be at least 1',
    'contenttypes.field.properties.renderOptions.height.error': 'Height must be at least 1',
    'contenttypes.field.properties.hideLabel.label': 'Hide Label'
});

const MOCK_SAVED_VARIABLE: DotFieldVariable = {
    clazz: DotCMSClazzes.FIELD_VARIABLE,
    fieldId: 'field-id-456',
    id: 'var-id-789',
    key: 'customFieldOptions',
    value: JSON.stringify({ showAsModal: false, width: '398px', height: '400px' })
};

const MOCK_FIELD: DotCMSContentTypeField = {
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

describe('DotCustomFieldSettingsComponent', () => {
    let spectator: Spectator<DotCustomFieldSettingsComponent>;
    let component: DotCustomFieldSettingsComponent;
    let dotFieldVariablesService: SpyObject<DotFieldVariablesService>;
    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;

    /**
     * We use the real DotRenderOptionsSettingsComponent in imports so that viewChild.required()
     * can resolve it. DotFieldVariablesService is mocked at the provider level so the section
     * component gets the mock through DI.
     */
    const createComponent = createComponentFactory({
        component: DotCustomFieldSettingsComponent,
        imports: [
            DotRenderOptionsSettingsComponent,
            DotHideLabelSettingsComponent,
            FormsModule,
            ReactiveFormsModule,
            CheckboxModule,
            InputTextModule,
            ToggleSwitchModule,
            DotMessagePipe
        ],
        providers: [
            FormBuilder,
            mockProvider(DotFieldVariablesService, {
                save: jest.fn(() => of(MOCK_SAVED_VARIABLE))
            }),
            { provide: DotMessageService, useValue: messageServiceMock },
            mockProvider(DotHttpErrorManagerService, {
                handle: jest.fn(() => of(null))
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: MOCK_FIELD
            } as unknown
        });
        dotFieldVariablesService = spectator.inject(DotFieldVariablesService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
        component = spectator.component;
        spectator.detectChanges();
    });

    describe('template', () => {
        it('should always render the dot-hide-label-settings child component', () => {
            expect(spectator.query(DotHideLabelSettingsComponent)).not.toBeNull();
        });

        it('should pass the field input to dot-hide-label-settings', () => {
            const child = spectator.query(DotHideLabelSettingsComponent);
            expect(child.$field()).toEqual(MOCK_FIELD);
        });

        it('should still render dot-hide-label-settings when renderMode is component', () => {
            spectator.setInput('renderMode', DotRenderModes.COMPONENT);
            spectator.detectChanges();

            expect(spectator.query(DotHideLabelSettingsComponent)).not.toBeNull();
        });

        it('should render the dot-render-options-settings child component (default iframe mode)', () => {
            const child = spectator.query(DotRenderOptionsSettingsComponent);
            expect(child).not.toBeNull();
        });

        it('should pass the field input to dot-render-options-settings', () => {
            const child = spectator.query(DotRenderOptionsSettingsComponent);
            expect(child.$field()).toEqual(MOCK_FIELD);
        });

        it('should hide dot-render-options-settings when renderMode input is component', () => {
            spectator.setInput('renderMode', DotRenderModes.COMPONENT);
            spectator.detectChanges();

            expect(spectator.query(DotRenderOptionsSettingsComponent)).toBeNull();
        });

        it('should show dot-render-options-settings when renderMode input is iframe', () => {
            spectator.setInput('renderMode', DotRenderModes.IFRAME);
            spectator.detectChanges();

            expect(spectator.query(DotRenderOptionsSettingsComponent)).not.toBeNull();
        });

        it('should fall back to fieldVariables when renderMode input is undefined', () => {
            const componentModeField: DotCMSContentTypeField = {
                ...MOCK_FIELD,
                fieldVariables: [
                    {
                        clazz: DotCMSClazzes.FIELD_VARIABLE,
                        fieldId: 'field-id-456',
                        id: 'var-render-mode',
                        key: NEW_RENDER_MODE_VARIABLE_KEY,
                        value: DotRenderModes.COMPONENT
                    }
                ]
            };

            spectator.setInput('field', componentModeField);
            // no renderMode input → falls back to fieldVariables
            spectator.detectChanges();

            expect(spectator.query(DotRenderOptionsSettingsComponent)).toBeNull();
        });
    });

    describe('$valid output', () => {
        it('should emit false when form is pristine (no dirty section)', () => {
            jest.spyOn(component.$valid, 'emit');

            // Trigger a valueChanges event without dirtying — by patching via setValue (no markAsDirty)
            const renderOptionsInstance = spectator.query(
                DotRenderOptionsSettingsComponent
            ) as DotRenderOptionsSettingsComponent;
            renderOptionsInstance.form.controls['showAsModal'].setValue(false, { emitEvent: true });

            expect(component.$valid.emit).toHaveBeenCalledWith(false);
        });

        it('should emit true when a section is dirty and valid', () => {
            jest.spyOn(component.$valid, 'emit');

            const renderOptionsInstance = spectator.query(
                DotRenderOptionsSettingsComponent
            ) as DotRenderOptionsSettingsComponent;
            renderOptionsInstance.form.controls['showAsModal'].setValue(true);
            renderOptionsInstance.form.markAsDirty();
            // Trigger valueChanges
            renderOptionsInstance.form.controls['showAsModal'].setValue(true, { emitEvent: true });

            expect(component.$valid.emit).toHaveBeenCalledWith(true);
        });
    });

    describe('saveSettings()', () => {
        it('should not call DotFieldVariablesService.save when no section is dirty', () => {
            component.saveSettings();

            expect(dotFieldVariablesService.save).not.toHaveBeenCalled();
        });

        it('should not emit $save when no sections are dirty', () => {
            jest.spyOn(component.$save, 'emit');

            component.saveSettings();

            expect(component.$save.emit).not.toHaveBeenCalled();
        });

        it('should call save on the renderOptions section when it is dirty', () => {
            jest.spyOn(component.$save, 'emit');

            const renderOptionsInstance = spectator.query(
                DotRenderOptionsSettingsComponent
            ) as DotRenderOptionsSettingsComponent;
            renderOptionsInstance.form.controls['showAsModal'].setValue(true);
            renderOptionsInstance.form.markAsDirty();

            component.saveSettings();

            expect(dotFieldVariablesService.save).toHaveBeenCalled();
            expect(component.$save.emit).toHaveBeenCalled();
        });

        it('should emit $save after successful save', () => {
            jest.spyOn(component.$save, 'emit');

            const renderOptionsInstance = spectator.query(
                DotRenderOptionsSettingsComponent
            ) as DotRenderOptionsSettingsComponent;
            renderOptionsInstance.form.markAsDirty();

            component.saveSettings();

            expect(component.$save.emit).toHaveBeenCalled();
        });

        it('should handle errors via DotHttpErrorManagerService and still emit $save after recovery', () => {
            jest.spyOn(dotFieldVariablesService, 'save').mockReturnValue(
                throwError(() => new Error('Network error'))
            );
            jest.spyOn(component.$save, 'emit');

            const renderOptionsInstance = spectator.query(
                DotRenderOptionsSettingsComponent
            ) as DotRenderOptionsSettingsComponent;
            renderOptionsInstance.form.markAsDirty();

            component.saveSettings();

            // catchError recovers via handle() which returns of(null),
            // so the subscribe callback runs and $save IS emitted
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
            expect(component.$save.emit).toHaveBeenCalled();
        });

        it('should call save on the hideLabel section when it is dirty', () => {
            jest.spyOn(component.$save, 'emit');

            const hideLabelInstance = spectator.query(
                DotHideLabelSettingsComponent
            ) as DotHideLabelSettingsComponent;
            hideLabelInstance.form.controls['hideLabel'].setValue(true);
            hideLabelInstance.form.markAsDirty();

            component.saveSettings();

            expect(dotFieldVariablesService.save).toHaveBeenCalledWith(
                MOCK_FIELD,
                expect.objectContaining({ key: 'hideLabel' })
            );
            expect(component.$save.emit).toHaveBeenCalled();
        });

        it('should call save on both sections and emit $save once when both are dirty', () => {
            jest.spyOn(component.$save, 'emit');

            const renderOptionsInstance = spectator.query(
                DotRenderOptionsSettingsComponent
            ) as DotRenderOptionsSettingsComponent;
            renderOptionsInstance.form.controls['showAsModal'].setValue(true);
            renderOptionsInstance.form.markAsDirty();

            const hideLabelInstance = spectator.query(
                DotHideLabelSettingsComponent
            ) as DotHideLabelSettingsComponent;
            hideLabelInstance.form.controls['hideLabel'].setValue(true);
            hideLabelInstance.form.markAsDirty();

            component.saveSettings();

            expect(dotFieldVariablesService.save).toHaveBeenCalledTimes(2);
            expect(component.$save.emit).toHaveBeenCalledTimes(1);
        });
    });

    describe('ngOnChanges / $changeControls output', () => {
        it('should emit $changeControls when $isVisible changes to true', () => {
            const emitSpy = jest.spyOn(component.$changeControls, 'emit');

            spectator.setInput('isVisible', true);

            expect(emitSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    accept: expect.objectContaining({
                        action: expect.any(Function),
                        label: 'Save',
                        disabled: true
                    }),
                    cancel: expect.objectContaining({ label: 'Cancel' })
                })
            );
        });

        it('should not emit $changeControls when $isVisible changes to false', () => {
            spectator.setInput('isVisible', true);
            const emitSpy = jest.spyOn(component.$changeControls, 'emit');

            spectator.setInput('isVisible', false);

            expect(emitSpy).not.toHaveBeenCalled();
        });

        it('should emit $changeControls with accept.disabled false when a section is dirty and valid', () => {
            const renderOptionsInstance = spectator.query(
                DotRenderOptionsSettingsComponent
            ) as DotRenderOptionsSettingsComponent;
            renderOptionsInstance.form.controls['showAsModal'].setValue(true);
            renderOptionsInstance.form.markAsDirty();

            const emitSpy = jest.spyOn(component.$changeControls, 'emit');
            spectator.setInput('isVisible', true);

            const emitted = emitSpy.mock.calls[0][0] as ReturnType<typeof Object.assign>;
            expect(emitted.accept.disabled).toBe(false);
        });

        it('should call saveSettings when the emitted accept.action is invoked', () => {
            const saveSpy = jest.spyOn(component, 'saveSettings');
            const emitSpy = jest.spyOn(component.$changeControls, 'emit');

            spectator.setInput('isVisible', true);

            const emitted = emitSpy.mock.calls[0][0] as { accept: { action: () => void } };
            emitted.accept.action();

            expect(saveSpy).toHaveBeenCalled();
        });
    });

    describe('$valid output (invalid dirty state)', () => {
        it('should emit false when a section is dirty but invalid', () => {
            jest.spyOn(component.$valid, 'emit');

            const renderOptionsInstance = spectator.query(
                DotRenderOptionsSettingsComponent
            ) as DotRenderOptionsSettingsComponent;
            renderOptionsInstance.form.controls['showAsModal'].setValue(true);
            renderOptionsInstance.form.controls['customFieldWidth'].setValue(0);
            renderOptionsInstance.form.markAsDirty();
            renderOptionsInstance.form.controls['showAsModal'].setValue(true, { emitEvent: true });

            expect(component.$valid.emit).toHaveBeenCalledWith(false);
        });
    });
});
