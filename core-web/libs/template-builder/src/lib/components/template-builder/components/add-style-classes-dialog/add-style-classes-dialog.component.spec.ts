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
            DynamicDialogRef
        ]
    });

    beforeEach(() => {
        spectator = createHost(
            '<dotcms-add-style-classes-dialog></dotcms-add-style-classes-dialog>'
        );

        ref = spectator.inject(DynamicDialogRef);

        spectator.detectChanges();

        input = spectator.query('#auto-complete-input');

        mockMatchMedia();
    });

    it('should have an update button', () => {
        expect(spectator.query(byTestId('update-btn'))).toBeTruthy();
    });

    it('should trigger filterClasses when focusing on the input', () => {
        const filterMock = jest.spyOn(spectator.component, 'filterClasses');
        const query = CLASS_NAME_MOCK;

        input.value = query;

        spectator.click(input);

        spectator.detectChanges();

        expect(filterMock).toHaveBeenCalledWith(query);
    });

    it('should trigger save when clicking on update-btn', () => {
        const closeMock = jest.spyOn(ref, 'close');

        const updateBtn = spectator.query(byTestId('update-btn'));

        spectator.dispatchFakeEvent(updateBtn, 'onClick');

        spectator.detectChanges();

        expect(closeMock).toHaveBeenCalled();
    });
});
