import { Spectator, createComponentFactory, byTestId } from '@ngneat/spectator';

import { Component } from '@angular/core';

import { EditEmaToolbarComponent } from './edit-ema-toolbar.component';

@Component({
    template: `<dot-edit-ema-toolbar>
        <ng-container left><div data-testId="left-content"></div></ng-container>
        <ng-container right><div data-testId="right-content"></div></ng-container>
    </dot-edit-ema-toolbar>`
})
class TestHostComponent {}

describe('EditEmaToolbarComponent', () => {
    let spectator: Spectator<TestHostComponent>;

    const createComponent = createComponentFactory({
        component: TestHostComponent,
        imports: [EditEmaToolbarComponent],
        providers: []
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('DOM', () => {
        it('should have left-content on left', () => {
            const leftContent = spectator.query(byTestId('toolbar-left-content'));

            expect(leftContent.querySelector('[data-testId="left-content"]')).not.toBeNull();
        });

        it('should have right-content on right', () => {
            const rightContent = spectator.query(byTestId('toolbar-right-content'));

            expect(rightContent.querySelector('[data-testId="right-content"]')).not.toBeNull();
        });
    });
});
