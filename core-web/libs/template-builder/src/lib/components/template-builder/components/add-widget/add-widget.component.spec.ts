import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddWidgetComponent } from './add-widget.component';
import { colIcon, rowIcon } from './utils/icons';

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
        component.icon = rowIcon;
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
            component.icon = colIcon;
            fixture.detectChanges();
            expect(component.icon).toBe(colIcon);
        });
    });
});
