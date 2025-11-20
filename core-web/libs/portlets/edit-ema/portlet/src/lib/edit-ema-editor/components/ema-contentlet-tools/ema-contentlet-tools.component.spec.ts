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
        contentletsId: [],
        variantId: '123'
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
                        contentletArea: contentletAreaMock,
                        isEnterprise: false
                    }
                }))
        );

        it("should have a drag image with the contentlet's contentType", () => {
            expect(spectator.query(byTestId('drag-image'))).toHaveText('test');
        });

        it('should close menus when contentlet @input was changed', () => {
            const spyHideMenus = jest.spyOn(spectator.component, 'hideMenus');

            const hideMenu = jest.spyOn(spectator.component.menu, 'hide');
            // Open menu
            spectator.click(byTestId('menu-add'));

            //Change contentlet hover
            spectator.setInput('contentletArea', {
                ...contentletAreaMock,
                payload: {
                    ...contentletAreaMock.payload,
                    contentlet: {
                        ...contentletAreaMock.payload.contentlet,
                        identifier: 'new-identifier'
                    }
                }
            });

            expect(spyHideMenus).toHaveBeenCalled();
            expect(hideMenu).toHaveBeenCalled();
        });

        describe('events', () => {
            it('should emit delete on delete button click', () => {
                const deleteSpy = jest.spyOn(spectator.component.delete, 'emit');
                spectator.click(byTestId('delete-button'));
                expect(deleteSpy).toHaveBeenCalledWith(contentletAreaMock.payload);
            });

            it('should emit edit on edit button click', () => {
                const deleteSpy = jest.spyOn(spectator.component.edit, 'emit');
                spectator.click(byTestId('edit-button'));
                expect(deleteSpy).toHaveBeenCalledWith(contentletAreaMock.payload);
            });

            it('should set drag image', () => {
                const dragButton = spectator.debugElement.query(
                    By.css('[data-testId="drag-button"]')
                );

                const dragImageSpy = jest.fn();

                spectator.triggerEventHandler(dragButton, 'dragstart', {
                    dataTransfer: {
                        setDragImage: dragImageSpy
                    }
                });

                expect(dragImageSpy).toHaveBeenCalled();
            });

            describe('top button', () => {
                it('should open menu on add button click', () => {
                    spectator.click(byTestId('add-top-button'));
                    expect(spectator.query('.p-menu-overlay')).not.toBeNull();
                });

                it('should call addContent on Content option click', () => {
                    const addSpy = jest.spyOn(spectator.component.addContent, 'emit');
                    spectator.click(byTestId('add-top-button'));
                    spectator.click(byText('Content'));
                    expect(addSpy).toHaveBeenCalledWith({
                        ...contentletAreaMock.payload,
                        position: 'before'
                    } as ActionPayload);
                });

                it('should not call addForm on Form option click', () => {
                    spectator.click(byTestId('add-bottom-button'));
                    const formOption = spectator.query(byText('Form'));
                    expect(formOption).toBeNull();
                });

                it('should call addWidget on Widget option click', () => {
                    const addSpy = jest.spyOn(spectator.component.addWidget, 'emit');
                    spectator.click(byTestId('add-top-button'));
                    spectator.click(byText('Widget'));
                    expect(addSpy).toHaveBeenCalledWith({
                        ...contentletAreaMock.payload,
                        position: 'before'
                    } as ActionPayload);
                });

                describe('isEnterprise', () => {
                    beforeEach(
                        () =>
                            (spectator = createComponent({
                                props: {
                                    contentletArea: contentletAreaMock,
                                    isEnterprise: true
                                }
                            }))
                    );

                    it('should render form option', () => {
                        spectator.click(byTestId('add-top-button'));
                        expect(spectator.query(byText('Form'))).toBeDefined();
                    });

                    it('should call addForm on Form option click', () => {
                        const addSpy = jest.spyOn(spectator.component.addForm, 'emit');
                        spectator.click(byTestId('add-top-button'));
                        spectator.click(byText('Form'));
                        expect(addSpy).toHaveBeenCalledWith({
                            ...contentletAreaMock.payload,
                            position: 'before'
                        } as ActionPayload);
                    });
                });
            });

            describe('bottom button', () => {
                it('should open menu on button click', () => {
                    spectator.click(byTestId('add-bottom-button'));
                    expect(spectator.query('.p-menu-overlay')).not.toBeNull();
                });

                it('should call addContent on Content option click', () => {
                    const addSpy = jest.spyOn(spectator.component.addContent, 'emit');
                    spectator.click(byTestId('add-bottom-button'));
                    spectator.click(byText('Content'));
                    expect(addSpy).toHaveBeenCalledWith({
                        ...contentletAreaMock.payload,
                        position: 'after'
                    } as ActionPayload);
                });

                it('should not call addForm on Form option click', () => {
                    spectator.click(byTestId('add-bottom-button'));
                    const formOption = spectator.query(byText('Form'));
                    expect(formOption).toBeNull();
                });

                it('should call addWidget on Widget option click', () => {
                    const addSpy = jest.spyOn(spectator.component.addWidget, 'emit');
                    spectator.click(byTestId('add-bottom-button'));
                    spectator.click(byText('Widget'));
                    expect(addSpy).toHaveBeenCalledWith({
                        ...contentletAreaMock.payload,
                        position: 'after'
                    } as ActionPayload);
                });

                describe('isEnterprise', () => {
                    beforeEach(
                        () =>
                            (spectator = createComponent({
                                props: {
                                    contentletArea: contentletAreaMock,
                                    isEnterprise: true
                                }
                            }))
                    );

                    it('should render form option', () => {
                        spectator.click(byTestId('add-bottom-button'));
                        expect(spectator.query(byText('Form'))).toBeDefined();
                    });

                    it('should call addForm on Form option click', () => {
                        const addSpy = jest.spyOn(spectator.component.addForm, 'emit');
                        spectator.click(byTestId('add-bottom-button'));
                        spectator.click(byText('Form'));
                        expect(addSpy).toHaveBeenCalledWith({
                            ...contentletAreaMock.payload,
                            position: 'after'
                        } as ActionPayload);
                    });
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
                    zIndex: '1',
                    width: '128px'
                });
            });
        });

        describe('delete button', () => {
            it('should enable delete button when disableDeleteButton is false', () => {
                spectator.setInput('disableDeleteButton', null);
                spectator.detectChanges();
                // In Angular 20, ng-reflect-* attributes are not available
                // Verify the disabled property on the p-button component instance
                const deleteButtonDebugElement = spectator.debugElement.query(
                    By.css('[data-testId="delete-button"]')
                );
                const deleteButtonComponent = deleteButtonDebugElement?.componentInstance;
                expect(deleteButtonComponent?.disabled).toBe(false);
            });

            it('should disable delete button when disableDeleteButton is true', () => {
                spectator.setInput('disableDeleteButton', 'Cannot delete this contentlet');
                spectator.detectChanges();
                // In Angular 20, ng-reflect-* attributes are not available
                // Verify the disabled property on the p-button component instance
                const deleteButtonDebugElement = spectator.debugElement.query(
                    By.css('[data-testId="delete-button"]')
                );
                const deleteButtonComponent = deleteButtonDebugElement?.componentInstance;
                expect(deleteButtonComponent?.disabled).toBe(true);
            });
        });
    });

    describe('small contentlet', () => {
        beforeEach(
            () =>
                (spectator = createComponent({
                    props: {
                        contentletArea: {
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
                        contentletArea: {
                            ...contentletAreaMock,
                            width: 180,
                            payload: {
                                contentlet: {
                                    identifier: 'TEMP_EMPTY_CONTENTLET',
                                    inode: 'Fake inode',
                                    title: 'Fake title',
                                    contentType: 'Fake content type'
                                },
                                container: {
                                    uuid: '',
                                    acceptTypes: '',
                                    identifier: '',
                                    maxContentlets: 0,
                                    variantId: ''
                                },
                                language_id: '1',
                                pageContainers: [],
                                pageId: '1',
                                position: 'after'
                            }
                        }
                    }
                }))
        );

        it('should only render the add button', () => {
            expect(spectator.query(byTestId('add-top-button'))).toBeDefined();
            expect(spectator.query(byTestId('add-bottom-button'))).toBeNull();
            expect(spectator.query(byTestId('actions'))).toBeNull();
        });
    });

    describe('Contentlet outside container', () => {
        beforeEach(
            () =>
                (spectator = createComponent({
                    props: {
                        contentletArea: {
                            ...contentletAreaMock,
                            width: 180,
                            payload: {
                                contentlet: {
                                    identifier: 'contentlet-identifier-123',
                                    inode: 'contentlet-inode-123',
                                    title: 'Hello World',
                                    contentType: 'test'
                                },
                                container: null,
                                language_id: '1',
                                pageContainers: [],
                                pageId: '1',
                                position: 'after'
                            }
                        }
                    }
                }))
        );

        it('should only render the edit button', () => {
            expect(spectator.query(byTestId('edit-button'))).toBeDefined();

            const toBeNullTestIDs = [
                'add-top-button',
                'add-bottom-button',
                'menu-add',
                'delete-button',
                'drag-button',
                'edit-vtl-button',
                'menu-vtl'
            ];

            toBeNullTestIDs.forEach((testId) => {
                expect(spectator.query(byTestId(testId))).toBeNull();
            });
        });
    });

    describe('VTL contentlet', () => {
        beforeEach(
            () =>
                (spectator = createComponent({
                    props: {
                        contentletArea: {
                            ...contentletAreaMock,
                            payload: {
                                ...contentletAreaMock.payload,
                                vtlFiles: [
                                    {
                                        inode: '123',
                                        name: 'test.vtl'
                                    }
                                ]
                            }
                        }
                    }
                }))
        );

        it('should set right position for actions', () => {
            const topButton = spectator.query(byTestId('actions'));
            expect(topButton).toHaveStyle({
                position: 'absolute',
                left: '414px',
                top: '80px',
                zIndex: '1'
            });
        });
    });
});
