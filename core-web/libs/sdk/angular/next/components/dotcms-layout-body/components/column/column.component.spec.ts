import { expect } from '@jest/globals';
import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { ColumnComponent } from './column.component';

describe('ColumnComponent', () => {
    let spectator: Spectator<ColumnComponent>;

    const createComponent = createComponentFactory({
        component: ColumnComponent
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

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should apply custom class', () => {
        const columnElement = spectator.query('.column');
        expect(columnElement?.classList.contains('custom-class')).toBe(true);
    });

    it('should set width percentage', () => {
        const columnElement = spectator.query('.column');
        const computedStyle = window.getComputedStyle(columnElement as Element);
        expect(computedStyle.width).toBe('50%');
    });

    it('should render containers', () => {
        const mockContainers = [
            { identifier: 'test-container-1' },
            { identifier: 'test-container-2' }
        ];

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

        const containers = spectator.queryAll('dotcms-container');
        expect(containers.length).toBe(2);
    });
});
