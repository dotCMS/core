import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotEventsService } from '@dotcms/data-access';
import { DotSpinnerComponent } from '@dotcms/ui';

import { DotGlobalMessageComponent } from './dot-global-message.component';

describe('DotGlobalMessageComponent', () => {
    let component: DotGlobalMessageComponent;
    let fixture: ComponentFixture<DotGlobalMessageComponent>;
    let dotEventsService: DotEventsService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [DotGlobalMessageComponent, DotSpinnerComponent],
            providers: [DotEventsService]
        }).compileComponents();

        fixture = TestBed.createComponent(DotGlobalMessageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        dotEventsService = TestBed.inject(DotEventsService);
    });

    it('should set the value of the message with the corresponding icon and life time', () => {
        dotEventsService.notify('dot-global-message', {
            value: 'test',
            type: 'loading',
            life: 3000
        });
        expect(component.message).toEqual({
            value: 'test',
            type: 'loading',
            icon: 'loading',
            life: 3000
        });
    });

    it('should show dotSpinner for events type loading', () => {
        dotEventsService.notify('dot-global-message', { value: 'test', type: 'loading' });
        fixture.detectChanges();
        const dotSpinner = fixture.debugElement.query(By.css('dot-spinner'));
        const dotIcon = fixture.debugElement.query(By.css('[data-testId="message-icon"]'));

        expect(dotSpinner).toBeDefined();
        expect(dotIcon).toBeNull();
    });

    it('should show dotIcon for any event type expect loading', () => {
        dotEventsService.notify('dot-global-message', { value: 'test' });
        fixture.detectChanges();
        const dotSpinner = fixture.debugElement.query(By.css('dot-spinner'));
        const dotIcon = fixture.debugElement.query(By.css('[data-testId="message-icon"]'));

        expect(dotSpinner).toBeNull();
        expect(dotIcon).toBeDefined();
    });

    it('should set visibility to false after 10 ms', (done) => {
        dotEventsService.notify('dot-global-message', { value: 'test', life: 1 });
        expect(component.classes).toContain('dot-global-message--visible');
        // TODO: Find a way to get rid of timeouts.
        setTimeout(() => {
            expect(component.classes).not.toContain('dot-global-message--visible');
            done();
        }, 10);
    });

    it('should set value to success event', () => {
        dotEventsService.notify('dot-global-message', { value: 'test', type: 'success' });
        fixture.detectChanges();

        const dotIcon = fixture.debugElement.query(By.css('[data-testId="message-icon"]'));
        const message = fixture.debugElement.query(By.css('[data-testId="message-text"]'));

        expect(dotIcon.nativeElement.classList).toContain('pi-check-circle');
        expect(message.nativeElement.textContent).toContain('test');
        expect(component.classes).toContain('success');
    });

    it('should set value to waring event', () => {
        dotEventsService.notify('dot-global-message', {
            value: 'warning message',
            type: 'warning'
        });
        fixture.detectChanges();

        const dotIcon = fixture.debugElement.query(By.css('[data-testId="message-icon"]'));
        const message = fixture.debugElement.query(By.css('[data-testId="message-text"]'));

        expect(dotIcon.nativeElement.classList).toContain('pi-exclamation-triangle');
        expect(message.nativeElement.textContent).toContain('warning message');
        expect(component.classes).toContain('warning');
    });

    it('should set value to error event', () => {
        dotEventsService.notify('dot-global-message', { value: 'error message', type: 'error' });
        fixture.detectChanges();

        const dotIcon = fixture.debugElement.query(By.css('[data-testId="message-icon"]'));
        const message = fixture.debugElement.query(By.css('[data-testId="message-text"]'));

        expect(dotIcon.nativeElement.classList).toContain('pi-exclamation-circle');
        expect(message.nativeElement.textContent).toContain('error message');
        expect(component.classes).toContain('error');
    });
});
