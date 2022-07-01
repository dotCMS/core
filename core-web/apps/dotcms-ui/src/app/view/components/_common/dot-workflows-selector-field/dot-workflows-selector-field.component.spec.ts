import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { DebugElement, Component } from '@angular/core';
import { waitForAsync } from '@angular/core/testing';

import { DotWorkflowsSelectorFieldComponent } from './dot-workflows-selector-field.component';

import { DotWorkflowService } from './../../../../api/services/dot-workflow/dot-workflow.service';
import { MultiSelect } from 'primeng/multiselect';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { UntypedFormGroup, UntypedFormBuilder } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { mockWorkflows, DotWorkflowServiceMock } from '@tests/dot-workflow-service.mock';
import { DOTTestBed } from '@tests/dot-test-bed';

const messageServiceMock = new MockDotMessageService({
    'dot.common.select.workflows': 'Pick it up',
    'dot.common.archived': 'Archivado'
});

@Component({
    selector: 'dot-fake-form',
    template: `
        <form [formGroup]="form">
            <dot-workflows-selector-field
                formControlName="workflows"
            ></dot-workflows-selector-field>
            {{ form.value | json }}
        </form>
    `
})
class FakeFormComponent {
    form: UntypedFormGroup;

    constructor(private fb: UntypedFormBuilder) {
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
        beforeEach(
            waitForAsync(() => {
                DOTTestBed.configureTestingModule({
                    declarations: [DotWorkflowsSelectorFieldComponent],
                    providers: [
                        {
                            provide: DotWorkflowService,
                            useClass: DotWorkflowServiceMock
                        },
                        {
                            provide: DotMessageService,
                            useValue: messageServiceMock
                        }
                    ],
                    imports: [BrowserAnimationsModule]
                });

                fixture = DOTTestBed.createComponent(DotWorkflowsSelectorFieldComponent);
                component = fixture.componentInstance;
                de = fixture.debugElement;
                dotWorkflowService = de.injector.get(DotWorkflowService);
                spyOn(dotWorkflowService, 'get').and.callThrough();
                spyOn(component, 'propagateChange');
            })
        );

        describe('no params', () => {
            beforeEach(() => {
                fixture.detectChanges();
                multiselect = de.query(By.css('p-multiSelect')).componentInstance;
            });

            it('should have have a multiselect', () => {
                expect(multiselect).not.toBe(null);
            });

            it('should have maxSelectedLabels set correctly', () => {
                expect(multiselect.maxSelectedLabels).toBe(3);
            });

            it('should have default label', () => {
                expect(multiselect.defaultLabel).toEqual('Pick it up');
            });

            it('should have append to bobdy', () => {
                expect(multiselect.appendTo).toEqual('body');
            });

            it('should get workflow list from server', () => {
                expect(dotWorkflowService.get).toHaveBeenCalledTimes(1);
            });

            describe('show options', () => {
                beforeEach(() => {
                    de.query(By.css('.p-multiselect')).triggerEventHandler('click', {
                        target: {
                            isSameNode: () => false
                        }
                    });
                    fixture.detectChanges();
                });

                it('should fill the workflows options', () => {
                    const itemsLabels = de
                        .queryAll(By.css('.p-multiselect-items .workflow__label'))
                        .map((item) => item.nativeElement.innerText);
                    expect(itemsLabels).toEqual(mockWorkflows.map((workflow) => workflow.name));
                });

                it('should have archived item and message', () => {
                    const archivedItems = de.queryAll(By.css('.workflow__archive-label'));
                    expect(archivedItems.length).toBe(1, 'archivedItems');
                    expect(archivedItems[0].nativeElement.innerText).toBe(mockWorkflows[1].name);
                    const archivedMessage = de.queryAll(By.css('.workflow__archive-message'));
                    expect(archivedMessage.length).toBe(1, 'archivedMessage');
                    expect(archivedMessage[0].nativeElement.innerText).toBe('(Archivado)');
                });
            });
        });
    });

    describe('value accessor', () => {
        let fixtureHost: ComponentFixture<FakeFormComponent>;
        let deHost: DebugElement;
        let innerMultiselect: DebugElement;

        beforeEach(
            waitForAsync(() => {
                DOTTestBed.configureTestingModule({
                    declarations: [FakeFormComponent, DotWorkflowsSelectorFieldComponent],
                    providers: [
                        {
                            provide: DotWorkflowService,
                            useClass: DotWorkflowServiceMock
                        },
                        {
                            provide: DotMessageService,
                            useValue: messageServiceMock
                        }
                    ],
                    imports: []
                });

                fixtureHost = DOTTestBed.createComponent(FakeFormComponent);
                deHost = fixtureHost.debugElement;
                component = deHost.query(By.css('dot-workflows-selector-field')).componentInstance;
                innerMultiselect = deHost
                    .query(By.css('dot-workflows-selector-field'))
                    .query(By.css('p-multiSelect'));
            })
        );

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

        it('should set disabled', () => {
            fixtureHost.componentInstance.form.get('workflows').disable();
            fixtureHost.detectChanges();
            expect(innerMultiselect.componentInstance.disabled).toBe(true);
        });
    });
});
