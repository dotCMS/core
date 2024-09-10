import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { DotWysiwygTinymceComponent } from './dot-wysiwyg-tinymce.component';
import { DotWysiwygTinymceService } from './service/dot-wysiwyg-tinymce.service';

import { createFormGroupDirectiveMock, WYSIWYG_MOCK } from '../../../../utils/mocks';
import { DotWysiwygPluginService } from '../../dot-wysiwyg-plugin/dot-wysiwyg-plugin.service';

const mockSystemWideConfig = { systemWideOption: 'value' };

describe('DotWysiwygTinymceComponent', () => {
    let spectator: Spectator<DotWysiwygTinymceComponent>;

    const createComponent = createComponentFactory({
        component: DotWysiwygTinymceComponent,
        componentViewProviders: [
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock()
            },
            {
                provide: DotWysiwygPluginService,
                useValue: {
                    initializePlugins: jest.fn()
                }
            },
            mockProvider(DotWysiwygTinymceService, {
                getProps: () => of(mockSystemWideConfig)
            }),

            provideHttpClientTesting()
        ],
        providers: [FormGroupDirective, provideHttpClientTesting()]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: WYSIWYG_MOCK
            } as unknown,
            detectChanges: true
        });

        spectator.detectChanges();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});
