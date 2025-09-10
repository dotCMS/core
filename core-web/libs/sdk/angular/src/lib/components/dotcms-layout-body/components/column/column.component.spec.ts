import { expect } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { DotCMSColumnContainer } from '@dotcms/types';

import { ColumnComponent } from './column.component';

import { ContainerComponent } from '../container/container.component';

describe('ColumnComponent', () => {
    let spectator: Spectator<ColumnComponent>;

    const createComponent = createComponentFactory({
        component: ColumnComponent,
        imports: [MockComponent(ContainerComponent)]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                column: {
                    containers: [],
                    widthPercent: 50,
                    width: 6,
                    leftOffset: 0,
                    left: 0,
                    preview: false,
                    styleClass: 'custom-class'
                }
            }
        });
    });

    it('should apply custom class', () => {
        const columnElement = spectator.query(byTestId('dotcms-column'));
        expect(columnElement?.classList.contains('custom-class')).toBe(true);
    });

    it('should render containers', () => {
        const mockContainers = [
            { identifier: 'test-container-1' },
            { identifier: 'test-container-2' }
        ] as unknown as DotCMSColumnContainer[];

        spectator.setInput({
            column: {
                containers: mockContainers,
                widthPercent: 50,
                width: 6,
                leftOffset: 0,
                left: 0,
                preview: false,
                styleClass: ''
            }
        });

        spectator.detectChanges();

        const containers = spectator.queryAll('dotcms-container');
        expect(containers.length).toBe(2);
    });
});
