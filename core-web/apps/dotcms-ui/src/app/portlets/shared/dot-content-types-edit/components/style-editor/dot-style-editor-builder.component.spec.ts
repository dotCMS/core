import { of, throwError } from 'rxjs';

import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

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
    'style.editor.form.builder.saved.message': 'Style Editor schema saved successfully'
};

const dotMessageServiceMock = {
    get: (key: string, ...args: string[]) => {
        const template = MOCK_MESSAGES[key] ?? key;
        return args.reduce((acc, arg, i) => acc.replace(`{${i}}`, arg), template);
    }
};

const MOCK_CONTENT_TYPE = {
    id: 'content-type-id',
    variable: 'testType',
    name: 'Test Type',
    metadata: {}
} as DotCMSContentType;

describe('DotStyleEditorBuilderComponent', () => {
    let fixture: ComponentFixture<DotStyleEditorBuilderComponent>;
    let comp: DotStyleEditorBuilderComponent;
    let de: DebugElement;
    let crudService: { putData: jest.Mock };

    function setup(contentType?: DotCMSContentType): void {
        fixture = TestBed.createComponent(DotStyleEditorBuilderComponent);
        if (contentType) {
            fixture.componentRef.setInput('contentType', contentType);
        }
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        fixture.detectChanges();
    }

    function clickAddSection(): void {
        de.query(By.css('[data-testid="add-section-btn"] button')).nativeElement.click();
        fixture.detectChanges();
    }

    beforeEach(async () => {
        crudService = { putData: jest.fn().mockReturnValue(of({})) };

        await TestBed.configureTestingModule({
            imports: [DotStyleEditorBuilderComponent],
            providers: [
                { provide: DotCrudService, useValue: crudService },
                { provide: DotHttpErrorManagerService, useValue: { handle: jest.fn() } },
                { provide: DotMessageDisplayService, useValue: { push: jest.fn() } },
                { provide: DotMessageService, useValue: dotMessageServiceMock }
            ]
        }).compileComponents();
    });

    describe('Sections', () => {
        it('should add a section when "Add New Section" is clicked', () => {
            setup();
            expect(comp.$sections().length).toBe(0);

            clickAddSection();

            expect(comp.$sections().length).toBe(1);
        });

        it('should show a confirmation dialog before deleting a section', () => {
            setup();
            clickAddSection();

            de.query(By.css('[data-testid="delete-section-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(comp.$confirmState()?.header).toBe('Delete Section');
        });

        it('should remove the section after confirming the delete', () => {
            setup();
            clickAddSection();
            expect(comp.$sections().length).toBe(1);

            de.query(By.css('[data-testid="delete-section-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            // Dialog actions use dynamic [label] binding, so invoke the callback directly
            comp.$confirmState()
                ?.actions.find((a) => a.label === 'Delete')
                ?.callback();
            fixture.detectChanges();

            expect(comp.$sections().length).toBe(0);
        });

        it('should keep the section when the delete confirmation is cancelled', () => {
            setup();
            clickAddSection();

            de.query(By.css('[data-testid="delete-section-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            // Dialog actions use dynamic [label] binding, so invoke the callback directly
            comp.$confirmState()
                ?.actions.find((a) => a.label === 'Cancel')
                ?.callback();
            fixture.detectChanges();

            expect(comp.$sections().length).toBe(1);
            expect(comp.$confirmState()).toBeNull();
        });

        it('should move a section up when its move-up button is clicked', () => {
            setup();
            clickAddSection();
            clickAddSection();

            const firstTitle = comp.$sections()[0].uid;
            const secondTitle = comp.$sections()[1].uid;

            // Move the second section up
            de.queryAll(
                By.css('[data-testid="move-section-up-btn"] button')
            )[1].nativeElement.click();
            fixture.detectChanges();

            expect(comp.$sections()[0].uid).toBe(secondTitle);
            expect(comp.$sections()[1].uid).toBe(firstTitle);
        });
    });

    describe('Dirty state and Cancel', () => {
        it('should not show the Cancel button when the form is not dirty', () => {
            setup();

            expect(de.query(By.css('[data-testid="cancel-btn"]'))).toBeNull();
        });

        it('should show the Cancel button after a section is added', () => {
            setup();
            clickAddSection();

            expect(de.query(By.css('[data-testid="cancel-btn"]'))).not.toBeNull();
        });

        it('should open an "Unsaved Changes" dialog when Cancel is clicked', () => {
            setup();
            clickAddSection();

            de.query(By.css('[data-testid="cancel-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(comp.$confirmState()?.header).toBe('Unsaved Changes');
        });

        it('should discard all changes when "Leave without saving" is clicked', () => {
            setup();
            clickAddSection();

            de.query(By.css('[data-testid="cancel-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            // Dialog actions use dynamic [label] binding, so invoke the callback directly
            comp.$confirmState()
                ?.actions.find((a) => a.label === 'Leave without saving')
                ?.callback();
            fixture.detectChanges();

            expect(comp.$sections().length).toBe(0);
            expect(comp.$confirmState()).toBeNull();
        });

        it('should close the dialog without discarding when "Cancel" in the dialog is clicked', () => {
            setup();
            clickAddSection();

            de.query(By.css('[data-testid="cancel-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            // Dialog actions use dynamic [label] binding, so invoke the callback directly
            comp.$confirmState()
                ?.actions.find((a) => a.label === 'Cancel')
                ?.callback();
            fixture.detectChanges();

            expect(comp.$sections().length).toBe(1);
            expect(comp.$confirmState()).toBeNull();
        });
    });

    describe('Save', () => {
        it('should mark saveAttempted and not call the API when the form is invalid', () => {
            setup(MOCK_CONTENT_TYPE);
            clickAddSection();

            // The default new section has a field with label:'New Field' and identifier:'newField'
            // but we'll set the section title to empty to ensure at least one validation check
            // Actually the default field IS valid (input type, non-empty label/identifier)
            // To make it invalid: clear the label from the field form in the DOM
            const labelInput = de.query(By.css('input[placeholder="New Field"]'));
            labelInput.nativeElement.value = '';
            labelInput.nativeElement.dispatchEvent(new Event('input'));
            fixture.detectChanges();

            expect(comp.$saveAttempted()).toBe(false);

            de.query(By.css('[data-testid="save-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(comp.$saveAttempted()).toBe(true);
            expect(crudService.putData).not.toHaveBeenCalled();
        });

        it('should call the CRUD API when the form is valid', () => {
            setup(MOCK_CONTENT_TYPE);
            // No sections → empty form is valid (nothing to validate)
            de.query(By.css('[data-testid="save-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(crudService.putData).toHaveBeenCalledWith(
                `v1/contenttype/id/${MOCK_CONTENT_TYPE.id}`,
                expect.anything()
            );
        });

        it('should handle API errors by calling the error manager', () => {
            const httpErrorManager = TestBed.inject(
                DotHttpErrorManagerService
            ) as jest.Mocked<DotHttpErrorManagerService>;
            crudService.putData.mockReturnValue(throwError(() => new Error('Server error')));

            setup(MOCK_CONTENT_TYPE);
            de.query(By.css('[data-testid="save-btn"] button')).nativeElement.click();
            fixture.detectChanges();

            expect(httpErrorManager.handle).toHaveBeenCalled();
        });
    });
});
