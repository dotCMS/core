import { waitForAsync, ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '@tests/dot-test-bed';
import { BehaviorSubject } from 'rxjs';
import { DebugElement, Component, OnInit } from '@angular/core';

import { DotWorkflowsActionsSelectorFieldComponent } from './dot-workflows-actions-selector-field.component';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { By } from '@angular/platform-browser';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { DotWorkflowsActionsSelectorFieldService } from './services/dot-workflows-actions-selector-field.service';
import { DotCMSWorkflow } from '@dotcms/dotcms-models';
import { mockWorkflows } from '@tests/dot-workflow-service.mock';
import { SelectItemGroup } from 'primeng/api';
import { Dropdown, DropdownModule } from 'primeng/dropdown';

@Component({
    selector: 'dot-fake-form',
    template: `
        <form [formGroup]="form">
            <dot-workflows-actions-selector-field
                formControlName="action"
                [workflows]="workfows"
            ></dot-workflows-actions-selector-field>
            {{ form.value | json }}
        </form>
    `
})
class FakeFormComponent implements OnInit {
    form: UntypedFormGroup;
    workfows: DotCMSWorkflow[] = [];

    constructor(private fb: UntypedFormBuilder) {}

    ngOnInit() {
        this.form = this.fb.group({
            action: [{ value: '456', disabled: false }]
        });
    }
}

const messageServiceMock = new MockDotMessageService({
    'contenttypes.selector.workflow.action': 'Select an action'
});

let mockActionsGrouped: SelectItemGroup[];

class DotWorkflowsActionsSelectorFieldServiceMock {
    private data$: BehaviorSubject<SelectItemGroup[]> = new BehaviorSubject([]);

    get() {
        return this.data$;
    }

    load() {
        this.data$.next(mockActionsGrouped);
    }
}

describe('DotWorkflowsActionsSelectorFieldComponent', () => {
    let fixtureHost: ComponentFixture<FakeFormComponent>;
    let deHost: DebugElement;
    let componentHost: FakeFormComponent;
    let component: DotWorkflowsActionsSelectorFieldComponent;
    let de: DebugElement;
    let dropdownDe: DebugElement;
    let dropdown: Dropdown;
    let dotWorkflowsActionsSelectorFieldService: DotWorkflowsActionsSelectorFieldService;

    const getDropdownDebugElement = () => de.query(By.css('p-dropdown'));
    const getDropdownComponent = () => getDropdownDebugElement().componentInstance;

    beforeEach(
        waitForAsync(() => {
            DOTTestBed.configureTestingModule({
                declarations: [DotWorkflowsActionsSelectorFieldComponent, FakeFormComponent],
                providers: [
                    {
                        provide: DotMessageService,
                        useValue: messageServiceMock
                    },
                    {
                        provide: DotWorkflowsActionsSelectorFieldService,
                        useClass: DotWorkflowsActionsSelectorFieldServiceMock
                    }
                ],
                imports: [DropdownModule]
            });
        })
    );

    beforeEach(() => {
        fixtureHost = DOTTestBed.createComponent(FakeFormComponent);
        deHost = fixtureHost.debugElement;
        componentHost = deHost.componentInstance;
        de = deHost.query(By.css('dot-workflows-actions-selector-field'));
        component = de.componentInstance;

        mockActionsGrouped = [
            {
                label: 'Workflow 1',
                value: 'workflow',
                items: [
                    {
                        label: 'Hello',
                        value: '123'
                    },
                    {
                        label: 'World',
                        value: '456'
                    }
                ]
            }
        ];

        dotWorkflowsActionsSelectorFieldService = deHost.injector.get(
            DotWorkflowsActionsSelectorFieldService
        );

        spyOn(dotWorkflowsActionsSelectorFieldService, 'get').and.callThrough();
        spyOn(dotWorkflowsActionsSelectorFieldService, 'load').and.callThrough();
    });

    describe('initialization', () => {
        beforeEach(() => {
            fixtureHost.detectChanges();
        });

        it('should load actions', () => {
            expect(dotWorkflowsActionsSelectorFieldService.load).toHaveBeenCalledTimes(1);
            expect(dotWorkflowsActionsSelectorFieldService.load).toHaveBeenCalledWith([]);
        });

        it('should subscribe to actions', () => {
            expect(dotWorkflowsActionsSelectorFieldService.get).toHaveBeenCalledTimes(1);
        });
    });

    describe('p-dropdown', () => {
        describe('attributes', () => {
            describe('basics', () => {
                beforeEach(() => {
                    fixtureHost.detectChanges();
                    dropdown = getDropdownComponent();
                });

                it('should have basics', () => {
                    expect(dropdown.appendTo).toBe('body');
                    expect(dropdown.group).toBe(true);
                    expect(dropdown.placeholder).toBe('Select an action');
                    expect(dropdown.style).toEqual({ width: '100%' });
                    expect(dropdown.autoDisplayFirst).toBe(false);
                });
            });

            describe('disable', () => {
                it('should be disable when actions list is empty', () => {
                    mockActionsGrouped = [];
                    fixtureHost.detectChanges();
                    dropdown = getDropdownComponent();
                    expect(dropdown.disabled).toBe(true);
                });

                it('should be enaled when actions list is filled', () => {
                    fixtureHost.detectChanges();
                    dropdown = getDropdownComponent();
                    expect(dropdown.disabled).toBe(false);
                });
            });

            describe('options', () => {
                it('should have', () => {
                    fixtureHost.detectChanges();
                    dropdown = getDropdownComponent();
                    expect(dropdown.options).toEqual([
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
                    fixtureHost.detectChanges();
                    dropdown = getDropdownComponent();
                    expect(dropdown.options).toEqual([]);
                });
            });
        });
    });

    describe('ControlValueAccessor', () => {
        beforeEach(() => {
            fixtureHost.detectChanges();
            dropdownDe = getDropdownDebugElement();
            dropdown = dropdownDe.componentInstance;
        });

        it('should set value', () => {
            expect(component.value).toBe('456');
        });

        it('should propagate changes', () => {
            dropdownDe.triggerEventHandler('onChange', {
                originalEvent: {},
                value: '123'
            });

            expect(componentHost.form.value).toEqual({
                action: '123'
            });
        });

        it('should propagate empty string', () => {
            dropdownDe.triggerEventHandler('onChange', {
                originalEvent: {},
                value: null
            });

            expect(componentHost.form.value).toEqual({
                action: ''
            });
        });

        it('should set disabled', () => {
            componentHost.form.get('action').disable();
            fixtureHost.detectChanges();
            expect(dropdown.disabled).toBe(true);
        });
    });

    describe('@Inputs', () => {
        describe('workflows', () => {
            it('should reload actions', () => {
                const mock = [mockWorkflows[0], mockWorkflows[1]];
                componentHost.workfows = mock;
                fixtureHost.detectChanges();
                expect(dotWorkflowsActionsSelectorFieldService.load).toHaveBeenCalledWith(
                    jasmine.arrayContaining(mock)
                );
            });
        });
    });

    describe('clear', () => {
        it('should', () => {
            fixtureHost.detectChanges();
            expect(component.value).toBe('456');
            mockActionsGrouped = [
                {
                    label: '',
                    value: '',
                    items: [
                        {
                            label: '',
                            value: ''
                        }
                    ]
                }
            ];

            componentHost.workfows = [
                {
                    ...mockWorkflows[1]
                }
            ];
            fixtureHost.detectChanges();
            expect(componentHost.form.value).toEqual({ action: '' });
        });
    });
});
