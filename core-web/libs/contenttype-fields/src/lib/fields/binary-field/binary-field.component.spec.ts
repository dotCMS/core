import { expect, it } from '@jest/globals';
import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotMessageService } from '@dotcms/data-access';

import { BinaryFieldComponent } from './binary-field.component';

import { CONTENTTYPE_FIELDS_MESSAGE_MOCK } from '../../utils/mock';

describe('BinaryFieldComponent', () => {
    let spectator: Spectator<BinaryFieldComponent>;

    const createComponent = createComponentFactory({
        component: BinaryFieldComponent,
        imports: [
            NoopAnimationsModule,
            ButtonModule,
            DialogModule,
            MonacoEditorModule,
            HttpClientTestingModule
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: CONTENTTYPE_FIELDS_MESSAGE_MOCK
            }
        ]
    });

    beforeEach(() => (spectator = createComponent()));

    describe('Write Code', () => {
        it('should show code editor when code button is clicked', () => {
            const button = spectator.query(byTestId('code-button'));

            // expect the code editor to be hidden
            expect(spectator.component.visible).toBe(false);

            spectator.click(button);
            const codeEditor = spectator.query(byTestId('code-editor'));

            // expect the code editor to be visible
            expect(codeEditor).toBeTruthy();
            expect(spectator.component.visible).toBe(true);
        });
    });
});
