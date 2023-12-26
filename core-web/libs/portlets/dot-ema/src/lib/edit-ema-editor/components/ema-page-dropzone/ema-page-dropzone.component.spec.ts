import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';

import { EmaPageDropzoneComponent, Row } from './ema-page-dropzone.component';

import { ActionPayload } from '../../../shared/models';

const ACTION_MOCK: ActionPayload = {
    container: {
        acceptTypes: 'file',
        contentletsId: ['123', '455'],
        identifier: '789',
        maxContentlets: 100,
        uuid: '2'
    },
    language_id: '1',
    pageContainers: [
        {
            identifier: '123',
            uuid: '1',
            contentletsId: ['123', '455']
        }
    ],
    pageId: '123'
};

export const BOUNDS_MOCK: Row[] = [
    {
        x: 0,
        y: 0,
        width: 1000,
        height: 200,
        columns: [
            {
                x: 0,
                y: 0,
                width: 500,
                height: 100,
                containers: [
                    {
                        x: 10,
                        y: 10,
                        width: 980,
                        height: 180,
                        contentlets: [
                            {
                                x: 20,
                                y: 20,
                                width: 940,
                                height: 140,
                                payload: null
                            }
                        ],
                        payload: null
                    }
                ]
            }
        ]
    }
];

describe('EmaPageDropzoneComponent', () => {
    let spectator: Spectator<EmaPageDropzoneComponent>;
    const createComponent = createComponentFactory({
        component: EmaPageDropzoneComponent,
        imports: [CommonModule]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render rows, columns, containers, and contentlets based on input', () => {
        spectator.setInput('rows', BOUNDS_MOCK);
        expect(spectator.queryAll('[data-type="row"]')).toHaveLength(1);
        expect(spectator.queryAll('[data-type="column"]')).toHaveLength(1);
        expect(spectator.queryAll('[data-type="container"]')).toHaveLength(1);
        expect(spectator.queryAll('[data-type="contentlet"]')).toHaveLength(1);
    });

    describe('css', () => {
        it('should apply styles to row correctly', () => {
            spectator.setInput('rows', BOUNDS_MOCK);

            const element = spectator.query('[data-type="row"]');
            const style = getComputedStyle(element);

            expect(style.position).toEqual('absolute');
            expect(style.left).toEqual('0px');
            expect(style.top).toEqual('0px');
            expect(style.width).toEqual('1000px');
            expect(style.height).toEqual('200px');
        });

        it('should apply styles to columns correctly', () => {
            spectator.setInput('rows', BOUNDS_MOCK);

            const element = spectator.query('[data-type="column"]');
            const style = getComputedStyle(element);

            expect(style.position).toEqual('absolute');
            expect(style.left).toEqual('0px');
            expect(style.top).toEqual('0px');
            expect(style.width).toEqual('500px');
            expect(style.height).toEqual('100px');
        });

        it('should apply styles to container correctly', () => {
            spectator.setInput('rows', BOUNDS_MOCK);

            const element = spectator.query('[data-type="container"]');
            const style = getComputedStyle(element);

            expect(style.position).toEqual('absolute');
            expect(style.left).toEqual('10px');
            expect(style.top).toEqual('10px');
            expect(style.width).toEqual('980px');
            expect(style.height).toEqual('180px');
        });

        it('should apply styles to contentlet correctly', () => {
            spectator.setInput('rows', BOUNDS_MOCK);

            const element = spectator.query('[data-type="contentlet"]');
            const style = getComputedStyle(element);

            expect(style.position).toEqual('absolute');
            expect(style.left).toEqual('20px');
            expect(style.top).toEqual('20px');
            expect(style.width).toEqual('940px');
            expect(style.height).toEqual('140px');
        });
    });

    describe('events', () => {
        it('should handle drop event correctly', () => {
            jest.spyOn(spectator.component.place, 'emit');

            spectator.setInput('rows', BOUNDS_MOCK);
            spectator.detectComponentChanges();

            spectator.triggerEventHandler('div[data-type="contentlet"]', 'drop', {
                target: {
                    clientY: 100,
                    getBoundingClientRect: () => {
                        return {
                            top: 100,
                            height: 100
                        };
                    },
                    dataset: {
                        payload: JSON.stringify(ACTION_MOCK)
                    }
                }
            });

            spectator.detectChanges();

            // Assert that the pointerPosition is reset
            expect(spectator.component.pointerPosition).toEqual({
                left: '0',
                width: '0',
                opacity: '0',
                top: '0'
            });

            expect(spectator.component.place.emit).toHaveBeenCalledWith({
                ...ACTION_MOCK,
                position: 'after'
            });

            // Additional assertions as necessary
        });

        it('should set pointer on drag over', () => {
            spectator.setInput('rows', BOUNDS_MOCK);
            spectator.detectComponentChanges();

            const stopPropagationSpy = jest.fn();
            const preventDefaultSpy = jest.fn();

            spectator.triggerEventHandler('div[data-type="contentlet"]', 'dragover', {
                stopPropagation: stopPropagationSpy,
                preventDefault: preventDefaultSpy,
                target: {
                    clientY: 100,
                    getBoundingClientRect: () => {
                        return {
                            left: 100,
                            top: 100,
                            width: 100,
                            height: 100
                        };
                    },
                    dataset: {
                        type: 'contentlet',
                        payload: JSON.stringify(ACTION_MOCK)
                    }
                }
            });

            spectator.detectChanges();

            expect(spectator.component.pointerPosition).toEqual({
                left: '100px',
                opacity: '1',
                top: '200px',
                width: '100px'
            });

            expect(stopPropagationSpy).toHaveBeenCalledTimes(1);
            expect(preventDefaultSpy).toHaveBeenCalledTimes(1);
        });
    });

    // Additional tests as necessary
});
