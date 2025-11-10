import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Component, forwardRef } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import {
    ControlValueAccessor,
    FormControl,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { DropdownModule } from 'primeng/dropdown';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService, mockLocales } from '@dotcms/utils-testing';

import { LanguageFieldComponent } from './components/language-field/language-field.component';
import { SiteFieldComponent } from './components/site-field/site-field.component';
import { SearchComponent, DEBOUNCE_TIME } from './search.component';

import { TreeNodeItem } from '../../../../../../models/dot-edit-content-host-folder-field.interface';
import { DotEditContentService } from '../../../../../../services/dot-edit-content.service';
import { SearchParams } from '../../../../models/search.model';

// Mock components for testing
@Component({
    selector: 'dot-language-field',
    template: '<input [formControlName]="null" />',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MockLanguageFieldComponent),
            multi: true
        }
    ]
})
class MockLanguageFieldComponent implements ControlValueAccessor {
    languageControl = new FormControl({ isoCode: 'en-US', id: 1 });

    // eslint-disable-next-line @typescript-eslint/no-empty-function
    writeValue(): void {}
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    registerOnChange(): void {}
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    registerOnTouched(): void {}
}

@Component({
    selector: 'dot-site-field',
    template: '<input [formControlName]="null" />',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MockSiteFieldComponent),
            multi: true
        }
    ]
})
class MockSiteFieldComponent implements ControlValueAccessor {
    siteControl = new FormControl({
        label: 'demo.dotcms.com',
        data: { id: 'site123', type: 'site' }
    });

    // eslint-disable-next-line @typescript-eslint/no-empty-function
    writeValue(): void {}
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    registerOnChange(): void {}
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    registerOnTouched(): void {}
}

describe('SearchComponent', () => {
    let spectator: Spectator<SearchComponent>;
    let component: SearchComponent;

    const messageServiceMock = new MockDotMessageService({
        'dot.file.relationship.dialog.search.language.failed': 'Failed to load languages'
    });

    const mockSites: TreeNodeItem[] = [
        {
            label: 'demo.dotcms.com',
            data: {
                id: '123',
                hostname: 'demo.dotcms.com',
                path: '',
                type: 'site'
            },
            icon: 'pi pi-globe',
            leaf: false,
            children: []
        }
    ];

    const mockFolders = {
        parent: {
            id: 'parent-id',
            hostName: 'demo.dotcms.com',
            path: '/parent',
            addChildrenAllowed: true
        },
        folders: [
            {
                label: 'folder1',
                data: {
                    id: 'folder1',
                    hostname: 'demo.dotcms.com',
                    path: 'folder1',
                    type: 'folder' as const
                },
                icon: 'pi pi-folder',
                leaf: true,
                children: []
            }
        ]
    };

    const createComponent = createComponentFactory({
        component: SearchComponent,
        imports: [
            ReactiveFormsModule,
            ButtonModule,
            DropdownModule,
            InputGroupModule,
            InputTextModule,
            OverlayPanelModule,
            ChipModule,
            MockLanguageFieldComponent,
            MockSiteFieldComponent
        ],
        mocks: [DotMessagePipe],
        detectChanges: true,
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            mockProvider(DotEditContentService, {
                getSitesTreePath: jest.fn().mockReturnValue(of(mockSites)),
                getFoldersTreeNode: jest.fn().mockReturnValue(of(mockFolders))
            }),
            mockProvider(DotLanguagesService, {
                get: jest.fn().mockReturnValue(of(mockLocales))
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                isLoading: false
            } as unknown
        });
        component = spectator.component;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('Form Initialization', () => {
        it('should initialize form with default values', () => {
            const formValues = component.form.getRawValue();
            expect(formValues).toEqual({
                query: '',
                systemSearchableFields: {
                    languageId: -1,
                    siteOrFolderId: ''
                }
            });
        });

        it('should have valid form controls', () => {
            expect(component.form.get('query')).toBeTruthy();
            expect(component.form.get('systemSearchableFields')).toBeTruthy();
            expect(component.form.get('systemSearchableFields').get('languageId')).toBeTruthy();
            expect(component.form.get('systemSearchableFields').get('siteOrFolderId')).toBeTruthy();
        });
    });

    describe('Active Filters', () => {
        beforeEach(() => {
            // Set up mock for language field component
            const mockLanguageField = {
                languageControl: {
                    value: { isoCode: 'en-US', id: 1 }
                }
            } as unknown as LanguageFieldComponent;
            jest.spyOn(component, '$languageField').mockReturnValue(mockLanguageField);

            // Set up mock for site field component
            const mockSiteField = {
                siteControl: {
                    value: {
                        label: 'demo.dotcms.com',
                        data: { id: 'site123', type: 'site' }
                    }
                }
            } as unknown as SiteFieldComponent;
            jest.spyOn(component, '$siteField').mockReturnValue(mockSiteField);
        });

        it('should return empty filters when no active search params', () => {
            expect(component.$activeFilters()).toEqual([]);
        });

        it('should show language filter when language is selected', () => {
            component.$activeSearchParams.set({
                query: 'test',
                systemSearchableFields: { languageId: 1 }
            });

            const filters = component.$activeFilters();
            expect(filters).toHaveLength(1);
            expect(filters[0]).toEqual({
                label: 'en-US',
                value: 1,
                type: 'language'
            });
        });

        it('should show site filter when site is selected', () => {
            component.$activeSearchParams.set({
                query: 'test',
                systemSearchableFields: { siteId: 'site123' }
            });

            const filters = component.$activeFilters();
            expect(filters).toHaveLength(1);
            expect(filters[0]).toEqual({
                label: 'demo.dotcms.com',
                value: 'site:site123',
                type: 'site'
            });
        });

        it('should show folder filter when folder is selected', () => {
            component.$activeSearchParams.set({
                query: 'test',
                systemSearchableFields: { folderId: 'folder123' }
            });

            const filters = component.$activeFilters();
            expect(filters).toHaveLength(1);
            expect(filters[0]).toEqual({
                label: 'demo.dotcms.com',
                value: 'folder:folder123',
                type: 'folder'
            });
        });

        it('should show multiple filters when multiple are selected', () => {
            component.$activeSearchParams.set({
                query: 'test',
                systemSearchableFields: {
                    languageId: 1,
                    siteId: 'site123'
                }
            });

            const filters = component.$activeFilters();
            expect(filters).toHaveLength(2);
            expect(filters.some((f) => f.type === 'language')).toBe(true);
            expect(filters.some((f) => f.type === 'site')).toBe(true);
        });

        it('should not show language filter when languageId is -1', () => {
            component.$activeSearchParams.set({
                query: 'test',
                systemSearchableFields: { languageId: -1 }
            });

            const filters = component.$activeFilters();
            expect(filters).toHaveLength(0);
        });
    });

    describe('removeFilter', () => {
        it('should remove language filter and trigger search', () => {
            const searchSpy = jest.spyOn(component.onSearch, 'emit');

            component.form.patchValue({
                systemSearchableFields: { languageId: 1 }
            });

            component.removeFilter('language');

            expect(component.form.get('systemSearchableFields.languageId')?.value).toBe(-1);
            expect(searchSpy).toHaveBeenCalled();
        });

        it('should remove site filter and trigger search', () => {
            const searchSpy = jest.spyOn(component.onSearch, 'emit');

            component.form.patchValue({
                systemSearchableFields: { siteOrFolderId: 'site:123' }
            });

            component.removeFilter('site');

            expect(component.form.get('systemSearchableFields.siteOrFolderId')?.value).toBe('');
            expect(searchSpy).toHaveBeenCalled();
        });

        it('should remove folder filter and trigger search', () => {
            const searchSpy = jest.spyOn(component.onSearch, 'emit');

            component.form.patchValue({
                systemSearchableFields: { siteOrFolderId: 'folder:123' }
            });

            component.removeFilter('folder');

            expect(component.form.get('systemSearchableFields.siteOrFolderId')?.value).toBe('');
            expect(searchSpy).toHaveBeenCalled();
        });
    });

    describe('getValues', () => {
        it('should handle site type correctly', () => {
            component.form.patchValue({
                query: 'test search',
                systemSearchableFields: {
                    languageId: 2,
                    siteOrFolderId: 'site:site123'
                }
            });

            const result = component.getValues();

            expect(result).toEqual({
                query: 'test search',
                systemSearchableFields: {
                    languageId: 2,
                    siteId: 'site123'
                }
            });
        });

        it('should handle folder type correctly', () => {
            component.form.patchValue({
                query: 'test search',
                systemSearchableFields: {
                    languageId: 2,
                    siteOrFolderId: 'folder:folder123'
                }
            });

            const result = component.getValues();

            expect(result).toEqual({
                query: 'test search',
                systemSearchableFields: {
                    languageId: 2,
                    folderId: 'folder123'
                }
            });
        });

        it('should handle empty siteOrFolderId', () => {
            component.form.patchValue({
                query: 'test search',
                systemSearchableFields: {
                    languageId: 2,
                    siteOrFolderId: ''
                }
            });

            const result = component.getValues();

            expect(result).toEqual({
                query: 'test search',
                systemSearchableFields: {
                    languageId: 2
                }
            });
        });

        it('should handle malformed siteOrFolderId (no colon)', () => {
            component.form.patchValue({
                query: 'test search',
                systemSearchableFields: {
                    languageId: 2,
                    siteOrFolderId: 'invalidformat'
                }
            });

            const result = component.getValues();

            expect(result).toEqual({
                query: 'test search',
                systemSearchableFields: {
                    languageId: 2
                }
            });
        });

        it('should filter out empty values and -1', () => {
            component.form.patchValue({
                query: '',
                systemSearchableFields: {
                    languageId: -1,
                    siteOrFolderId: ''
                }
            });

            const result = component.getValues();

            expect(result).toEqual({
                query: '',
                systemSearchableFields: {}
            });
        });
    });

    describe('Display Label Methods', () => {
        it('should get language display label from control value', () => {
            const mockLanguageField = {
                languageControl: {
                    value: { isoCode: 'en-US', id: 1 }
                }
            } as unknown as LanguageFieldComponent;
            jest.spyOn(component, '$languageField').mockReturnValue(mockLanguageField);

            const label = component['getLanguageDisplayLabel'](1);
            expect(label).toBe('en-US');
        });

        it('should fallback to language ID when no control value', () => {
            jest.spyOn(component, '$languageField').mockReturnValue(null);

            const label = component['getLanguageDisplayLabel'](1);
            expect(label).toBe('Language Id: 1');
        });

        it('should get site display label from control value', () => {
            const mockSiteField = {
                siteControl: {
                    value: {
                        label: 'demo.dotcms.com',
                        data: { id: 'site123', type: 'site' }
                    }
                }
            } as unknown as SiteFieldComponent;
            jest.spyOn(component, '$siteField').mockReturnValue(mockSiteField);

            const label = component['getSiteDisplayLabel']('site123');
            expect(label).toBe('demo.dotcms.com');
        });

        it('should fallback to ID when no control value', () => {
            jest.spyOn(component, '$siteField').mockReturnValue(null);

            const label = component['getSiteDisplayLabel']('site123');
            expect(label).toBe('site123');
        });

        it('should truncate long labels to 45 characters', () => {
            const longLabel = 'a'.repeat(45);
            const mockSiteField = {
                siteControl: {
                    value: {
                        label: longLabel,
                        data: { id: 'site123', type: 'site' }
                    }
                }
            } as unknown as SiteFieldComponent;
            jest.spyOn(component, '$siteField').mockReturnValue(mockSiteField);

            const label = component['getSiteDisplayLabel']('site123');
            expect(label).toBe(longLabel.substring(0, 45) + '...');
        });
    });

    describe('clearForm', () => {
        beforeEach(() => {
            // Set some values in the form
            component.form.patchValue({
                query: 'test query',
                systemSearchableFields: {
                    languageId: 1,
                    siteOrFolderId: 'site:site1'
                }
            });
        });

        it('should reset form to initial values', () => {
            component.clearForm();

            const formValues = component.form.getRawValue();
            expect(formValues).toEqual({
                query: '',
                systemSearchableFields: {
                    languageId: -1,
                    siteOrFolderId: ''
                }
            });
        });

        it('should hide overlay panel', () => {
            const hideSpy = jest.spyOn(component.$overlayPanel(), 'hide');

            component.clearForm();

            expect(hideSpy).toHaveBeenCalled();
        });

        it('should clear active search parameters', () => {
            component.$activeSearchParams.set({
                query: 'test',
                systemSearchableFields: { languageId: 1 }
            });

            component.clearForm();

            expect(component.$activeSearchParams()).toEqual({});
        });

        it('should emit empty search', () => {
            const searchSpy = jest.spyOn(component.onSearch, 'emit');

            component.clearForm();

            expect(searchSpy).toHaveBeenCalledWith({});
        });
    });

    describe('doSearch', () => {
        it('should emit form values and hide overlay panel (site)', () => {
            const searchParams: SearchParams = {
                query: 'test search',
                systemSearchableFields: {
                    languageId: 2,
                    siteId: 'site123'
                }
            };

            const hideSpy = jest.spyOn(component.$overlayPanel(), 'hide');
            const searchSpy = jest.spyOn(component.onSearch, 'emit');

            component.form.patchValue({
                query: 'test search',
                systemSearchableFields: {
                    languageId: 2,
                    siteOrFolderId: 'site:site123'
                }
            });
            component.doSearch();

            expect(hideSpy).toHaveBeenCalled();
            expect(searchSpy).toHaveBeenCalledWith(searchParams);
        });

        it('should emit form values and hide overlay panel (folder)', () => {
            const searchParams: SearchParams = {
                query: 'test search',
                systemSearchableFields: {
                    languageId: 2,
                    folderId: 'folder123'
                }
            };

            const hideSpy = jest.spyOn(component.$overlayPanel(), 'hide');
            const searchSpy = jest.spyOn(component.onSearch, 'emit');

            component.form.patchValue({
                query: 'test search',
                systemSearchableFields: {
                    languageId: 2,
                    siteOrFolderId: 'folder:folder123'
                }
            });
            component.doSearch();

            expect(hideSpy).toHaveBeenCalled();
            expect(searchSpy).toHaveBeenCalledWith(searchParams);
        });

        it('should emit empty values when form is in initial state', () => {
            const searchSpy = jest.spyOn(component.onSearch, 'emit');

            component.doSearch();

            expect(searchSpy).toHaveBeenCalledWith({
                query: '',
                systemSearchableFields: {}
            });
        });

        it('should update active search parameters', () => {
            const searchParams = {
                query: 'test search',
                systemSearchableFields: {
                    languageId: 2,
                    siteId: 'site123'
                }
            };

            component.form.patchValue({
                query: 'test search',
                systemSearchableFields: {
                    languageId: 2,
                    siteOrFolderId: 'site:site123'
                }
            });

            component.doSearch();

            expect(component.$activeSearchParams()).toEqual(searchParams);
        });

        it('should set isLoading to true when search is performed', () => {
            const openFiltersButton = spectator.query(
                'p-button[data-testid="open-filters-button"] button'
            );
            spectator.click(openFiltersButton);

            spectator.setInput('isLoading', true);
            spectator.detectChanges();

            const searchButton = spectator.query<HTMLButtonElement>(
                'p-button[data-testid="search-button"] button'
            );

            expect(searchButton.disabled).toBeTruthy();
        });
    });

    describe('Debounced Search', () => {
        it('should trigger search automatically after debounce delay', fakeAsync(() => {
            const searchSpy = jest.spyOn(component.onSearch, 'emit');

            // Set query value
            component.form.get('query')?.setValue('test search');

            // Fast-forward time by less than debounce time - should not trigger search
            tick(DEBOUNCE_TIME - 1);
            expect(searchSpy).not.toHaveBeenCalled();

            // Fast-forward remaining time - should trigger search
            tick(1);
            expect(searchSpy).toHaveBeenCalledWith({
                query: 'test search',
                systemSearchableFields: {}
            });
        }));

        it('should include system search fields in debounced search', fakeAsync(() => {
            const searchSpy = jest.spyOn(component.onSearch, 'emit');

            // Set form values
            component.form.patchValue({
                query: 'test',
                systemSearchableFields: {
                    languageId: 1,
                    siteOrFolderId: 'site:123'
                }
            });

            tick(DEBOUNCE_TIME);

            expect(searchSpy).toHaveBeenCalledWith({
                query: 'test',
                systemSearchableFields: {
                    languageId: 1,
                    siteId: '123'
                }
            });
        }));
    });

    describe('Integration Tests', () => {
        it('should update form values when input changes', () => {
            const queryInput = spectator.query('input[formControlName="query"]');
            spectator.typeInElement('test query', queryInput);

            expect(component.form.get('query').value).toBe('test query');
        });

        it('should trigger debounced search when typing in input', fakeAsync(() => {
            const searchSpy = jest.spyOn(component.onSearch, 'emit');
            const queryInput = spectator.query('input[formControlName="query"]');

            spectator.typeInElement('test search', queryInput);

            // Should not trigger immediately
            expect(searchSpy).not.toHaveBeenCalled();

            // Wait for debounce
            tick(DEBOUNCE_TIME);

            expect(searchSpy).toHaveBeenCalledWith({
                query: 'test search',
                systemSearchableFields: {}
            });
        }));

        it('should trigger search when search button is clicked (site)', () => {
            const searchSpy = jest.spyOn(component.onSearch, 'emit');

            component.form.patchValue({
                query: 'test search',
                systemSearchableFields: {
                    languageId: 1,
                    siteOrFolderId: 'site:site123'
                }
            });

            const openFiltersButton = spectator.query(
                'p-button[data-testid="open-filters-button"] button'
            );
            spectator.click(openFiltersButton);

            const searchButton = spectator.query('p-button[data-testid="search-button"] button');
            spectator.click(searchButton);

            expect(searchSpy).toHaveBeenCalledWith({
                query: 'test search',
                systemSearchableFields: {
                    languageId: 1,
                    siteId: 'site123'
                }
            });
        });

        it('should trigger search when search button is clicked (folder)', () => {
            const searchSpy = jest.spyOn(component.onSearch, 'emit');

            component.form.patchValue({
                query: 'test search',
                systemSearchableFields: {
                    languageId: 1,
                    siteOrFolderId: 'folder:folder123'
                }
            });

            const openFiltersButton = spectator.query(
                'p-button[data-testid="open-filters-button"] button'
            );
            spectator.click(openFiltersButton);

            const searchButton = spectator.query('p-button[data-testid="search-button"] button');
            spectator.click(searchButton);

            expect(searchSpy).toHaveBeenCalledWith({
                query: 'test search',
                systemSearchableFields: {
                    languageId: 1,
                    folderId: 'folder123'
                }
            });
        });

        it('should clear form when clear button is clicked', () => {
            component.form.patchValue({
                query: 'test query',
                systemSearchableFields: {
                    languageId: 1,
                    siteOrFolderId: 'site:site123'
                }
            });

            const openFiltersButton = spectator.query(
                'p-button[data-testid="open-filters-button"] button'
            );
            spectator.click(openFiltersButton);

            const clearButton = spectator.query('p-button[data-testid="clear-button"] button');
            spectator.click(clearButton);

            expect(component.form.getRawValue()).toEqual({
                query: '',
                systemSearchableFields: {
                    languageId: -1,
                    siteOrFolderId: ''
                }
            });
        });

        it('should display filter chips when filters are active', () => {
            component.$activeSearchParams.set({
                query: 'test',
                systemSearchableFields: {
                    languageId: 1,
                    siteId: 'site123'
                }
            });

            // Mock the child components to return values
            const mockLanguageField = {
                languageControl: { value: { isoCode: 'en-US', id: 1 } }
            } as unknown as LanguageFieldComponent;
            const mockSiteField = {
                siteControl: { value: { label: 'demo.dotcms.com' } }
            } as unknown as SiteFieldComponent;
            jest.spyOn(component, '$languageField').mockReturnValue(mockLanguageField);
            jest.spyOn(component, '$siteField').mockReturnValue(mockSiteField);

            spectator.detectChanges();

            const chips = spectator.queryAll('p-chip');
            expect(chips.length).toBe(2);
        });

        it('should remove filter when chip is removed', () => {
            const searchSpy = jest.spyOn(component.onSearch, 'emit');

            component.$activeSearchParams.set({
                query: 'test',
                systemSearchableFields: { languageId: 1 }
            });

            // Mock the child components
            const mockLanguageField = {
                languageControl: { value: { isoCode: 'en-US', id: 1 } }
            } as unknown as LanguageFieldComponent;
            jest.spyOn(component, '$languageField').mockReturnValue(mockLanguageField);

            spectator.detectChanges();

            const chip = spectator.query('p-chip');
            expect(chip).toBeTruthy();

            // Simulate chip removal
            component.removeFilter('language');

            expect(searchSpy).toHaveBeenCalled();
        });
    });
});
