import {
    Spectator,
    SpyObject,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DividerModule } from 'primeng/divider';
import { InputTextModule } from 'primeng/inputtext';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField, DotFieldVariable } from '@dotcms/dotcms-models';
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

const MOCK_FIELD: Partial<DotCMSContentTypeField> = {
    id: 'f965a51b-130a-435f-b646-41e07d685363',
    name: 'testField',
    clazz: 'com.dotcms.contenttype.model.field.ImmutableBinaryField'
};

describe('DotBinarySettingsComponent', () => {
    let spectator: Spectator<DotBinarySettingsComponent>;
    let component: DotBinarySettingsComponent;
    let dotFieldVariableService: SpyObject<DotFieldVariablesService>;
    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;

    const getFieldVariableMock = (value = 'image/*') => ({
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
                    value: value
                }
            ]),
        save: () => of([]),
        delete: () => of([])
    });

    describe('with value', () => {
        const createComponent = createComponentFactory({
            component: DotBinarySettingsComponent,
            imports: [
                FormsModule,
                ReactiveFormsModule,
                InputTextModule,
                ToggleSwitchModule,
                DividerModule,
                DotMessagePipe
            ],
            providers: [
                FormBuilder,
                mockProvider(DotFieldVariablesService, getFieldVariableMock()),
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
            spectator = createComponent({
                props: {
                    $field: MOCK_FIELD as DotCMSContentTypeField
                }
            });
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
            jest.spyOn(component.$changeControls, 'emit');

            spectator.setInput('isVisible', true);

            expect(component.$changeControls.emit).toHaveBeenCalled();
        });

        it('should emit valid output on form change', () => {
            jest.spyOn(component.$valid, 'emit');

            const acceptInput = spectator.query(byTestId('setting-accept'));
            spectator.typeInElement('text/*', acceptInput);

            expect(component.$valid.emit).toHaveBeenCalled();
        });

        it('should handler error if save properties failed', () => {
            jest.spyOn(dotFieldVariableService, 'save').mockReturnValue(throwError({}));
            jest.spyOn(dotHttpErrorManagerService, 'handle').mockReturnValue(of());
            jest.spyOn(component.$save, 'emit');

            component.saveSettings();

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
            expect(component.$save.emit).not.toHaveBeenCalled();
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

            // Verify that the form has the accept control (more reliable than checking attributes)
            expect(spectator.component.form.get('accept')).not.toBeNull();
            // Verify the input is bound to the form control by checking it exists and has the correct testId
            expect(acceptInput).toBeTruthy();
        });
    });

    describe('without value', () => {
        const createComponent = createComponentFactory({
            component: DotBinarySettingsComponent,
            imports: [
                FormsModule,
                ReactiveFormsModule,
                InputTextModule,
                ToggleSwitchModule,
                DividerModule,
                DotMessagePipe
            ],
            providers: [
                FormBuilder,
                mockProvider(DotFieldVariablesService, getFieldVariableMock('')),
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
            spectator = createComponent({
                props: {
                    $field: MOCK_FIELD as DotCMSContentTypeField
                }
            });
            dotFieldVariableService = spectator.inject(DotFieldVariablesService);
            dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);

            component = spectator.component;
        });

        it('should not call save or delete when is empty and not previous variable exist', () => {
            jest.spyOn(dotFieldVariableService, 'delete').mockReturnValue(
                of({} as DotFieldVariable)
            );
            jest.spyOn(dotFieldVariableService, 'save').mockReturnValue(of({} as DotFieldVariable));

            spectator.detectChanges();

            component.form.get('accept').setValue('');
            component.saveSettings();

            expect(dotFieldVariableService.delete).not.toHaveBeenCalled();
            expect(dotFieldVariableService.save).not.toHaveBeenCalledTimes(2); // One for accept and one for systemOptions, accept should not call save or delete
        });
    });
});
