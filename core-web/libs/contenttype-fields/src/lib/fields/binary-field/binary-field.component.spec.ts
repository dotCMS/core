import { expect, it } from '@jest/globals';
import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotMessageService } from '@dotcms/data-access';

import { DotBinaryFieldComponent } from './binary-field.component';

import { CONTENTTYPE_FIELDS_MESSAGE_MOCK } from '../../utils/mock';

describe('DotBinaryFieldComponent', () => {
    let spectator: Spectator<DotBinaryFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotBinaryFieldComponent,
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

    it('should exist', () => {
        expect(spectator.component).toBeTruthy();
    });
});
