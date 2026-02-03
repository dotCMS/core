import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ComponentFixture } from '@angular/core/testing';

import { DotCMSClazzes, DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    ContentletEditData,
    DotUveContentletQuickEditComponent
} from './dot-uve-contentlet-quick-edit.component';

describe('DotUveContentletQuickEditComponent', () => {
    let spectator: Spectator<DotUveContentletQuickEditComponent>;
    let fixture: ComponentFixture<DotUveContentletQuickEditComponent>;

    const createComponent = createComponentFactory({
        component: DotUveContentletQuickEditComponent
    });

    const mockContentletEditData: ContentletEditData = {
        container: {
            identifier: 'container-123',
            uuid: 'uuid-123',
            acceptTypes: 'test',
            maxContentlets: 1,
            variantId: 'DEFAULT'
        },
        contentlet: {
            identifier: 'contentlet-123',
            inode: 'inode-123',
            title: 'Test Contentlet',
            contentType: 'TestType',
            baseType: 'CONTENT',
            archived: false,
            folder: 'folder-123',
            hasTitleImage: false,
            host: 'host-123',
            locked: false,
            modDate: '2024-01-01',
            sortOrder: 0,
            stInode: 'stInode-123',
            titleField: 'Test Title',
            hostName: 'demo.dotcms.com',
            languageId: 1,
            live: true,
            modUser: 'admin',
            working: true,
            owner: 'admin',
            modUserName: 'Admin User',
            titleImage: 'test',
            url: '/test-contentlet'
        } as DotCMSContentlet,
        fields: [
            {
                name: 'Test Field',
                variable: 'testField',
                clazz: DotCMSClazzes.TEXT,
                required: true,
                readOnly: false,
                dataType: 'TEXT'
            }
        ]
    };

    beforeEach(() => {
        spectator = createComponent({
            props: {
                data: mockContentletEditData,
                loading: false
            }
        });
        fixture = spectator.fixture;
        spectator.detectChanges(); // Trigger effect to build form
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should build form when data is provided', () => {
        spectator.detectChanges(); // Ensure form is built and rendered
        const formElement = spectator.query('form');
        expect(formElement).toBeTruthy();

        const testFieldInput = spectator.query('input[formcontrolname="testField"]') || spectator.query('#testField');
        expect(testFieldInput).toBeTruthy();
    });

    it('should display form fields', () => {
        const label = spectator.query('label');
        expect(label).toHaveText('Test Field');
    });

    it('should emit submit event when form is valid and submitted', () => {
        spectator.detectChanges(); // Ensure form is built and rendered
        let emittedData: Record<string, unknown> | undefined;
        spectator.component.submit.subscribe((data) => (emittedData = data));

        const input = (spectator.query('input[formcontrolname="testField"]') || spectator.query('#testField')) as HTMLInputElement;
        expect(input).toBeTruthy();
        spectator.typeInElement('test value', input);
        spectator.detectChanges();

        spectator.click('button[type="submit"]');
        spectator.detectChanges();

        expect(emittedData).toBeDefined();
        expect(emittedData?.['testField']).toBe('test value');
    });

    it('should emit cancel event when cancel button is clicked', () => {
        let cancelEmitted = false;
        spectator.component.cancel.subscribe(() => (cancelEmitted = true));

        const cancelButton = spectator.query('button[type="button"]');
        expect(cancelButton).toBeTruthy();

        if (cancelButton) {
            spectator.click(cancelButton);
        }

        expect(cancelEmitted).toBe(true);
    });

    it('should disable buttons when loading', () => {
        fixture.componentRef.setInput('loading', true);
        spectator.detectChanges();

        const cancelButton = spectator.query('button[type="button"]') as HTMLButtonElement;
        const submitButton = spectator.query('button[type="submit"]') as HTMLButtonElement;

        expect(cancelButton.disabled).toBe(true);
        expect(submitButton.disabled).toBe(true);
    });

    it('should display empty state when no fields', () => {
        fixture.componentRef.setInput('data', {
            ...mockContentletEditData,
            fields: []
        });
        spectator.detectChanges();

        expect(spectator.query('.empty-state')).toExist();
        expect(spectator.query('.empty-message')).toHaveText('Select a contentlet');
    });

    it('should mark required fields with CSS class', () => {
        const label = spectator.query('label');
        expect(label).toHaveClass('p-label-input-required');
    });

    it('should emit cancel event on Escape key', () => {
        let cancelEmitted = false;
        spectator.component.cancel.subscribe(() => (cancelEmitted = true));

        const form = spectator.query('form');
        expect(form).toBeTruthy();

        if (form) {
            form.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
            spectator.detectChanges();
        }

        expect(cancelEmitted).toBe(true);
    });

    it('should include inode in form if contentlet has inode', () => {
        const inodeInput = spectator.query('input[formcontrolname="inode"]');
        expect(inodeInput).toBeTruthy();
        expect((inodeInput as HTMLInputElement).value).toBe('inode-123');
    });

    it('should not submit form when invalid', () => {
        let emittedData: Record<string, unknown> | undefined;
        spectator.component.submit.subscribe((data) => (emittedData = data));

        const input = spectator.query('input[formcontrolname="testField"]') as HTMLInputElement;
        spectator.typeInElement('', input); // Clear required field to make form invalid

        const submitButton = spectator.query('button[type="submit"]') as HTMLButtonElement;
        expect(submitButton.disabled).toBe(true); // Should be disabled when form is invalid

        expect(emittedData).toBeUndefined();
    });
});
