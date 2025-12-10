import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OnboardingAuthorComponent } from './onboarding-author.component';

describe('OnboardingAuthorComponent', () => {
    let component: OnboardingAuthorComponent;
    let fixture: ComponentFixture<OnboardingAuthorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [OnboardingAuthorComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(OnboardingAuthorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
