import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditEmaExperimentsComponent } from './edit-ema-experiments.component';

describe('EditEmaExperimentsComponent', () => {
    let component: EditEmaExperimentsComponent;
    let fixture: ComponentFixture<EditEmaExperimentsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EditEmaExperimentsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(EditEmaExperimentsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
