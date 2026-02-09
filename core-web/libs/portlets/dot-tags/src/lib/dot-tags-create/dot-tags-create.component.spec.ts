import { createComponentFactory, Spectator, byTestId } from '@ngneat/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotTag } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-site',
    standalone: true,
    template: '',
    providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: MockDotSiteComponent, multi: true }]
})
class MockDotSiteComponent implements ControlValueAccessor {
    writeValue(): void {
        /* noop */
    }
    registerOnChange(): void {
        /* noop */
    }
    registerOnTouched(): void {
        /* noop */
    }
}

jest.mock('@dotcms/ui', () => ({
    DotSiteComponent: MockDotSiteComponent
}));

import { DotTagsCreateComponent } from './dot-tags-create.component';

const MOCK_TAG: DotTag = {
    id: 'tag-1',
    label: 'existing-tag',
    siteId: 'site-1',
    siteName: 'Site 1',
    persona: false
};

describe('DotTagsCreateComponent', () => {
    describe('create mode', () => {
        let spectator: Spectator<DotTagsCreateComponent>;
        const mockRef = { close: jest.fn() };

        const createComponent = createComponentFactory({
            component: DotTagsCreateComponent,
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
            providers: [
                { provide: DynamicDialogRef, useValue: mockRef },
                { provide: DynamicDialogConfig, useValue: { data: {} } }
            ]
        });

        beforeEach(() => {
            mockRef.close.mockClear();
            spectator = createComponent();
        });

        it('should have an empty form initially', () => {
            expect(spectator.component.form.value).toEqual({ name: '', siteId: '' });
        });

        it('should have isEdit as false', () => {
            expect(spectator.component.isEdit).toBe(false);
        });

        it('should not close dialog when form is invalid', () => {
            spectator.component.onSubmit();
            expect(mockRef.close).not.toHaveBeenCalled();
        });

        it('should close dialog with form value when valid', () => {
            spectator.component.form.patchValue({ name: 'new-tag', siteId: 'site-1' });
            spectator.component.onSubmit();
            expect(mockRef.close).toHaveBeenCalledWith({ name: 'new-tag', siteId: 'site-1' });
        });

        it('should show Save label on submit button', () => {
            const button = spectator.query(byTestId('tag-save-btn'));
            expect(button).toBeTruthy();
            expect((button as HTMLElement).outerHTML).toContain('Save');
        });
    });

    describe('edit mode', () => {
        let spectator: Spectator<DotTagsCreateComponent>;
        const mockRef = { close: jest.fn() };

        const createComponent = createComponentFactory({
            component: DotTagsCreateComponent,
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
            providers: [
                { provide: DynamicDialogRef, useValue: mockRef },
                { provide: DynamicDialogConfig, useValue: { data: { tag: MOCK_TAG } } }
            ]
        });

        beforeEach(() => {
            mockRef.close.mockClear();
            spectator = createComponent();
        });

        it('should patch form with tag data', () => {
            expect(spectator.component.form.value).toEqual({
                name: 'existing-tag',
                siteId: 'site-1'
            });
        });

        it('should set isEdit to true', () => {
            expect(spectator.component.isEdit).toBe(true);
        });

        it('should show Update label on submit button', () => {
            const button = spectator.query(byTestId('tag-save-btn'));
            expect(button).toBeTruthy();
            expect((button as HTMLElement).outerHTML).toContain('Update');
        });
    });
});
