import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotHttpErrorManagerService, DotMessageDisplayService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotCMSClazzes, DotCMSContentTypeField, DotFieldVariable } from '@dotcms/dotcms-models';
import { DotKeyValueComponent } from '@dotcms/ui';
import { EMPTY_FIELD } from '@dotcms/utils';
import {
    dotcmsContentTypeFieldBasicMock,
    DotFieldVariablesServiceMock,
    LoginServiceMock,
    mockFieldVariables
} from '@dotcms/utils-testing';

import { DotContentTypeFieldsVariablesComponent } from './dot-content-type-fields-variables.component';
import { DotDialogActions, DotFieldVariablesService } from './services/dot-field-variables.service';

import { DOTTestBed } from '../../../../../../test/dot-test-bed';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-content-type-fields-variables [field]="value"></dot-content-type-fields-variables>
    `,
    standalone: false
})
class TestHostComponent {
    value: DotCMSContentTypeField = {
        ...dotcmsContentTypeFieldBasicMock,
        contentTypeId: 'ddf29c1e-babd-40a8-bfed-920fc9b8c77',
        id: mockFieldVariables[0].fieldId
    };
}

describe('DotContentTypeFieldsVariablesComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let deHost: DebugElement;
    let comp: DotContentTypeFieldsVariablesComponent;
    let de: DebugElement;
    let dotFieldVariableService: DotFieldVariablesService;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [TestHostComponent],
            imports: [DotKeyValueComponent, DotContentTypeFieldsVariablesComponent],
            providers: [
                { provide: LoginService, useClass: LoginServiceMock },
                {
                    provide: DotFieldVariablesService,
                    useClass: DotFieldVariablesServiceMock
                },
                DotMessageDisplayService
            ]
        });

        fixtureHost = DOTTestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        de = deHost.query(By.css('dot-content-type-fields-variables'));
        comp = de.componentInstance;

        dotFieldVariableService = de.injector.get(DotFieldVariablesService);
    });

    it('should load the component with one empty row', () => {
        jest.spyOn(dotFieldVariableService, 'load').mockReturnValue(of([]));
        fixtureHost.detectChanges();
        expect(comp.$fieldVariables().length).toBe(0);
    });

    /** Hands the editor a new list, as `updatedList` does. */
    const changeTo = (variables: DotFieldVariable[]) =>
        de.query(By.css('dot-key-value-ng')).triggerEventHandler('updatedList', variables);

    describe('holding edits until Save (#37191)', () => {
        beforeEach(() => {
            jest.spyOn(dotFieldVariableService, 'load').mockReturnValue(of(mockFieldVariables));
            jest.spyOn(dotFieldVariableService, 'save').mockImplementation((_f, v) =>
                of(v as DotFieldVariable)
            );
            jest.spyOn(dotFieldVariableService, 'delete').mockImplementation((_f, v) =>
                of(v as DotFieldVariable)
            );
            fixtureHost.detectChanges();
        });

        it('should write nothing while the admin edits', () => {
            // The whole point of the change: the tab used to persist on every keystroke
            // of a row, so Cancel had nothing left to cancel.
            changeTo([{ key: 'newKey', value: 'newValue' } as DotFieldVariable]);

            expect(dotFieldVariableService.save).not.toHaveBeenCalled();
            expect(dotFieldVariableService.delete).not.toHaveBeenCalled();
        });

        it('should write the added and changed pairs on save', () => {
            const kept = mockFieldVariables[0];
            const edited = { ...mockFieldVariables[1], value: 'changed' };
            const added = { key: 'brandNew', value: 'v' } as DotFieldVariable;

            changeTo([kept, edited, added]);
            comp.saveChanges();

            expect(dotFieldVariableService.save).toHaveBeenCalledTimes(2);
            expect(dotFieldVariableService.save).toHaveBeenCalledWith(comp.field, edited);
            expect(dotFieldVariableService.save).toHaveBeenCalledWith(comp.field, added);
            // An untouched pair is not re-sent.
            expect(dotFieldVariableService.save).not.toHaveBeenCalledWith(comp.field, kept);
        });

        it('should delete the pairs removed from the list', () => {
            const removed = mockFieldVariables[0];

            changeTo(mockFieldVariables.filter(({ key }) => key !== removed.key));
            comp.saveChanges();

            expect(dotFieldVariableService.delete).toHaveBeenCalledWith(comp.field, removed);
            expect(dotFieldVariableService.delete).toHaveBeenCalledTimes(1);
        });

        it('should report back so the dialog can close', () => {
            const saved = jest.fn();
            comp.$save.subscribe(saved);

            changeTo([...mockFieldVariables, { key: 'k', value: 'v' } as DotFieldVariable]);
            comp.saveChanges();

            expect(saved).toHaveBeenCalled();
        });

        it('should close without writing when nothing changed', () => {
            const saved = jest.fn();
            comp.$save.subscribe(saved);

            comp.saveChanges();

            expect(dotFieldVariableService.save).not.toHaveBeenCalled();
            expect(dotFieldVariableService.delete).not.toHaveBeenCalled();
            expect(saved).toHaveBeenCalled();
        });

        it('should not count re-adding a removed pair as a change', () => {
            // Compared by key and value: the field is exactly as it was stored.
            changeTo([]);
            changeTo(mockFieldVariables);

            expect(comp.$hasChanges()).toBe(false);
        });
    });

    describe('the dialog footer', () => {
        beforeEach(() => {
            jest.spyOn(dotFieldVariableService, 'load').mockReturnValue(of(mockFieldVariables));
            fixtureHost.detectChanges();
        });

        it('should offer a Save the dialog can render, disabled until something changes', () => {
            const controls: DotDialogActions[] = [];
            comp.$changeControls.subscribe((c) => controls.push(c));

            changeTo(mockFieldVariables);
            expect(controls.at(-1).accept.disabled).toBe(true);

            changeTo([{ key: 'fresh', value: 'v' } as DotFieldVariable]);
            expect(controls.at(-1).accept.disabled).toBe(false);
        });
    });

    describe('server failures (US2 scenario 5)', () => {
        let httpErrorManager: DotHttpErrorManagerService;
        const httpError = new HttpErrorResponse({ status: 500, statusText: 'Server Error' });

        beforeEach(() => {
            httpErrorManager = de.injector.get(DotHttpErrorManagerService);
            jest.spyOn(httpErrorManager, 'handle').mockReturnValue(of(null));
            jest.spyOn(dotFieldVariableService, 'load').mockReturnValue(of(mockFieldVariables));
            fixtureHost.detectChanges();
        });

        it('should surface a failed save and keep the dialog open', () => {
            jest.spyOn(dotFieldVariableService, 'save').mockReturnValue(
                throwError(() => httpError)
            );
            const saved = jest.fn();
            comp.$save.subscribe(saved);

            changeTo([
                ...mockFieldVariables,
                { key: 'newKey', value: 'newValue' } as DotFieldVariable
            ]);
            comp.saveChanges();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(httpError);
            // Closing on a failed write would look like it succeeded.
            expect(saved).not.toHaveBeenCalled();
        });

        it('should surface a failed delete and keep the edits on screen', () => {
            jest.spyOn(dotFieldVariableService, 'delete').mockReturnValue(
                throwError(() => httpError)
            );
            const pending = mockFieldVariables.slice(1);
            const saved = jest.fn();
            comp.$save.subscribe(saved);

            changeTo(pending);
            comp.saveChanges();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(httpError);
            expect(saved).not.toHaveBeenCalled();
            // The admin's work is still there to retry with.
            expect(comp.$fieldVariables()).toEqual(pending);
        });
    });

    describe('editor capabilities (FR-024, FR-030)', () => {
        beforeEach(() => {
            jest.spyOn(dotFieldVariableService, 'load').mockReturnValue(of(mockFieldVariables));
            fixtureHost.detectChanges();
        });

        it('should offer reordering, which every consumer has', () => {
            expect(de.query(By.css('[data-testId="dot-key-value-drag-handle"]'))).toBeTruthy();
        });

        it('should not offer hidden values', () => {
            const editor = de.query(By.css('dot-key-value-ng'));

            expect(editor.componentInstance.$showHiddenField()).toBe(false);
            expect(de.query(By.css('[data-testId="dot-key-value-hidden-switch"]'))).toBeNull();
        });
    });

    describe('Block Editor Field', () => {
        const BLOCK_EDITOR_FIELD: DotCMSContentTypeField = {
            ...EMPTY_FIELD,
            clazz: DotCMSClazzes.BLOCK_EDITOR,
            contentTypeId: 'ddf29c1e-babd-40a8-bfed-920fc9b8c77',
            id: mockFieldVariables[0].fieldId
        };

        beforeEach(() => {
            fixtureHost.componentInstance.value = BLOCK_EDITOR_FIELD;
        });

        it('should set variable correctly', () => {
            jest.spyOn(dotFieldVariableService, 'load').mockReturnValue(of(mockFieldVariables));
            fixtureHost.detectChanges();
            expect(comp.$fieldVariables().length).toBe(mockFieldVariables.length);
        });

        it('should not set allowedBlocks variable', () => {
            jest.spyOn(dotFieldVariableService, 'load').mockReturnValue(
                of([
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                        fieldId: 'f965a51b-130a-435f-b646-41e07d685363',
                        id: '9671d2c3-793b-41af-a485-e2c5fcba5fb',
                        key: 'allowedBlocks',
                        value: 'dotImage'
                    }
                ])
            );
            fixtureHost.detectChanges();
            expect(comp.$fieldVariables().length).toBe(0);
        });
    });

    describe('Custom Field', () => {
        const CUSTOM_FIELD: DotCMSContentTypeField = {
            ...EMPTY_FIELD,
            clazz: DotCMSClazzes.CUSTOM_FIELD,
            contentTypeId: 'ddf29c1e-babd-40a8-bfed-920fc9b8c77',
            id: mockFieldVariables[0].fieldId
        };

        beforeEach(() => {
            fixtureHost.componentInstance.value = CUSTOM_FIELD;
        });

        it('should filter out customFieldOptions variable', () => {
            jest.spyOn(dotFieldVariableService, 'load').mockReturnValue(
                of([
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                        fieldId: mockFieldVariables[0].fieldId,
                        id: 'options-id',
                        key: 'customFieldOptions',
                        value: '{"showAsModal":true}'
                    }
                ])
            );
            fixtureHost.detectChanges();
            expect(comp.$fieldVariables().length).toBe(0);
        });

        it('should NOT filter out newRenderMode variable', () => {
            jest.spyOn(dotFieldVariableService, 'load').mockReturnValue(
                of([
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                        fieldId: mockFieldVariables[0].fieldId,
                        id: 'render-mode-id',
                        key: 'newRenderMode',
                        value: 'IFRAME'
                    }
                ])
            );
            fixtureHost.detectChanges();
            expect(comp.$fieldVariables().length).toBe(1);
            expect(comp.$fieldVariables()[0].key).toBe('newRenderMode');
        });

        it('should display other variables while filtering customFieldOptions', () => {
            jest.spyOn(dotFieldVariableService, 'load').mockReturnValue(
                of([
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                        fieldId: mockFieldVariables[0].fieldId,
                        id: 'other-id',
                        key: 'someOtherKey',
                        value: 'someValue'
                    },
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                        fieldId: mockFieldVariables[0].fieldId,
                        id: 'options-id',
                        key: 'customFieldOptions',
                        value: '{}'
                    }
                ])
            );
            fixtureHost.detectChanges();
            expect(comp.$fieldVariables().length).toBe(1);
            expect(comp.$fieldVariables()[0].key).toBe('someOtherKey');
        });
    });
});
