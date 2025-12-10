import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OnboardingDevComponent } from './onboarding-dev.component';

describe('OnboardingDevComponent', () => {
    let component: OnboardingDevComponent;
    let fixture: ComponentFixture<OnboardingDevComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [OnboardingDevComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(OnboardingDevComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
