import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditEmaPersonaSelectorComponent } from './edit-ema-persona-selector.component';

describe('EditEmaPersonaSelectorComponent', () => {
    let component: EditEmaPersonaSelectorComponent;
    let fixture: ComponentFixture<EditEmaPersonaSelectorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EditEmaPersonaSelectorComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(EditEmaPersonaSelectorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
