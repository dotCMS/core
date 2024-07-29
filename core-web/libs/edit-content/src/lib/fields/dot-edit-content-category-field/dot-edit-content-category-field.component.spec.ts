import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { fakeAsync } from '@angular/core/testing';
import { ControlContainer, FormControl, FormGroup } from '@angular/forms';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotCategoryFieldSidebarComponent } from './components/dot-category-field-sidebar/dot-category-field-sidebar.component';
import { DotEditContentCategoryFieldComponent } from './dot-edit-content-category-field.component';
import {
    CATEGORY_FIELD_CONTENTLET_MOCK,
    CATEGORY_FIELD_MOCK,
    CATEGORY_FIELD_VARIABLE_NAME,
    CATEGORY_HIERARCHY_MOCK,
    SELECTED_LIST_MOCK
} from './mocks/category-field.mocks';
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
        imports: [MockComponent(DotCategoryFieldSidebarComponent)],
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
                expect(spectator.query(byTestId('show-sidebar-btn'))).not.toBeNull();
            });

            it('should the button be type=button', () => {
                const selectBtn = spectator.query<HTMLButtonElement>(byTestId('show-sidebar-btn'));
                expect(selectBtn.type).toBe('button');
            });

            it('should display the category list with chips when there are categories', () => {
                expect(spectator.query(byTestId('category-chip-list'))).not.toBeNull();
            });

            it('should categoryFieldControl has the values loaded on the store', () => {
                const categoryValue = spectator.component.categoryFieldControl.value;

                expect(categoryValue).toEqual(SELECTED_LIST_MOCK);
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

        it('should invoke `showCategoriesSidebar` method when the select button is clicked', () => {
            const selectBtn = spectator.query(byTestId('show-sidebar-btn'));
            const showCategoriesSidebarSpy = jest.spyOn(
                spectator.component,
                'openCategoriesSidebar'
            );
            expect(selectBtn).not.toBeNull();

            spectator.click(selectBtn);

            expect(showCategoriesSidebarSpy).toHaveBeenCalled();
        });

        it('should disable the `Select` button after `openCategoriesSidebar` method is invoked', () => {
            const selectBtn = spectator.query(byTestId('show-sidebar-btn')) as HTMLButtonElement;
            expect(selectBtn).not.toBeNull();

            spectator.click(selectBtn);

            spectator.detectChanges();

            expect(selectBtn.disabled).toBe(true);
        });

        it('should create a DotEditContentCategoryFieldSidebarComponent instance when the `Select` button is clicked', async () => {
            const selectBtn = spectator.query<HTMLButtonElement>(byTestId('show-sidebar-btn'));
            expect(selectBtn).not.toBeNull();

            expect(spectator.query(DotCategoryFieldSidebarComponent)).toBeNull();

            spectator.click(selectBtn);
            await spectator.fixture.whenStable();

            expect(spectator.query(DotCategoryFieldSidebarComponent)).not.toBeNull();
        });

        it('should remove DotEditContentCategoryFieldSidebarComponent when `closedSidebar` emit', fakeAsync(async () => {
            const selectBtn = spectator.query(byTestId('show-sidebar-btn')) as HTMLButtonElement;

            expect(selectBtn).not.toBeNull();
            spectator.click(selectBtn);
            await spectator.fixture.whenStable();

            const sidebarComponentRef = spectator.query(DotCategoryFieldSidebarComponent);
            expect(sidebarComponentRef).not.toBeNull();

            sidebarComponentRef.closedSidebar.emit();

            spectator.detectComponentChanges();

            // Check if the sidebar component is removed
            expect(spectator.query(DotCategoryFieldSidebarComponent)).toBeNull();

            // Check if the button is enabled again
            expect(selectBtn.disabled).toBe(false);

            // Check if the form has the correct value
            const categoryValue = spectator.component.categoryFieldControl.value;

            expect(categoryValue).toEqual(SELECTED_LIST_MOCK);
        }));

        it('should set categoryFieldControl value when adding a new category', () => {
            store.addSelected({
                key: '1234',
                value: 'test'
            });
            spectator.flushEffects();

            const categoryValue = spectator.component.categoryFieldControl.value;

            expect(categoryValue).toEqual([...SELECTED_LIST_MOCK, '1234']);
        });

        it('should set categoryFieldControl value when removing a category', () => {
            store.removeSelected(SELECTED_LIST_MOCK[0]);

            spectator.flushEffects();

            const categoryValue = spectator.component.categoryFieldControl.value;

            expect(categoryValue).toEqual([SELECTED_LIST_MOCK[1]]);
        });
    });
});
