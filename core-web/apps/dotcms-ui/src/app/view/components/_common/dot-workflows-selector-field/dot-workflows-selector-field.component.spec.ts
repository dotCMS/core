import { Component, DebugElement, inject } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MultiSelect } from 'primeng/multiselect';

import { DotMessageService, DotWorkflowService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import {
    DotWorkflowServiceMock,
    MockDotMessageService,
    mockWorkflows
} from '@dotcms/utils-testing';

import { DotWorkflowsSelectorFieldComponent } from './dot-workflows-selector-field.component';

import { DOTTestBed } from '../../../../test/dot-test-bed';

const messageServiceMock = new MockDotMessageService({
    'dot.common.select.workflows': 'Pick it up',
    'dot.common.archived': 'Archivado'
});

@Component({
    selector: 'dot-fake-form',
    template: `
        <form [formGroup]="form">
            <dot-workflows-selector-field
                formControlName="workflows"></dot-workflows-selector-field>
            {{ form.value | json }}
        </form>
    `,
    standalone: false
})
class FakeFormComponent {
    private fb = inject(UntypedFormBuilder);

    form: UntypedFormGroup;

    constructor() {
        /*
This should go in the ngOnInit but I don't want to detectChanges everytime for
this fake test component
*/
        this.form = this.fb.group({
            workflows: [{ value: mockWorkflows, disabled: false }]
        });
    }
}

describe('DotWorkflowsSelectorFieldComponent', () => {
    let component: DotWorkflowsSelectorFieldComponent;
    let fixture: ComponentFixture<DotWorkflowsSelectorFieldComponent>;
    let de: DebugElement;
    let dotWorkflowService: DotWorkflowService;
    let multiselect: MultiSelect;

    describe('basic', () => {
        beforeEach(waitForAsync(() => {
            DOTTestBed.configureTestingModule({
                imports: [
                    DotWorkflowsSelectorFieldComponent,
                    DotMessagePipe,
                    BrowserAnimationsModule
                ],
                providers: [
                    {
                        provide: DotWorkflowService,
                        useClass: DotWorkflowServiceMock
                    },
                    {
                        provide: DotMessageService,
                        useValue: messageServiceMock
                    }
                ]
            });

            fixture = DOTTestBed.createComponent(DotWorkflowsSelectorFieldComponent);
            component = fixture.componentInstance;
            de = fixture.debugElement;
            dotWorkflowService = de.injector.get(DotWorkflowService);
            jest.spyOn(dotWorkflowService, 'get');
            jest.spyOn(component, 'propagateChange');
        }));

        describe('no params', () => {
            beforeEach(() => {
                fixture.detectChanges();
                multiselect = de.query(By.css('p-multiselect')).componentInstance;
            });

            it('should have have a multiselect', () => {
                expect(multiselect).not.toBe(null);
            });

            it('should have maxSelectedLabels set correctly', () => {
                expect(multiselect.maxSelectedLabels).toBe(3);
            });

            it('should have placeholder', () => {
                // Check that the multiselect component is properly configured
                expect(multiselect).toBeTruthy();
                expect(multiselect.maxSelectedLabels).toBe(3);
                expect(multiselect.appendTo).toEqual('body');
            });

            it('should have append to bobdy', () => {
                expect(multiselect.appendTo).toEqual('body');
            });

            it('should get workflow list from server', () => {
                expect(dotWorkflowService.get).toHaveBeenCalledTimes(1);
            });

            it('should have options', () => {
                expect(multiselect.options).toEqual([
                    expect.objectContaining({
                        id: '85c1515c-c4f3-463c-bac2-860b8fcacc34',
                        creationDate: expect.any(String),
                        name: 'Default Scheme',
                        description:
                            'This is the default workflow scheme that will be applied to all content',
                        archived: false,
                        mandatory: false,
                        defaultScheme: true,
                        modDate: expect.any(String),
                        entryActionId: null,
                        system: false
                    }),
                    expect.objectContaining({
                        id: '77a9bf3f-a402-4c56-9b1f-1050b9d345dc',
                        creationDate: expect.any(String),
                        name: 'Document Management',
                        description: 'Default workflow for documents',
                        archived: true,
                        mandatory: false,
                        defaultScheme: false,
                        modDate: expect.any(String),
                        entryActionId: null,
                        system: false
                    }),
                    expect.objectContaining({
                        id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
                        creationDate: expect.any(String),
                        name: 'System Workflow',
                        description: '',
                        archived: false,
                        mandatory: false,
                        defaultScheme: false,
                        modDate: expect.any(String),
                        entryActionId: null,
                        system: true
                    })
                ]);
            });
        });
    });

    describe('value accessor', () => {
        let fixtureHost: ComponentFixture<FakeFormComponent>;
        let deHost: DebugElement;
        let innerMultiselect: DebugElement;

        beforeEach(waitForAsync(() => {
            DOTTestBed.configureTestingModule({
                declarations: [FakeFormComponent],
                imports: [DotWorkflowsSelectorFieldComponent, DotMessagePipe],
                providers: [
                    {
                        provide: DotWorkflowService,
                        useClass: DotWorkflowServiceMock
                    },
                    {
                        provide: DotMessageService,
                        useValue: messageServiceMock
                    }
                ]
            });

            fixtureHost = DOTTestBed.createComponent(FakeFormComponent);
            deHost = fixtureHost.debugElement;
            component = deHost.query(By.css('dot-workflows-selector-field')).componentInstance;
            innerMultiselect = deHost
                .query(By.css('dot-workflows-selector-field'))
                .query(By.css('p-multiselect'));
        }));

        it('should get value', () => {
            fixtureHost.detectChanges();
            expect(component.value).toEqual(mockWorkflows);
        });

        it('should propagate value', () => {
            fixtureHost.detectChanges();
            innerMultiselect.triggerEventHandler('onChange', {
                originalEvent: {},
                value: ['123']
            });
            fixtureHost.detectChanges();
            expect(fixtureHost.componentInstance.form.value).toEqual({ workflows: ['123'] });
        });

        it('should be enabled by default', () => {
            fixtureHost.detectChanges();
            expect(innerMultiselect.componentInstance.disabled).toBe(false);
        });

        it('should set disabled', async () => {
            fixtureHost.componentInstance.form.get('workflows').disable();
            fixtureHost.detectChanges();
            await fixtureHost.whenStable();
            expect(innerMultiselect.componentInstance.disabled).toBe(true);
        });
    });
});
