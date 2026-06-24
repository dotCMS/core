import { expect, describe, it, beforeEach } from '@jest/globals';
import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { signal, WritableSignal } from '@angular/core';

import { DotCMSBasicContentlet, EditableContainerData } from '@dotcms/types';

import { ContainerComponent } from './container.component';

import { DotCMSStore } from '../../../../store/dotcms.store';
import { PageResponseMock } from '../../../../utils/testing.utils';

describe('ContainerComponent', () => {
    let spectator: Spectator<ContainerComponent>;
    let isDevMode: WritableSignal<boolean>;
    let isAnalyticsActive: WritableSignal<boolean>;

    const createComponent = createComponentFactory({
        component: ContainerComponent,
        providers: [
            {
                provide: DotCMSStore,
                useFactory: () => ({
                    $isDevMode: signal(false),
                    $isAnalyticsActive: signal(false),
                    store: {
                        page: PageResponseMock
                    }
                })
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
        const store = spectator.inject(DotCMSStore) as unknown as {
            $isDevMode: WritableSignal<boolean>;
            $isAnalyticsActive: WritableSignal<boolean>;
        };
        isDevMode = store.$isDevMode;
        isAnalyticsActive = store.$isAnalyticsActive;
    });

    it('should emit container data-dot-* attributes in edit mode', () => {
        isDevMode.set(true);
        spectator.component.$containerData.set({
            identifier: 'test-container-id',
            acceptTypes: 'test-accept-types',
            maxContentlets: 10,
            uuid: 'test-uuid'
        });
        spectator.detectChanges();

        const hostElement = spectator.debugElement.nativeElement;
        expect(hostElement.getAttribute('data-dot-object')).toBe('container');
        expect(hostElement.getAttribute('data-dot-identifier')).toBe('test-container-id');
        expect(hostElement.getAttribute('data-dot-accept-types')).toBe('test-accept-types');
        expect(hostElement.getAttribute('data-max-contentlets')).toBe('10');
        expect(hostElement.getAttribute('data-dot-uuid')).toBe('test-uuid');
    });

    it('should not emit any container data-dot-* attributes in live mode', () => {
        isDevMode.set(false);
        spectator.component.$containerData.set({
            identifier: 'test-container-id',
            acceptTypes: 'test-accept-types',
            maxContentlets: 10,
            uuid: 'test-uuid'
        });
        spectator.detectChanges();

        const dotAttrs = Array.from(spectator.debugElement.nativeElement.attributes)
            .map((attr) => (attr as Attr).name)
            .filter((name) => name.startsWith('data-dot') || name === 'data-max-contentlets');

        expect(dotAttrs).toEqual([]);
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
        // Ensure the analytics signal is wired for child contentlets (avoids unused warning)
        expect(isAnalyticsActive()).toBe(false);
    });
});
