import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotConfirmationService } from './../../../../api/services/dot-confirmation/dot-confirmation.service';
import { DotContainerSelectorModule } from './../../../../view/components/dot-container-selector/dot-container-selector.module';
import { DotEditLayoutGridComponent } from './dot-edit-layout-grid.component';
import { DotEditLayoutService } from '../../shared/services/dot-edit-layout.service';
import { DotEventsService } from '../../../../api/services/dot-events/dot-events.service';
import { DotLayoutBody } from '../../shared/models/dot-layout-body.model';
import { FormControl, FormGroup } from '@angular/forms';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { NgGridModule } from 'angular2-grid';
import { PaginatorService } from './../../../../api/services/paginator/paginator.service';
import { TemplateContainersCacheService } from '../../template-containers-cache.service';

let fakeValue: DotLayoutBody;

@Component({
    selector: 'dot-test-host-component',
    template: `<form [formGroup]="form">
                    <dot-edit-layout-grid formControlName="body" ></dot-edit-layout-grid>
                </form>`
})
class TestHostComponent {
    form: FormGroup;
    constructor() {
        this.form = new FormGroup({
            body: new FormControl(fakeValue)
        });
    }
}

describe('DotEditLayoutGridComponent', () => {
    let component: DotEditLayoutGridComponent;
    let de: DebugElement;
    let hostComponentfixture: ComponentFixture<TestHostComponent>;

    beforeEach(() => {
        fakeValue = {
            rows: [
                {
                    columns: [
                        {
                            // containers: ['1'],
                            containers: [],
                            leftOffset: 1,
                            width: 2
                        }
                    ]
                }
            ]
        };

        const messageServiceMock = new MockDotMessageService({
            cancel: 'Cancel'
        });

        DOTTestBed.configureTestingModule({
            declarations: [DotEditLayoutGridComponent, TestHostComponent],
            imports: [NgGridModule, DotContainerSelectorModule, BrowserAnimationsModule],
            providers: [
                DotConfirmationService,
                DotEditLayoutService,
                TemplateContainersCacheService,
                PaginatorService,
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        hostComponentfixture = DOTTestBed.createComponent(TestHostComponent);
        de = hostComponentfixture.debugElement.query(By.css('dot-edit-layout-grid'));
        component = de.componentInstance;

        hostComponentfixture.detectChanges();
    });

    it('should show set one element in the grid of 12 columns', () => {
        hostComponentfixture.componentInstance.form = new FormGroup({
            body: new FormControl({})
        });

        hostComponentfixture.detectChanges();

        expect(component.grid.length).toEqual(1);
        expect(component.grid[0].config.sizex).toEqual(12);
    });

    it('should add one Container to the grid of 3 columns', () => {
        component.addBox();
        expect(component.grid.length).toEqual(2);
        expect(component.grid[1].config.sizex).toEqual(3);
    });

    it('should add a new Container in the same row', () => {
        component.addBox();
        component.addBox();

        expect(component.grid.length).toEqual(3);
        expect(component.grid[1].config.row).toEqual(1);
        expect(component.grid[2].config.row).toEqual(1);
    });

    it('should add a new Container in a new row, when there is no space in the last row', () => {
        fakeValue.rows[0].columns[0].width = 12;
        hostComponentfixture.componentInstance.form = new FormGroup({
            body: new FormControl(fakeValue)
        });

        hostComponentfixture.detectChanges();

        component.addBox();
        expect(component.grid.length).toEqual(2);
        expect(component.grid[1].config.row).toEqual(2);
    });

    it('should remove one Container from the Grid', () => {
        component.addBox();
        const dotConfirmationService = hostComponentfixture.debugElement.injector.get(DotConfirmationService);
        spyOn(dotConfirmationService, 'confirm').and.callFake(conf => {
            conf.accept();
        });
        component.onRemoveContainer(1);
        expect(component.grid.length).toEqual(1);
    });

    it('should create a new row with a basic configuration object', () => {
        fakeValue.rows[0].columns[0].width = 12;
        hostComponentfixture.componentInstance.form = new FormGroup({
            body: new FormControl(fakeValue)
        });

        hostComponentfixture.detectChanges();

        component.addBox();
        expect(component.grid[1].config).toEqual({
            row: 2,
            sizex: 3,
            col: 1,
            fixed: true,
            maxCols: 12,
            maxRows: 1
        });
    });

    it(
        'should remove the empty rows in the grid',
        fakeAsync(() => {
            component.addBox();
            component.addBox();
            component.grid[0].config.row = 5;
            component.grid[0].config.sizex = 5;
            component.grid[1].config.row = 2;
            component.grid[2].config.row = 4;
            component.grid[2].config.sizex = 1;
            component.updateModel();
            tick();
            expect(component.grid[0].config.sizex).toEqual(3);
            expect(component.grid[1].config.sizex).toEqual(1);
            expect(component.grid[2].config.sizex).toEqual(5);
        })
    );

    it('should Propagate Change after a grid box is deleted', () => {
        component.addBox();
        const dotConfirmationService = hostComponentfixture.debugElement.injector.get(DotConfirmationService);
        spyOn(dotConfirmationService, 'confirm').and.callFake(conf => {
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

    it(
        'should resize the grid when the left menu is toggle',
        fakeAsync(() => {
            const dotEventsService = hostComponentfixture.debugElement.injector.get(DotEventsService);
            spyOn(component.ngGrid, 'triggerResize');
            dotEventsService.notify('dot-side-nav-toggle');
            tick(210);
            expect(component.ngGrid.triggerResize).toHaveBeenCalled();
        })
    );

    it(
        'should resize the grid when the layout sidebar change',
        fakeAsync(() => {
            const dotEventsService = hostComponentfixture.debugElement.injector.get(DotEventsService);
            spyOn(component.ngGrid, 'triggerResize');
            dotEventsService.notify('layout-sidebar-change');
            tick(0);
            expect(component.ngGrid.triggerResize).toHaveBeenCalled();
        })
    );

    it('should call writeValue to define the initial value of grid', () => {
        hostComponentfixture.detectChanges();
        expect(component.value).toEqual(fakeValue);
    });
});
