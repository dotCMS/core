import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DragBoxComponent } from './drag-box.component';

import { colIcon, rowIcon } from '../../assets/icons';

describe('DragBoxComponent', () => {
    let component: DragBoxComponent;
    let fixture: ComponentFixture<DragBoxComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DragBoxComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DragBoxComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        component.label = 'test';
        component.icon = 'row';
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('inputs', () => {
        it('should set label', () => {
            expect(component.label).toBe('test');
        });

        it('should have row icon', () => {
            expect(component.src).toBe(rowIcon);
        });

        it('should have row icon', () => {
            component.icon = 'column';
            fixture.detectChanges();
            expect(component.src).toBe(colIcon);
        });
    });

    describe('dragging', () => {
        it('should set dragging to true on mouse down and add drag-box__dragging class', () => {
            const element = fixture.nativeElement;
            const box = element.querySelector('.drag-box');
            element.dispatchEvent(new MouseEvent('mousedown'));
            expect(component.isDragging).toBe(true);
            expect(box.classList.contains('drag-box__dragging')).toBe(true);
        });

        it('should set dragging to false on mouse up and remove drag-box__dragging class', () => {
            const element = fixture.nativeElement;
            const box = element.querySelector('.drag-box');
            element.dispatchEvent(new MouseEvent('mouseup'));
            expect(component.isDragging).toBe(false);
            expect(box.classList.contains('drag-box__dragging')).toBe(false);
        });
    });
});
