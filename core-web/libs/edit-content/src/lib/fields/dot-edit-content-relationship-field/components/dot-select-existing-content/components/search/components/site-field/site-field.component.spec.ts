import { createFakeEvent } from '@ngneat/spectator';
import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ReactiveFormsModule } from '@angular/forms';

import { TreeSelectModule } from 'primeng/treeselect';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { SiteFieldComponent } from './site-field.component';
import { SiteFieldStore } from './site-field.store';

import {
    TreeNodeItem,
    TreeNodeSelectItem
} from '../../../../../../../../models/dot-edit-content-host-folder-field.interface';
import { TruncatePathPipe } from '../../../../../../../../pipes/truncate-path.pipe';
import { DotEditContentService } from '../../../../../../../../services/dot-edit-content.service';

describe('SiteFieldComponent', () => {
    let spectator: Spectator<SiteFieldComponent>;
    let component: SiteFieldComponent;
    let store: InstanceType<typeof SiteFieldStore>;

    const messageServiceMock = new MockDotMessageService({
        'dot.file.relationship.dialog.search.language.failed': 'Failed to load languages'
    });

    const mockSites: TreeNodeItem[] = [
        {
            label: 'demo.dotcms.com',
            data: {
                id: '123',
                hostname: 'demo.dotcms.com',
                path: '',
                type: 'site'
            },
            icon: 'pi pi-globe',
            leaf: false,
            children: []
        }
    ];

    const mockFolders = {
        parent: {
            id: 'parent-id',
            hostName: 'demo.dotcms.com',
            path: '/parent',
            addChildrenAllowed: true
        },
        folders: [
            {
                label: 'folder1',
                data: {
                    id: 'folder1',
                    hostname: 'demo.dotcms.com',
                    path: 'folder1',
                    type: 'folder' as const
                },
                icon: 'pi pi-folder',
                leaf: true,
                children: []
            }
        ]
    };

    const createComponent = createComponentFactory({
        component: SiteFieldComponent,
        imports: [ReactiveFormsModule, TreeSelectModule, TruncatePathPipe, DotMessagePipe],
        componentProviders: [SiteFieldStore],
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            mockProvider(DotEditContentService, {
                getSitesTreePath: jest.fn().mockReturnValue(of(mockSites)),
                getFoldersTreeNode: jest.fn().mockReturnValue(of(mockFolders))
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
        component = spectator.component;
        store = spectator.inject(SiteFieldStore, true);
    });

    it('should create', () => {
        spectator.detectChanges();

        expect(component).toBeTruthy();
    });

    describe('Initial State', () => {
        it('should initialize with empty site control', () => {
            spectator.detectChanges();

            expect(component.siteControl.value).toBe('');
        });

        it('should load sites on init', () => {
            const loadSitesSpy = jest.spyOn(store, 'loadSites');

            spectator.detectChanges();

            expect(loadSitesSpy).toHaveBeenCalled();
        });
    });

    describe('ViewChild Access', () => {
        it('should access TreeSelect component', () => {
            spectator.detectChanges();

            const treeSelect = component.$treeSelect();
            expect(treeSelect).toBeDefined();
        });
    });

    describe('Effects', () => {
        it('should call onChange when valueToSave changes', () => {
            const onChangeSpy = jest.fn();
            component.registerOnChange(onChangeSpy);

            spectator.detectChanges();

            const mockEvent: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node: {
                    label: 'Test Node',
                    data: { id: '123', hostname: 'test.com', path: 'test', type: 'folder' }
                }
            };

            store.chooseNode(mockEvent);
            spectator.detectChanges();

            expect(onChangeSpy).toHaveBeenCalledWith('folder:123');
        });

        it('should call onChange with empty string when valueToSave is null', () => {
            const onChangeSpy = jest.fn();
            component.registerOnChange(onChangeSpy);

            spectator.detectChanges();

            store.clearSelection();
            spectator.detectChanges();

            expect(onChangeSpy).toHaveBeenCalledWith('');
        });

        it('should update TreeSelect when nodeExpanded changes', () => {
            spectator.detectChanges();

            const treeSelect = component.$treeSelect();
            const mockTreeViewChild = {
                updateSerializedValue: jest.fn()
            };
            const mockCd = {
                detectChanges: jest.fn()
            };

            // Mock the treeViewChild and cd properties
            Object.defineProperty(treeSelect, 'treeViewChild', {
                value: mockTreeViewChild,
                writable: true
            });
            Object.defineProperty(treeSelect, 'cd', {
                value: mockCd,
                writable: true
            });

            const mockEvent: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node: {
                    label: 'Parent',
                    data: {
                        id: 'parent-id',
                        hostname: 'demo.dotcms.com',
                        path: 'parent',
                        type: 'folder' as const
                    },
                    icon: 'pi pi-folder',
                    leaf: false,
                    children: []
                }
            };

            store.loadChildren(mockEvent);
            spectator.detectChanges();

            expect(mockTreeViewChild.updateSerializedValue).toHaveBeenCalled();
            expect(mockCd.detectChanges).toHaveBeenCalled();
        });

        it('should not update TreeSelect when treeViewChild is not available', () => {
            spectator.detectChanges();

            const treeSelect = component.$treeSelect();

            // Ensure treeViewChild is null
            Object.defineProperty(treeSelect, 'treeViewChild', {
                value: null,
                writable: true
            });

            const mockEvent: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node: {
                    label: 'Parent',
                    data: {
                        id: 'parent-id',
                        hostname: 'demo.dotcms.com',
                        path: 'parent',
                        type: 'folder' as const
                    },
                    icon: 'pi pi-folder',
                    leaf: false,
                    children: []
                }
            };

            expect(() => {
                store.loadChildren(mockEvent);
                spectator.detectChanges();
            }).not.toThrow();
        });
    });

    describe('ControlValueAccessor Implementation', () => {
        const testValue = 'test-site-id';

        it('should write value to form control', () => {
            spectator.detectChanges();

            component.writeValue(testValue);
            expect(component.siteControl.value).toBe('');
        });

        it('should clear selection when writeValue is called with empty string', () => {
            const clearSelectionSpy = jest.spyOn(store, 'clearSelection');
            spectator.detectChanges();

            component.writeValue('');

            expect(component.siteControl.value).toBe('');
            expect(clearSelectionSpy).toHaveBeenCalled();
        });

        it('should register onChange callback', () => {
            const onChangeSpy = jest.fn();
            component.registerOnChange(onChangeSpy);

            const mockEvent: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node: {
                    label: 'Test Node',
                    data: { id: '123', hostname: 'test.com', path: 'test', type: 'folder' }
                }
            };

            const expectedValue = `${mockEvent.node.data.type}:${mockEvent.node.data.id}`;

            store.chooseNode(mockEvent);
            spectator.detectChanges();

            expect(onChangeSpy).toHaveBeenCalledWith(expectedValue);
        });

        it('should register onTouched callback', () => {
            const onTouchedSpy = jest.fn();
            component.registerOnTouched(onTouchedSpy);

            // Trigger touched state
            component.siteControl.markAsTouched();
            spectator.detectChanges();

            // Note: Since we're not explicitly calling onTouched in the component,
            // this test mainly verifies the registration
            expect(onTouchedSpy).not.toHaveBeenCalled();
        });

        describe('Disabled State', () => {
            it('should disable the form control', () => {
                component.setDisabledState(true);
                expect(component.siteControl.disabled).toBe(true);
            });

            it('should enable the form control', () => {
                // First disable
                component.setDisabledState(true);
                expect(component.siteControl.disabled).toBe(true);

                // Then enable
                component.setDisabledState(false);
                expect(component.siteControl.disabled).toBe(false);
            });
        });
    });

    describe('Edge Cases', () => {
        it('should handle undefined value in writeValue', () => {
            component.writeValue(undefined);
            expect(component.siteControl.value).toBe('');
        });

        it('should handle null value in writeValue', () => {
            component.writeValue(null);
            expect(component.siteControl.value).toBe('');
        });

        it('should handle empty string in writeValue', () => {
            const clearSelectionSpy = jest.spyOn(store, 'clearSelection');

            component.writeValue('');

            expect(component.siteControl.value).toBe('');
            expect(clearSelectionSpy).toHaveBeenCalled();
        });

        it('should not emit change when writeValue is called with same value', () => {
            const onChangeSpy = jest.fn();
            component.registerOnChange(onChangeSpy);

            const testValue = 'test-value';
            component.writeValue(testValue);
            component.writeValue(testValue);

            expect(onChangeSpy).not.toHaveBeenCalled();
        });
    });
});
