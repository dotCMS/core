import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditEmaNavigationBarComponent } from './edit-ema-navigation-bar.component';

describe('EditEmaNavigationBarComponent', () => {
    let component: EditEmaNavigationBarComponent;
    let fixture: ComponentFixture<EditEmaNavigationBarComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EditEmaNavigationBarComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(EditEmaNavigationBarComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
