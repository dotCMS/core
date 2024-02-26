import { Spectator, byTestId, byText, createComponentFactory } from '@ngneat/spectator/jest';

import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { EmaContentletToolsComponent } from './ema-contentlet-tools.component';

import { ActionPayload } from '../../../shared/models';

const payload: ActionPayload = {
    language_id: '1',
    pageContainers: [
        {
            identifier: 'test',
            uuid: 'test',
            contentletsId: []
        }
    ],
    contentlet: {
        identifier: 'contentlet-identifier-123',
        inode: 'contentlet-inode-123',
        title: 'Hello World',
        contentType: 'test'
    },
    container: {
        identifier: 'test',
        acceptTypes: 'test',
        uuid: 'test',
        maxContentlets: 1,
        contentletsId: []
    },
    pageId: 'test',
    position: 'after'
};

const contentletAreaMock = { x: 100, y: 100, width: 500, height: 100, payload };

describe('EmaContentletToolsComponent', () => {
    let spectator: Spectator<EmaContentletToolsComponent>;
    const createComponent = createComponentFactory({
        component: EmaContentletToolsComponent,
        imports: [ButtonModule, MenuModule],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    content: 'Content',
                    Widget: 'Widget',
                    form: 'Form'
                })
            }
        ]
    });

    describe('default', () => {
        beforeEach(
            () =>
                (spectator = createComponent({
                    props: {
                        contentlet: contentletAreaMock
                    }
                }))
        );

        it("should have a drag image with the contentlet's contentType", () => {
            expect(spectator.query(byTestId('drag-image'))).toHaveText('test');
        });

        describe('events', () => {
            it('should emit delete on delete button click', () => {
                const deleteSpy = jest.spyOn(spectator.component.delete, 'emit');
                spectator.click('[data-testId="delete-button"]');
                expect(deleteSpy).toHaveBeenCalledWith(contentletAreaMock.payload);
            });

            it('should emit edit on edit button click', () => {
                const deleteSpy = jest.spyOn(spectator.component.edit, 'emit');
                spectator.click('[data-testId="edit-button"]');
                expect(deleteSpy).toHaveBeenCalledWith(contentletAreaMock.payload);
            });

            it('should emit move on move button drag', () => {
                const moveSpy = jest.spyOn(spectator.component.moveStart, 'emit');

                const dragButton = spectator.debugElement.query(
                    By.css('[data-testId="drag-button"]')
                );

                spectator.triggerEventHandler(dragButton, 'dragstart', {
                    dataTransfer: {
                        setDragImage: jest.fn()
                    }
                });

                expect(moveSpy).toHaveBeenCalledWith(contentletAreaMock.payload);
            });

            it('should emit dragStop', () => {
                const dragStopSpy = jest.spyOn(spectator.component.moveStop, 'emit');
                const dragButton = spectator.debugElement.query(
                    By.css('[data-testId="drag-button"]')
                );
                spectator.triggerEventHandler(dragButton, 'dragend', {});
                expect(dragStopSpy).toHaveBeenCalled();
            });

            describe('top button', () => {
                it('should open menu on add button click', () => {
                    spectator.click('[data-testId="add-top-button"]');
                    expect(spectator.query('.p-menu-overlay')).not.toBeNull();
                });

                it('should call addContent on Content option click', () => {
                    const addSpy = jest.spyOn(spectator.component.addContent, 'emit');
                    spectator.click('[data-testId="add-top-button"]');
                    spectator.click(byText('Content'));
                    expect(addSpy).toHaveBeenCalledWith({
                        ...contentletAreaMock.payload,
                        position: 'before'
                    } as ActionPayload);
                });

                it('should call addForm on Form option click', () => {
                    const addSpy = jest.spyOn(spectator.component.addForm, 'emit');
                    spectator.click('[data-testId="add-top-button"]');
                    spectator.click(byText('Form'));
                    expect(addSpy).toHaveBeenCalledWith({
                        ...contentletAreaMock.payload,
                        position: 'before'
                    } as ActionPayload);
                });

                it('should call addWidget on Widget option click', () => {
                    const addSpy = jest.spyOn(spectator.component.addWidget, 'emit');
                    spectator.click('[data-testId="add-top-button"]');
                    spectator.click(byText('Widget'));
                    expect(addSpy).toHaveBeenCalledWith({
                        ...contentletAreaMock.payload,
                        position: 'before'
                    } as ActionPayload);
                });
            });

            describe('bottom button', () => {
                it('should open menu on button click', () => {
                    spectator.click('[data-testId="add-bottom-button"]');
                    expect(spectator.query('.p-menu-overlay')).not.toBeNull();
                });

                it('should call addContent on Content option click', () => {
                    const addSpy = jest.spyOn(spectator.component.addContent, 'emit');
                    spectator.click('[data-testId="add-bottom-button"]');
                    spectator.click(byText('Content'));
                    expect(addSpy).toHaveBeenCalledWith({
                        ...contentletAreaMock.payload,
                        position: 'after'
                    } as ActionPayload);
                });
                it('should call addForm on Form option click', () => {
                    const addSpy = jest.spyOn(spectator.component.addForm, 'emit');
                    spectator.click('[data-testId="add-bottom-button"]');
                    spectator.click(byText('Form'));
                    expect(addSpy).toHaveBeenCalledWith({
                        ...contentletAreaMock.payload,
                        position: 'after'
                    } as ActionPayload);
                });
                it('should call addWidget on Widget option click', () => {
                    const addSpy = jest.spyOn(spectator.component.addWidget, 'emit');
                    spectator.click('[data-testId="add-bottom-button"]');
                    spectator.click(byText('Widget'));
                    expect(addSpy).toHaveBeenCalledWith({
                        ...contentletAreaMock.payload,
                        position: 'after'
                    } as ActionPayload);
                });
            });
        });

        describe('position', () => {
            it('should set position for bounds div', () => {
                const bounds = spectator.query(byTestId('bounds'));
                expect(bounds).toHaveStyle({
                    left: '100px',
                    top: '100px',
                    width: '500px',
                    height: '100px'
                });
            });

            it('should set center position for top button', () => {
                const topButton = spectator.query(byTestId('add-top-button'));
                expect(topButton).toHaveStyle({
                    position: 'absolute',
                    left: '330px',
                    top: '80px',
                    zIndex: '1'
                });
            });

            it('should set center position for bottom button', () => {
                const topButton = spectator.query(byTestId('add-bottom-button'));
                expect(topButton).toHaveStyle({
                    position: 'absolute',
                    top: '180px',
                    left: '330px',
                    zIndex: '1'
                });
            });

            it('should set right position for actions', () => {
                const topButton = spectator.query(byTestId('actions'));
                expect(topButton).toHaveStyle({
                    position: 'absolute',
                    left: '464px',
                    top: '80px',
                    zIndex: '1'
                });
            });
        });
    });

    describe('small contentlet', () => {
        beforeEach(
            () =>
                (spectator = createComponent({
                    props: {
                        contentlet: {
                            ...contentletAreaMock,
                            width: 180
                        }
                    }
                }))
        );

        describe('position', () => {
            it('should set left position for top button', () => {
                const topButton = spectator.query(byTestId('add-top-button'));
                expect(topButton).toHaveStyle({
                    position: 'absolute',
                    left: '108px',
                    top: '80px',
                    zIndex: '1'
                });
            });

            it('should set center position for bottom button', () => {
                const topButton = spectator.query(byTestId('add-bottom-button'));
                expect(topButton).toHaveStyle({
                    position: 'absolute',
                    top: '180px',
                    left: '170px',
                    zIndex: '1'
                });
            });
        });
    });

    describe('empty container', () => {
        beforeEach(
            () =>
                (spectator = createComponent({
                    props: {
                        contentlet: {
                            ...contentletAreaMock,
                            width: 180,
                            payload: {
                                contentlet: {
                                    identifier: 'TEMP_EMPTY_CONTENTLET',
                                    inode: 'Fake inode',
                                    title: 'Fake title',
                                    contentType: 'Fake content type'
                                },
                                container: undefined,
                                language_id: '1',
                                pageContainers: [],
                                pageId: '1',
                                position: 'after'
                            }
                        }
                    }
                }))
        );

        it('should render only add button', () => {
            expect(spectator.query(byTestId('add-top-button'))).toBeDefined();
            expect(spectator.query(byTestId('add-bottom-button'))).toBeNull();
            expect(spectator.query(byTestId('actions'))).toBeNull();
        });
    });
});
