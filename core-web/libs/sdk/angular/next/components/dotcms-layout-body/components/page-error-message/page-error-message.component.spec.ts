import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotCMSPageRendererMode } from '@dotcms/uve/types';

import { PageErrorMessageComponent } from './page-error-message.component';

import { DotCMSContextService } from '../../../../services/dotcms-context/dotcms-context.service';

describe('PageErrorMessageComponent', () => {
    let spectator: Spectator<PageErrorMessageComponent>;
    let component: PageErrorMessageComponent;
    let dotcmsContextService: jest.Mocked<DotCMSContextService>;

    const createComponent = createComponentFactory({
        component: PageErrorMessageComponent,
        mocks: [DotCMSContextService],
        detectChanges: false
    });

    beforeEach(() => {
        dotcmsContextService = {
            isDevMode: jest.fn()
        } as unknown as jest.Mocked<DotCMSContextService>;

        spectator = createComponent({
            props: {
                mode: 'production' as DotCMSPageRendererMode
            },
            providers: [
                {
                    provide: DotCMSContextService,
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
