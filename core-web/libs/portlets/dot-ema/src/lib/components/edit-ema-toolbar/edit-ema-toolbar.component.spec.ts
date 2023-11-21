import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditEmaToolbarComponent } from './edit-ema-toolbar.component';

describe('EditEmaToolbarComponent', () => {
    let component: EditEmaToolbarComponent;
    let fixture: ComponentFixture<EditEmaToolbarComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EditEmaToolbarComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(EditEmaToolbarComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
