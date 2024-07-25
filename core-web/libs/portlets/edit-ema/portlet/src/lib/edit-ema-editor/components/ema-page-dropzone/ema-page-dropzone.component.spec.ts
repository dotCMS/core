import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { EmaPageDropzoneComponent } from './ema-page-dropzone.component';

import {
    ITEM_MOCK,
    BOUNDS_MOCK,
    getBoundsMock,
    ACTION_MOCK,
    BOUNDS_EMPTY_CONTAINER_MOCK
} from '../../../shared/mocks';

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
                containers: [],
                dragItem: ITEM_MOCK
            }
        });
        dotMessageService = spectator.inject(DotMessageService, true);
    });

    it('should render containers, and contentlets based on input', () => {
        spectator.setInput('containers', BOUNDS_MOCK);
        expect(spectator.queryAll('[data-type="container"]')).toHaveLength(1);
        expect(spectator.queryAll('[data-type="contentlet"]')).toHaveLength(2);
    });

    describe('css', () => {
        it('should apply styles to container correctly', () => {
            spectator.setInput('containers', BOUNDS_MOCK);

            const element = spectator.query('[data-type="container"]');
            const style = getComputedStyle(element);

            expect(style.position).toEqual('absolute');
            expect(style.left).toEqual('10px');
            expect(style.top).toEqual('10px');
            expect(style.width).toEqual('980px');
            expect(style.height).toEqual('180px');
        });

        it('should apply styles to contentlet correctly', () => {
            spectator.setInput('containers', BOUNDS_MOCK);

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
        describe('contentlet', () => {
            it('should show drop zone error when container is full', () => {
                const spyDotMessageSerivice = jest.spyOn(dotMessageService, 'get');

                const NEW_BOUNDS_MOCK = getBoundsMock({
                    ...ACTION_MOCK,
                    container: {
                        ...ACTION_MOCK.container,
                        maxContentlets: 2
                    }
                });

                spectator.setInput('dragItem', ITEM_MOCK);
                spectator.setInput('containers', NEW_BOUNDS_MOCK);
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
                    left: '0px',
                    top: '0px',
                    width: '980px',
                    height: '180px'
                });

                // Check that the place event is not emitted
                expect(spyDotMessageSerivice).toHaveBeenCalledWith(
                    'edit.ema.page.dropzone.max.contentlets',
                    '2'
                );
            });

            it('should show one maximum content error when container is full and only allow one', () => {
                const spyDotMessageSerivice = jest.spyOn(dotMessageService, 'get');
                const NEW_BOUNDS_MOCK = getBoundsMock({
                    ...ACTION_MOCK,
                    container: {
                        ...ACTION_MOCK.container,
                        maxContentlets: 1
                    }
                });

                spectator.setInput('dragItem', ITEM_MOCK);
                spectator.setInput('containers', NEW_BOUNDS_MOCK);
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
                    left: '0px',
                    top: '0px',
                    width: '980px',
                    height: '180px'
                });

                expect(spyDotMessageSerivice).toHaveBeenCalledWith(
                    'edit.ema.page.dropzone.one.max.contentlet',
                    '1'
                );
            });

            it('should set pointer on drag over', () => {
                spectator.setInput('dragItem', ITEM_MOCK);
                spectator.setInput('containers', BOUNDS_MOCK);
                spectator.detectComponentChanges();

                spectator.triggerEventHandler('div[data-type="contentlet"]', 'dragover', {
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
                            payload: JSON.stringify(ACTION_MOCK),
                            dropzone: 'true'
                        }
                    }
                });

                spectator.detectChanges();

                expect(spectator.component.pointerPosition).toEqual({
                    left: '100px',
                    opacity: '1',
                    top: '200px',
                    width: '100px',
                    height: '3px'
                });
            });
        });

        describe('empty container', () => {
            it('should show drop zone error when the contentType is not accepted in the container', () => {
                const spyDotMessageSerivice = jest.spyOn(dotMessageService, 'get');

                spectator.setInput('dragItem', {
                    ...ITEM_MOCK,
                    contentType: 'NOT_ACCEPTED_CONTENT_TYPE'
                });
                spectator.setInput('containers', BOUNDS_EMPTY_CONTAINER_MOCK);
                spectator.detectChanges();

                const errorZone = spectator.query('.drop-zone_error');
                const errorZoneText = errorZone.querySelector('span').textContent;

                // Check that the error message is displayed
                expect(errorZone).toBeTruthy();
                expect(errorZoneText.trim()).toBe(
                    'The contentlet type NOT_ACCEPTED_CONTENT_TYPE is not valid for this container'
                );

                expect(spyDotMessageSerivice).toHaveBeenCalledWith(
                    'edit.ema.page.dropzone.invalid.contentlet.type',
                    'NOT_ACCEPTED_CONTENT_TYPE'
                );
            });

            it('should set pointer on drag over', () => {
                spectator.setInput('dragItem', ITEM_MOCK);
                spectator.setInput('containers', BOUNDS_EMPTY_CONTAINER_MOCK);
                spectator.detectComponentChanges();

                spectator.triggerEventHandler('div[data-type="container"]', 'dragover', {
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
                            payload: JSON.stringify(ACTION_MOCK),
                            dropzone: 'true',
                            empty: 'true'
                        }
                    }
                });

                spectator.detectChanges();

                expect(spectator.component.pointerPosition).toEqual({
                    left: '100px',
                    opacity: '0.1',
                    top: '100px',
                    width: '100px',
                    height: '100px'
                });
            });
        });
    });
});
