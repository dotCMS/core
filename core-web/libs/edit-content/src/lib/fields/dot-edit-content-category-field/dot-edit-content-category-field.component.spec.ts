import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { fakeAsync } from '@angular/core/testing';
import { ControlContainer, FormControl, FormGroup } from '@angular/forms';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotCategoryFieldDialogComponent } from './components/dot-category-field-dialog/dot-category-field-dialog.component';
import { DotEditContentCategoryFieldComponent } from './dot-edit-content-category-field.component';
import {
    CATEGORY_FIELD_CONTENTLET_MOCK,
    CATEGORY_FIELD_MOCK,
    CATEGORY_FIELD_VARIABLE_NAME,
    CATEGORY_HIERARCHY_MOCK,
    CATEGORY_LEVEL_2,
    MOCK_SELECTED_CATEGORIES_OBJECT
} from './mocks/category-field.mocks';
import { DotCategoryFieldKeyValueObj } from './models/dot-category-field.models';
import { CategoriesService } from './services/categories.service';
import { CategoryFieldStore } from './store/content-category-field.store';

import { createFormGroupDirectiveMock } from '../../utils/mocks';

const FAKE_FORM_GROUP = new FormGroup({
    [CATEGORY_FIELD_VARIABLE_NAME]: new FormControl()
});

describe('DotEditContentCategoryFieldComponent', () => {
    let spectator: Spectator<DotEditContentCategoryFieldComponent>;
    let store: InstanceType<typeof CategoryFieldStore>;

    const createComponent = createComponentFactory({
        component: DotEditContentCategoryFieldComponent,
        imports: [MockComponent(DotCategoryFieldDialogComponent)],
        componentViewProviders: [
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock(FAKE_FORM_GROUP)
            },
            mockProvider(CategoriesService)
        ],
        providers: [
            mockProvider(DotMessageService),
            mockProvider(HttpClient),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    describe('Elements', () => {
        describe('With selected', () => {
            beforeEach(() => {
                spectator = createComponent({
                    props: {
                        contentlet: CATEGORY_FIELD_CONTENTLET_MOCK,
                        field: CATEGORY_FIELD_MOCK
                    },
                    providers: [
                        mockProvider(CategoriesService, {
                            getSelectedHierarchy: jest
                                .fn()
                                .mockReturnValue(of(CATEGORY_HIERARCHY_MOCK))
                        })
                    ]
                });
            });

            it('should render a button for selecting categories', () => {
                expect(spectator.query(byTestId('show-dialog-btn'))).not.toBeNull();
            });

            it('should the button be type=button', () => {
                const selectBtn = spectator.query<HTMLButtonElement>(byTestId('show-dialog-btn'));
                expect(selectBtn.type).toBe('button');
            });

            it('should display the category list with chips when there are categories', () => {
                expect(spectator.query(byTestId('category-chip-list'))).not.toBeNull();
            });

            it('should categoryFieldControl has the values loaded on the store', () => {
                const categoryValue = spectator.component.categoryFieldControl.value;

                expect(categoryValue).toEqual(MOCK_SELECTED_CATEGORIES_OBJECT);
            });
        });

        describe('No selected', () => {
            it('should not display the category list with chips when there are no categories', () => {
                spectator = createComponent({
                    props: {
                        contentlet: [] as unknown as DotCMSContentlet,
                        field: CATEGORY_FIELD_MOCK
                    },
                    providers: [
                        mockProvider(CategoriesService, {
                            getSelectedHierarchy: jest.fn().mockReturnValue(of([]))
                        })
                    ]
                });

                spectator.detectChanges();

                expect(spectator.query(byTestId('category-chip-list'))).toBeNull();
            });
        });
    });

    describe('Interactions', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    contentlet: CATEGORY_FIELD_CONTENTLET_MOCK,
                    field: CATEGORY_FIELD_MOCK
                },
                providers: [
                    mockProvider(CategoriesService, {
                        getSelectedHierarchy: jest.fn().mockReturnValue(of(CATEGORY_HIERARCHY_MOCK))
                    })
                ]
            });

            store = spectator.inject(CategoryFieldStore, true);

            spectator.detectChanges();
        });

        it('should invoke `showCategoriesDialog` method when the select button is clicked', () => {
            const selectBtn = spectator.query(byTestId('show-dialog-btn'));
            const showCategoriesDialogSpy = jest.spyOn(spectator.component, 'openCategoriesDialog');
            expect(selectBtn).not.toBeNull();

            spectator.click(selectBtn);

            expect(showCategoriesDialogSpy).toHaveBeenCalled();
        });

        it('should disable the `Select` button after `openCategoriesDialog` method is invoked', () => {
            const selectBtn = spectator.query(byTestId('show-dialog-btn')) as HTMLButtonElement;
            expect(selectBtn).not.toBeNull();

            spectator.click(selectBtn);

            spectator.detectChanges();

            expect(selectBtn.disabled).toBe(true);
        });

        it('should create a DotCategoryFieldDialogComponent instance when the `Select` button is clicked', async () => {
            const selectBtn = spectator.query<HTMLButtonElement>(byTestId('show-dialog-btn'));
            expect(selectBtn).not.toBeNull();

            expect(spectator.query(DotCategoryFieldDialogComponent)).toBeNull();

            spectator.click(selectBtn);
            await spectator.fixture.whenStable();

            expect(spectator.query(DotCategoryFieldDialogComponent)).not.toBeNull();
        });

        it('should remove DotCategoryFieldDialogComponent when `closedDialog` emit', fakeAsync(async () => {
            const selectBtn = spectator.query(byTestId('show-dialog-btn')) as HTMLButtonElement;

            expect(selectBtn).not.toBeNull();
            spectator.click(selectBtn);
            await spectator.fixture.whenStable();

            const dialogComponentRef = spectator.query(DotCategoryFieldDialogComponent);
            expect(dialogComponentRef).not.toBeNull();

            dialogComponentRef.closedDialog.emit();

            spectator.detectComponentChanges();

            // Check if the dialog component is removed
            expect(spectator.query(DotCategoryFieldDialogComponent)).toBeNull();

            // Check if the button is enabled again
            expect(selectBtn.disabled).toBe(false);

            // Check if the form has the correct value
            const categoryValue = spectator.component.categoryFieldControl.value;

            expect(categoryValue).toEqual(MOCK_SELECTED_CATEGORIES_OBJECT);
        }));

        it('should set categoryFieldControl value when adding a new category', () => {
            const newItem: DotCategoryFieldKeyValueObj = {
                key: CATEGORY_LEVEL_2[0].key,
                value: CATEGORY_LEVEL_2[0].categoryName,
                inode: CATEGORY_LEVEL_2[0].inode,
                path: CATEGORY_LEVEL_2[0].categoryName
            };

            // this apply selected to the dialog
            store.openDialog();

            // Add the new category
            store.addSelected(newItem);
            // Apply the selected to the field
            store.applyDialogSelection();

            spectator.detectChanges();

            const categoryValues = spectator.component.categoryFieldControl.value;

            expect(categoryValues).toEqual([...MOCK_SELECTED_CATEGORIES_OBJECT, newItem]);
        });

        it('should set categoryFieldControl value when removing a category', () => {
            const categoryValue: DotCategoryFieldKeyValueObj[] =
                spectator.component.categoryFieldControl.value;

            const expectedSelected = [categoryValue[0]];

            expect(categoryValue.length).toBe(2);
            store.openDialog(); // this apply selected to the dialog

            store.removeSelected(categoryValue[1].key);
            store.applyDialogSelection(); // this apply the selected in the dialog to the final selected

            spectator.detectChanges();

            const newCategoryValue = spectator.component.categoryFieldControl.value;

            expect(newCategoryValue).toEqual(expectedSelected);
            expect(newCategoryValue.length).toBe(1);
        });
    });
});
