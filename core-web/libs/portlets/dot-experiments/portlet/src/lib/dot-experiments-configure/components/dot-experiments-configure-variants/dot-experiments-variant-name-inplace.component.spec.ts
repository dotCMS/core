import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MAX_INPUT_TITLE_LENGTH } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsVariantNameInplaceComponent } from './dot-experiments-variant-name-inplace.component';

const VARIANT_NAME = 'variant a';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.variants.rename': 'Rename variant',
    'dot.common.save': 'Save',
    'dot.common.cancel': 'Cancel'
});

describe('DotExperimentsVariantNameInplaceComponent', () => {
    let spectator: Spectator<DotExperimentsVariantNameInplaceComponent>;
    let emit: jest.SpyInstance;

    const createComponent = createComponentFactory({
        component: DotExperimentsVariantNameInplaceComponent,
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
        detectChanges: false
    });

    const nameInput = () => spectator.query<HTMLInputElement>(byTestId('variant-name-input'));

    const clickButton = (testId: string) => {
        const host = spectator.query(byTestId(testId));
        spectator.click(host?.querySelector('button') as HTMLElement);
        spectator.detectChanges();
    };

    const isButtonDisabled = (testId: string): boolean =>
        (spectator.query(byTestId(testId))?.querySelector('button') as HTMLButtonElement).disabled;

    const startEditing = () => clickButton('variant-name-edit-btn');

    const type = (text: string) => {
        spectator.typeInElement(text, nameInput() as HTMLInputElement);
        spectator.detectChanges();
    };

    beforeEach(() => {
        spectator = createComponent();
        spectator.setInput('name', VARIANT_NAME);
        spectator.detectChanges();
        emit = jest.spyOn(spectator.component.$nameChanged, 'emit');
    });

    describe('display mode', () => {
        it('should read as plain text with a way into the editor', () => {
            expect(spectator.query(byTestId('variant-name'))?.textContent).toContain(VARIANT_NAME);
            expect(spectator.query(byTestId('variant-name-edit-btn'))).not.toBeNull();
            expect(nameInput()).toBeNull();
        });

        it('should follow the persisted name when it changes', () => {
            spectator.setInput('name', 'renamed variant');
            spectator.detectChanges();

            expect(spectator.query(byTestId('variant-name'))?.textContent).toContain(
                'renamed variant'
            );
        });

        it('should offer no pencil while disabled', () => {
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            expect(spectator.query(byTestId('variant-name-edit-btn'))).toBeNull();
            expect(spectator.query(byTestId('variant-name'))?.textContent).toContain(VARIANT_NAME);
        });
    });

    describe('edit mode', () => {
        beforeEach(() => startEditing());

        it('should open on the persisted name', () => {
            expect(nameInput()?.value).toBe(VARIANT_NAME);
            expect(spectator.query(byTestId('variant-name'))).toBeNull();
        });

        it('should emit the new name and close on save', () => {
            type('variant b');

            clickButton('variant-name-save-btn');

            expect(emit).toHaveBeenCalledWith('variant b');
            expect(nameInput()).toBeNull();
        });

        it('should emit the name trimmed', () => {
            type('  variant b  ');

            clickButton('variant-name-save-btn');

            expect(emit).toHaveBeenCalledWith('variant b');
        });

        it('should save on Enter', () => {
            type('variant b');

            spectator.dispatchKeyboardEvent(nameInput() as HTMLInputElement, 'keydown', 'Enter');
            spectator.detectChanges();

            expect(emit).toHaveBeenCalledWith('variant b');
            expect(nameInput()).toBeNull();
        });

        it('should discard the draft on Escape', () => {
            type('variant b');

            spectator.dispatchKeyboardEvent(nameInput() as HTMLInputElement, 'keydown', 'Escape');
            spectator.detectChanges();

            expect(emit).not.toHaveBeenCalled();
            expect(spectator.query(byTestId('variant-name'))?.textContent).toContain(VARIANT_NAME);
        });

        it('should discard the draft on cancel', () => {
            type('variant b');

            clickButton('variant-name-cancel-btn');

            expect(emit).not.toHaveBeenCalled();
            expect(spectator.query(byTestId('variant-name'))?.textContent).toContain(VARIANT_NAME);
        });

        it('should reopen on the persisted name after a cancel', () => {
            type('variant b');
            clickButton('variant-name-cancel-btn');

            startEditing();

            expect(nameInput()?.value).toBe(VARIANT_NAME);
        });

        it('should not allow saving the name it opened with', () => {
            expect(isButtonDisabled('variant-name-save-btn')).toBe(true);
        });

        it('should not allow saving a blank name', () => {
            type('   ');

            expect(isButtonDisabled('variant-name-save-btn')).toBe(true);
        });

        it('should not allow saving a name the backend would reject', () => {
            type('x'.repeat(MAX_INPUT_TITLE_LENGTH + 1));

            expect(isButtonDisabled('variant-name-save-btn')).toBe(true);
        });

        it('should allow saving a changed, valid name', () => {
            type('variant b');

            expect(isButtonDisabled('variant-name-save-btn')).toBe(false);
        });

        it('should never emit while saving is disabled', () => {
            type('   ');

            clickButton('variant-name-save-btn');

            expect(emit).not.toHaveBeenCalled();
        });

        it('should drop the draft when the persisted name changes underneath it', () => {
            // What a landed — or rejected — rename does: the row reverts to what the store holds
            // rather than leaving a stale draft in the editor.
            type('variant b');

            spectator.setInput('name', 'renamed by the server');
            spectator.detectChanges();

            expect(nameInput()?.value).toBe('renamed by the server');
        });
    });
});
