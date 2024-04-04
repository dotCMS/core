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

        it('should trigger messageService when clicking on ema-copy-url', () => {
            const messageService = spectator.inject(MessageService, true);
            const messageServiceSpy = jest.spyOn(messageService, 'add');
            spectator.detectChanges();

            const button = spectator.debugElement.query(By.css('[data-testId="ema-copy-url"]'));

            spectator.triggerEventHandler(button, 'cdkCopyToClipboardCopied', {});

            expect(messageServiceSpy).toHaveBeenCalledWith({
                severity: 'success',
                summary: 'Copied',
                life: 3000
            });
        });

        it('should call navigate when selecting a language', () => {
            spectator.detectChanges();
            const router = spectator.inject(Router);

            jest.spyOn(router, 'navigate');

            spectator.triggerEventHandler(EditEmaLanguageSelectorComponent, 'selected', 2);
            spectator.detectChanges();

            expect(router.navigate).toHaveBeenCalledWith([], {
                queryParams: { language_id: 2 },
                queryParamsHandling: 'merge'
            });
        });
    });

    describe('persona selector', () => {
        it('should have a persona selector', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('persona-selector'))).not.toBeNull();
        });

        it("should open a confirmation dialog when selecting a persona that it's not personalized", () => {
            const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
            spectator.detectChanges();

            spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                ...DEFAULT_PERSONA,
                identifier: '123',
                pageId: '123',
                personalized: false
            });
            spectator.detectChanges();

            expect(confirmDialogOpen).toHaveBeenCalled();
        });

        it('should fetchPersonas and navigate when confirming the personalization', () => {
            const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
            spectator.detectChanges();

            spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                ...DEFAULT_PERSONA,
                identifier: '123',
                pageId: '123',
                personalized: false
            });
            spectator.detectChanges();

            expect(confirmDialogOpen).toHaveBeenCalled();

            const confirmDialog = spectator.query(byTestId('confirm-dialog'));
            const personaSelector = spectator.debugElement.query(
                By.css('[data-testId="persona-selector"]')
            ).componentInstance;
            const routerSpy = jest.spyOn(spectator.inject(Router), 'navigate');
            const fetchPersonasSpy = jest.spyOn(personaSelector, 'fetchPersonas');

            spectator.detectChanges();

            expect(confirmDialogOpen).toHaveBeenCalled();

            confirmDialog
                .querySelector('.p-confirm-dialog-accept')
                .dispatchEvent(new Event('click')); // This is the internal button, coudln't find a better way to test it

            spectator.detectChanges();

            expect(routerSpy).toBeCalledWith([], {
                queryParams: { 'com.dotmarketing.persona.id': '123' },
                queryParamsHandling: 'merge'
            });
            expect(fetchPersonasSpy).toHaveBeenCalled();
        });

        it('should reset the value on personalization rejection', () => {
            const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
            spectator.detectChanges();

            spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                ...DEFAULT_PERSONA,
                identifier: '123',
                pageId: '123',
                personalized: false
            });
            spectator.detectChanges();

            expect(confirmDialogOpen).toHaveBeenCalled();

            const confirmDialog = spectator.query(byTestId('confirm-dialog'));
            const personaSelector = spectator.debugElement.query(
                By.css('[data-testId="persona-selector"]')
            ).componentInstance;

            const resetValueSpy = jest.spyOn(personaSelector, 'resetValue');

            spectator.detectChanges();

            expect(confirmDialogOpen).toHaveBeenCalled();

            confirmDialog
                .querySelector('.p-confirm-dialog-reject')
                .dispatchEvent(new Event('click')); // This is the internal button, coudln't find a better way to test it

            spectator.detectChanges();

            expect(resetValueSpy).toHaveBeenCalled();
        });

        it('should open a confirmation dialog when despersonalize is triggered', () => {
            const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
            spectator.detectChanges();

            spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'despersonalize', {
                ...DEFAULT_PERSONA,
                pageId: '123',
                selected: false
            });
            spectator.detectChanges();

            expect(confirmDialogOpen).toHaveBeenCalled();
        });

        it('should fetchPersonas when confirming the despersonalization', () => {
            const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
            spectator.detectChanges();

            spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'despersonalize', {
                ...DEFAULT_PERSONA,
                pageId: '123',
                selected: false
            });
            spectator.detectChanges();

            expect(confirmDialogOpen).toHaveBeenCalled();

            const confirmDialog = spectator.query(byTestId('confirm-dialog'));
            const personaSelector = spectator.debugElement.query(
                By.css('[data-testId="persona-selector"]')
            ).componentInstance;

            const fetchPersonasSpy = jest.spyOn(personaSelector, 'fetchPersonas');

            spectator.detectChanges();

            expect(confirmDialogOpen).toHaveBeenCalled();

            confirmDialog
                .querySelector('.p-confirm-dialog-accept')
                .dispatchEvent(new Event('click')); // This is the internal button, coudln't find a better way to test it

            spectator.detectChanges();

            expect(fetchPersonasSpy).toHaveBeenCalled();
        });

        it('should navigate with default persona as current persona when the selected is the same as the despersonalized', () => {
            const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
            spectator.detectChanges();

            spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'despersonalize', {
                ...CUSTOM_PERSONA,
                pageId: '123',
                selected: true
            });
            spectator.detectChanges();

            expect(confirmDialogOpen).toHaveBeenCalled();

            const confirmDialog = spectator.query(byTestId('confirm-dialog'));

            const routerSpy = jest.spyOn(spectator.inject(Router), 'navigate');

            spectator.detectChanges();

            expect(confirmDialogOpen).toHaveBeenCalled();

            confirmDialog
                .querySelector('.p-confirm-dialog-accept')
                .dispatchEvent(new Event('click')); // This is the internal button, coudln't find a better way to test it

            spectator.detectChanges();

            expect(routerSpy).toHaveBeenCalledWith([], {
                queryParams: {
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                },
                queryParamsHandling: 'merge'
            });
        });
    });

    it('should show the info display when you cannot edit the page', () => {
        store.load({
            url: 'index',
            language_id: '6',
            'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
        });

        spectator.detectChanges();

        const infoDisplay = spectator.query(byTestId('info-display'));

        expect(infoDisplay).not.toBeNull();
    });

    it('should show the info display when trying to edit a variant of a running experiment', () => {
        store.load({
            url: 'index',
            language_id: '6',
            'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier,
            experimentId: 'i-have-a-running-experiment'
        }); // This will load a page with a running experiment

        spectator.detectChanges();

        const infoDisplay = spectator.query(byTestId('info-display'));

        expect(infoDisplay).not.toBeNull();
    });

    it('should show the info display when trying to edit a variant of an scheduled experiment', () => {
        store.load({
            url: 'index',
            language_id: '6',
            'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier,
            experimentId: 'i-have-a-scheduled-experiment'
        }); // This will load a page with a scheduled experiment

        spectator.detectChanges();

        const infoDisplay = spectator.query(byTestId('info-display'));

        expect(infoDisplay).not.toBeNull();
    });

    it('should render the running experiment component', () => {
        store.load({
            url: 'index',
            language_id: '5',
            'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier,
            experimentId: 'i-have-a-running-experiment'
        }); // This will load a page with a running experiment

        spectator.detectChanges();

        const runningExperiment = spectator.query(byTestId('ema-running-experiment'));

        expect(runningExperiment).not.toBeNull();
    });

    it('should show the components that need showed on preview mode', () => {
        const componentsToShow = ['info-display']; // Test id of components that should show when entering preview modes

        spectator.detectChanges();

        const iphone = { ...mockDotDevices[0], icon: 'someIcon' };

        store.setDevice(iphone);
        spectator.detectChanges();

        componentsToShow.forEach((testId) => {
            expect(spectator.query(byTestId(testId))).not.toBeNull();
        });
    });

    describe('API URL', () => {
        it('should have the url setted with the current language and persona', () => {
            spectator.detectChanges();

            const button = spectator.debugElement.query(By.css('[data-testId="ema-api-link"]'));

            expect(button.nativeElement.href).toBe(
                'http://localhost/api/v1/page/json/page-one?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&mode=EDIT_MODE'
            );
        });

        it('should open a new tab', () => {
            spectator.detectChanges();

            const button = spectator.debugElement.query(By.css('[data-testId="ema-api-link"]'));

            expect(button.nativeElement.target).toBe('_blank');
        });
    });

    describe('language selector', () => {
        it('should have a language selector', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('language-selector'))).not.toBeNull();
        });

        it("should have the current language as label in the language selector's button", () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('language-button')).textContent).toBe('English - US');
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
