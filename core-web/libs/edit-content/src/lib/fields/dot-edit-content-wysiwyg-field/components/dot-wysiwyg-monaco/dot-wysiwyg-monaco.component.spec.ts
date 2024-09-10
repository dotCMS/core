import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { ControlContainer } from '@angular/forms';

import { DotWysiwygMonacoComponent } from './dot-wysiwyg-monaco.component';

import { createFormGroupDirectiveMock, WYSIWYG_MOCK } from '../../../../utils/mocks';

describe('DotWysiwygMonacoComponent', () => {
    let spectator: Spectator<DotWysiwygMonacoComponent>;

    const createComponent = createComponentFactory({
        component: DotWysiwygMonacoComponent,
        componentViewProviders: [
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock()
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: WYSIWYG_MOCK
            } as unknown,
            detectChanges: false
        });
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});
