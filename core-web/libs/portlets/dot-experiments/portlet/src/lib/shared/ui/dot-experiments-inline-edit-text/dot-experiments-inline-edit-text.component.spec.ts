import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { DotFieldValidationMessageComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsInlineEditTextComponent } from './dot-experiments-inline-edit-text.component';

const messageServiceMock = new MockDotMessageService({
    'dot.common.inplace.empty.text': 'default message',
    'experiments.configure.description.add': 'click',
    'experiments.configure.scheduling.start': 'When the experiment start'
});

const EMPTY_TEXT = '';
const SHORT_TEXT = 'short text';
const LONG_TEXT =
    'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed condimentum eros sit amet malesuada mattis. Morbi ac congue lectus, ut vestibulum velit. Ut sed ornare metus. Proin a orci lacus. Aenean odio lacus, fringilla eu ipsum non, pellentesque sagittis purus. Integer non.';
const NEW_EXPERIMENT_DESCRIPTION = 'new experiment description';

describe('DotExperimentsInlineEditTextComponent', () => {
    let spectator: Spectator<DotExperimentsInlineEditTextComponent>;
    const createComponent = createComponentFactory({
        component: DotExperimentsInlineEditTextComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should has a Inplace component', () => {
        expect(spectator.query('p-inplace')).toExist();
    });

    describe('@Input empty', () => {
        it('should show a message of `add a description`', () => {
            spectator.setInput('text', '');
            expect(spectator.query(byTestId('empty-text-message'))).toExist();
            expect(spectator.query(byTestId('empty-text-message'))).toContainText(
                'default message'
            );
        });

        it('should show a empty message sent by `@Input emptyText` ', () => {
            spectator.setInput('text', '');
            spectator.setInput('emptyTextMessage', 'message.sent.by.input');
            expect(spectator.query(byTestId('empty-text-message'))).toExist();
            expect(spectator.query(byTestId('empty-text-message'))).toContainText(
                'message.sent.by.input'
            );
        });

        it('should disable the Inplace with `@Input disabled` ', () => {
            spectator.setInput('disabled', true);
            expect(spectator.component.$disabled()).toBe(true);
            const inplaceElement = spectator.query('p-inplace');
            expect(inplaceElement).toExist();
        });
    });

    describe('@Input `text` not empty', () => {
        beforeEach(() => {
            const text = SHORT_TEXT;
            spectator.setInput({ text });
        });

        it('should show a icon to edit', () => {
            expect(spectator.query(byTestId('text-input'))).toExist();
            expect(spectator.query(byTestId('text-input-button'))).toExist();
        });

        it('should change the `maxLength` Validator if the `@Input maxCharacterLength` is sent', () => {
            const maxLength = 5;
            spectator.setInput('text', SHORT_TEXT);
            spectator.setInput('maxCharacterLength', maxLength);

            expect(spectator.component.form.invalid).toEqual(true);
        });

        it('should emit text trimmed from the input', () => {
            const TEXT_WITH_SPACES = '  text with spaces  ';
            const TEXT_WITHOUT_SPACES = 'text with spaces';

            let output;
            spectator.output('$textChanged').subscribe((result) => (output = result));

            spectator.component.form.controls['text'].setValue(TEXT_WITH_SPACES);
            spectator.component.saveAction();

            expect(output).toBe(TEXT_WITHOUT_SPACES);
        });

        it('should add the Validator `required` if the `@Input required` is true', () => {
            spectator.setInput('text', EMPTY_TEXT);
            spectator.setInput('required', true);

            spectator.component.form.controls['text'].setValue(SHORT_TEXT);
            expect(spectator.component.form.invalid).toEqual(false);

            spectator.component.form.controls['text'].setValue(EMPTY_TEXT);
            expect(spectator.component.form.invalid).toEqual(true);
        });

        describe('/interactions', () => {
            it('should show an input if you click on edit', () => {
                spectator.click(byTestId('text-input'));
                expect(spectator.query(byTestId('inplace-input'))).toExist();
            });

            it('should not show an input if you press `ESC` in the keyboard', () => {
                jest.spyOn(spectator.component, 'deactivateInplace');

                spectator.click(byTestId('text-input'));
                expect(spectator.query(byTestId('inplace-input'))).toExist();

                spectator.dispatchKeyboardEvent(
                    spectator.query(byTestId('inplace-input')),
                    'keydown',
                    'Escape'
                );

                expect(spectator.component.deactivateInplace).toHaveBeenCalled();
                expect(spectator.query(byTestId('inplace-input'))).not.toExist();
            });

            it('should save button be disabled if the input has more than `MAX_INPUT_DESCRIPTIVE_LENGTH` ', () => {
                spectator.setInput('text', LONG_TEXT);
                spectator.dispatchMouseEvent(byTestId('text-input'), 'click');

                expect(spectator.component.form.invalid).toBe(true);
                // PrimeNG button disabled state is controlled by form.invalid || textControl.pristine
                expect(
                    spectator.component.form.invalid || spectator.component.textControl.pristine
                ).toBe(true);
                expect(spectator.query(DotFieldValidationMessageComponent)).toExist();
            });

            it('should emit the changed text when click on save icon', () => {
                let output;
                spectator.output('$textChanged').subscribe((result) => (output = result));

                spectator.dispatchMouseEvent(byTestId('text-input'), 'click');

                spectator.component.form.controls['text'].setValue(NEW_EXPERIMENT_DESCRIPTION);
                spectator.component.form.controls['text'].markAsDirty();

                spectator.detectComponentChanges();
                // PrimeNG button disabled state is controlled by form.invalid || textControl.pristine
                expect(spectator.component.form.invalid).toBe(false);
                expect(spectator.component.textControl.pristine).toBe(false);
                const saveButton = spectator.query(byTestId('text-save-btn'));
                expect(saveButton).toExist();

                // Find the actual button element inside PrimeNG component and click it
                const actualButton = saveButton.querySelector('button') as HTMLButtonElement;
                expect(actualButton).toBeTruthy();
                spectator.click(actualButton);
                spectator.detectChanges();

                expect(output).toBe(NEW_EXPERIMENT_DESCRIPTION);
            });

            it('should save button be loading if the isLoading @Input is true ', () => {
                spectator.dispatchMouseEvent(byTestId('text-input'), 'click');
                spectator.setInput('isLoading', true);
                spectator.detectChanges();

                expect(spectator.component.$isLoading()).toBe(true);
                const saveButton = spectator.query(byTestId('text-save-btn'));
                expect(saveButton).toExist();
            });

            it('should deactivate the inplace if isLoading input has `previousValue= true` and `currentValue = false` ', () => {
                const deactivateInplaceSpy = jest.spyOn(spectator.component, 'deactivateInplace');
                // saving
                spectator.setInput('isLoading', true);
                // finished saving
                spectator.setInput('isLoading', false);

                expect(deactivateInplaceSpy).toHaveBeenCalled();
            });

            it('should deactivate the textControl if isLoading input has `currentValue = true` ', () => {
                spectator.dispatchMouseEvent(byTestId('text-input'), 'click');

                const input = spectator.query(byTestId('inplace-input')) as HTMLInputElement;

                expect(input.disabled).toBe(false);

                spectator.setInput('isLoading', true);
                expect(input.disabled).toBe(true);
            });

            it('should show `dot-field-validation-message` message error by default', () => {
                spectator.setInput('text', SHORT_TEXT);
                spectator.setInput('required', true);

                spectator.click(byTestId('text-input'));

                spectator.component.form.controls['text'].setValue(EMPTY_TEXT);
                spectator.component.form.updateValueAndValidity();
                spectator.detectComponentChanges();

                expect(spectator.component.form.invalid).toEqual(true);

                expect(spectator.query(DotFieldValidationMessageComponent)).toExist();
            });

            it("shouldn't show `dot-field-validation-message` message error if the showError input is `false`", () => {
                spectator.setInput('text', SHORT_TEXT);
                spectator.setInput('required', true);
                spectator.setInput('showErrorMsg', false);

                spectator.click(byTestId('text-input'));

                spectator.component.form.controls['text'].setValue(EMPTY_TEXT);
                spectator.component.form.updateValueAndValidity();
                spectator.detectComponentChanges();

                expect(spectator.component.form.invalid).toEqual(true);

                expect(spectator.query(DotFieldValidationMessageComponent)).not.toExist();
            });
        });
    });
});
