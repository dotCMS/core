import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';

import { DotStyleEditorFieldFormComponent } from './dot-style-editor-field-form.component';
import { BuilderField } from './models';

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
    'style.editor.form.builder.field.option.add': 'Add Option',
    'style.editor.form.builder.field.description.placeholder': 'e.g. Enter your text here...',
    'style.editor.form.builder.field.error.label.required': 'Label is required',
    'style.editor.form.builder.field.error.identifier.required': 'Identifier is required',
    'style.editor.form.builder.field.error.options.required': 'At least one option is required',
    'style.editor.form.builder.field.type.short.text': 'Short Text',
    'style.editor.form.builder.field.type.dropdown': 'Dropdown',
    'style.editor.form.builder.field.type.radio': 'Radio Buttons',
    'style.editor.form.builder.field.type.checkbox.group': 'Checkbox Group'
};

const dotMessageServiceMock = {
    get: (key: string, ...args: string[]) => {
        const template = MOCK_MESSAGES[key] ?? key;
        return args.reduce((acc, arg, i) => acc.replace(`{${i}}`, arg), template);
    }
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
    let fixture: ComponentFixture<DotStyleEditorFieldFormComponent>;
    let comp: DotStyleEditorFieldFormComponent;
    let de: DebugElement;

    function setup(field: BuilderField = INPUT_FIELD, showErrors = false): void {
        fixture = TestBed.createComponent(DotStyleEditorFieldFormComponent);
        fixture.componentRef.setInput('field', field);
        fixture.componentRef.setInput('isFirst', false);
        fixture.componentRef.setInput('isLast', false);
        fixture.componentRef.setInput('showErrors', showErrors);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        fixture.detectChanges();
    }

    function typeInInput(input: DebugElement, value: string): void {
        input.nativeElement.value = value;
        input.nativeElement.dispatchEvent(new Event('input'));
        fixture.detectChanges();
    }

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotStyleEditorFieldFormComponent],
            providers: [{ provide: DotMessageService, useValue: dotMessageServiceMock }]
        }).compileComponents();
    });

    describe('Label → Identifier auto-link', () => {
        it('should auto-generate the identifier when the label changes', () => {
            setup();

            typeInInput(de.query(By.css('input[placeholder="New Field"]')), 'Background Color');

            // Check the component state signal directly — ngModel DOM sync may lag behind
            expect(comp.$identifier()).toBe('backgroundColor');
        });

        it('should stop updating the identifier after it has been manually edited', () => {
            setup();

            typeInInput(de.query(By.css('input[placeholder="fieldId"]')), 'myCustomId');
            typeInInput(de.query(By.css('input[placeholder="New Field"]')), 'New Label');

            expect(comp.$identifier()).toBe('myCustomId');
        });

        it('should show the reset button after the identifier is manually edited', () => {
            setup();

            expect(de.query(By.css('[data-testid="reset-identifier-btn"]'))).toBeNull();

            typeInInput(de.query(By.css('input[placeholder="fieldId"]')), 'custom');

            expect(de.query(By.css('[data-testid="reset-identifier-btn"]'))).not.toBeNull();
        });

        it('should re-link identifier to label and hide reset button after clicking reset', () => {
            setup({ ...INPUT_FIELD, label: 'My Label', identifier: 'myLabel' });

            typeInInput(de.query(By.css('input[placeholder="fieldId"]')), 'custom');
            de.query(By.css('[data-testid="reset-identifier-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(de.query(By.css('[data-testid="reset-identifier-btn"]'))).toBeNull();

            // Typing in label now updates identifier again — check component state signal
            typeInInput(de.query(By.css('input[placeholder="New Field"]')), 'Relabeled');

            expect(comp.$identifier()).toBe('relabeled');
        });
    });

    describe('Field type', () => {
        it('should show the description input when type is input', () => {
            setup(INPUT_FIELD);

            expect(
                de.query(By.css('input[placeholder="e.g. Enter your text here..."]'))
            ).not.toBeNull();
        });

        it('should show the options card when type is dropdown', () => {
            setup(DROPDOWN_FIELD);

            expect(de.query(By.css('input[placeholder="Label"]'))).not.toBeNull();
        });

        it('should hide the description input when type is not input', () => {
            setup(DROPDOWN_FIELD);

            expect(
                de.query(By.css('input[placeholder="e.g. Enter your text here..."]'))
            ).toBeNull();
        });
    });

    describe('Options management', () => {
        it('should add an option row when "Add Option" is clicked', () => {
            setup(DROPDOWN_FIELD);
            const before = de.queryAll(By.css('input[placeholder="Label"]')).length;

            de.query(By.css('[data-testid="add-option-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(de.queryAll(By.css('input[placeholder="Label"]')).length).toBe(before + 1);
        });

        it('should remove an option row when its remove button is clicked', () => {
            setup(DROPDOWN_FIELD);
            const before = de.queryAll(By.css('input[placeholder="Label"]')).length;

            de.query(By.css('[data-testid="remove-option-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(de.queryAll(By.css('input[placeholder="Label"]')).length).toBe(before - 1);
        });

        it('should auto-generate option value from option label', () => {
            setup(DROPDOWN_FIELD);

            typeInInput(de.queryAll(By.css('input[placeholder="Label"]'))[0], 'Heading One');

            // Check the component state signal directly — ngModel DOM sync may lag behind
            expect(comp.$options()[0].value).toBe('headingOne');
        });

        it('should stop updating option value once it has been manually edited', () => {
            setup(DROPDOWN_FIELD);

            typeInInput(de.queryAll(By.css('input[placeholder="value"]'))[0], 'manual-value');
            typeInInput(de.queryAll(By.css('input[placeholder="Label"]'))[0], 'New Label');

            expect(comp.$options()[0].value).toBe('manual-value');
        });
    });

    describe('Output events', () => {
        it('should emit fieldChange with updated label when the label input changes', () => {
            setup();
            let emitted: BuilderField | undefined;
            comp.fieldChange.subscribe((f) => (emitted = f));

            typeInInput(de.query(By.css('input[placeholder="New Field"]')), 'Updated Label');

            expect(emitted?.label).toBe('Updated Label');
            expect(emitted?.uid).toBe(INPUT_FIELD.uid);
        });

        it('should emit delete when the delete button is clicked', () => {
            setup();
            let emitted = false;
            comp.delete.subscribe(() => (emitted = true));

            de.query(By.css('[data-testid="delete-field-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(emitted).toBe(true);
        });

        it('should emit moveUp when the move-up button is clicked', () => {
            setup();
            let emitted = false;
            comp.moveUp.subscribe(() => (emitted = true));

            de.query(By.css('[data-testid="move-up-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(emitted).toBe(true);
        });

        it('should emit moveDown when the move-down button is clicked', () => {
            setup();
            let emitted = false;
            comp.moveDown.subscribe(() => (emitted = true));

            de.query(By.css('[data-testid="move-down-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(emitted).toBe(true);
        });

        it('should disable move-up when isFirst is true', () => {
            fixture = TestBed.createComponent(DotStyleEditorFieldFormComponent);
            fixture.componentRef.setInput('field', INPUT_FIELD);
            fixture.componentRef.setInput('isFirst', true);
            fixture.componentRef.setInput('isLast', false);
            fixture.componentRef.setInput('showErrors', false);
            de = fixture.debugElement;
            fixture.detectChanges();

            const btn = de.query(By.css('[data-testid="move-up-btn"] button'));
            expect(btn.nativeElement.disabled).toBe(true);
        });

        it('should disable move-down when isLast is true', () => {
            fixture = TestBed.createComponent(DotStyleEditorFieldFormComponent);
            fixture.componentRef.setInput('field', INPUT_FIELD);
            fixture.componentRef.setInput('isFirst', false);
            fixture.componentRef.setInput('isLast', true);
            fixture.componentRef.setInput('showErrors', false);
            de = fixture.debugElement;
            fixture.detectChanges();

            const btn = de.query(By.css('[data-testid="move-down-btn"] button'));
            expect(btn.nativeElement.disabled).toBe(true);
        });
    });

    describe('Validation errors (showErrors=true)', () => {
        it('should show a label error when label is empty', () => {
            setup({ ...INPUT_FIELD, label: '' }, true);

            const errors = de.queryAll(By.css('small.text-red-500'));
            expect(
                errors.some((e) => e.nativeElement.textContent.includes('Label is required'))
            ).toBe(true);
        });

        it('should show an identifier error when identifier is empty', () => {
            setup({ ...INPUT_FIELD, identifier: '' }, true);

            const errors = de.queryAll(By.css('small.text-red-500'));
            expect(
                errors.some((e) => e.nativeElement.textContent.includes('Identifier is required'))
            ).toBe(true);
        });

        it('should show an options-count error when a dropdown has no options', () => {
            setup({ ...DROPDOWN_FIELD, options: [] }, true);

            const errors = de.queryAll(By.css('small.text-red-500'));
            expect(
                errors.some((e) =>
                    e.nativeElement.textContent.includes('At least one option is required')
                )
            ).toBe(true);
        });

        it('should not show any errors when showErrors is false', () => {
            setup({ ...INPUT_FIELD, label: '', identifier: '' }, false);

            expect(de.queryAll(By.css('small.text-red-500')).length).toBe(0);
        });
    });
});
