import { expect } from '@jest/globals';
import { SpectatorHost, byTestId, createHostFactory } from '@ngneat/spectator';

import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { AddStyleClassesDialogComponent } from './add-style-classes-dialog.component';
import { DotAddStyleClassesDialogStore } from './store/add-style-classes-dialog.store';

import { MOCK_SELECTED_STYLE_CLASSES, addStyleClassesStoreMock } from '../../utils/mocks';

describe('AddStyleClassesDialogComponent', () => {
    let component: AddStyleClassesDialogComponent;
    let spectator: SpectatorHost<AddStyleClassesDialogComponent>;

    const createHost = createHostFactory({
        component: AddStyleClassesDialogComponent,
        imports: [
            AutoCompleteModule,
            FormsModule,
            NoopAnimationsModule,
            ButtonModule,
            HttpClientModule
        ],
        providers: [
            {
                provide: DynamicDialogConfig,
                useValue: {
                    data: {
                        selectedClasses: MOCK_SELECTED_STYLE_CLASSES
                    }
                }
            },
            {
                provide: DotAddStyleClassesDialogStore,
                useValue: addStyleClassesStoreMock
            },
            DynamicDialogRef
        ]
    });

    beforeEach(() => {
        spectator = createHost(
            '<dotcms-add-style-classes-dialog></dotcms-add-style-classes-dialog>',
            {}
        );

        component = spectator.component;
        spectator.detectChanges();

        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(), // Deprecated
                removeListener: jest.fn(), // Deprecated
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn()
            }))
        });
    });

    it('should have an autocomplete component', () => {
        expect(spectator.query('p-autocomplete')).toBeTruthy();
    });

    it('should have an update button', () => {
        expect(spectator.query(byTestId('update-btn'))).toBeTruthy();
    });

    it('should trigger filterMock when focusing on the input', () => {
        const filterMock = jest.spyOn(component, 'filterClasses');

        const input = spectator.query('#my-autocomplete') as HTMLInputElement;

        spectator.click(input);

        spectator.detectChanges();

        expect(filterMock).toHaveBeenCalled();
    });

    it('should trigger addClassByCommaOrSpace when typing on the input and adding comma', () => {
        const addMock = jest.spyOn(component, 'addClassByCommaOrSpace');

        const input = spectator.query('#my-autocomplete') as HTMLInputElement;

        input.value = 'd-none,';

        spectator.click(input);

        spectator.detectChanges();

        expect(addMock).toHaveBeenCalledWith('d-none,');
        expect(component.selectedClasses).toContainEqual({ klass: 'd-none' });
    });

    it('should trigger addClassByCommaOrSpace when typing on the input and adding space', () => {
        const addMock = jest.spyOn(component, 'addClassByCommaOrSpace');

        const input = spectator.query('#my-autocomplete') as HTMLInputElement;

        input.value = 'd-none,';

        spectator.click(input);

        spectator.detectChanges();

        expect(addMock).toHaveBeenCalledWith('d-none,');
        expect(component.selectedClasses).toContainEqual({ klass: 'd-none' });
    });

    it('should filter selectedClasses from filteredClasses when filteringClasses', () => {
        const input = spectator.query('#my-autocomplete') as HTMLInputElement;

        spectator.click(input);

        spectator.detectChanges();

        expect(component.filteredClasses).not.toContainEqual({ klass: 'd-flex' });
    });

    it('should trigger closeDialog when clicking on update-btn', () => {
        const closeMock = jest.spyOn(component, 'closeDialog');

        const updateBtn = spectator.query(byTestId('update-btn'));

        spectator.dispatchFakeEvent(updateBtn, 'onClick');

        spectator.detectChanges();

        expect(closeMock).toHaveBeenCalled();
    });
});
