import { Spectator, SpyObject, byTestId, createComponentFactory } from '@ngneat/spectator';
import { of, throwError } from 'rxjs';

import { NgFor } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DividerModule } from 'primeng/divider';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessageService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService, mockFieldVariables } from '@dotcms/utils-testing';

import { DotBinarySettingsComponent } from './dot-binary-settings.component';

import { DotFieldVariablesService } from '../fields/dot-content-type-fields-variables/services/dot-field-variables.service';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.dropzone.action.save': 'Save',
    'contenttypes.dropzone.action.cancel': 'Cancel'
});

const systemOptions = JSON.stringify({
    allowURLImport: false,
    allowFileNameEdit: false,
    allowCodeWrite: true
});

describe('DotBinarySettingsComponent', () => {
    let spectator: Spectator<DotBinarySettingsComponent>;
    let component: DotBinarySettingsComponent;
    let dotFieldVariableService: SpyObject<DotFieldVariablesService>;
    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;

    const createComponent = createComponentFactory({
        component: DotBinarySettingsComponent,
        imports: [
            NgFor,
            FormsModule,
            ReactiveFormsModule,
            InputTextModule,
            InputSwitchModule,
            DividerModule,
            DotMessagePipe
        ],
        providers: [
            FormBuilder,
            {
                provide: DotFieldVariablesService,
                useValue: {
                    load: () =>
                        of([
                            {
                                clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
                                fieldId: 'f965a51b-130a-435f-b646-41e07d685363',
                                id: '9671d2c3-793b-41af-a485-e2c5fcba5fb',
                                key: 'systemOptions',
                                value: systemOptions
                            },
                            {
                                clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
                                fieldId: 'f965a51b-130a-435f-b646-41e07d685363',
                                id: '9671d2c3-793b-41af-a485-e2c5fcba5fb',
                                key: 'accept',
                                value: 'image/*'
                            }
                        ]),
                    save: () => of([]),
                    delete: () => of([])
                }
            },
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            {
                provide: DotHttpErrorManagerService,
                useValue: {
                    handle: () => of([])
                }
            }
        ]
    });

    beforeEach(async () => {
        spectator = createComponent({});
        dotFieldVariableService = spectator.inject(DotFieldVariablesService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);

        component = spectator.component;
    });

    it('should setup form values', async () => {
        expect(component.form.get('accept').value).toBe('image/*');
        expect(component.form.get('allowURLImport').value).toBe(false);
        expect(component.form.get('allowCodeWrite').value).toBe(true);
        expect(component.form.get('allowFileNameEdit').value).toBe(false);
    });

    it('should emit changeControls when isVisible input is true', () => {
        spyOn(component.changeControls, 'emit');

        spectator.setInput('isVisible', true);

        expect(component.changeControls.emit).toHaveBeenCalled();
    });

    it('should emit valid output on form change', () => {
        spyOn(component.valid, 'emit');

        component.form.get('accept').setValue('text/*');

        expect(component.valid.emit).toHaveBeenCalled();
    });

    it('should save properties on saveSettings', () => {
        spyOn(dotFieldVariableService, 'save').and.returnValue(of(mockFieldVariables[0]));
        spyOn(component.save, 'emit');

        component.saveSettings();

        expect(dotFieldVariableService.save).toHaveBeenCalledTimes(2); // One for accept and one for systemOptions
        expect(component.save.emit).toHaveBeenCalled();
        expect(component.settingsMap['accept'].variable).toEqual(mockFieldVariables[0]);
    });

    it('should delete properties on saveSettings when is empty', () => {
        spyOn(dotFieldVariableService, 'delete').and.returnValue(of(mockFieldVariables[0]));
        spyOn(component.save, 'emit');

        component.form.get('accept').setValue('');
        component.saveSettings();

        expect(dotFieldVariableService.delete).toHaveBeenCalled();
        expect(component.save.emit).toHaveBeenCalled();
        expect(component.settingsMap['accept'].variable).toEqual(mockFieldVariables[0]);
    });

    it('should handler error if save properties failed', () => {
        spyOn(dotFieldVariableService, 'save').and.returnValue(throwError({}));
        spyOn(dotHttpErrorManagerService, 'handle').and.returnValue(of());
        spyOn(component.save, 'emit');

        component.saveSettings();

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
        expect(component.save.emit).not.toHaveBeenCalled();
    });
    it('should not call save or delete when is empty and not previous variable exist', () => {
        spyOn(dotFieldVariableService, 'load');
        spyOn(dotFieldVariableService, 'delete').and.returnValue(of([]));
        spyOn(dotFieldVariableService, 'save').and.returnValue(of([]));

        // Couldn't find a way to force a no variable from load method
        component.settingsMap = {
            accept: {
                key: 'accept',
                variable: null
            },
            systemOptions: {
                key: 'systemOptions',
                variable: null
            }
        };

        component.form.get('accept').setValue('');
        component.saveSettings();

        expect(dotFieldVariableService.delete).not.toHaveBeenCalled();
        expect(dotFieldVariableService.save).not.toHaveBeenCalledTimes(2); // One for accept and one for systemOptions, accept should not call save or delete
    });

    it('should have 3 switches with the corresponding control name', () => {
        const switches = spectator.queryAll(byTestId('setting-switch'));

        expect(switches.length).toBe(3);
        expect(
            switches.find((s) => s.getAttribute('ng-reflect-name') === 'allowURLImport')
        ).not.toBeNull();
        expect(
            switches.find((s) => s.getAttribute('ng-reflect-name') === 'allowCodeWrite')
        ).not.toBeNull();
        expect(
            switches.find((s) => s.getAttribute('ng-reflect-name') === 'allowFileNameEdit')
        ).not.toBeNull();
    });

    it('should have 1 input with the control name accept', () => {
        const [acceptInput] = spectator.queryAll(byTestId('setting-accept'));

        expect(acceptInput).not.toBeNull();

        expect(acceptInput.getAttribute('ng-reflect-name')).toBe('accept');
    });
});
