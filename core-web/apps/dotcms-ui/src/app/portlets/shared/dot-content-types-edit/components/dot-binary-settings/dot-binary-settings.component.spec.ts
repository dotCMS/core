import { Spectator, SpyObject, byTestId, createComponentFactory } from '@ngneat/spectator';
import { of, throwError } from 'rxjs';

import { NgFor } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DividerModule } from 'primeng/divider';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessageService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotBinarySettingsComponent } from './dot-binary-settings.component';

import { DotFieldVariablesService } from '../fields/dot-content-type-fields-variables/services/dot-field-variables.service';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.dropzone.action.save': 'Save',
    'contenttypes.dropzone.action.cancel': 'Cancel'
});

const SYSTEM_OPTIONS = JSON.stringify({
    allowURLImport: false,
    allowCodeWrite: true,
    allowGenerateImg: false
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
                                value: SYSTEM_OPTIONS
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

    beforeEach(() => {
        spectator = createComponent();
        dotFieldVariableService = spectator.inject(DotFieldVariablesService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);

        component = spectator.component;
    });

    it('should setup form values', () => {
        expect(component.form.get('accept').value).toBe('image/*');
        expect(component.form.get('systemOptions').value).toEqual({
            allowURLImport: false,
            allowCodeWrite: true,
            allowGenerateImg: false
        });
    });

    it('should emit changeControls when isVisible input is true', () => {
        spyOn(component.changeControls, 'emit');

        spectator.setInput('isVisible', true);

        expect(component.changeControls.emit).toHaveBeenCalled();
    });

    it('should emit valid output on form change', () => {
        spyOn(component.valid, 'emit');

        const acceptInput = spectator.query(byTestId('setting-accept'));
        spectator.typeInElement('text/*', acceptInput);

        expect(component.valid.emit).toHaveBeenCalled();
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
            switches.find((s) => s.getAttribute('ng-reflect-name') === 'allowGenerateImg')
        ).not.toBeNull();
    });

    it('should have 1 input with the control name accept', () => {
        const [acceptInput] = spectator.queryAll(byTestId('setting-accept'));

        expect(acceptInput).not.toBeNull();

        expect(acceptInput.getAttribute('ng-reflect-name')).toBe('accept');
    });
});
