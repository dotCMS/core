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

describe('DotEditLayoutGridComponent', () => {
    let component: DotEditLayoutGridComponent;
    let fixture: ComponentFixture<DotEditLayoutGridComponent>;

    beforeEach(() => {
        const messageServiceMock = new MockMessageService({
            cancel: 'Cancel'
        });

        DOTTestBed.configureTestingModule({
            declarations: [DotEditLayoutGridComponent],
            imports: [NgGridModule, DotContainerSelectorModule],
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
});
