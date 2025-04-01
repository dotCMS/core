import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotCMSContentlet } from '@dotcms/uve/types';

import { ContentletComponent } from './contentlet.component';

import { DotCMSContextService } from '../../../../services/dotcms-context/dotcms-context.service';

describe('ContentletComponent', () => {
    let spectator: Spectator<ContentletComponent>;
    let component: ContentletComponent;
    let dotcmsContextService: jest.Mocked<DotCMSContextService>;

    const createComponent = createComponentFactory({
        component: ContentletComponent,
        mocks: [DotCMSContextService],
        detectChanges: false
    });

    beforeEach(() => {
        dotcmsContextService = {
            context: null,
            isDevMode: jest.fn()
        } as unknown as jest.Mocked<DotCMSContextService>;

        spectator = createComponent({
            props: {
                contentlet: {} as DotCMSContentlet,
                container: 'test-container'
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
