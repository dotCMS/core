/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { Component, DebugElement, EventEmitter, HostBinding, Input, Output } from '@angular/core';
import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DotContainerSelectorLayoutModule } from '@components/dot-container-selector-layout/dot-container-selector-layout.module';
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { DotEditLayoutService } from '@dotcms/app/api/services/dot-edit-layout/dot-edit-layout.service';
import { DotTemplateContainersCacheService } from '@dotcms/app/api/services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import {
    DotAlertConfirmService,
    DotEventsService,
    DotMessageService,
    PaginatorService
} from '@dotcms/data-access';
import { NgGridModule } from '@dotcms/dot-layout-grid';
import { DotAutofocusModule } from '@dotcms/dot-rules';
import { DotLayoutBody } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditLayoutGridComponent } from './dot-edit-layout-grid.component';

let fakeValue: DotLayoutBody;

@Component({
    selector: 'dot-test-host-component',
    template: `
        <form [formGroup]="form">
            <dot-edit-layout-grid formControlName="body"></dot-edit-layout-grid>
        </form>
    `
})
class TestHostComponent {
    form: UntypedFormGroup;

    constructor() {
        this.createForm();
    }

    createForm(): void {
        this.form = new UntypedFormGroup({
            body: new UntypedFormControl(fakeValue)
        });
    }
}

@Component({
    selector: 'dot-dialog',
    template: '<ng-content></ng-content>'
})
class DotDialogMockComponent {
    @Input()
    actions: DotDialogActions;

    @Input()
    @HostBinding('class.active')
    visible: boolean;

    @Input()
    header = '';

    @Input()
    width: string;

    @Output()
    hide: EventEmitter<any> = new EventEmitter();

    close(): void {}
}

@Component({
    selector: 'dot-icon-button',
    template: ''
})
class UiDotIconButtonMockComponent {}

describe('DotEditLayoutGridComponent', () => {
    let component: DotEditLayoutGridComponent;
    let de: DebugElement;
    let hostComponentfixture: ComponentFixture<TestHostComponent>;
    let dotEditLayoutService: DotEditLayoutService;

    beforeEach(() => {
        fakeValue = {
            rows: [
                {
                    styleClass: '',
                    columns: [
                        {
                            styleClass: '',
                            containers: [],
                            leftOffset: 1,
                            width: 2
                        }
                    ]
                }
            ]
        };

        const messageServiceMock = new MockDotMessageService({
            cancel: 'Cancel',
            'dot.common.dialog.accept': 'Accept',
            'dot.common.dialog.reject': 'Cancel',
            'editpage.action.cancel': 'Cancel',
            'editpage.action.delete': 'Delete',
            'editpage.action.save': 'Save',
            'editpage.confirm.header': 'Header',
            'editpage.confirm.message.delete': 'Delete',
            'editpage.confirm.message.delete.warning': 'Warning',
            'editpage.layout.css.class.add.to.box': 'Add class to box',
            'editpage.layout.css.class.add.to.row': 'Add class to row'
        });

        DOTTestBed.configureTestingModule({
            declarations: [
                DotEditLayoutGridComponent,
                TestHostComponent,
                DotDialogMockComponent,
                UiDotIconButtonMockComponent
            ],
            imports: [
                NgGridModule,
                DotContainerSelectorLayoutModule,
                BrowserAnimationsModule,
                UiDotIconButtonTooltipModule,
                DotAutofocusModule,
                DotMessagePipe
            ],
            providers: [
                DotAlertConfirmService,
                DotEditLayoutService,
                DotTemplateContainersCacheService,
                PaginatorService,
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        hostComponentfixture = DOTTestBed.createComponent(TestHostComponent);
        de = hostComponentfixture.debugElement.query(By.css('dot-edit-layout-grid'));
        component = de.componentInstance;

        dotEditLayoutService = de.injector.get(DotEditLayoutService);

        hostComponentfixture.detectChanges();
    });

    it('should show set one element in the grid of 12 columns', () => {
        hostComponentfixture.componentInstance.form = new UntypedFormGroup({
            body: new UntypedFormControl({})
        });

        hostComponentfixture.detectChanges();

        expect(component.grid.boxes.length).toEqual(1);
        expect(component.grid.boxes[0].config.sizex).toEqual(12);
    });

    it('should subscribe to layour service and call addBox', () => {
        spyOn(component, 'addBox');
        dotEditLayoutService.addBox();
        expect(component.addBox).toHaveBeenCalledTimes(1);
    });

    it('should add one Container to the grid of 3 columns', () => {
        component.addBox();
        expect(component.grid.boxes.length).toEqual(2);
        expect(component.grid.boxes[1].config.sizex).toEqual(3);
    });

    it('should add a new Container in the same row', () => {
        component.addBox();
        component.addBox();

        expect(component.grid.boxes.length).toEqual(3);
        expect(component.grid.boxes[1].config.row).toEqual(1);
        expect(component.grid.boxes[2].config.row).toEqual(1);
    });

    it('should each box has its own payload object', () => {
        component.addBox();
        component.addBox();

        const length = component.grid.boxes.length;

        expect(component.grid.boxes[length - 1].config.payload).not.toBe(
            component.grid.boxes[length - 2].config.payload
        );
    });

    it('should add a new add class button', () => {
        fakeValue.rows[0].columns[0].width = 12;
        hostComponentfixture.componentInstance.form = new UntypedFormGroup({
            body: new UntypedFormControl(fakeValue)
        });

        hostComponentfixture.detectChanges();

        component.addBox();

        hostComponentfixture.detectChanges();

        const addRowClassButtons = hostComponentfixture.debugElement.queryAll(
            By.css('.box__add-row-class-button')
        );

        expect(addRowClassButtons.length).toBe(2);
    });

    it('should add a new Container in a new row, when there is no space in the last row', () => {
        fakeValue.rows[0].columns[0].width = 12;
        hostComponentfixture.componentInstance.form = new UntypedFormGroup({
            body: new UntypedFormControl(fakeValue)
        });

        hostComponentfixture.detectChanges();

        component.addBox();
        expect(component.grid.boxes.length).toEqual(2);
        expect(component.grid.boxes[1].config.row).toEqual(2);
    });

    it('should remove one Container from the Grid', () => {
        component.addBox();
        const dotDialogService =
            hostComponentfixture.debugElement.injector.get(DotAlertConfirmService);
        spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
            conf.accept();
        });
        component.onRemoveContainer(1);
        expect(component.grid.boxes.length).toEqual(1);
    });

    it('should create a new row with a basic configuration object', () => {
        fakeValue.rows[0].columns[0].width = 12;
        hostComponentfixture.componentInstance.form = new UntypedFormGroup({
            body: new UntypedFormControl(fakeValue)
        });

        hostComponentfixture.detectChanges();

        component.addBox();
        expect(component.grid.boxes[1].config).toEqual({
            row: 2,
            sizex: 3,
            col: 1,
            fixed: true,
            maxCols: 12,
            maxRows: 1,
            payload: {
                styleClass: ''
            }
        });
    });

    it('should remove the empty rows in the grid', fakeAsync(() => {
        component.addBox();
        component.addBox();
        component.grid.boxes[0].config.row = 5;
        component.grid.boxes[0].config.sizex = 5;
        component.grid.boxes[1].config.row = 2;
        component.grid.boxes[2].config.row = 4;
        component.grid.boxes[2].config.sizex = 1;
        component.updateModel();
        tick();
        expect(component.grid.boxes[0].config.sizex).toEqual(3);
        expect(component.grid.boxes[1].config.sizex).toEqual(1);
        expect(component.grid.boxes[2].config.sizex).toEqual(5);
    }));

    it('should Propagate Change after a grid box is deleted', () => {
        component.addBox();
        const dotDialogService =
            hostComponentfixture.debugElement.injector.get(DotAlertConfirmService);
        spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
            conf.accept();
        });
        spyOn(component, 'propagateChange');
        component.onRemoveContainer(1);
        expect(component.propagateChange).toHaveBeenCalledWith(fakeValue);
    });

    it('should Propagate Change after a grid box is moved', () => {
        spyOn(component, 'propagateChange');
        component.updateModel();
        expect(component.propagateChange).toHaveBeenCalledWith(fakeValue);
    });

    it('should Propagate Change after a grid box is added', () => {
        fakeValue.rows[0].columns.push({
            containers: [],
            leftOffset: 3,
            width: 3
        });
        spyOn(component, 'propagateChange');
        component.addBox();
        expect(component.propagateChange).toHaveBeenCalled();
    });

    it('should resize the grid when the left menu is toggle', fakeAsync(() => {
        const dotEventsService = hostComponentfixture.debugElement.injector.get(DotEventsService);
        spyOn(component.ngGrid, 'triggerResize');
        dotEventsService.notify('dot-side-nav-toggle');
        tick(210);
        expect(component.ngGrid.triggerResize).toHaveBeenCalled();
    }));

    it('should resize the grid when the layout sidebar change', fakeAsync(() => {
        const dotEventsService = hostComponentfixture.debugElement.injector.get(DotEventsService);
        spyOn(component.ngGrid, 'triggerResize');
        dotEventsService.notify('layout-sidebar-change');
        tick(0);
        expect(component.ngGrid.triggerResize).toHaveBeenCalled();
    }));

    it('should call writeValue to define the initial value of grid', () => {
        hostComponentfixture.detectChanges();
        expect(component.value).toEqual(fakeValue);
    });

    it('should be multiple true on dot-container-selector', () => {
        const containerSelector = de.query(By.css('dot-container-selector-layout'));
        expect(containerSelector.attributes['ng-reflect-multiple']).toBeTruthy();
    });

    it('should have a dot-dialog but not form', () => {
        const dotDialog = hostComponentfixture.debugElement.query(By.css('dot-dialog'));
        const form = hostComponentfixture.debugElement.query(By.css('dot-dialog form'));

        expect(dotDialog).not.toBeNull();
        expect(form).toBeNull();
    });

    describe('show dialog for add class', () => {
        let dotDialog;
        let dotText;
        let dotDialogForm;

        function showDialog(type) {
            const addRowClassButtons = hostComponentfixture.debugElement.query(
                By.css(
                    type === 'box'
                        ? `.box__add-box-class-button`
                        : `.box__add-${type}-class-button dot-icon-button-tooltip`
                )
            );

            addRowClassButtons.triggerEventHandler('click', null);
            hostComponentfixture.detectChanges();

            dotDialog = hostComponentfixture.debugElement.query(
                By.css('dot-dialog')
            ).componentInstance;
            dotDialogForm = hostComponentfixture.debugElement.query(By.css('dot-dialog form'));
            dotText = hostComponentfixture.debugElement.query(By.css('.box__add-class-text'));
        }

        it('should show dot-dialog when click any add class to row button', () => {
            showDialog('row');
            expect(dotDialog.visible).toBe(true);
            expect(dotDialog.header).toBe('Add class to row');
            expect(dotText.nativeElement.value).toBe('');
            expect(dotDialogForm).toBeDefined();
        });

        it('should show dot-dialog when click any add class to box button', () => {
            showDialog('box');
            expect(dotDialog.visible).toBe(true);
            expect(dotDialog.header).toBe('Add class to box');
            expect(dotText.nativeElement.value).toBe('');
            expect(dotDialogForm).toBeDefined();
        });
    });

    it('should set row class as text value', () => {
        fakeValue.rows[0].styleClass = 'test_row_class';
        hostComponentfixture.componentInstance.createForm();
        hostComponentfixture.detectChanges();

        const addRowClassButtons = hostComponentfixture.debugElement.query(
            By.css('.box__add-row-class-button dot-icon-button-tooltip')
        );
        addRowClassButtons.triggerEventHandler('click', null);

        hostComponentfixture.detectChanges();

        const dotText = hostComponentfixture.debugElement.query(By.css('.box__add-class-text'));
        expect(dotText.nativeElement.value).toBe('test_row_class');
    });

    it('should trigger change when row class is added', () => {
        const dotDialog = hostComponentfixture.debugElement.query(By.css('dot-dialog'));
        spyOn(dotDialog.componentInstance, 'close').and.callThrough();

        const addRowClassButtons = hostComponentfixture.debugElement.query(
            By.css('.box__add-row-class-button dot-icon-button-tooltip')
        );
        addRowClassButtons.triggerEventHandler('click', null);

        component.form.setValue({ classToAdd: 'test_row_class' });
        hostComponentfixture.detectChanges();

        const dotText = hostComponentfixture.debugElement.query(By.css('.box__add-class-text'));
        expect(dotText.nativeElement.value).toBe('test_row_class');

        dotDialog.componentInstance.actions.accept.action(dotDialog.componentInstance);

        expect(hostComponentfixture.componentInstance.form.value.body.rows[0].styleClass).toBe(
            'test_row_class'
        );
        expect(dotDialog.componentInstance.close).toHaveBeenCalled();
    });

    it('should trigger change when row class is add', () => {
        fakeValue.rows[0].styleClass = 'test_row_class';
        hostComponentfixture.componentInstance.createForm();
        hostComponentfixture.detectChanges();

        const dotDialog = hostComponentfixture.debugElement.query(By.css('dot-dialog'));
        spyOn(dotDialog.componentInstance, 'close').and.callThrough();

        const addRowClassButtons = hostComponentfixture.debugElement.query(
            By.css('.box__add-row-class-button dot-icon-button-tooltip')
        );
        addRowClassButtons.triggerEventHandler('click', null);

        component.form.setValue({ classToAdd: 'test_row_class_2' });

        hostComponentfixture.detectChanges();

        const dotText = hostComponentfixture.debugElement.query(By.css('.box__add-class-text'));
        expect(dotText.nativeElement.value).toBe('test_row_class_2');

        dotDialog.componentInstance.actions.accept.action(dotDialog.componentInstance);

        expect(hostComponentfixture.componentInstance.form.value.body.rows[0].styleClass).toBe(
            'test_row_class_2'
        );
        expect(dotDialog.componentInstance.close).toHaveBeenCalled();
    });

    it('should show dot-dialog when click any add class to column button', () => {
        const addRowClassButtons = hostComponentfixture.debugElement.query(
            By.css('.box__add-box-class-button')
        );
        addRowClassButtons.triggerEventHandler('click', null);

        hostComponentfixture.detectChanges();

        const dotDialog = hostComponentfixture.debugElement.query(By.css('dot-dialog'));
        expect(dotDialog.componentInstance.visible).toBe(true);

        const dotText = hostComponentfixture.debugElement.query(By.css('.box__add-class-text'));
        expect(dotText.nativeElement.value).toBe('');
    });

    it('should disabled accept add class ok button when class is undefined', () => {
        const addRowClassButtons = hostComponentfixture.debugElement.query(
            By.css('.box__add-box-class-button')
        );
        addRowClassButtons.triggerEventHandler('click', null);

        hostComponentfixture.detectChanges();

        expect(component.addClassDialogActions.accept.disabled).toBe(true);
    });

    it('should enabled accept add class ok button when class is not undefined', () => {
        const addRowClassButtons = hostComponentfixture.debugElement.query(
            By.css('.box__add-box-class-button')
        );
        addRowClassButtons.triggerEventHandler('click', null);

        component.form.setValue({ classToAdd: 'test_class' });

        hostComponentfixture.detectChanges();

        expect(component.addClassDialogActions.accept.disabled).toBe(false);
    });

    it('should set column class as text value', () => {
        fakeValue.rows[0].columns[0].styleClass = 'test_column_class';
        hostComponentfixture.componentInstance.createForm();
        hostComponentfixture.detectChanges();

        const addRowClassButtons = hostComponentfixture.debugElement.query(
            By.css('.box__add-box-class-button')
        );
        addRowClassButtons.triggerEventHandler('click', null);

        hostComponentfixture.detectChanges();

        const dotText = hostComponentfixture.debugElement.query(By.css('.box__add-class-text'));
        expect(dotText.nativeElement.value).toBe('test_column_class');
    });

    it('should trigger change when column class is added', () => {
        const dotDialog = hostComponentfixture.debugElement.query(By.css('dot-dialog'));
        spyOn(dotDialog.componentInstance, 'close').and.callThrough();

        const addRowClassButtons = hostComponentfixture.debugElement.query(
            By.css('.box__add-box-class-button')
        );
        addRowClassButtons.triggerEventHandler('click', null);

        component.form.setValue({ classToAdd: 'test_column_class' });

        hostComponentfixture.detectChanges();

        const dotText = hostComponentfixture.debugElement.query(By.css('.box__add-class-text'));
        expect(dotText.nativeElement.value).toBe('test_column_class');

        dotDialog.componentInstance.actions.accept.action(dotDialog.componentInstance);

        expect(
            hostComponentfixture.componentInstance.form.value.body.rows[0].columns[0].styleClass
        ).toBe('test_column_class');
        expect(dotDialog.componentInstance.close).toHaveBeenCalled();
    });

    it('should trigger change when column class is edit', () => {
        hostComponentfixture.componentInstance.form.value.body.rows[0].columns[0].styleClass =
            'test_column_class';
        hostComponentfixture.componentInstance.createForm();
        hostComponentfixture.detectChanges();

        const dotDialog = hostComponentfixture.debugElement.query(By.css('dot-dialog'));
        spyOn(dotDialog.componentInstance, 'close').and.callThrough();

        const addRowClassButtons = hostComponentfixture.debugElement.query(
            By.css('.box__add-box-class-button')
        );
        addRowClassButtons.triggerEventHandler('click', null);

        component.form.setValue({ classToAdd: 'test_column_class_2' });

        hostComponentfixture.detectChanges();

        const dotText = hostComponentfixture.debugElement.query(By.css('.box__add-class-text'));
        expect(dotText.nativeElement.value).toBe('test_column_class_2');

        dotDialog.componentInstance.actions.accept.action(dotDialog.componentInstance);

        expect(
            hostComponentfixture.componentInstance.form.value.body.rows[0].columns[0].styleClass
        ).toBe('test_column_class_2');
        expect(dotDialog.componentInstance.close).toHaveBeenCalled();
    });
});
