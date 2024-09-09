import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UveEditStyleFormComponent } from './uve-edit-style-form.component';

describe('UveEditStyleFormComponent', () => {
    let component: UveEditStyleFormComponent;
    let fixture: ComponentFixture<UveEditStyleFormComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [UveEditStyleFormComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(UveEditStyleFormComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
