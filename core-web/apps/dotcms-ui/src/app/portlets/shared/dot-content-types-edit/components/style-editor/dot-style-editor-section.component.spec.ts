import { Spectator, byTestId, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

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
    let spectator: Spectator<DotStyleEditorSectionComponent>;

    const createComponent = createComponentFactory({
        component: DotStyleEditorSectionComponent,
        providers: [
            mockProvider(DotMessageService, {
                get: (key: string, ...args: string[]) => {
                    const template = MOCK_MESSAGES[key] ?? key;

                    return args.reduce((acc, arg, i) => acc.replace(`{${i}}`, arg), template);
                }
            })
        ]
    });

    function setup(section: BuilderSection = MOCK_SECTION, isFirst = false, isLast = false): void {
        spectator = createComponent({
            props: { section, isFirst, isLast, showErrors: false } as unknown
        });
    }

    describe('Section header actions', () => {
        it('should emit delete when the trash button is clicked', () => {
            setup();
            jest.spyOn(spectator.component.delete, 'emit');

            spectator.query(byTestId('delete-section-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.delete.emit).toHaveBeenCalled();
        });

        it('should emit moveUp when the move-up button is clicked', () => {
            setup(MOCK_SECTION, false, false);
            jest.spyOn(spectator.component.moveUp, 'emit');

            spectator.query(byTestId('move-section-up-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.moveUp.emit).toHaveBeenCalled();
        });

        it('should emit moveDown when the move-down button is clicked', () => {
            setup(MOCK_SECTION, false, false);
            jest.spyOn(spectator.component.moveDown, 'emit');

            spectator.query(byTestId('move-section-down-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.moveDown.emit).toHaveBeenCalled();
        });

        it('should disable move-up button when section is first', () => {
            setup(MOCK_SECTION, true, false);

            const btn = spectator.query(byTestId('move-section-up-btn'))?.querySelector('button');
            expect(btn?.disabled).toBe(true);
        });

        it('should disable move-down button when section is last', () => {
            setup(MOCK_SECTION, false, true);

            const btn = spectator.query(byTestId('move-section-down-btn'))?.querySelector('button');
            expect(btn?.disabled).toBe(true);
        });
    });

    describe('Title editing', () => {
        it('should emit titleChange when the title input is changed', () => {
            setup();
            jest.spyOn(spectator.component.titleChange, 'emit');

            const titleInput = spectator.query(
                'input[placeholder="Section Title"]'
            ) as HTMLInputElement;
            titleInput.value = 'Colors';
            titleInput.dispatchEvent(new Event('input'));
            spectator.detectChanges();

            expect(spectator.component.titleChange.emit).toHaveBeenCalledWith('Colors');
        });
    });

    describe('Field interactions', () => {
        it('should emit addField when the "Add Field" button is clicked', () => {
            setup();
            jest.spyOn(spectator.component.addField, 'emit');

            spectator.query(byTestId('add-field-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.addField.emit).toHaveBeenCalled();
        });

        it('should emit removeField with the field uid when field delete is triggered', () => {
            setup();
            jest.spyOn(spectator.component.removeField, 'emit');

            spectator.query(byTestId('delete-field-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.removeField.emit).toHaveBeenCalledWith(MOCK_FIELD.uid);
        });

        it('should emit moveFieldUp with the field uid when field move-up is triggered', () => {
            // MOCK_FIELD is at index 1 (not first) so its Move-Up button is enabled
            setup(MOCK_SECTION_TWO_FIELDS);
            jest.spyOn(spectator.component.moveFieldUp, 'emit');

            // Second field's move-up button (index 1 → enabled)
            spectator.queryAll(byTestId('move-up-btn'))[1]?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.moveFieldUp.emit).toHaveBeenCalledWith(MOCK_FIELD.uid);
        });

        it('should emit moveFieldDown with the field uid when field move-down is triggered', () => {
            // MOCK_FIELD_2 is at index 0 (not last) so its Move-Down button is enabled
            setup(MOCK_SECTION_TWO_FIELDS);
            jest.spyOn(spectator.component.moveFieldDown, 'emit');

            // First field's move-down button (index 0 → enabled)
            spectator.queryAll(byTestId('move-down-btn'))[0]?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.moveFieldDown.emit).toHaveBeenCalledWith(MOCK_FIELD_2.uid);
        });

        it('should emit fieldChange when a field label is updated', () => {
            setup();
            jest.spyOn(spectator.component.fieldChange, 'emit');

            const labelInput = spectator.query(
                'input[placeholder="New Field"]'
            ) as HTMLInputElement;
            labelInput.value = 'Line Height';
            labelInput.dispatchEvent(new Event('input'));
            spectator.detectChanges();

            expect(spectator.component.fieldChange.emit).toHaveBeenCalledWith(
                expect.objectContaining({ uid: MOCK_FIELD.uid, label: 'Line Height' })
            );
        });
    });

    describe('Empty state', () => {
        it('should show the empty message when the section has no fields', () => {
            setup({ ...MOCK_SECTION, fields: [] });

            expect(spectator.element.textContent).toContain('No fields yet');
        });

        it('should not show the empty message when the section has fields', () => {
            setup(MOCK_SECTION);

            expect(spectator.element.textContent).not.toContain('No fields yet');
        });
    });
});
