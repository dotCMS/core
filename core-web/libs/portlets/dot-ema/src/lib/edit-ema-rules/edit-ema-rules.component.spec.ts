import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditEmaRulesComponent } from './edit-ema-rules.component';

describe('EditEmaRulesComponent', () => {
    let component: EditEmaRulesComponent;
    let fixture: ComponentFixture<EditEmaRulesComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EditEmaRulesComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(EditEmaRulesComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
