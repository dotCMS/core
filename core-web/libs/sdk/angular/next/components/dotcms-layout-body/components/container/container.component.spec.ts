import { expect } from '@jest/globals';
import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { DotCMSColumnContainer } from '@dotcms/uve/types';

import { ContainerComponent } from './container.component';

import { DotCMSStore } from '../../../../store/dotcms.store';

describe('ContainerComponent', () => {
    let spectator: Spectator<ContainerComponent>;

    const createComponent = createComponentFactory({
        component: ContainerComponent,
        providers: [
            {
                provide: DotCMSStore,
                useValue: {
                    isDevMode: jest.fn().mockReturnValue(false)
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                container: {
                    identifier: 'test-container'
                } as DotCMSColumnContainer
            }
        });
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should render empty container message when no contentlets', () => {
        const emptyMessage = spectator.query('.empty-container');
        expect(emptyMessage?.textContent).toBe('This container is empty.');
    });

    it('should render contentlets when available', () => {
        const mockContentlets = [{ title: 'Test Contentlet 1' }, { title: 'Test Contentlet 2' }];

        spectator.setInput({
            container: {
                ...spectator.component.container,
                contentlets: mockContentlets
            }
        });

        const contentlets = spectator.queryAll('.contentlet-wrapper');
        expect(contentlets.length).toBe(2);
        expect(contentlets[0].textContent).toBe('Test Contentlet 1');
        expect(contentlets[1].textContent).toBe('Test Contentlet 2');
    });

    it('should set data attributes in edit mode', () => {
        const dotCMSContextService = spectator.inject(DotCMSStore);
        jest.spyOn(dotCMSContextService, 'isDevMode').mockReturnValue(true);

        spectator.detectChanges();

        const container = spectator.query('.container');
        expect(container?.getAttribute('data-dot-accept-types')).toBe('test-accept-types');
        expect(container?.getAttribute('data-dot-identifier')).toBe('test-container');
        expect(container?.getAttribute('data-max-contentlets')).toBe('10');
        expect(container?.getAttribute('data-dot-uuid')).toBe('test-uuid');
    });

    it('should not set data attributes in production mode', () => {
        const container = spectator.query('.container');
        expect(container?.getAttribute('data-dot-accept-types')).toBeNull();
        expect(container?.getAttribute('data-dot-identifier')).toBeNull();
        expect(container?.getAttribute('data-max-contentlets')).toBeNull();
        expect(container?.getAttribute('data-dot-uuid')).toBeNull();
    });
});
