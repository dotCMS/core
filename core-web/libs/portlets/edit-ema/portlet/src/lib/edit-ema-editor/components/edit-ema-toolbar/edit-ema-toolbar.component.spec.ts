import { Spectator, createComponentFactory, byTestId } from '@ngneat/spectator';

import { Component } from '@angular/core';

import { EditEmaToolbarComponent } from './edit-ema-toolbar.component';

@Component({
    template: `<dot-edit-ema-toolbar>
        <ng-container title><div data-testId="title-content"></div></ng-container>
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

    describe('events', () => {
        it('should call setDevice from the store', () => {
            const setDeviceMock = jest.spyOn(store, 'setDevice');

            spectator.detectChanges();

            const deviceSelector = spectator.debugElement.query(
                By.css('[data-testId="dot-device-selector"]')
            );

            const iphone = { ...mockDotDevices[0], icon: 'someIcon' };

            spectator.triggerEventHandler(deviceSelector, 'selected', iphone);
            spectator.detectChanges();

            expect(setDeviceMock).toHaveBeenCalledWith(iphone);
        });

        it('should open seo results when clicking on a social media tile', () => {
            const setSocialMediaMock = jest.spyOn(store, 'setSocialMedia');

            store.load({
                url: 'index',
                language_id: '3',
                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
            });

            jest.runOnlyPendingTimers();

            const deviceSelector = spectator.debugElement.query(
                By.css('[data-testId="dot-device-selector"]')
            );

            spectator.triggerEventHandler(deviceSelector, 'changeSeoMedia', 'Facebook');

            expect(spectator.query(byTestId('results-seo-tool'))).not.toBeNull(); // This components share the same logic as the preview by device

            expect(setSocialMediaMock).toHaveBeenCalledWith('Facebook');
        });
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

        it('should have title-content on title', () => {
            const titleContent = spectator.query(byTestId('title-content'));

            expect(titleContent).not.toBeNull();
        });
    });
});
