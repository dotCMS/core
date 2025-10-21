import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotUveLockOverlayComponent } from './dot-uve-lock-overlay.component';

describe('DotUveLockOverlayComponent', () => {
    let component: DotUveLockOverlayComponent;
    let fixture: ComponentFixture<DotUveLockOverlayComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotUveLockOverlayComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotUveLockOverlayComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should display the lock overlay', () => {
        const overlay = fixture.debugElement.query(By.css('.lock-overlay'));
        expect(overlay).toBeTruthy();
    });

    it('should display the lock icon', () => {
        const icon = fixture.debugElement.query(By.css('.lock-overlay__icon i.pi-lock'));
        expect(icon).toBeTruthy();
    });

    it('should display the title "Lock the page to start editing"', () => {
        const title = fixture.debugElement.query(By.css('.lock-overlay__title'));
        expect(title.nativeElement.textContent).toContain('Lock the page to start editing');
    });

    it('should display the message "Use the switch above to lock and unlock"', () => {
        const message = fixture.debugElement.query(By.css('.lock-overlay__message'));
        expect(message.nativeElement.textContent).toContain(
            'Use the switch above to lock and unlock'
        );
    });

    it('should have proper test id', () => {
        const overlay = fixture.debugElement.query(By.css('[data-testId="lock-overlay"]'));
        expect(overlay).toBeTruthy();
    });
});
