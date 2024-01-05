import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EditEmaUiComponent } from './edit-ema-ui.component';

describe('EditEmaUiComponent', () => {
    let component: EditEmaUiComponent;
    let fixture: ComponentFixture<EditEmaUiComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EditEmaUiComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(EditEmaUiComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
