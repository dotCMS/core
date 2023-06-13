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

import {
    MOCK_SELECTED_STYLE_CLASSES,
    addStyleClassesStoreMock,
    mockMatchMedia
} from '../../utils/mocks';

describe('AddStyleClassesDialogComponent', () => {
    let component: AddStyleClassesDialogComponent;
    let spectator: SpectatorHost<AddStyleClassesDialogComponent>;
    let input: HTMLInputElement;
    let ref: DynamicDialogRef;

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

        ref = spectator.inject(DynamicDialogRef);

        input = spectator.query('#auto-complete-input');

        mockMatchMedia();
    });

    it('should have an update button', () => {
        expect(spectator.query(byTestId('update-btn'))).toBeTruthy();
    });

    it('should trigger filterMock when focusing on the input', () => {
        const filterMock = jest.spyOn(component, 'filterClasses');

        spectator.click(input);

        spectator.detectChanges();

        expect(filterMock).toHaveBeenCalled();
    });

    it('should trigger addClassByCommaOrSpace when typing on the input and adding comma', () => {
        input.value = 'd-none,'; // This has a comma

        spectator.click(input);

        spectator.detectChanges();

        expect(component.selectedClasses).toContainEqual({ cssClass: 'd-none' });
    });

    it('should trigger addClassByCommaOrSpace when typing on the input and adding space', () => {
        input.value = 'd-none '; // This has a space

        spectator.click(input);

        spectator.detectChanges();

        expect(component.selectedClasses).toContainEqual({ cssClass: 'd-none' });
    });

    it('should filter selectedClasses from filteredClasses when filteringClasses', () => {
        spectator.click(input);

        spectator.detectChanges();

        expect(component.filteredClasses).not.toContainEqual({ cssClass: 'd-flex' });
    });

    it('should trigger saveClass when clicking on update-btn', () => {
        const closeMock = jest.spyOn(ref, 'close');

        const updateBtn = spectator.query(byTestId('update-btn'));

        spectator.dispatchFakeEvent(updateBtn, 'onClick');

        spectator.detectChanges();

        expect(closeMock).toHaveBeenCalled();
    });
});
