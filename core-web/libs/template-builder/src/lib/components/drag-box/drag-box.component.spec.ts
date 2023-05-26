import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DragBoxComponent } from './drag-box.component';

import { boxIcon, rowIcon } from '../../assets/icons';

fdescribe('DragBoxComponent', () => {
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
        component.type = 'row';
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('inputs', () => {
        it('should set label', () => {
            expect(component.label).toBe('test');
        });

        it('should have row icon', () => {
            expect(component.icon).toBe(rowIcon);
        });

        it('should have box icon', () => {
            component.type = 'box';
            fixture.detectChanges();
            expect(component.icon).toBe(boxIcon);
        });
    });

    describe('dragging', () => {
        it('should set dragging to true on mouse down and add drag-box--dragging class', () => {
            const element = fixture.nativeElement;
            const box = element.querySelector('.drag-box');
            element.dispatchEvent(new MouseEvent('mousedown'));
            expect(component.isDragging).toBe(true);
            expect(box.classList.contains('drag-box--dragging')).toBe(true);
        });

        it('should set dragging to false on mouse up and remove drag-box--dragging class', () => {
            const element = fixture.nativeElement;
            const box = element.querySelector('.drag-box');
            element.dispatchEvent(new MouseEvent('mouseup'));
            expect(component.isDragging).toBe(false);
            expect(box.classList.contains('drag-box--dragging')).toBe(false);
        });

        it('should set dragging to false on dragend and remove drag-box--dragging class', () => {
            const element = fixture.nativeElement;
            const box = element.querySelector('.drag-box');
            element.dispatchEvent(new MouseEvent('dragend'));
            expect(component.isDragging).toBe(false);
            expect(box.classList.contains('drag-box--dragging')).toBe(false);
        });
    });
});
