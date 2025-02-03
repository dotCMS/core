import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { SearchParams } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/models/search.model';
import { DotMessagePipe } from '@dotcms/ui';

import { LanguageFieldComponent } from './components/language-field/language-field.component';
import { SiteFieldComponent } from './components/site-field/site-field.component';
import { SearchComponent } from './search.compoment';

describe('SearchComponent', () => {
    let spectator: Spectator<SearchComponent>;
    let component: SearchComponent;

    const createComponent = createComponentFactory({
        component: SearchComponent,
        imports: [
            ReactiveFormsModule,
            ButtonModule,
            DropdownModule,
            InputGroupModule,
            InputTextModule,
            OverlayPanelModule
        ],
        declarations: [MockComponent(LanguageFieldComponent), MockComponent(SiteFieldComponent)],
        mocks: [DotMessagePipe],
        detectChanges: true
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
                languageId: -1,
                siteId: ''
            });
        });

        it('should have valid form controls', () => {
            expect(component.form.get('query')).toBeTruthy();
            expect(component.form.get('languageId')).toBeTruthy();
            expect(component.form.get('siteId')).toBeTruthy();
        });
    });

    describe('clearForm', () => {
        beforeEach(() => {
            // Set some values in the form
            component.form.patchValue({
                query: 'test query',
                languageId: 1,
                siteId: 'site1'
            });
        });

        it('should reset form to initial values', () => {
            component.clearForm();

            const formValues = component.form.getRawValue();
            expect(formValues).toEqual({
                query: '',
                languageId: -1,
                siteId: ''
            });
        });

        it('should hide overlay panel', () => {
            const hideSpy = jest.spyOn(component.$overlayPanel(), 'hide');

            component.clearForm();

            expect(hideSpy).toHaveBeenCalled();
        });
    });

    describe('doSearch', () => {
        it('should emit form values and hide overlay panel', () => {
            const searchParams: SearchParams = {
                query: 'test search',
                languageId: 2,
                siteId: 'site123'
            };

            const hideSpy = jest.spyOn(component.$overlayPanel(), 'hide');
            const searchSpy = jest.spyOn(component.onSearch, 'emit');

            component.form.patchValue(searchParams);
            component.doSearch();

            expect(hideSpy).toHaveBeenCalled();
            expect(searchSpy).toHaveBeenCalledWith(searchParams);
        });

        it('should emit empty values when form is in initial state', () => {
            const searchSpy = jest.spyOn(component.onSearch, 'emit');

            component.doSearch();

            expect(searchSpy).toHaveBeenCalledWith({
                query: '',
                languageId: -1,
                siteId: ''
            });
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

    describe('Integration Tests', () => {
        it('should update form values when input changes', () => {
            const queryInput = spectator.query('input[formControlName="query"]');
            spectator.typeInElement('test query', queryInput);

            expect(component.form.get('query').value).toBe('test query');
        });

        it('should trigger search when search button is clicked', () => {
            const searchSpy = jest.spyOn(component.onSearch, 'emit');

            component.form.patchValue({
                query: 'test search',
                languageId: 1,
                siteId: 'site1'
            });

            const openFiltersButton = spectator.query(
                'p-button[data-testid="open-filters-button"] button'
            );
            spectator.click(openFiltersButton);

            const searchButton = spectator.query('p-button[data-testid="search-button"] button');
            spectator.click(searchButton);

            expect(searchSpy).toHaveBeenCalledWith({
                query: 'test search',
                languageId: 1,
                siteId: 'site1'
            });
        });

        it('should clear form when clear button is clicked', () => {
            component.form.patchValue({
                query: 'test query',
                languageId: 1,
                siteId: 'site1'
            });

            const openFiltersButton = spectator.query(
                'p-button[data-testid="open-filters-button"] button'
            );
            spectator.click(openFiltersButton);

            const clearButton = spectator.query('p-button[data-testid="clear-button"] button');
            spectator.click(clearButton);

            expect(component.form.getRawValue()).toEqual({
                query: '',
                languageId: -1,
                siteId: ''
            });
        });
    });
});
