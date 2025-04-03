import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotCMSContentlet } from '@dotcms/uve/types';

import { ContentletComponent } from './contentlet.component';

import { DotCMSStore } from '../../../../store/dotcms.store';

describe('ContentletComponent', () => {
    let spectator: Spectator<ContentletComponent>;
    let component: ContentletComponent;
    let dotcmsContextService: jest.Mocked<DotCMSStore>;

    const createComponent = createComponentFactory({
        component: ContentletComponent,
        mocks: [DotCMSStore],
        detectChanges: false
    });

    beforeEach(() => {
        dotcmsContextService = {
            context: null,
            isDevMode: jest.fn()
        } as unknown as jest.Mocked<DotCMSStore>;

        spectator = createComponent({
            props: {
                contentlet: {} as DotCMSContentlet,
                container: 'test-container'
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
