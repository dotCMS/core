import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';

import { DotStyleEditorSectionComponent } from './dot-style-editor-section.component';
import { BuilderField, BuilderSection } from './models';

const MOCK_MESSAGES: Record<string, string> = {
    'style.editor.form.builder.section.title.placeholder': 'Section Title',
    'style.editor.form.builder.section.delete.title': 'Delete section',
    'style.editor.form.builder.section.move.up.title': 'Move section up',
    'style.editor.form.builder.section.move.down.title': 'Move section down',
    'style.editor.form.builder.section.empty': 'No fields yet. Click below to add one.',
    'style.editor.form.builder.section.add.field': 'Add Field to {0}',
    'style.editor.form.builder.field.delete.title': 'Delete field',
    'style.editor.form.builder.field.move.up.title': 'Move up',
    'style.editor.form.builder.field.move.down.title': 'Move down',
    'style.editor.form.builder.field.new': 'New Field',
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

const MOCK_FIELD: BuilderField = {
    uid: 'field-uid-1',
    type: 'input',
    label: 'Font Size',
    identifier: 'fontSize',
    inputType: 'text',
    placeholder: '',
    columns: 1,
    options: []
};

const MOCK_FIELD_2: BuilderField = {
    uid: 'field-uid-2',
    type: 'input',
    label: 'Line Height',
    identifier: 'lineHeight',
    inputType: 'text',
    placeholder: '',
    columns: 1,
    options: []
};

const MOCK_SECTION: BuilderSection = {
    uid: 'section-uid',
    title: 'Typography',
    fields: [MOCK_FIELD]
};

const MOCK_SECTION_TWO_FIELDS: BuilderSection = {
    uid: 'section-uid',
    title: 'Typography',
    fields: [MOCK_FIELD_2, MOCK_FIELD]
};

describe('DotStyleEditorSectionComponent', () => {
    let fixture: ComponentFixture<DotStyleEditorSectionComponent>;
    let comp: DotStyleEditorSectionComponent;
    let de: DebugElement;

    function setup(section: BuilderSection = MOCK_SECTION, isFirst = false, isLast = false): void {
        fixture = TestBed.createComponent(DotStyleEditorSectionComponent);
        fixture.componentRef.setInput('section', section);
        fixture.componentRef.setInput('isFirst', isFirst);
        fixture.componentRef.setInput('isLast', isLast);
        fixture.componentRef.setInput('showErrors', false);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        fixture.detectChanges();
    }

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotStyleEditorSectionComponent],
            providers: [{ provide: DotMessageService, useValue: dotMessageServiceMock }]
        }).compileComponents();
    });

    describe('Section header actions', () => {
        it('should emit delete when the trash button is clicked', () => {
            setup();
            let emitted = false;
            comp.delete.subscribe(() => (emitted = true));

            de.query(By.css('[data-testid="delete-section-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(emitted).toBe(true);
        });

        it('should emit moveUp when the move-up button is clicked', () => {
            setup(MOCK_SECTION, false, false);
            let emitted = false;
            comp.moveUp.subscribe(() => (emitted = true));

            de.query(By.css('[data-testid="move-section-up-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(emitted).toBe(true);
        });

        it('should emit moveDown when the move-down button is clicked', () => {
            setup(MOCK_SECTION, false, false);
            let emitted = false;
            comp.moveDown.subscribe(() => (emitted = true));

            de.query(By.css('[data-testid="move-section-down-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(emitted).toBe(true);
        });

        it('should disable move-up button when section is first', () => {
            setup(MOCK_SECTION, true, false);

            const btn = de.query(By.css('[data-testid="move-section-up-btn"] button'));
            expect(btn.nativeElement.disabled).toBe(true);
        });

        it('should disable move-down button when section is last', () => {
            setup(MOCK_SECTION, false, true);

            const btn = de.query(By.css('[data-testid="move-section-down-btn"] button'));
            expect(btn.nativeElement.disabled).toBe(true);
        });
    });

    describe('Title editing', () => {
        it('should emit titleChange when the title input is changed', () => {
            setup();
            let emitted: string | undefined;
            comp.titleChange.subscribe((t) => (emitted = t));

            const titleInput = de.query(By.css('input[placeholder="Section Title"]'));
            titleInput.nativeElement.value = 'Colors';
            titleInput.nativeElement.dispatchEvent(new Event('input'));
            fixture.detectChanges();

            expect(emitted).toBe('Colors');
        });
    });

    describe('Field interactions', () => {
        it('should emit addField when the "Add Field" button is clicked', () => {
            setup();
            let emitted = false;
            comp.addField.subscribe(() => (emitted = true));

            de.query(By.css('[data-testid="add-field-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(emitted).toBe(true);
        });

        it('should emit removeField with the field uid when field delete is triggered', () => {
            setup();
            let emittedUid: string | undefined;
            comp.removeField.subscribe((uid) => (emittedUid = uid));

            de.query(By.css('[data-testid="delete-field-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(emittedUid).toBe(MOCK_FIELD.uid);
        });

        it('should emit moveFieldUp with the field uid when field move-up is triggered', () => {
            // MOCK_FIELD is at index 1 (not first) so its Move-Up button is enabled
            setup(MOCK_SECTION_TWO_FIELDS);
            let emittedUid: string | undefined;
            comp.moveFieldUp.subscribe((uid) => (emittedUid = uid));

            // Second field's move-up button (index 1 → enabled)
            de.queryAll(By.css('[data-testid="move-up-btn"] button'))[1].nativeElement.click();
            fixture.detectChanges();

            expect(emittedUid).toBe(MOCK_FIELD.uid);
        });

        it('should emit moveFieldDown with the field uid when field move-down is triggered', () => {
            // MOCK_FIELD_2 is at index 0 (not last) so its Move-Down button is enabled
            setup(MOCK_SECTION_TWO_FIELDS);
            let emittedUid: string | undefined;
            comp.moveFieldDown.subscribe((uid) => (emittedUid = uid));

            // First field's move-down button (index 0 → enabled)
            de.queryAll(By.css('[data-testid="move-down-btn"] button'))[0].nativeElement.click();
            fixture.detectChanges();

            expect(emittedUid).toBe(MOCK_FIELD_2.uid);
        });

        it('should emit fieldChange when a field label is updated', () => {
            setup();
            let emittedField: BuilderField | undefined;
            comp.fieldChange.subscribe((f) => (emittedField = f));

            const labelInput = de.query(By.css('input[placeholder="New Field"]'));
            labelInput.nativeElement.value = 'Line Height';
            labelInput.nativeElement.dispatchEvent(new Event('input'));
            fixture.detectChanges();

            expect(emittedField?.uid).toBe(MOCK_FIELD.uid);
            expect(emittedField?.label).toBe('Line Height');
        });
    });

    describe('Empty state', () => {
        it('should show the empty message when the section has no fields', () => {
            setup({ ...MOCK_SECTION, fields: [] });

            expect(de.nativeElement.textContent).toContain('No fields yet');
        });

        it('should not show the empty message when the section has fields', () => {
            setup(MOCK_SECTION);

            expect(de.nativeElement.textContent).not.toContain('No fields yet');
        });
    });
});
