import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotConfigurationActionBarComponent } from './dot-configuration-action-bar.component';

import { DotConfigurationSaveState } from '../../store/dot-configuration.store';

const messages = new MockDotMessageService({
    'configuration.action-bar.status.saved': 'All changes saved',
    'configuration.action-bar.status.unsaved': 'Unsaved changes',
    'configuration.action-bar.status.saving': 'Saving changes...',
    'configuration.action-bar.status.failed': 'Could not save {0}. Your unsaved changes are kept.',
    'configuration.section.locale': 'Locale',
    'configuration.action-bar.restore-defaults': 'Restore Defaults',
    'configuration.action-bar.cancel': 'Cancel',
    'configuration.action-bar.save': 'Save Changes'
});

describe('DotConfigurationActionBarComponent', () => {
    let spectator: Spectator<DotConfigurationActionBarComponent>;

    const createComponent = createComponentFactory({
        component: DotConfigurationActionBarComponent,
        providers: [{ provide: DotMessageService, useValue: messages }],
        detectChanges: false
    });

    const button = (testId: string): HTMLButtonElement | null =>
        spectator.query(byTestId(testId))?.querySelector('button') ?? null;

    const render = (inputs: Partial<Record<string, unknown>> = {}) => {
        spectator.setInput({ saveState: DotConfigurationSaveState.SAVED, ...inputs });
        spectator.detectChanges();
    };

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('status', () => {
        it('says every change is saved when the form is clean', () => {
            render();

            expect(spectator.query(byTestId('configuration-action-bar-status'))).toHaveText(
                'All changes saved'
            );
        });

        it('says there are unsaved changes', () => {
            render({ saveState: DotConfigurationSaveState.UNSAVED, dirty: true });

            expect(spectator.query(byTestId('configuration-action-bar-status'))).toHaveText(
                'Unsaved changes'
            );
        });

        it('names the section that failed and announces it as an alert', () => {
            render({
                saveState: DotConfigurationSaveState.FAILED,
                failedSectionKey: 'configuration.section.locale',
                dirty: true
            });
            const status = spectator.query(byTestId('configuration-action-bar-status'));

            expect(status).toHaveText('Could not save Locale. Your unsaved changes are kept.');
            expect(status).toHaveAttribute('role', 'alert');
        });
    });

    describe('buttons', () => {
        it('disables Cancel and Save when there is nothing to save', () => {
            render();

            expect(button('configuration-action-bar-cancel-btn')).toBeDisabled();
            expect(button('configuration-action-bar-save-btn')).toBeDisabled();
        });

        it('enables Save when the form can be saved', () => {
            render({ saveState: DotConfigurationSaveState.UNSAVED, dirty: true, canSave: true });

            expect(button('configuration-action-bar-save-btn')).not.toBeDisabled();
        });

        it('disables every action while saving', () => {
            render({ saveState: DotConfigurationSaveState.SAVING, dirty: true, canSave: false });

            expect(button('configuration-action-bar-restore-btn')).toBeDisabled();
            expect(button('configuration-action-bar-cancel-btn')).toBeDisabled();
            expect(button('configuration-action-bar-save-btn')).toBeDisabled();
        });

        it('emits save when Save Changes is clicked', () => {
            render({ saveState: DotConfigurationSaveState.UNSAVED, dirty: true, canSave: true });
            const spy = vi.spyOn(spectator.component.save, 'emit');

            spectator.click(button('configuration-action-bar-save-btn') as HTMLButtonElement);

            expect(spy).toHaveBeenCalled();
        });

        it('emits discard when Cancel is clicked', () => {
            render({ saveState: DotConfigurationSaveState.UNSAVED, dirty: true });
            const spy = vi.spyOn(spectator.component.discard, 'emit');

            spectator.click(button('configuration-action-bar-cancel-btn') as HTMLButtonElement);

            expect(spy).toHaveBeenCalled();
        });

        it('emits restoreDefaults when Restore Defaults is clicked', () => {
            render();
            const spy = vi.spyOn(spectator.component.restoreDefaults, 'emit');

            spectator.click(button('configuration-action-bar-restore-btn') as HTMLButtonElement);

            expect(spy).toHaveBeenCalled();
        });
    });
});
