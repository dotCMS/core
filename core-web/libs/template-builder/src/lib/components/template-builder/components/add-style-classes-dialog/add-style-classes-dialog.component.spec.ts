import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator';
import { of } from 'rxjs';

import { AsyncPipe, NgIf } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { AddStyleClassesDialogComponent } from './add-style-classes-dialog.component';
import { DotAddStyleClassesDialogStore } from './store/add-style-classes-dialog.store';

import {
    CLASS_NAME_MOCK,
    DOT_MESSAGE_SERVICE_TB_MOCK,
    MOCK_SELECTED_STYLE_CLASSES,
    MOCK_STYLE_CLASSES_FILE,
    mockMatchMedia
} from '../../utils/mocks';

describe('AddStyleClassesDialogComponent', () => {
    let spectator: SpectatorHost<AddStyleClassesDialogComponent>;
    let input: HTMLInputElement;
    let ref: DynamicDialogRef;
    let store: DotAddStyleClassesDialogStore;

    const createHost = createHostFactory({
        component: AddStyleClassesDialogComponent,
        imports: [
            AutoCompleteModule,
            FormsModule,
            ButtonModule,
            DotMessagePipe,
            NgIf,
            AsyncPipe,
            HttpClientModule,
            NoopAnimationsModule
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
                provide: HttpClient,
                useValue: {
                    get: () => of(MOCK_STYLE_CLASSES_FILE)
                }
            },
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            },
            DotAddStyleClassesDialogStore,
            DynamicDialogRef
        ]
    });

    beforeEach(() => {
        spectator = createHost(
            '<dotcms-add-style-classes-dialog></dotcms-add-style-classes-dialog>'
        );

        ref = spectator.inject(DynamicDialogRef);
        store = spectator.inject(DotAddStyleClassesDialogStore);

        spectator.detectChanges();

        input = spectator.query('#auto-complete-input');

        mockMatchMedia();
    });

    it('should have an update button', () => {
        expect(spectator.query(byTestId('update-btn'))).toBeTruthy();
    });

    it('should trigger filterClasses when focusing on the input', () => {
        const filterMock = jest.spyOn(store, 'filterClasses');
        const query = CLASS_NAME_MOCK;

        input.value = query;

        spectator.click(input);

        spectator.detectChanges();

        expect(filterMock).toHaveBeenCalledWith(query);
    });

    it('should trigger addClass when autocomplete emits onSelect', () => {
        const autoComplete = spectator.query('p-autocomplete');

        const addClassMock = jest.spyOn(store, 'addClass');

        spectator.dispatchFakeEvent(autoComplete, 'onSelect');

        spectator.detectChanges();

        expect(addClassMock).toHaveBeenCalled();
    });

    it('should trigger removeLastClass when autocomplete emits onUnselect', () => {
        const autoComplete = spectator.query('p-autocomplete');

        const removeLastClassMock = jest.spyOn(store, 'removeLastClass');

        spectator.dispatchFakeEvent(autoComplete, 'onUnselect');

        spectator.detectChanges();

        expect(removeLastClassMock).toHaveBeenCalled();
    });

    it('should trigger saveClass when clicking on update-btn', () => {
        const closeMock = jest.spyOn(ref, 'close');

        const updateBtn = spectator.query(byTestId('update-btn'));

        spectator.dispatchFakeEvent(updateBtn, 'onClick');

        spectator.detectChanges();

        expect(closeMock).toHaveBeenCalled();
    });

    it('should trigger addClass when enter is pressed', () => {
        const addClassMock = jest.spyOn(store, 'addClass');

        spectator.typeInElement(CLASS_NAME_MOCK, input);
        spectator.keyboard.pressEnter(input);

        spectator.detectChanges();

        expect(addClassMock).toHaveBeenCalledWith({ cssClass: CLASS_NAME_MOCK });
    });
});
