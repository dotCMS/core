import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { EmaPageDropzoneComponent } from './ema-page-dropzone.component';
import { Row } from './types';

import { ClientData } from '../../../shared/models';

const ACTION_MOCK: ClientData = {
    container: {
        acceptTypes: 'file',
        identifier: '789',
        maxContentlets: 100,
        uuid: '2'
    }
};

const ITEM_MOCK = {
    contentType: 'file',
    baseType: 'FILEASSET'
};

const getBoundsMock = (payload: ClientData): Row[] => {
    return [
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
                                },
                                {
                                    x: 40,
                                    y: 20,
                                    width: 940,
                                    height: 140,
                                    payload: null
                                }
                            ],
                            payload
                        }
                    ]
                }
            ]
        }
    ];
};

export const BOUNDS_MOCK: Row[] = getBoundsMock(ACTION_MOCK);

const messageServiceMock = new MockDotMessageService({
    'edit.ema.page.dropzone.invalid.contentlet.type':
        'The contentlet type {0} is not valid for this container',
    'edit.ema.page.dropzone.max.contentlets': 'Container only accepts {0} contentlets',
    'edit.ema.page.dropzone.one.max.contentlet': 'Container only accepts one contentlet'
});

describe('EmaPageDropzoneComponent', () => {
    let spectator: Spectator<EmaPageDropzoneComponent>;
    let dotMessageService: DotMessageService;

    const createComponent = createComponentFactory({
        component: EmaPageDropzoneComponent,
        imports: [CommonModule, HttpClientTestingModule],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                rows: [],
                item: ITEM_MOCK
            }
        });
        dotMessageService = spectator.inject(DotMessageService, true);
    });

    it('should render rows, columns, containers, and contentlets based on input', () => {
        spectator.setInput('rows', BOUNDS_MOCK);
        expect(spectator.queryAll('[data-type="row"]')).toHaveLength(1);
        expect(spectator.queryAll('[data-type="column"]')).toHaveLength(1);
        expect(spectator.queryAll('[data-type="container"]')).toHaveLength(1);
        expect(spectator.queryAll('[data-type="contentlet"]')).toHaveLength(2);
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

            spectator.setInput('item', ITEM_MOCK);
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

        it('should allow drag and drop when baseType is WIDGET', () => {
            jest.spyOn(spectator.component.place, 'emit');

            spectator.setInput('item', {
                baseType: 'WIDGET',
                contentType: 'NOT_ACCEPTED_CONTENT_TYPE'
            });
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

            const errorZone = spectator.query('.drop-zone_error');

            // Check that the error message is not displayed
            expect(errorZone).toBeFalsy();

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
        });

        it('should not emit place event when the contentType is not accepted in the container', () => {
            const spyDotMessageSerivice = jest.spyOn(dotMessageService, 'get');
            jest.spyOn(spectator.component.place, 'emit');

            spectator.setInput('item', {
                ...ITEM_MOCK,
                contentType: 'NOT_ACCEPTED_CONTENT_TYPE'
            });
            spectator.setInput('rows', BOUNDS_MOCK);
            spectator.detectComponentChanges();

            spectator.triggerEventHandler('div.drop-zone_error', 'drop', {
                target: {}
            });

            spectator.detectChanges();

            const errorZone = spectator.query('.drop-zone_error');
            const errorZoneText = errorZone.querySelector('span').textContent;

            // Check that the error message is displayed
            expect(errorZone).toBeTruthy();
            expect(errorZoneText.trim()).toBe(
                'The contentlet type NOT_ACCEPTED_CONTENT_TYPE is not valid for this container'
            );

            expect(spectator.component.place.emit).not.toHaveBeenCalled();
            expect(spyDotMessageSerivice).toHaveBeenCalledWith(
                'edit.ema.page.dropzone.invalid.contentlet.type',
                'NOT_ACCEPTED_CONTENT_TYPE'
            );
        });

        it('should not emit place event when container is full', () => {
            const spyDotMessageSerivice = jest.spyOn(dotMessageService, 'get');
            jest.spyOn(spectator.component.place, 'emit');
            const NEW_BOUNDS_MOCK = getBoundsMock({
                ...ACTION_MOCK,
                container: {
                    ...ACTION_MOCK.container,
                    maxContentlets: 2
                }
            });

            spectator.setInput('item', ITEM_MOCK);
            spectator.setInput('rows', NEW_BOUNDS_MOCK);
            spectator.detectComponentChanges();

            spectator.triggerEventHandler('div.drop-zone_error', 'drop', {
                target: {}
            });

            spectator.detectChanges();

            const errorZone = spectator.query('.drop-zone_error') as HTMLElement;
            const errorZoneText = errorZone.querySelector('span').textContent;

            const { left, top, width, height } = errorZone.style;
            const errorZoneReact = {
                left,
                top,
                width,
                height
            };

            // Check that the error message is displayed
            expect(errorZone).toBeTruthy();
            expect(errorZoneText.trim()).toBe('Container only accepts 2 contentlets');
            expect(errorZoneReact).toEqual({
                left: '10px',
                top: '10px',
                width: '980px',
                height: '180px'
            });

            // Check that the place event is not emitted
            expect(spectator.component.place.emit).not.toHaveBeenCalled();
            expect(spyDotMessageSerivice).toHaveBeenCalledWith(
                'edit.ema.page.dropzone.max.contentlets',
                '2'
            );
        });

        it('should show one maximum content error when container is full and only allow one', () => {
            jest.spyOn(spectator.component.place, 'emit');

            const spyDotMessageSerivice = jest.spyOn(dotMessageService, 'get');
            const NEW_BOUNDS_MOCK = getBoundsMock({
                ...ACTION_MOCK,
                container: {
                    ...ACTION_MOCK.container,
                    maxContentlets: 1
                }
            });

            spectator.setInput('item', ITEM_MOCK);
            spectator.setInput('rows', NEW_BOUNDS_MOCK);
            spectator.detectComponentChanges();

            spectator.triggerEventHandler('div.drop-zone_error', 'drop', {
                target: {}
            });

            spectator.detectChanges();

            const errorZone = spectator.query('.drop-zone_error') as HTMLElement;
            const errorZoneText = errorZone.querySelector('span').textContent;

            const { left, top, width, height } = errorZone.style;
            const errorZoneReact = {
                left,
                top,
                width,
                height
            };

            // Check that the error message is displayed
            expect(errorZone).toBeTruthy();
            expect(errorZoneText.trim()).toBe('Container only accepts one contentlet');
            expect(errorZoneReact).toEqual({
                left: '10px',
                top: '10px',
                width: '980px',
                height: '180px'
            });

            // Check that the place event is not emitted
            expect(spectator.component.place.emit).not.toHaveBeenCalled();
            expect(spyDotMessageSerivice).toHaveBeenCalledWith(
                'edit.ema.page.dropzone.one.max.contentlet',
                '1'
            );
        });

        it('should set pointer on drag over', () => {
            spectator.setInput('item', ITEM_MOCK);
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
});
