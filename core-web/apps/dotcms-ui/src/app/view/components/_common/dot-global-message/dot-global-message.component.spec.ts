import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { By } from '@angular/platform-browser';

import { DotEventsService } from '@dotcms/data-access';
import { DotSpinnerComponent } from '@dotcms/ui';

import { DotGlobalMessageComponent } from './dot-global-message.component';

describe('DotGlobalMessageComponent', () => {
    let spectator: Spectator<DotGlobalMessageComponent>;
    let dotEventsService: DotEventsService;

    const createComponent = createComponentFactory({
        component: DotGlobalMessageComponent,
        imports: [DotSpinnerComponent],
        providers: [DotEventsService]
    });

    beforeEach(() => {
        spectator = createComponent();
        dotEventsService = spectator.inject(DotEventsService);
    });

    it('should set the value of the message with the corresponding icon and life time', () => {
        dotEventsService.notify('dot-global-message', {
            value: 'test',
            type: 'loading',
            life: 3000
        });
        expect(spectator.component.message).toEqual({
            value: 'test',
            type: 'loading',
            icon: 'loading',
            life: 3000
        });
    });

    it('should show dotSpinner for events type loading', () => {
        dotEventsService.notify('dot-global-message', { value: 'test', type: 'loading' });
        expect(spectator.component.message.type).toBe('loading');
        expect(spectator.component.message.icon).toBe('loading');
    });

    it('should show dotIcon for any event type expect loading', () => {
        dotEventsService.notify('dot-global-message', { value: 'test' });
        spectator.fixture.detectChanges(false);

        const dotSpinner = spectator.debugElement.query(By.css('dot-spinner'));
        const dotIcon = spectator.debugElement.query(By.css('[data-testId="message-icon"]'));

        expect(dotSpinner).toBeNull();
        expect(dotIcon).toBeDefined();
    });

    it('should set visibility to false after 10 ms', (done) => {
        dotEventsService.notify('dot-global-message', { value: 'test', life: 1 });
        expect(spectator.component.classes).toContain('dot-global-message--visible');
        setTimeout(() => {
            expect(spectator.component.classes).not.toContain('dot-global-message--visible');
            done();
        }, 10);
    });

    it('should set value to success event', () => {
        dotEventsService.notify('dot-global-message', { value: 'test', type: 'success' });
        spectator.fixture.detectChanges(false);

        const dotIcon = spectator.debugElement.query(By.css('[data-testId="message-icon"]'));

        expect(dotIcon).toBeTruthy();
        expect(spectator.component.message.icon).toContain('pi-check-circle');
        expect(spectator.component.message.value).toBe('test');
        expect(spectator.component.classes).toContain('success');
    });

    it('should set value to warning event', () => {
        dotEventsService.notify('dot-global-message', {
            value: 'warning message',
            type: 'warning'
        });
        spectator.fixture.detectChanges(false);

        const dotIcon = spectator.debugElement.query(By.css('[data-testId="message-icon"]'));

        expect(dotIcon).toBeTruthy();
        expect(spectator.component.message.icon).toContain('pi-exclamation-triangle');
        expect(spectator.component.message.value).toBe('warning message');
        expect(spectator.component.classes).toContain('warning');
    });

    it('should set value to error event', () => {
        dotEventsService.notify('dot-global-message', { value: 'error message', type: 'error' });
        spectator.fixture.detectChanges(false);

        const dotIcon = spectator.debugElement.query(By.css('[data-testId="message-icon"]'));

        expect(dotIcon).toBeTruthy();
        expect(spectator.component.message.icon).toContain('pi-exclamation-circle');
        expect(spectator.component.message.value).toBe('error message');
        expect(spectator.component.classes).toContain('error');
    });
});
