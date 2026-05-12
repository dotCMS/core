import {
    Spectator,
    SpyObject,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import {
    DotCrudService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService
} from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

import { DotStyleEditorBuilderComponent } from './dot-style-editor-builder.component';

const MOCK_MESSAGES: Record<string, string> = {
    'style.editor.form.builder.dialog.unsaved.header': 'Unsaved Changes',
    'style.editor.form.builder.dialog.delete.section.header': 'Delete Section',
    'style.editor.form.builder.dialog.delete.field.header': 'Delete Field',
    'style.editor.form.builder.dialog.cancel': 'Cancel',
    'style.editor.form.builder.dialog.delete': 'Delete',
    'style.editor.form.builder.dialog.leave': 'Leave without saving',
    'style.editor.form.builder.dialog.save.close': 'Save and Close',
    'style.editor.form.builder.section.default.title': 'New Section',
    'style.editor.form.builder.field.new': 'New Field',
    'style.editor.form.builder.field.identifier.placeholder': 'fieldId',
    'style.editor.form.builder.saved.message': 'Style Editor schema saved successfully'
};

const MOCK_CONTENT_TYPE = {
    id: 'content-type-id',
    variable: 'testType',
    name: 'Test Type',
    metadata: {}
} as DotCMSContentType;

describe('DotStyleEditorBuilderComponent', () => {
    let spectator: Spectator<DotStyleEditorBuilderComponent>;

    const createComponent = createComponentFactory({
        component: DotStyleEditorBuilderComponent,
        providers: [
            mockProvider(DotCrudService, { putData: jest.fn().mockReturnValue(of({})) }),
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() }),
            mockProvider(DotMessageDisplayService, { push: jest.fn() }),
            {
                provide: DotMessageService,
                useValue: {
                    get: (key: string, ...args: string[]) => {
                        const template = MOCK_MESSAGES[key] ?? key;

                        return args.reduce((acc, arg, i) => acc.replace(`{${i}}`, arg), template);
                    }
                }
            }
        ]
    });

    function setup(contentType?: DotCMSContentType): void {
        spectator = createComponent();
        if (contentType) {
            spectator.setInput('contentType', contentType);
        }
    }

    function clickAddSection(): void {
        spectator.query(byTestId('add-section-btn'))?.querySelector('button')?.click();
        spectator.detectChanges();
    }

    function clickAddField(): void {
        spectator.query(byTestId('add-field-btn'))?.querySelector('button')?.click();
        spectator.detectChanges();
    }

    function typeInIdentifierInput(index: number, value: string): void {
        const inputs = spectator.queryAll('input[placeholder="fieldId"]') as HTMLInputElement[];
        const input = inputs[index];
        input.value = value;
        input.dispatchEvent(new Event('input'));
        spectator.detectChanges();
    }

    function typeInLabelInput(index: number, value: string): void {
        const inputs = spectator.queryAll('input[placeholder="New Field"]') as HTMLInputElement[];
        const input = inputs[index];
        input.value = value;
        input.dispatchEvent(new Event('input'));
        spectator.detectChanges();
    }

    describe('Sections', () => {
        it('should add a section when "Add New Section" is clicked', () => {
            setup();
            expect(spectator.component.$sections().length).toBe(0);

            clickAddSection();

            expect(spectator.component.$sections().length).toBe(1);
        });

        it('should show a confirmation dialog before deleting a section', () => {
            setup();
            clickAddSection();

            spectator.query(byTestId('delete-section-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.$confirmState()?.header).toBe('Delete Section');
        });

        it('should remove the section after confirming the delete', () => {
            setup();
            clickAddSection();
            expect(spectator.component.$sections().length).toBe(1);

            spectator.query(byTestId('delete-section-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            // Dialog actions use dynamic [label] binding, so invoke the callback directly
            spectator.component
                .$confirmState()
                ?.actions.find((a) => a.label === 'Delete')
                ?.callback();
            spectator.detectChanges();

            expect(spectator.component.$sections().length).toBe(0);
        });

        it('should keep the section when the delete confirmation is cancelled', () => {
            setup();
            clickAddSection();

            spectator.query(byTestId('delete-section-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            // Dialog actions use dynamic [label] binding, so invoke the callback directly
            spectator.component
                .$confirmState()
                ?.actions.find((a) => a.label === 'Cancel')
                ?.callback();
            spectator.detectChanges();

            expect(spectator.component.$sections().length).toBe(1);
            expect(spectator.component.$confirmState()).toBeNull();
        });

        it('should move a section up when its move-up button is clicked', () => {
            setup();
            clickAddSection();
            clickAddSection();

            const firstUid = spectator.component.$sections()[0].uid;
            const secondUid = spectator.component.$sections()[1].uid;

            // Move the second section up
            spectator
                .queryAll(byTestId('move-section-up-btn'))[1]
                ?.querySelector('button')
                ?.click();
            spectator.detectChanges();

            expect(spectator.component.$sections()[0].uid).toBe(secondUid);
            expect(spectator.component.$sections()[1].uid).toBe(firstUid);
        });
    });

    describe('Dirty state and Cancel', () => {
        it('should not show the Cancel button when the form is not dirty', () => {
            setup();

            expect(spectator.query(byTestId('cancel-btn'))).toBeNull();
        });

        it('should show the Cancel button after a section is added', () => {
            setup();
            clickAddSection();

            expect(spectator.query(byTestId('cancel-btn'))).not.toBeNull();
        });

        it('should open an "Unsaved Changes" dialog when Cancel is clicked', () => {
            setup();
            clickAddSection();

            spectator.query(byTestId('cancel-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.$confirmState()?.header).toBe('Unsaved Changes');
        });

        it('should discard all changes when "Leave without saving" is clicked', () => {
            setup();
            clickAddSection();

            spectator.query(byTestId('cancel-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            // Dialog actions use dynamic [label] binding, so invoke the callback directly
            spectator.component
                .$confirmState()
                ?.actions.find((a) => a.label === 'Leave without saving')
                ?.callback();
            spectator.detectChanges();

            expect(spectator.component.$sections().length).toBe(0);
            expect(spectator.component.$confirmState()).toBeNull();
        });

        it('should close the dialog without discarding when "Cancel" in the dialog is clicked', () => {
            setup();
            clickAddSection();

            spectator.query(byTestId('cancel-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            // Dialog actions use dynamic [label] binding, so invoke the callback directly
            spectator.component
                .$confirmState()
                ?.actions.find((a) => a.label === 'Cancel')
                ?.callback();
            spectator.detectChanges();

            expect(spectator.component.$sections().length).toBe(1);
            expect(spectator.component.$confirmState()).toBeNull();
        });
    });

    describe('Save', () => {
        it('should mark saveAttempted and not call the API when the form is invalid', () => {
            setup(MOCK_CONTENT_TYPE);
            clickAddSection();

            // Clear the label of the new field to make it invalid
            const labelInput = spectator.query(
                'input[placeholder="New Field"]'
            ) as HTMLInputElement;
            labelInput.value = '';
            labelInput.dispatchEvent(new Event('input'));
            spectator.detectChanges();

            expect(spectator.component.$saveAttempted()).toBe(false);

            spectator.query(byTestId('save-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.$saveAttempted()).toBe(true);
            expect(spectator.inject(DotCrudService).putData).not.toHaveBeenCalled();
        });

        it('should call the CRUD API when the form is valid', () => {
            setup(MOCK_CONTENT_TYPE);
            // No sections → empty form is valid (nothing to validate)
            spectator.query(byTestId('save-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.inject(DotCrudService).putData).toHaveBeenCalledWith(
                `v1/contenttype/id/${MOCK_CONTENT_TYPE.id}`,
                expect.anything()
            );
        });

        it('should handle API errors by calling the error manager', () => {
            setup(MOCK_CONTENT_TYPE);

            const crudService: SpyObject<DotCrudService> = spectator.inject(DotCrudService);
            crudService.putData.mockReturnValue(throwError(() => new Error('Server error')));

            spectator.query(byTestId('save-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
        });
    });

    describe('Duplicate identifier validation', () => {
        beforeEach(() => {
            jest.clearAllMocks();
        });

        it('should detect a duplicate when two fields in the same section share an identifier', () => {
            setup(MOCK_CONTENT_TYPE);

            clickAddSection();
            clickAddField();
            typeInLabelInput(0, 'Field A');
            typeInLabelInput(1, 'Field B');
            typeInIdentifierInput(0, 'sharedId');
            typeInIdentifierInput(1, 'sharedId');

            expect(spectator.component.$duplicateIdentifiers().has('sharedId')).toBe(true);
        });

        it('should detect a duplicate when fields in different sections share an identifier', () => {
            setup(MOCK_CONTENT_TYPE);

            clickAddSection();
            clickAddSection();
            typeInLabelInput(0, 'Field A');
            typeInLabelInput(1, 'Field B');
            typeInIdentifierInput(0, 'sharedId');
            typeInIdentifierInput(1, 'sharedId');

            expect(spectator.component.$duplicateIdentifiers().has('sharedId')).toBe(true);
        });

        it('should block the API call and mark the save as attempted when duplicate identifiers exist', () => {
            setup(MOCK_CONTENT_TYPE);
            clickAddSection();
            clickAddField();
            typeInLabelInput(0, 'Field A');
            typeInLabelInput(1, 'Field B');
            typeInIdentifierInput(0, 'sharedId');
            typeInIdentifierInput(1, 'sharedId');

            spectator.query(byTestId('save-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.component.$saveAttempted()).toBe(true);
            expect(spectator.inject(DotCrudService).putData).not.toHaveBeenCalled();
        });

        it('should call the API after the user renames one of the duplicate identifiers to make it unique', () => {
            setup(MOCK_CONTENT_TYPE);
            clickAddSection();
            clickAddField();
            typeInLabelInput(0, 'Field A');
            typeInLabelInput(1, 'Field B');
            typeInIdentifierInput(0, 'sharedId');
            typeInIdentifierInput(1, 'sharedId');

            // Both fields share 'sharedId'. Rename the first one to resolve the clash.
            typeInIdentifierInput(0, 'uniqueId');

            spectator.query(byTestId('save-btn'))?.querySelector('button')?.click();
            spectator.detectChanges();

            expect(spectator.inject(DotCrudService).putData).toHaveBeenCalled();
        });
    });
});
