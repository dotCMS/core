import { ComponentFixture } from '@angular/core/testing';

import { NgGridModule } from 'angular2-grid';
import { Input, Component } from '@angular/core';

import { DotEditLayoutGridComponent } from './dot-edit-layout-grid.component';
import { DOTTestBed } from '../../../../test/dot-test-bed';

@Component({
    selector: 'action-button',
    template: ''
})
class TestActionButtonComponent {
    @Input() command?: ($event) => void;
}

describe('DotEditLayoutGridComponent', () => {
    let component: DotEditLayoutGridComponent;
    let fixture: ComponentFixture<DotEditLayoutGridComponent>;
    let addContainer: () => void;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotEditLayoutGridComponent, TestActionButtonComponent],
            imports: [NgGridModule]
        });

        fixture = DOTTestBed.createComponent(DotEditLayoutGridComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        addContainer = component.addContainer();
    });

    it('should show set one element in the grid of 12 columns', () => {
        expect(component.gridContainers.length).toEqual(1);
        expect(component.gridContainers[0].config.sizex).toEqual(12);
    });

    it('should add one Container to the grid of 3 columns', () => {
        addContainer();
        expect(component.gridContainers.length).toEqual(2);
        expect(component.gridContainers[1].config.sizex).toEqual(3);
    });

    it('should add a new Container in the same row', () => {
        addContainer();
        addContainer();
        expect(component.gridContainers.length).toEqual(3);
        expect(component.gridContainers[2].config.row).toEqual(2);
    });

    it('should add a new Container in a new row, when there is no space in the last row', () => {
        addContainer();
        expect(component.gridContainers.length).toEqual(2);
        expect(component.gridContainers[1].config.row).toEqual(2);
    });

    it('should remove one Container from the Grid', () => {
        addContainer();
        component.removeContainer(0);
        expect(component.gridContainers.length).toEqual(1);
    });

    it('should create a new row with a basic configuration object', () => {
        addContainer();
        expect(component.gridContainers[1].config).toBeDefined();
        expect(component.gridContainers[1].config.row).toBeDefined();
        expect(component.gridContainers[1].config.sizex).toBeDefined();
        expect(component.gridContainers[1].config.col).toBeDefined();
    });

});
