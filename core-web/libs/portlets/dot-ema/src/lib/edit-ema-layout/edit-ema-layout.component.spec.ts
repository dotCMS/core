import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditEmaLayoutComponent } from './edit-ema-layout.component';

describe('EditEmaLayoutComponent', () => {
    let component: EditEmaLayoutComponent;
    let fixture: ComponentFixture<EditEmaLayoutComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EditEmaLayoutComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(EditEmaLayoutComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
