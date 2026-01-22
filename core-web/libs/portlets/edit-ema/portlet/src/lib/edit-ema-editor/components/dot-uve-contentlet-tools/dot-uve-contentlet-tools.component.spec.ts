import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';
import { MockProvider } from 'ng-mocks';

import { DotMessageService } from '@dotcms/data-access';

import { DotUveContentletToolsComponent } from './dot-uve-contentlet-tools.component';

import { ContentletPayload, VTLFile } from '../../../shared/models';
import { ContentletArea } from '../ema-page-dropzone/types';

const MOCK_CONTENTLET_AREA: ContentletArea = {
    x: 100,
    y: 200,
    width: 300,
    height: 400,
    payload: {
        contentlet: {
            identifier: 'contentlet-identifier-123',
            inode: 'inode-123',
            title: 'Test Contentlet',
            contentType: 'test-content-type',
            baseType: 'CONTENT'
        },
        container: {
            acceptTypes: 'test',
            identifier: 'container-identifier-123',
            maxContentlets: 5,
            variantId: 'variant-123',
            uuid: 'uuid-123',
            contentletsId: ['contentlet-identifier-123']
        },
        language_id: '1',
        pageContainers: [],
        pageId: 'page-123',
        vtlFiles: [
            { inode: 'vtl-inode-1', name: 'template1.vtl' },
            { inode: 'vtl-inode-2', name: 'template2.vtl' }
        ]
    }
};

const MOCK_EMPTY_CONTENTLET_AREA: ContentletArea = {
    x: 100,
    y: 200,
    width: 300,
    height: 400,
    payload: {
        contentlet: {
            identifier: 'TEMP_EMPTY_CONTENTLET',
            inode: 'temp-inode',
            title: 'Empty',
            contentType: 'test'
        },
        container: {
            acceptTypes: 'test',
            identifier: 'container-identifier-123',
            maxContentlets: 5,
            variantId: 'variant-123',
            uuid: 'uuid-123'
        },
        language_id: '1',
        pageContainers: [],
        pageId: 'page-123'
    }
};

describe('DotUveContentletToolsComponent', () => {
    let spectator: Spectator<DotUveContentletToolsComponent>;

    const createComponent = createComponentFactory({
        component: DotUveContentletToolsComponent,
        providers: [
            MockProvider(DotMessageService, {
                get: (key: string) => {
                    const messages: Record<string, string> = {
                        content: 'Content',
                        Widget: 'Widget',
                        form: 'Form',
                        'uve.disable.delete.button.on.personalization':
                            'Cannot delete on personalization'
                    };

                    return messages[key] || key;
                }
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                isEnterprise: false,
                contentletArea: MOCK_CONTENTLET_AREA,
                allowContentDelete: true,
                showStyleEditorOption: false
            }
        });
        spectator.detectChanges();
    });

    describe('Rendering', () => {
        it('should create the component', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should render bounds container with correct styles', () => {
            const bounds = spectator.query(byTestId('bounds'));
            expect(bounds).toBeTruthy();

            const styles = (bounds as HTMLElement).style;
            expect(styles.left).toBe('100px');
            expect(styles.top).toBe('200px');
            expect(styles.width).toBe('300px');
            expect(styles.height).toBe('400px');
        });

        it('should render add buttons', () => {
            const addTopButton = spectator.query(byTestId('add-top-button'));
            const addBottomButton = spectator.query(byTestId('add-bottom-button'));

            expect(addTopButton).toBeTruthy();
            expect(addBottomButton).toBeTruthy();
        });

        it('should render actions container when not empty', () => {
            const actions = spectator.query(byTestId('actions'));
            expect(actions).toBeTruthy();
        });

        it('should NOT render actions container when container is empty', () => {
            spectator.setInput('contentletArea', MOCK_EMPTY_CONTENTLET_AREA);
            spectator.detectChanges();

            const actions = spectator.query(byTestId('actions'));
            expect(actions).toBeFalsy();
        });

        it('should NOT render bottom add button when container is empty', () => {
            spectator.setInput('contentletArea', MOCK_EMPTY_CONTENTLET_AREA);
            spectator.detectChanges();

            const addBottomButton = spectator.query(byTestId('add-bottom-button'));
            expect(addBottomButton).toBeFalsy();
        });
    });

    describe('Actions buttons', () => {
        it('should render edit VTL button when vtl files exist', () => {
            const editVtlButton = spectator.query(byTestId('edit-vtl-button'));
            expect(editVtlButton).toBeTruthy();
        });

        it('should NOT render edit VTL button when no vtl files', () => {
            const areaWithoutVtl = {
                ...MOCK_CONTENTLET_AREA,
                payload: { ...MOCK_CONTENTLET_AREA.payload, vtlFiles: undefined }
            };
            spectator.setInput('contentletArea', areaWithoutVtl);
            spectator.detectChanges();

            const editVtlButton = spectator.query(byTestId('edit-vtl-button'));
            expect(editVtlButton).toBeFalsy();
        });

        it('should render drag button', () => {
            const dragButton = spectator.query(byTestId('drag-button'));
            expect(dragButton).toBeTruthy();
        });

        it('should render delete button', () => {
            const deleteButton = spectator.query(byTestId('delete-button'));
            expect(deleteButton).toBeTruthy();
        });

        it('should render edit button', () => {
            const editButton = spectator.query(byTestId('edit-button'));
            expect(editButton).toBeTruthy();
        });

        it('should disable delete button when allowContentDelete is false', () => {
            spectator.setInput('allowContentDelete', false);
            spectator.detectChanges();

            const deleteButton = spectator.query(byTestId('delete-button')) as HTMLElement;
            const button = deleteButton.querySelector('button');
            expect(button?.disabled).toBe(true);
        });

        it('should enable delete button when allowContentDelete is true', () => {
            spectator.setInput('allowContentDelete', true);
            spectator.detectChanges();

            const deleteButton = spectator.query(byTestId('delete-button')) as HTMLElement;
            const button = deleteButton.querySelector('button');
            expect(button?.disabled).toBe(false);
        });
    });

    describe('Outputs', () => {
        describe('selectContent', () => {
            it('should emit selectContent when clicking palette button', () => {
                spectator.setInput('showStyleEditorOption', true);
                spectator.detectChanges();

                const paletteButton = spectator.query(byTestId('palette-button')) as Element;
                const handler = jest.fn();
                spectator.output('selectContent').subscribe(handler);
                spectator.click(paletteButton);
                expect(handler).toHaveBeenCalledWith(MOCK_CONTENTLET_AREA.payload);
            });
        });

        describe('editContent', () => {
            it('should emit editContent with context when clicking edit button', () => {
                const handler = jest.fn();
                spectator.output('editContent').subscribe(handler);

                const editButton = spectator.query(byTestId('edit-button')) as HTMLElement;
                spectator.click(editButton.querySelector('button') as Element);

                expect(handler).toHaveBeenCalledWith({
                    ...MOCK_CONTENTLET_AREA.payload,
                    position: 'after'
                });
            });
        });

        describe('deleteContent', () => {
            it('should emit deleteContent with context when clicking delete button', () => {
                const handler = jest.fn();
                spectator.output('deleteContent').subscribe(handler);

                const deleteButton = spectator.query(byTestId('delete-button')) as HTMLElement;
                spectator.click(deleteButton.querySelector('button') as Element);

                expect(handler).toHaveBeenCalledWith({
                    ...MOCK_CONTENTLET_AREA.payload,
                    position: 'after'
                });
            });
        });

        describe('addContent', () => {
            it('should emit addContent with type "content" when selecting content from menu', () => {
                const addTopButton = spectator.query(byTestId('add-top-button'));
                const button = addTopButton?.querySelector('button');
                spectator.click(button as Element);
                spectator.detectChanges();

                // Get the menu items and trigger the first command
                const handler = jest.fn();
                spectator.output('addContent').subscribe(handler);
                const menuItems = spectator.component.menuItems();
                menuItems[0].command?.({});

                expect(handler).toHaveBeenCalledWith({
                    type: 'content',
                    payload: {
                        ...MOCK_CONTENTLET_AREA.payload,
                        position: 'before'
                    }
                });
            });

            it('should emit addContent with type "widget" when selecting widget from menu', () => {
                const addBottomButton = spectator.query(byTestId('add-bottom-button'));
                const button = addBottomButton?.querySelector('button');
                spectator.click(button as Element);
                spectator.detectChanges();

                // Get the menu items and trigger the second command
                const handler = jest.fn();
                spectator.output('addContent').subscribe(handler);
                const menuItems = spectator.component.menuItems();
                menuItems[1].command?.({});

                expect(handler).toHaveBeenCalledWith({
                    type: 'widget',
                    payload: {
                        ...MOCK_CONTENTLET_AREA.payload,
                        position: 'after'
                    }
                });
            });

            it('should emit addContent with type "form" when enterprise and selecting form from menu', () => {
                spectator.setInput('isEnterprise', true);
                spectator.detectChanges();

                const addBottomButton = spectator.query(byTestId('add-bottom-button'));
                const button = addBottomButton?.querySelector('button');
                spectator.click(button as Element);
                spectator.detectChanges();

                // Get the menu items and trigger the third command (form)
                const handler = jest.fn();
                spectator.output('addContent').subscribe(handler);
                const menuItems = spectator.component.menuItems();
                menuItems[2].command?.({});

                expect(handler).toHaveBeenCalledWith({
                    type: 'form',
                    payload: {
                        ...MOCK_CONTENTLET_AREA.payload,
                        position: 'after'
                    }
                });
            });
        });

        describe('editVTL', () => {
            it('should emit editVTL with file when clicking VTL menu item', () => {
                const expectedFile: VTLFile = {
                    inode: 'vtl-inode-1',
                    name: 'template1.vtl'
                };

                const editVtlButton = spectator.query(byTestId('edit-vtl-button'));
                const button = editVtlButton?.querySelector('button');
                spectator.click(button as Element);
                spectator.detectChanges();

                // Get the VTL menu items and trigger the first command
                const handler = jest.fn();
                spectator.output('editVTL').subscribe(handler);
                const vtlMenuItems = spectator.component.vtlMenuItems();
                vtlMenuItems[0].command?.({});

                expect(handler).toHaveBeenCalledWith(expectedFile);
            });

            it('should emit editVTL with second file when clicking second VTL menu item', () => {
                const expectedFile: VTLFile = {
                    inode: 'vtl-inode-2',
                    name: 'template2.vtl'
                };

                const editVtlButton = spectator.query(byTestId('edit-vtl-button'));
                const button = editVtlButton?.querySelector('button');
                spectator.click(button as Element);
                spectator.detectChanges();

                // Get the VTL menu items and trigger the second command
                const handler = jest.fn();
                spectator.output('editVTL').subscribe(handler);
                const vtlMenuItems = spectator.component.vtlMenuItems();
                vtlMenuItems[1].command?.({});

                expect(handler).toHaveBeenCalledWith(expectedFile);
            });
        });
    });

    describe('Computed signals', () => {
        describe('contentContext', () => {
            it('should combine contentletArea payload with buttonPosition', () => {
                expect(spectator.component.contentContext()).toEqual({
                    ...MOCK_CONTENTLET_AREA.payload,
                    position: 'after'
                });
            });

            it('should update position to "before" when clicking top add button', () => {
                const addTopButton = spectator.query(byTestId('add-top-button'));
                const button = addTopButton?.querySelector('button');
                spectator.click(button as Element);
                spectator.detectChanges();

                expect(spectator.component.contentContext().position).toBe('before');
            });

            it('should update position to "after" when clicking bottom add button', () => {
                const addBottomButton = spectator.query(byTestId('add-bottom-button'));
                const button = addBottomButton?.querySelector('button');
                spectator.click(button as Element);
                spectator.detectChanges();

                expect(spectator.component.contentContext().position).toBe('after');
            });
        });

        describe('hasVtlFiles', () => {
            it('should return true when vtl files exist', () => {
                expect(spectator.component.hasVtlFiles()).toBe(true);
            });

            it('should return false when no vtl files', () => {
                const areaWithoutVtl = {
                    ...MOCK_CONTENTLET_AREA,
                    payload: { ...MOCK_CONTENTLET_AREA.payload, vtlFiles: undefined }
                };
                spectator.setInput('contentletArea', areaWithoutVtl);
                spectator.detectChanges();

                expect(spectator.component.hasVtlFiles()).toBe(false);
            });

            it('should return false when vtl files is empty array', () => {
                const areaWithEmptyVtl = {
                    ...MOCK_CONTENTLET_AREA,
                    payload: { ...MOCK_CONTENTLET_AREA.payload, vtlFiles: [] }
                };
                spectator.setInput('contentletArea', areaWithEmptyVtl);
                spectator.detectChanges();

                expect(spectator.component.hasVtlFiles()).toBe(false);
            });
        });

        describe('isContainerEmpty', () => {
            it('should return false for regular contentlet', () => {
                expect(spectator.component.isContainerEmpty()).toBe(false);
            });

            it('should return true when contentlet identifier is TEMP_EMPTY_CONTENTLET', () => {
                spectator.setInput('contentletArea', MOCK_EMPTY_CONTENTLET_AREA);
                spectator.detectChanges();

                expect(spectator.component.isContainerEmpty()).toBe(true);
            });
        });

        describe('delete button behavior', () => {
            it('should enable delete button when delete is allowed', () => {
                spectator.setInput('allowContentDelete', true);
                spectator.detectChanges();

                const deleteButton = spectator.query(byTestId('delete-button')) as HTMLElement;
                const button = deleteButton?.querySelector('button');

                expect(button?.disabled).toBe(false);
            });

            it('should disable delete button when delete is not allowed', () => {
                spectator.setInput('allowContentDelete', false);
                spectator.detectChanges();

                const deleteButton = spectator.query(byTestId('delete-button')) as HTMLElement;
                const button = deleteButton?.querySelector('button');

                expect(button?.disabled).toBe(true);
            });
        });

        describe('menuItems', () => {
            it('should have 3 items (content, widget, form)', () => {
                spectator.setInput('isEnterprise', false);
                spectator.detectChanges();

                const items = spectator.component.menuItems();
                expect(items).toHaveLength(3);
                expect(items[0].label).toBe('Content');
                expect(items[1].label).toBe('Widget');
                expect(items[2].label).toBe('Form');
            });
        });

        describe('vtlMenuItems', () => {
            it('should create menu items from vtl files', () => {
                const items = spectator.component.vtlMenuItems();
                expect(items).toHaveLength(2);
                expect(items[0].label).toBe('template1.vtl');
                expect(items[1].label).toBe('template2.vtl');
            });

            it('should return undefined when no vtl files', () => {
                const areaWithoutVtl = {
                    ...MOCK_CONTENTLET_AREA,
                    payload: { ...MOCK_CONTENTLET_AREA.payload, vtlFiles: undefined }
                };
                spectator.setInput('contentletArea', areaWithoutVtl);
                spectator.detectChanges();

                expect(spectator.component.vtlMenuItems()).toBeUndefined();
            });
        });

        describe('boundsStyles', () => {
            it('should apply correct inline styles from contentletArea dimensions', () => {
                const bounds = spectator.query(byTestId('bounds')) as HTMLElement;

                expect(bounds.style.left).toBe('100px');
                expect(bounds.style.top).toBe('200px');
                expect(bounds.style.width).toBe('300px');
                expect(bounds.style.height).toBe('400px');
            });

            it('should default to 0px when contentletArea values are undefined', () => {
                const areaWithUndefined = { ...MOCK_CONTENTLET_AREA, x: undefined };
                spectator.setInput('contentletArea', areaWithUndefined as ContentletArea);
                spectator.detectChanges();

                const bounds = spectator.query(byTestId('bounds')) as HTMLElement;
                expect(bounds.style.left).toBe('0px');
            });
        });

        describe('dragPayload', () => {
            it('should return valid drag payload when contentlet exists', () => {
                const payload = spectator.component.dragPayload();
                expect(payload).toEqual({
                    container: MOCK_CONTENTLET_AREA.payload.container,
                    contentlet: MOCK_CONTENTLET_AREA.payload.contentlet,
                    showLabelImage: true,
                    move: true
                });
            });

            it('should return null values when contentlet does not exist', () => {
                const areaWithoutContentlet = {
                    ...MOCK_CONTENTLET_AREA,
                    payload: {
                        ...MOCK_CONTENTLET_AREA.payload,
                        contentlet: undefined as unknown as ContentletPayload
                    }
                };
                spectator.setInput('contentletArea', areaWithoutContentlet);
                spectator.detectChanges();

                const payload = spectator.component.dragPayload();
                expect(payload).toEqual({
                    container: null,
                    contentlet: null,
                    showLabelImage: false,
                    move: false
                });
            });
        });
    });

    describe('Position flag behavior', () => {
        it('should emit addContent with "before" position when clicking top add button', () => {
            const handler = jest.fn();
            spectator.output('addContent').subscribe(handler);

            const addTopButton = spectator.query(byTestId('add-top-button'));
            const button = addTopButton?.querySelector('button');
            spectator.click(button as Element);
            spectator.detectChanges();

            // Get the menu items and trigger the first command
            const menuItems = spectator.component.menuItems();
            menuItems[0].command?.({});

            expect(handler).toHaveBeenCalledWith({
                type: 'content',
                payload: expect.objectContaining({
                    position: 'before'
                })
            });
        });

        it('should emit addContent with "after" position when clicking bottom add button', () => {
            const handler = jest.fn();
            spectator.output('addContent').subscribe(handler);

            const addBottomButton = spectator.query(byTestId('add-bottom-button'));
            const button = addBottomButton?.querySelector('button');
            spectator.click(button as Element);
            spectator.detectChanges();

            // Get the menu items and trigger the first command
            const menuItems = spectator.component.menuItems();
            menuItems[0].command?.({});

            expect(handler).toHaveBeenCalledWith({
                type: 'content',
                payload: expect.objectContaining({
                    position: 'after'
                })
            });
        });
    });

    describe('Style Editor Features', () => {
        describe('Palette button visibility', () => {
            it('should NOT render palette button when showStyleEditorOption is false', () => {
                spectator.setInput('showStyleEditorOption', false);
                spectator.detectChanges();

                const paletteButton = spectator.query(byTestId('palette-button'));
                expect(paletteButton).toBeFalsy();
            });

            it('should render palette button when showStyleEditorOption is true', () => {
                spectator.setInput('showStyleEditorOption', true);
                spectator.detectChanges();

                const paletteButton = spectator.query(byTestId('palette-button'));
                expect(paletteButton).toBeTruthy();
            });

            it('should hide palette button when showStyleEditorOption changes to false', () => {
                // First enable it
                spectator.setInput('showStyleEditorOption', true);
                spectator.detectChanges();

                let paletteButton = spectator.query(byTestId('palette-button'));
                expect(paletteButton).toBeTruthy();

                // Then disable it
                spectator.setInput('showStyleEditorOption', false);
                spectator.detectChanges();

                paletteButton = spectator.query(byTestId('palette-button'));
                expect(paletteButton).toBeFalsy();
            });

            it('should NOT render palette button when container is empty even if showStyleEditorOption is true', () => {
                spectator.setInput('showStyleEditorOption', true);
                spectator.setInput('contentletArea', MOCK_EMPTY_CONTENTLET_AREA);
                spectator.detectChanges();

                const paletteButton = spectator.query(byTestId('palette-button'));
                expect(paletteButton).toBeFalsy();
            });
        });
    });

    describe('Effect behavior', () => {
        it('should hide menus when contentletArea changes', () => {
            // Open a menu by clicking the add button
            const addTopButton = spectator.query(byTestId('add-top-button'));
            const button = addTopButton?.querySelector('button');
            spectator.click(button as Element);
            spectator.detectChanges();

            // Verify menu is open by checking if it exists in the DOM
            let menu = document.querySelector('.p-menu');
            expect(menu).toBeTruthy();

            // Change contentletArea - this should trigger the effect that hides menus
            const newArea = { ...MOCK_CONTENTLET_AREA, x: 500 };
            spectator.setInput('contentletArea', newArea);
            spectator.detectChanges();

            // Menu should be hidden now
            menu = document.querySelector('.p-menu.p-component-overlay-visible');
            expect(menu).toBeFalsy();
        });
    });

    describe('Drag attributes', () => {
        it('should set correct drag attributes on drag button', () => {
            const dragButton = spectator.query(byTestId('drag-button')) as HTMLElement;

            expect(dragButton?.getAttribute('draggable')).toBe('true');
            expect(dragButton?.getAttribute('data-type')).toBe('contentlet');
            expect(dragButton?.getAttribute('data-use-custom-drag-image')).toBe('true');
        });

        it('should include drag payload in data-item attribute', () => {
            const dragButton = spectator.query(byTestId('drag-button')) as HTMLElement;
            const dataItem = dragButton?.getAttribute('data-item');

            expect(dataItem).toBeTruthy();
            const parsedItem = JSON.parse(dataItem);
            expect(parsedItem.contentlet).toEqual(MOCK_CONTENTLET_AREA.payload.contentlet);
            expect(parsedItem.container).toEqual(MOCK_CONTENTLET_AREA.payload.container);
            expect(parsedItem.showLabelImage).toBe(true);
            expect(parsedItem.move).toBe(true);
        });
    });
});
