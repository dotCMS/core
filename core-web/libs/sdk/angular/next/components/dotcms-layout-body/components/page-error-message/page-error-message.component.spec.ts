import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotCMSPageRendererMode } from '@dotcms/uve/types';

import { PageErrorMessageComponent } from './page-error-message.component';

import { DotCMSStore } from '../../../../store/dotcms.store';

describe('PageErrorMessageComponent', () => {
    let spectator: Spectator<PageErrorMessageComponent>;
    let component: PageErrorMessageComponent;
    let dotcmsContextService: jest.Mocked<DotCMSStore>;

    const createComponent = createComponentFactory({
        component: PageErrorMessageComponent,
        mocks: [DotCMSStore],
        detectChanges: false
    });

    beforeEach(() => {
        dotcmsContextService = {
            isDevMode: jest.fn()
        } as unknown as jest.Mocked<DotCMSStore>;

        spectator = createComponent({
            props: {
                mode: 'production' as DotCMSPageRendererMode
            },
            providers: [
                {
                    provide: DotCMSStore,
                    useValue: dotcmsContextService
                }
            ]
        });

        component = spectator.component;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
