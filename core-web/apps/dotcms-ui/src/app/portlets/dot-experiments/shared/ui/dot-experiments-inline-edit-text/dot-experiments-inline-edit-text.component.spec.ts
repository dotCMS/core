import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { ButtonDirective } from 'primeng/button';
import { Inplace } from 'primeng/inplace';

import { DotFieldValidationMessageComponent } from '@components/_common/dot-field-validation-message/dot-field-validation-message';
import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsInlineEditTextComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-inline-edit-text/dot-experiments-inline-edit-text.component';

const messageServiceMock = new MockDotMessageService({
    'dot.common.inplace.empty.text': 'default message',
    'experiments.configure.description.add': 'click',
    'experiments.configure.scheduling.start': 'When the experiment start'
});

const SHORT_TEXT = 'short text';
const LONG_TEXT =
    'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed condimentum eros sit amet malesuada mattis. Morbi ac congue lectus, ut vestibulum velit. Ut sed ornare metus. Proin a orci lacus. Aenean odio lacus, fringilla eu ipsum non, pellentesque sagittis purus. Integer non.';
const NEW_EXPERIMENT_DESCRIPTION = 'new experiment description';

describe('DotExperimentsExperimentSummaryComponent', () => {
    let spectator: Spectator<DotExperimentsInlineEditTextComponent>;
    const createComponent = createComponentFactory({
        imports: [],
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
        expect(spectator.query(Inplace)).toExist();
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
            spectator.setInput('emptyText', 'message.sent.by.input');
            expect(spectator.query(byTestId('empty-text-message'))).toExist();
            expect(spectator.query(byTestId('empty-text-message'))).toContainText(
                'message.sent.by.input'
            );
        });

        it('should disable the Inplace with `@Input disabled` ', () => {
            spectator.setInput('disabled', true);
            expect(spectator.query(Inplace).disabled).toBe(true);
        });
    });

    describe('@Input `text` not empty', () => {
        beforeEach(() => {
            const text = SHORT_TEXT;
            spectator.setInput({ text });
        });

        it('should show a icon to edit', () => {
            expect(spectator.query(byTestId('text-input'))).toExist();
            expect(spectator.query(byTestId('text-input-icon'))).toExist();
        });

        it('should change the `maxLength` Validator if the `@Input maxCharacterLength` is sent', () => {
            const maxLength = 5;
            spectator.setInput('text', SHORT_TEXT);
            spectator.setInput('maxCharacterLength', maxLength);

            expect(spectator.component.form.invalid).toEqual(true);
        });

        describe('/interactions', () => {
            it('should show an input if you click on edit', () => {
                spectator.dispatchMouseEvent(byTestId('text-input'), 'click');
                expect(spectator.query(byTestId('inplace-input'))).toExist();
            });

            it('should save button be disabled if the input has more than `MAX_INPUT_DESCRIPTIVE_LENGTH` ', () => {
                spectator.setInput('text', LONG_TEXT);
                spectator.dispatchMouseEvent(byTestId('text-input'), 'click');

                expect(spectator.component.form.invalid).toBe(true);
                expect(
                    (spectator.query(byTestId('text-save-btn')) as HTMLButtonElement).disabled
                ).toBe(true);
                expect(spectator.query(DotFieldValidationMessageComponent)).toExist();
            });

            it('should emit the changed text when click on save icon', () => {
                let output;
                spectator.output('textChanged').subscribe((result) => (output = result));

                spectator.dispatchMouseEvent(byTestId('text-input'), 'click');

                spectator.component.form.controls['text'].setValue(NEW_EXPERIMENT_DESCRIPTION);
                spectator.component.form.controls['text'].markAsDirty();

                spectator.detectComponentChanges();
                const saveButton = spectator.query(byTestId('text-save-btn')) as HTMLButtonElement;
                expect(saveButton.disabled).toBe(false);

                spectator.dispatchMouseEvent(byTestId('text-save-btn'), 'click');

                expect(output).toBe(NEW_EXPERIMENT_DESCRIPTION);
            });

            it('should save button be loading if the isLoading @Input is true ', () => {
                spectator.dispatchMouseEvent(byTestId('text-input'), 'click');
                spectator.setInput('isLoading', true);

                expect(spectator.query(ButtonDirective).loading).toBe(true);
            });

            it('should deactivate the inplace if isLoading input has `previusValue= true` and `currentValue = false` ', () => {
                const deactivate = spyOn(spectator.component.inplace, 'deactivate');
                // saving
                spectator.setInput('isLoading', true);
                // finished saving
                spectator.setInput('isLoading', false);

                expect(deactivate).toHaveBeenCalled();
            });
        });
    });
});
