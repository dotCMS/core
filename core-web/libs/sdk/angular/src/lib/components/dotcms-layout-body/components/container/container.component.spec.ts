import { expect, describe, it, beforeEach, jest } from '@jest/globals';
import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { DotCMSBasicContentlet, EditableContainerData } from '@dotcms/types';

import { ContainerComponent } from './container.component';

import { DotCMSStore } from '../../../../store/dotcms.store';
import { PageResponseMock } from '../../../../utils/testing.utils';
describe('ContainerComponent', () => {
    let spectator: Spectator<ContainerComponent>;

    const createComponent = createComponentFactory({
        component: ContainerComponent,
        providers: [
            {
                provide: DotCMSStore,
                useValue: {
                    $isDevMode: jest.fn().mockReturnValue(false),
                    store: {
                        page: PageResponseMock
                    }
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                container: {
                    identifier: '//demo.dotcms.com/application/containers/default/',
                    uuid: '1',
                    historyUUIDs: []
                }
            }
        });
    });

    it('should set host container attributes correctly', () => {
        const hostElement = spectator.debugElement.nativeElement;
        spectator.detectChanges();
        expect(hostElement.getAttribute('data-dot-object')).toBe('container');
        expect(hostElement.getAttribute('data-dot-identifier')).toBeDefined();
        expect(hostElement.getAttribute('data-dot-accept-types')).toBeDefined();
        expect(hostElement.getAttribute('data-max-contentlets')).toBeDefined();
        expect(hostElement.getAttribute('data-dot-uuid')).toBeDefined();
    });

    it('should display container not found when container data is null', () => {
        // Set container data to null
        spectator.component.$containerData.set(null);
        spectator.detectChanges();

        const notFoundComponent = spectator.query('dotcms-container-not-found');
        expect(notFoundComponent).toBeTruthy();
    });

    it('should display empty container when container has no contentlets', () => {
        // Set container data but empty contentlets
        spectator.component.$containerData.set({} as EditableContainerData);
        spectator.component.$contentlets.set([]);
        spectator.detectChanges();

        const emptyComponent = spectator.query('dotcms-empty-container');
        expect(emptyComponent).toBeTruthy();
    });

    it('should render contentlets when container has contentlets', () => {
        // Set container data with contentlets
        spectator.component.$containerData.set({} as EditableContainerData);
        spectator.component.$contentlets.set([
            { identifier: 'content-1' } as DotCMSBasicContentlet,
            { identifier: 'content-2' } as DotCMSBasicContentlet
        ]);
        spectator.detectChanges();

        const contentletComponents = spectator.queryAll('dotcms-contentlet');
        expect(contentletComponents.length).toBe(2);
    });
});
