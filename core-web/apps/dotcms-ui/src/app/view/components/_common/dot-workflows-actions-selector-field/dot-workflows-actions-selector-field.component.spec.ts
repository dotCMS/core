import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { BehaviorSubject } from 'rxjs';

import { JsonPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SelectItemGroup } from 'primeng/api';
import { Select, SelectModule } from 'primeng/select';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSWorkflow } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService, mockWorkflows } from '@dotcms/utils-testing';

import { DotWorkflowsActionsSelectorFieldComponent } from './dot-workflows-actions-selector-field.component';
import { DotWorkflowsActionsSelectorFieldService } from './services/dot-workflows-actions-selector-field.service';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.selector.workflow.action': 'Select an action'
});

let mockActionsGrouped: SelectItemGroup[] = [
    {
        label: 'Workflow 1',
        value: 'workflow',
        items: [
            { label: 'Hello', value: '123' },
            { label: 'World', value: '456' }
        ]
    }
];

class DotWorkflowsActionsSelectorFieldServiceMock {
    private data$ = new BehaviorSubject<SelectItemGroup[]>([]);

    get() {
        return this.data$;
    }

    load() {
        this.data$.next(mockActionsGrouped);
    }
}

@Component({
    selector: 'dot-fake-form',
    template: `
        <form [formGroup]="form">
            <dot-workflows-actions-selector-field
                [workflows]="workflows"
                formControlName="action" />
            {{ form.value | json }}
        </form>
    `,
    standalone: true,
    imports: [
        ReactiveFormsModule,
        DotWorkflowsActionsSelectorFieldComponent,
        DotMessagePipe,
        JsonPipe
    ]
})
class FakeFormComponent implements OnInit {
    private fb = inject(UntypedFormBuilder);

    form: UntypedFormGroup;
    workflows: DotCMSWorkflow[] = [];

    ngOnInit() {
        this.form = this.fb.group({
            action: [{ value: '456', disabled: false }]
        });
    }
}

describe('DotWorkflowsActionsSelectorFieldComponent', () => {
    let spectator: Spectator<FakeFormComponent>;
    let fieldComponent: DotWorkflowsActionsSelectorFieldComponent;
    let serviceMock: DotWorkflowsActionsSelectorFieldServiceMock;

    const createHost = createComponentFactory({
        component: FakeFormComponent,
        imports: [
            ReactiveFormsModule,
            DotWorkflowsActionsSelectorFieldComponent,
            SelectModule,
            DotMessagePipe,
            NoopAnimationsModule
        ],
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            {
                provide: DotWorkflowsActionsSelectorFieldService,
                useClass: DotWorkflowsActionsSelectorFieldServiceMock
            }
        ],
        detectChanges: false
    });

    function getFieldDe() {
        return spectator.debugElement.query(By.css('dot-workflows-actions-selector-field'));
    }

    /** Template uses p-select (PrimeNG Select), not p-dropdown. */
    function getSelectDe() {
        return getFieldDe()?.query(By.css('p-select')) ?? getFieldDe()?.query(By.directive(Select));
    }

    function getSelectInstance(): Select | null {
        const el = getSelectDe();
        return el?.componentInstance ?? null;
    }

    beforeEach(() => {
        mockActionsGrouped = [
            {
                label: 'Workflow 1',
                value: 'workflow',
                items: [
                    { label: 'Hello', value: '123' },
                    { label: 'World', value: '456' }
                ]
            }
        ];
        spectator = createHost();
        const fieldEl = getFieldDe();
        fieldComponent = fieldEl?.componentInstance as DotWorkflowsActionsSelectorFieldComponent;
        serviceMock = spectator.inject(
            DotWorkflowsActionsSelectorFieldService
        ) as unknown as DotWorkflowsActionsSelectorFieldServiceMock;
        jest.spyOn(serviceMock, 'get');
        jest.spyOn(serviceMock, 'load');
    });

    describe('initialization', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should load actions', () => {
            expect(serviceMock.load).toHaveBeenCalledTimes(1);
            expect(serviceMock.load).toHaveBeenCalledWith([]);
        });

        it('should subscribe to actions', () => {
            expect(serviceMock.get).toHaveBeenCalledTimes(1);
        });
    });

    describe('p-select', () => {
        describe('attributes', () => {
            describe('basics', () => {
                beforeEach(() => {
                    spectator.detectChanges();
                });

                it('should have basics', () => {
                    const dropdown = getSelectInstance();
                    expect(dropdown).toBeTruthy();
                    const appendTo =
                        typeof dropdown?.appendTo === 'function'
                            ? (dropdown.appendTo as () => string)()
                            : dropdown?.appendTo;
                    expect(appendTo).toBe('body');
                    expect(dropdown?.group).toBe(true);
                    const placeholder =
                        typeof dropdown?.placeholder === 'function'
                            ? (dropdown.placeholder as () => string)()
                            : dropdown?.placeholder;
                    expect(placeholder).toBe('Select an action');
                });
            });

            describe('disable', () => {
                it('should be disable when actions list is empty', async () => {
                    mockActionsGrouped = [];
                    spectator.detectChanges();
                    await spectator.fixture.whenStable();
                    spectator.detectChanges();
                    const dropdown = getSelectInstance();
                    const disabled =
                        typeof dropdown?.disabled === 'function'
                            ? (dropdown.disabled as () => boolean)()
                            : dropdown?.disabled;
                    expect(disabled).toBe(true);
                });

                it('should be enabled when actions list is filled', () => {
                    spectator.detectChanges();
                    const dropdown = getSelectInstance();
                    const disabled =
                        typeof dropdown?.disabled === 'function'
                            ? (dropdown.disabled as () => boolean)()
                            : dropdown?.disabled;
                    expect(disabled).toBe(false);
                });
            });

            describe('options', () => {
                it('should have', () => {
                    spectator.detectChanges();
                    const dropdown = getSelectInstance();
                    expect(dropdown?.options).toEqual([
                        {
                            label: 'Workflow 1',
                            value: 'workflow',
                            items: [
                                { label: 'Hello', value: '123' },
                                { label: 'World', value: '456' }
                            ]
                        }
                    ]);
                });

                it('should not have', () => {
                    mockActionsGrouped = [];
                    spectator.detectChanges();
                    const dropdown = getSelectInstance();
                    expect(dropdown?.options).toEqual([]);
                });
            });
        });
    });

    describe('ControlValueAccessor', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should set value', () => {
            expect(fieldComponent.value).toBe('456');
        });

        it('should propagate changes', () => {
            const selectDe = getSelectDe();
            selectDe?.triggerEventHandler('onChange', {
                originalEvent: {},
                value: '123'
            });
            spectator.detectChanges();
            expect(spectator.component.form.value).toEqual({ action: '123' });
        });

        it('should propagate empty string', () => {
            const selectDe = getSelectDe();
            selectDe?.triggerEventHandler('onChange', {
                originalEvent: {},
                value: null
            });
            spectator.detectChanges();
            expect(spectator.component.form.value).toEqual({ action: '' });
        });

        it('should set disabled', () => {
            spectator.component.form.get('action')?.disable();
            spectator.detectChanges();
            const dropdown = getSelectInstance();
            const disabled =
                typeof dropdown?.disabled === 'function'
                    ? (dropdown.disabled as () => boolean)()
                    : dropdown?.disabled;
            expect(disabled).toBe(true);
        });
    });

    describe('@Inputs', () => {
        describe('workflows', () => {
            it('should reload actions', () => {
                const mock = [mockWorkflows[0], mockWorkflows[1]];
                spectator.component.workflows = mock;
                spectator.detectChanges();
                expect(serviceMock.load).toHaveBeenCalledWith(expect.arrayContaining(mock));
            });
        });
    });

    describe('clear', () => {
        it('should clear value when options change and current value is not in list', () => {
            spectator.detectChanges();
            expect(fieldComponent.value).toBe('456');

            mockActionsGrouped = [
                {
                    label: '',
                    value: '',
                    items: [{ label: '', value: '' }]
                }
            ];
            spectator.component.workflows = [{ ...mockWorkflows[1] }];
            spectator.fixture.detectChanges(false);
            fieldComponent.handleChange({ value: '' });
            spectator.detectChanges();
            expect(spectator.component.form.value).toEqual({ action: '' });
        });
    });
});
