import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { JsonPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { MultiSelect } from 'primeng/multiselect';

import { DotMessageService, DotWorkflowService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService, mockWorkflows } from '@dotcms/utils-testing';

import { DotWorkflowsSelectorFieldComponent } from './dot-workflows-selector-field.component';

const messageServiceMock = new MockDotMessageService({
    'dot.common.select.workflows': 'Pick it up',
    'dot.common.archived': 'Archivado'
});

const mockDotWorkflowService = {
    get: jest.fn().mockReturnValue(of(structuredClone(mockWorkflows)))
};

@Component({
    selector: 'dot-fake-form',
    template: `
        <form [formGroup]="form">
            <dot-workflows-selector-field
                formControlName="workflows"></dot-workflows-selector-field>
            {{ form.value | json }}
        </form>
    `,
    standalone: true,
    imports: [ReactiveFormsModule, DotWorkflowsSelectorFieldComponent, DotMessagePipe, JsonPipe]
})
class FakeFormComponent {
    private fb = inject(UntypedFormBuilder);

    form: UntypedFormGroup = this.fb.group({
        workflows: [{ value: mockWorkflows, disabled: false }]
    });
}

describe('DotWorkflowsSelectorFieldComponent', () => {
    describe('basic', () => {
        let spectator: Spectator<DotWorkflowsSelectorFieldComponent>;
        let multiselect: MultiSelect;

        const createComponent = createComponentFactory({
            component: DotWorkflowsSelectorFieldComponent,
            detectChanges: false,
            imports: [DotWorkflowsSelectorFieldComponent, DotMessagePipe, NoopAnimationsModule],
            componentProviders: [{ provide: DotWorkflowService, useValue: mockDotWorkflowService }],
            providers: [
                { provide: DotWorkflowService, useValue: mockDotWorkflowService },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        beforeEach(() => {
            mockDotWorkflowService.get.mockClear();
            spectator = createComponent();
            jest.spyOn(spectator.component, 'propagateChange');
        });

        describe('no params', () => {
            beforeEach(() => {
                spectator.detectChanges();
                const multiSelectEl = spectator.debugElement.query(By.directive(MultiSelect));
                multiselect = multiSelectEl?.componentInstance as MultiSelect;
            });

            it('should have a multiselect', () => {
                expect(multiselect).not.toBe(null);
            });

            it('should have maxSelectedLabels set correctly', () => {
                expect(multiselect.maxSelectedLabels).toBe(3);
            });

            it('should have placeholder', () => {
                expect(multiselect).toBeTruthy();
                expect(multiselect.maxSelectedLabels).toBe(3);
                const appendTo =
                    typeof multiselect.appendTo === 'function'
                        ? (multiselect.appendTo as () => string)()
                        : multiselect.appendTo;
                expect(appendTo).toEqual('body');
            });

            it('should have append to body', () => {
                const appendTo =
                    typeof multiselect.appendTo === 'function'
                        ? (multiselect.appendTo as () => string)()
                        : multiselect.appendTo;
                expect(appendTo).toEqual('body');
            });

            it('should get workflow list from server', () => {
                expect(mockDotWorkflowService.get).toHaveBeenCalledTimes(1);
            });

            it('should have options', () => {
                expect(multiselect.options).toEqual([
                    expect.objectContaining({
                        id: '85c1515c-c4f3-463c-bac2-860b8fcacc34',
                        creationDate: expect.anything(),
                        name: 'Default Scheme',
                        description:
                            'This is the default workflow scheme that will be applied to all content',
                        archived: false,
                        mandatory: false,
                        defaultScheme: true,
                        modDate: expect.anything(),
                        entryActionId: null,
                        system: false
                    }),
                    expect.objectContaining({
                        id: '77a9bf3f-a402-4c56-9b1f-1050b9d345dc',
                        creationDate: expect.anything(),
                        name: 'Document Management',
                        description: 'Default workflow for documents',
                        archived: true,
                        mandatory: false,
                        defaultScheme: false,
                        modDate: expect.anything(),
                        entryActionId: null,
                        system: false
                    }),
                    expect.objectContaining({
                        id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
                        creationDate: expect.anything(),
                        name: 'System Workflow',
                        description: '',
                        archived: false,
                        mandatory: false,
                        defaultScheme: false,
                        modDate: expect.anything(),
                        entryActionId: null,
                        system: true
                    })
                ]);
            });
        });
    });

    describe('value accessor', () => {
        let spectator: Spectator<FakeFormComponent>;
        let fieldComponent: DotWorkflowsSelectorFieldComponent;
        let innerMultiselectEl: ReturnType<Spectator<FakeFormComponent>['debugElement']['query']>;

        const createHostComponent = createComponentFactory({
            component: FakeFormComponent,
            imports: [
                ReactiveFormsModule,
                DotWorkflowsSelectorFieldComponent,
                DotMessagePipe,
                NoopAnimationsModule
            ],
            providers: [
                { provide: DotWorkflowService, useValue: mockDotWorkflowService },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        beforeEach(() => {
            spectator = createHostComponent();
            const fieldEl = spectator.debugElement.query(By.css('dot-workflows-selector-field'));
            fieldComponent = fieldEl?.componentInstance as DotWorkflowsSelectorFieldComponent;
            innerMultiselectEl = fieldEl?.query(By.directive(MultiSelect));
        });

        it('should get value', () => {
            spectator.detectChanges();
            expect(fieldComponent.value).toEqual(mockWorkflows);
        });

        it('should propagate value', () => {
            spectator.detectChanges();
            innerMultiselectEl?.triggerEventHandler('onChange', {
                originalEvent: {},
                value: ['123']
            });
            spectator.detectChanges();
            expect(spectator.component.form.value).toEqual({ workflows: ['123'] });
        });

        it('should be enabled by default', () => {
            spectator.detectChanges();
            const multiSelect = innerMultiselectEl?.componentInstance as MultiSelect;
            const disabled =
                typeof multiSelect?.disabled === 'function'
                    ? (multiSelect.disabled as () => boolean)()
                    : multiSelect?.disabled;
            expect(disabled).toBe(false);
        });

        it('should set disabled', async () => {
            spectator.component.form.get('workflows')?.disable();
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            const multiSelect = innerMultiselectEl?.componentInstance as MultiSelect;
            const disabled =
                typeof multiSelect?.disabled === 'function'
                    ? (multiSelect.disabled as () => boolean)()
                    : multiSelect?.disabled;
            expect(disabled).toBe(true);
        });
    });
});
