import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotCMSContentlet } from '@dotcms/uve/types';

import { FallbackComponent } from './fallback-component.component';

import { DynamicComponentEntity } from '../../../../models';

describe('FallbackComponent', () => {
    let spectator: Spectator<FallbackComponent>;
    let component: FallbackComponent;

    const createComponent = createComponentFactory({
        component: FallbackComponent,
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentlet: {} as DotCMSContentlet,
                UserNoComponent: null as DynamicComponentEntity | null
            }
        });

        component = spectator.component;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
