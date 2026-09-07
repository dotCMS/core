import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { Component } from '@angular/core';

import { DotAiPromptInputComponent } from './dot-ai-prompt-input.component';

/**
 * Projection needs a real host, and a second component factory cannot share a file with the
 * direct one — the TestBed is already instantiated by then.
 */
@Component({
    imports: [DotAiPromptInputComponent],
    template: `
        <dot-ai-prompt-input [value]="value" (valueChange)="value = $event">
            <span promptStart data-testid="host-start">start</span>
            <button promptEnd data-testid="host-end">go</button>
        </dot-ai-prompt-input>
    `
})
class HostComponent {
    value = '';
}

describe('DotAiPromptInputComponent projection', () => {
    let spectator: Spectator<HostComponent>;

    const createHost = createComponentFactory(HostComponent);

    beforeEach(() => {
        spectator = createHost();
    });

    it('should render both slots', () => {
        expect(spectator.query(byTestId('host-start'))).toBeTruthy();
        expect(spectator.query(byTestId('host-end'))).toBeTruthy();
    });

    it('should push the trailing group right, so actions sit at the far edge', () => {
        const end = spectator.query(byTestId('host-end')) as HTMLElement;

        expect(end.closest('.ml-auto')).toBeTruthy();
    });

    it('should write typed text back to the host', () => {
        spectator.typeInElement(
            'hello',
            spectator.query(byTestId('ai-prompt-input')) as HTMLElement
        );

        expect(spectator.component.value).toBe('hello');
    });
});
