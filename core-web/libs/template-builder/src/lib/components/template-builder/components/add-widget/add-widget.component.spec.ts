import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddWidgetComponent } from './add-widget.component';
import { colIcon, rowIcon } from './icons';

describe('AddWidgetComponent', () => {
    let component: AddWidgetComponent;
    let fixture: ComponentFixture<AddWidgetComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [AddWidgetComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(AddWidgetComponent);
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

        it('should have col icon', () => {
            component.type = 'col';
            fixture.detectChanges();
            expect(component.icon).toBe(colIcon);
        });
    });

    describe('dragging', () => {
        it('should set dragging to true on mouse down and add add-widget--dragging class', () => {
            const element = fixture.nativeElement;
            const box = element.querySelector('.add-widget');
            element.dispatchEvent(new MouseEvent('mousedown'));
            expect(component.isDragging).toBe(true);
            expect(box.classList.contains('add-widget--dragging')).toBe(true);
        });

        it('should set dragging to false on mouse up and remove add-widget--dragging class', () => {
            const element = fixture.nativeElement;
            const box = element.querySelector('.add-widget');
            element.dispatchEvent(new MouseEvent('mouseup'));
            expect(component.isDragging).toBe(false);
            expect(box.classList.contains('add-widget--dragging')).toBe(false);
        });

        it('should set dragging to false on dragend and remove add-widget--dragging class', () => {
            const element = fixture.nativeElement;
            const box = element.querySelector('.add-widget');
            element.dispatchEvent(new MouseEvent('dragend'));
            expect(component.isDragging).toBe(false);
            expect(box.classList.contains('add-widget--dragging')).toBe(false);
        });
    });
});
