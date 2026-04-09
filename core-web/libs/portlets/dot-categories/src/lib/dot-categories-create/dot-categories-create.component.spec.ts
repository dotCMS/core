import { createComponentFactory, Spectator, byTestId } from '@ngneat/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotCategory } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotCategoriesCreateComponent } from './dot-categories-create.component';

const MOCK_CATEGORY: DotCategory = {
    categoryName: 'Existing Category',
    key: 'existing-key',
    categoryVelocityVarName: 'existingVar',
    sortOrder: 5,
    active: true,
    inode: 'cat-inode-1',
    identifier: 'cat-id-1',
    type: 'Category',
    childrenCount: 0,
    description: 'A test description',
    keywords: 'test,keywords',
    iDate: Date.now(),
    owner: 'system'
} as DotCategory;

describe('DotCategoriesCreateComponent', () => {
    describe('create mode', () => {
        let spectator: Spectator<DotCategoriesCreateComponent>;
        const mockRef = { close: jest.fn() };

        const createComponent = createComponentFactory({
            component: DotCategoriesCreateComponent,
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
            providers: [
                { provide: DynamicDialogRef, useValue: mockRef },
                { provide: DynamicDialogConfig, useValue: { data: {} } },
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({})
                }
            ]
        });

        beforeEach(() => {
            mockRef.close.mockClear();
            spectator = createComponent();
        });

        it('should have an empty form initially', () => {
            expect(spectator.component.form.getRawValue()).toEqual({
                categoryName: '',
                categoryVelocityVarName: '',
                key: '',
                keywords: ''
            });
        });

        it('should have isEdit as false', () => {
            expect(spectator.component.isEdit).toBe(false);
        });

        it('should not show parent hint when parentName is null', () => {
            const hint = spectator.query(byTestId('category-parent-hint'));
            expect(hint).toBeFalsy();
        });

        it('should not close dialog when form is invalid', () => {
            spectator.component.onSubmit();
            expect(mockRef.close).not.toHaveBeenCalled();
        });

        it('should close dialog with form value when valid', () => {
            spectator.component.form.patchValue({
                categoryName: 'New Category',
                key: 'new-cat'
            });
            spectator.component.onSubmit();
            expect(mockRef.close).toHaveBeenCalledWith(
                expect.objectContaining({
                    categoryName: 'New Category',
                    key: 'new-cat'
                })
            );
        });

        it('should show Save label on submit button', () => {
            const button = spectator.query(byTestId('category-save-btn'));
            expect(button).toBeTruthy();
            expect((button as HTMLElement).outerHTML).toContain('categories.save');
        });
    });

    describe('create mode with parent', () => {
        let spectator: Spectator<DotCategoriesCreateComponent>;
        const mockRef = { close: jest.fn() };

        const createComponent = createComponentFactory({
            component: DotCategoriesCreateComponent,
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
            providers: [
                { provide: DynamicDialogRef, useValue: mockRef },
                {
                    provide: DynamicDialogConfig,
                    useValue: { data: { parentName: 'Parent Category' } }
                },
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({})
                }
            ]
        });

        beforeEach(() => {
            mockRef.close.mockClear();
            spectator = createComponent();
        });

        it('should set parentName from config', () => {
            expect(spectator.component.parentName).toBe('Parent Category');
        });

        it('should show parent hint message', () => {
            const hint = spectator.query(byTestId('category-parent-hint'));
            expect(hint).toBeTruthy();
        });
    });

    describe('edit mode', () => {
        let spectator: Spectator<DotCategoriesCreateComponent>;
        const mockRef = { close: jest.fn() };

        const createComponent = createComponentFactory({
            component: DotCategoriesCreateComponent,
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
            providers: [
                { provide: DynamicDialogRef, useValue: mockRef },
                {
                    provide: DynamicDialogConfig,
                    useValue: { data: { category: MOCK_CATEGORY } }
                },
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({})
                }
            ]
        });

        beforeEach(() => {
            mockRef.close.mockClear();
            spectator = createComponent();
        });

        it('should patch form with category data', () => {
            expect(spectator.component.form.getRawValue()).toEqual({
                categoryName: 'Existing Category',
                categoryVelocityVarName: 'existingVar',
                key: 'existing-key',
                keywords: 'test,keywords'
            });
        });

        it('should set isEdit to true', () => {
            expect(spectator.component.isEdit).toBe(true);
        });

        it('should show Update label on submit button', () => {
            const button = spectator.query(byTestId('category-save-btn'));
            expect(button).toBeTruthy();
            expect((button as HTMLElement).outerHTML).toContain('categories.update');
        });
    });
});
