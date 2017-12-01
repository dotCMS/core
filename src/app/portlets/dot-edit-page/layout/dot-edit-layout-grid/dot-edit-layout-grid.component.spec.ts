import { MessageService } from './../../../../api/services/messages-service';
import { MockMessageService } from './../../../../test/message-service.mock';
import { PaginatorService } from './../../../../api/services/paginator/paginator.service';
import { DotConfirmationService } from './../../../../api/services/dot-confirmation/dot-confirmation.service';
import { DotContainerSelectorModule } from './../../../../view/components/dot-container-selector/dot-container-selector.module';
import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';

import { NgGridModule } from 'angular2-grid';

import { DotEditLayoutGridComponent } from './dot-edit-layout-grid.component';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotEditLayoutService } from '../../shared/services/dot-edit-layout.service';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Component, DebugElement } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';
import {DotEventsService} from '../../../../api/services/dot-events.service';

@Component({
    selector: 'dot-test-host-component',
    template: `<form [formGroup]="form">
                    <dot-edit-layout-grid formControlName="pageView" ></dot-edit-layout-grid>
                </form>`
})
class TestHostComponent {
    form: FormGroup;
    constructor() {
        this.form = new FormGroup({
            pageView: new FormControl('pageViewObj')
        });
    }
}

describe('DotEditLayoutGridComponent', () => {
    let component: DotEditLayoutGridComponent;
    let fixture: ComponentFixture<DotEditLayoutGridComponent>;
    let hostComponentfixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;

    beforeEach(() => {
        const messageServiceMock = new MockMessageService({
            cancel: 'Cancel'
        });

        DOTTestBed.configureTestingModule({
            declarations: [DotEditLayoutGridComponent, TestHostComponent],
            imports: [NgGridModule, DotContainerSelectorModule, BrowserAnimationsModule],
            providers: [
                DotConfirmationService,
                DotEditLayoutService,
                PaginatorService,
                { provide: MessageService, useValue: messageServiceMock }
            ]
        });

        fixture = DOTTestBed.createComponent(DotEditLayoutGridComponent);
        component = fixture.componentInstance;

        fixture.detectChanges();
    });

    it('should show set one element in the grid of 12 columns', () => {
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
        expect(component.grid[2].config.row).toEqual(2);
    });

    it('should add a new Container in a new row, when there is no space in the last row', () => {
        component.addBox();
        expect(component.grid.length).toEqual(2);
        expect(component.grid[1].config.row).toEqual(2);
    });

    it('should remove one Container from the Grid', () => {
        component.addBox();
        const dotConfirmationService = fixture.debugElement.injector.get(DotConfirmationService);
        spyOn(dotConfirmationService, 'confirm').and.callFake(conf => {
            conf.accept();
        });
        component.onRemoveContainer(1);
        expect(component.grid.length).toEqual(1);
    });

    it('should create a new row with a basic configuration object', () => {
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
            component.onDragStop();
            tick();
            expect(component.grid[0].config.sizex).toEqual(3);
            expect(component.grid[1].config.sizex).toEqual(1);
            expect(component.grid[2].config.sizex).toEqual(5);
        })
    );

    it('should Propagate Change after a grid box is deleted', () => {
        component.addBox();
        const dotConfirmationService = fixture.debugElement.injector.get(DotConfirmationService);
        spyOn(dotConfirmationService, 'confirm').and.callFake(conf => {
            conf.accept();
        });
        spyOn(component, 'propagateChange');
        component.onRemoveContainer(1);
        expect(component.propagateChange).toHaveBeenCalled();
    });

    it('should Propagate Change after a grid box is moved', () => {
        spyOn(component, 'propagateChange');
        component.onDragStop();
        expect(component.propagateChange).toHaveBeenCalled();
    });

    it('should Propagate Change after a grid box is added', () => {
        spyOn(component, 'propagateChange');
        component.addBox();
        expect(component.propagateChange).toHaveBeenCalled();
    });

    it( 'should resize the grid when the left menu is toggle', fakeAsync(() => {
        const dotEventsService = fixture.debugElement.injector.get(DotEventsService);
        spyOn( component.ngGrid, 'triggerResize');
        dotEventsService.notify( {name: 'dot-side-nav-toggle'});
        tick(160);
        expect(component.ngGrid.triggerResize).toHaveBeenCalled();
    }));

    it('should call writeValue to define the initial value of grid', () => {
        hostComponentfixture = DOTTestBed.createComponent(TestHostComponent);
        de = hostComponentfixture.debugElement.query(By.css('dot-edit-layout-grid'));
        const comp: DotEditLayoutGridComponent = de.componentInstance;
        spyOn(comp, 'writeValue');
        hostComponentfixture.detectChanges();
        expect(comp.writeValue).toHaveBeenCalledWith('pageViewObj');
    });
});
