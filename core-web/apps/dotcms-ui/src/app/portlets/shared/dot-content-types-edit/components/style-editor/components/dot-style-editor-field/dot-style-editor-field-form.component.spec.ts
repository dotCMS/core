import { Spectator, byTestId, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import { DotStyleEditorFieldFormComponent } from './dot-style-editor-field-form.component';

import { BuilderField } from '../../models';

const MOCK_MESSAGES: Record<string, string> = {
    'style.editor.form.builder.field.new': 'New Field',
    'style.editor.form.builder.field.identifier.placeholder': 'fieldId',
    'style.editor.form.builder.field.identifier.reset.title': 'Reset to auto-generated',
    'style.editor.form.builder.field.delete.title': 'Delete field',
    'style.editor.form.builder.field.move.up.title': 'Move up',
    'style.editor.form.builder.field.move.down.title': 'Move down',
    'style.editor.form.builder.field.option.label.placeholder': 'Label',
    'style.editor.form.builder.field.option.value.placeholder': 'value',
    'style.editor.form.builder.field.option.remove.title': 'Remove option',
    'style.editor.form.builder.field.option.move.up.title': 'Move option up',
    'style.editor.form.builder.field.option.move.down.title': 'Move option down',
    'style.editor.form.builder.field.option.add': 'Add Option',
    'style.editor.form.builder.field.description.placeholder': 'e.g. Enter your text here...',
    'style.editor.form.builder.field.error.label.required': 'Label is required',
    'style.editor.form.builder.field.error.identifier.required': 'Identifier is required',
    'style.editor.form.builder.field.error.identifier.duplicate':
        'Identifier must be unique across all fields',
    'style.editor.form.builder.field.error.options.required': 'At least one option is required',
    'style.editor.form.builder.field.type.short.text': 'Short Text',
    'style.editor.form.builder.field.type.dropdown': 'Dropdown',
    'style.editor.form.builder.field.type.radio': 'Radio Buttons',
    'style.editor.form.builder.field.type.checkbox.group': 'Checkbox Group'
};

const INPUT_FIELD: BuilderField = {
    uid: 'test-uid',
    type: 'input',
    label: 'Font Size',
    identifier: 'fontSize',
    inputType: 'text',
    placeholder: '',
    columns: 1,
    options: []
};

const DROPDOWN_FIELD: BuilderField = {
    uid: 'dropdown-uid',
    type: 'dropdown',
    label: 'Font Weight',
    identifier: 'fontWeight',
    inputType: 'text',
    placeholder: '',
    columns: 1,
    options: [
        { label: 'Bold', value: 'bold' },
        { label: 'Normal', value: 'normal' }
    ]
};

describe('DotStyleEditorFieldFormComponent', () => {
    let spectator: Spectator<DotStyleEditorFieldFormComponent>;

    const createComponent = createComponentFactory({
        component: DotStyleEditorFieldFormComponent,
        providers: [
            mockProvider(DotMessageService, {
                get: (key: string, ...args: string[]) => {
                    const template = MOCK_MESSAGES[key] ?? key;

                    return args.reduce((acc, arg, i) => acc.replace(`{${i}}`, arg), template);
                }
            })
        ]
    });

    function setup(
        field: BuilderField = INPUT_FIELD,
        showErrors = false,
        isDuplicateIdentifier = false
    ): void {
        spectator = createComponent({
            props: {
                field,
                isFirst: false,
                isLast: false,
                showErrors,
                isDuplicateIdentifier
            } as unknown
        });
    }

    function typeInInput(selector: string, value: string, index = 0): void {
        const inputs = spectator.queryAll(selector) as HTMLInputElement[];
        const input = inputs[index];
        if (input) {
            input.value = value;
            input.dispatchEvent(new Event('input'));
            spectator.detectChanges();
        }
    }

    describe('Label → Identifier auto-link', () => {
        it('should auto-generate the identifier when the label changes', () => {
            setup();

            typeInInput('input[placeholder="New Field"]', 'Background Color');

            // Check the component state signal directly — ngModel DOM sync may lag behind
            expect(spectator.component.$identifier()).toBe('backgroundColor');
        });

        it('should stop updating the identifier after it has been manually edited', () => {
            setup();

            typeInInput('input[placeholder="fieldId"]', 'myCustomId');
            typeInInput('input[placeholder="New Field"]', 'New Label');

            expect(spectator.component.$identifier()).toBe('myCustomId');
        });

        it('should show the reset button after the identifier is manually edited', () => {
            setup();

            expect(spectator.query(byTestId('reset-identifier-btn'))).toBeNull();

            typeInInput('input[placeholder="fieldId"]', 'custom');

            expect(spectator.query(byTestId('reset-identifier-btn'))).not.toBeNull();
        });

        it('should re-link identifier to label and hide reset button after clicking reset', () => {
            setup({ ...INPUT_FIELD, label: 'My Label', identifier: 'myLabel' });

            typeInInput('input[placeholder="fieldId"]', 'custom');
            spectator.query(byTestId('reset-identifier-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.query(byTestId('reset-identifier-btn'))).toBeNull();

            // Typing in label now updates identifier again — check component state signal
            typeInInput('input[placeholder="New Field"]', 'Relabeled');

            expect(spectator.component.$identifier()).toBe('relabeled');
        });
    });

    describe('Field type', () => {
        it('should show the description input when type is input', () => {
            setup(INPUT_FIELD);

            expect(
                spectator.query('input[placeholder="e.g. Enter your text here..."]')
            ).not.toBeNull();
        });

        it('should show the options card when type is dropdown', () => {
            setup(DROPDOWN_FIELD);

            expect(spectator.query('input[placeholder="Label"]')).not.toBeNull();
        });

        it('should hide the description input when type is not input', () => {
            setup(DROPDOWN_FIELD);

            expect(spectator.query('input[placeholder="e.g. Enter your text here..."]')).toBeNull();
        });
    });

    describe('Options management', () => {
        it('should add an option row when "Add Option" is clicked', () => {
            setup(DROPDOWN_FIELD);
            const before = spectator.queryAll('input[placeholder="Label"]').length;

            spectator.query(byTestId('add-option-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.queryAll('input[placeholder="Label"]').length).toBe(before + 1);
        });

        it('should remove an option row when its remove button is clicked', () => {
            setup(DROPDOWN_FIELD);
            const before = spectator.queryAll('input[placeholder="Label"]').length;

            spectator.query(byTestId('remove-option-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.queryAll('input[placeholder="Label"]').length).toBe(before - 1);
        });

        it('should auto-generate option value from option label', () => {
            setup(DROPDOWN_FIELD);

            typeInInput('input[placeholder="Label"]', 'Heading One');

            // Check the component state signal directly — ngModel DOM sync may lag behind
            expect(spectator.component.$options()[0].value).toBe('headingOne');
        });

        it('should stop updating option value once it has been manually edited', () => {
            setup(DROPDOWN_FIELD);

            typeInInput('input[placeholder="value"]', 'manual-value');
            typeInInput('input[placeholder="Label"]', 'New Label');

            expect(spectator.component.$options()[0].value).toBe('manual-value');
        });

        it('should move an option down when its move-down button is clicked', () => {
            setup(DROPDOWN_FIELD);

            const downBtns = spectator.queryAll(byTestId('move-option-down-btn'));
            downBtns[0]?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.$options().map((o) => o.value)).toEqual(['normal', 'bold']);
        });

        it('should move an option up when its move-up button is clicked', () => {
            setup(DROPDOWN_FIELD);

            const upBtns = spectator.queryAll(byTestId('move-option-up-btn'));
            upBtns[1]?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.$options().map((o) => o.value)).toEqual(['normal', 'bold']);
        });

        it('should disable move-up on the first option and move-down on the last option', () => {
            setup(DROPDOWN_FIELD);

            const upBtns = spectator.queryAll(byTestId('move-option-up-btn'));
            const downBtns = spectator.queryAll(byTestId('move-option-down-btn'));

            expect(upBtns[0]?.querySelector('button')?.disabled).toBe(true);
            expect(downBtns[downBtns.length - 1]?.querySelector('button')?.disabled).toBe(true);
        });

        it('should emit fieldChange with the reordered options', () => {
            setup(DROPDOWN_FIELD);
            jest.spyOn(spectator.component.fieldChange, 'emit');

            const downBtns = spectator.queryAll(byTestId('move-option-down-btn'));
            downBtns[0]?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.fieldChange.emit).toHaveBeenCalledWith(
                expect.objectContaining({
                    options: [
                        { label: 'Normal', value: 'normal' },
                        { label: 'Bold', value: 'bold' }
                    ]
                })
            );
        });
    });

    describe('Output events', () => {
        it('should emit fieldChange with updated label when the label input changes', () => {
            setup();
            jest.spyOn(spectator.component.fieldChange, 'emit');

            typeInInput('input[placeholder="New Field"]', 'Updated Label');

            expect(spectator.component.fieldChange.emit).toHaveBeenCalledWith(
                expect.objectContaining({ label: 'Updated Label', uid: INPUT_FIELD.uid })
            );
        });

        it('should emit delete when the delete button is clicked', () => {
            setup();
            jest.spyOn(spectator.component.delete, 'emit');

            spectator.query(byTestId('delete-field-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.delete.emit).toHaveBeenCalled();
        });

        it('should emit moveUp when the move-up button is clicked', () => {
            setup();
            jest.spyOn(spectator.component.moveUp, 'emit');

            spectator.query(byTestId('move-up-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.moveUp.emit).toHaveBeenCalled();
        });

        it('should emit moveDown when the move-down button is clicked', () => {
            setup();
            jest.spyOn(spectator.component.moveDown, 'emit');

            spectator.query(byTestId('move-down-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.moveDown.emit).toHaveBeenCalled();
        });

        it('should disable move-up when isFirst is true', () => {
            setup(INPUT_FIELD);
            spectator.setInput('isFirst', true);

            const btn = spectator.query(byTestId('move-up-btn'))?.querySelector('button');
            expect(btn?.disabled).toBe(true);
        });

        it('should disable move-down when isLast is true', () => {
            setup(INPUT_FIELD);
            spectator.setInput('isLast', true);

            const btn = spectator.query(byTestId('move-down-btn'))?.querySelector('button');
            expect(btn?.disabled).toBe(true);
        });
    });

    describe('Validation errors (showErrors=true)', () => {
        it('should show a label error when label is empty', () => {
            setup({ ...INPUT_FIELD, label: '' }, true);

            const errors = spectator.queryAll('small.text-red-500');
            expect(errors.some((e) => e.textContent?.includes('Label is required'))).toBe(true);
        });

        it('should show an identifier error when identifier is empty', () => {
            setup({ ...INPUT_FIELD, identifier: '' }, true);

            const errors = spectator.queryAll('small.text-red-500');
            expect(errors.some((e) => e.textContent?.includes('Identifier is required'))).toBe(
                true
            );
        });

        it('should show an options-count error when a dropdown has no options', () => {
            setup({ ...DROPDOWN_FIELD, options: [] }, true);

            const errors = spectator.queryAll('small.text-red-500');
            expect(
                errors.some((e) => e.textContent?.includes('At least one option is required'))
            ).toBe(true);
        });

        it('should not show any errors when showErrors is false', () => {
            setup({ ...INPUT_FIELD, label: '', identifier: '' }, false);

            expect(spectator.queryAll('small.text-red-500').length).toBe(0);
        });

        it('should show a duplicate identifier error when another field in the schema shares the same identifier', () => {
            setup(INPUT_FIELD, true, true);

            const errors = spectator.queryAll('small.text-red-500');
            expect(
                errors.some((e) =>
                    e.textContent?.includes('Identifier must be unique across all fields')
                )
            ).toBe(true);
        });

        it('should not show the duplicate identifier error before a save is attempted', () => {
            setup(INPUT_FIELD, false, true);

            expect(spectator.queryAll('small.text-red-500').length).toBe(0);
        });

        it('should apply invalid styling to the identifier input when the identifier is a duplicate', () => {
            setup(INPUT_FIELD, true, true);

            const identifierInput = spectator.query(
                'input[placeholder="fieldId"]'
            ) as HTMLInputElement;
            expect(identifierInput.classList.contains('ng-invalid')).toBe(true);
        });
    });
});
