import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MAX_INPUT_TITLE_LENGTH } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotExperimentsAddVariantDialogComponent,
    DotExperimentsAddVariantDialogData
} from './dot-experiments-add-variant-dialog.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.variants.add-dialog.default-name': 'Variant {0}',
    'experiments.configure.variants.add-dialog.name.label': 'Name',
    'experiments.configure.variants.add-dialog.submit': 'Add Variant',
    'experiments.configure.variants.add-dialog.cancel': 'Cancel'
});

describe('DotExperimentsAddVariantDialogComponent', () => {
    let spectator: Spectator<DotExperimentsAddVariantDialogComponent>;
    let dialogRef: DynamicDialogRef;

    const createComponent = createComponentFactory({
        component: DotExperimentsAddVariantDialogComponent,
        providers: [
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            { provide: DotMessageService, useValue: messageServiceMock }
        ],
        detectChanges: false
    });

    /** Opens the dialog with the names already in use, as the Variants card supplies them. */
    const render = (existingNames: string[] = []) => {
        const data: DotExperimentsAddVariantDialogData = { existingNames };
        spectator = createComponent({
            providers: [{ provide: DynamicDialogConfig, useValue: { data } }]
        });
        dialogRef = spectator.inject(DynamicDialogRef, true);
        spectator.detectChanges();
    };

    const nameInput = (): HTMLInputElement =>
        spectator.query(byTestId('add-variant-name-input')) as HTMLInputElement;

    const type = (text: string) => {
        spectator.typeInElement(text, nameInput());
        spectator.detectChanges();
    };

    /**
     * The form's own submit, which is what a press of Enter inside the field triggers in a
     * browser — jsdom does not implement implicit submission, so it is dispatched here.
     */
    const submit = () => {
        spectator.dispatchFakeEvent(byTestId('add-variant-form'), 'submit');
        spectator.detectChanges();
    };

    const clickButton = (testId: string) => {
        spectator.click(spectator.query(byTestId(testId))?.querySelector('button') as HTMLElement);
        spectator.detectChanges();
    };

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('form', () => {
        it('should render the name field', () => {
            render();

            expect(nameInput()).not.toBeNull();
        });

        it('should start empty, so the name is genuinely optional', () => {
            render();

            expect(nameInput().value).toBe('');
        });

        it('should offer a hint rather than an error before anything is typed', () => {
            render();

            expect(spectator.query(byTestId('add-variant-name-hint'))).not.toBeNull();
            expect(spectator.query(byTestId('add-variant-name-error'))).toBeNull();
        });
    });

    describe('submitting a name', () => {
        it('should close with the typed name', () => {
            render(['Original']);

            type('Blue CTA');
            submit();

            expect(dialogRef.close).toHaveBeenCalledWith({ name: 'Blue CTA' });
        });

        it('should trim the typed name', () => {
            render(['Original']);

            type('   Blue CTA   ');
            submit();

            expect(dialogRef.close).toHaveBeenCalledWith({ name: 'Blue CTA' });
        });

        it('should close when the submit button is pressed', () => {
            render(['Original']);

            type('Blue CTA');
            clickButton('add-variant-submit-btn');

            expect(dialogRef.close).toHaveBeenCalledWith({ name: 'Blue CTA' });
        });
    });

    describe('generated fallback name', () => {
        it('should close with the next free name when nothing was typed', () => {
            render(['Original', 'Variant 2']);

            submit();

            expect(dialogRef.close).toHaveBeenCalledWith({ name: 'Variant 3' });
        });

        it('should walk past a name that is already in use', () => {
            // Deleting a middle variant would otherwise regenerate a name still on screen.
            render(['Original', 'Variant 3']);

            submit();

            expect(dialogRef.close).toHaveBeenCalledWith({ name: 'Variant 4' });
        });

        it('should ignore the casing and padding of the names already in use', () => {
            render(['Original', '  variant 3  ']);

            submit();

            expect(dialogRef.close).toHaveBeenCalledWith({ name: 'Variant 4' });
        });

        it('should treat a name of only spaces as blank', () => {
            render(['Original']);

            type('   ');
            submit();

            expect(dialogRef.close).toHaveBeenCalledWith({ name: 'Variant 2' });
        });
    });

    describe('name length', () => {
        const tooLongName = 'x'.repeat(MAX_INPUT_TITLE_LENGTH + 1);

        it('should reveal an error instead of closing', () => {
            render();

            type(tooLongName);
            submit();

            expect(spectator.query(byTestId('add-variant-name-error'))).not.toBeNull();
            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('should accept a name of exactly the maximum length', () => {
            const longestName = 'x'.repeat(MAX_INPUT_TITLE_LENGTH);
            render();

            type(longestName);
            submit();

            expect(dialogRef.close).toHaveBeenCalledWith({ name: longestName });
        });
    });

    describe('cancelling', () => {
        // The X and ESC are PrimeNG's dialog chrome and close with `undefined` on their own;
        // the Cancel button is the one this component owns.
        it('should close with nothing', () => {
            render(['Original']);

            type('Blue CTA');
            clickButton('add-variant-cancel-btn');

            expect(dialogRef.close).toHaveBeenCalledWith();
        });
    });
});
