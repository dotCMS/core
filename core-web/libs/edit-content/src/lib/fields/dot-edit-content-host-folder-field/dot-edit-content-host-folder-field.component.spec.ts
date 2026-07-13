import {
    byTestId,
    createHostFactory,
    mockProvider,
    SpectatorHost,
    SpyObject
} from '@openng/spectator/jest';
import { of } from 'rxjs';

import { Component } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { ComponentStatus, DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotBrowsingService } from '@dotcms/ui';
import { createFakeContentlet, mockMatchMedia } from '@dotcms/utils-testing';

import { DotHostFolderFieldComponent } from './components/host-folder-field/host-folder-field.component';
import { DotEditContentHostFolderFieldComponent } from './dot-edit-content-host-folder-field.component';
import { HostFolderFiledStore } from './store/host-folder-field.store';

import { HOST_FOLDER_TEXT_MOCK, TREE_SELECT_SITES_MOCK } from '../../utils/mocks';

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props
    formGroup: FormGroup;
    field: DotCMSContentTypeField;
    contentlet: DotCMSContentlet;
}

describe('DotEditContentHostFolderFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentHostFolderFieldComponent, MockFormComponent>;
    let store: InstanceType<typeof HostFolderFiledStore>;
    let service: SpyObject<DotBrowsingService>;
    let hostFormControl: FormControl;
    let field: DotHostFolderFieldComponent;

    const createHost = createHostFactory({
        component: DotEditContentHostFolderFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        providers: [
            HostFolderFiledStore,
            mockProvider(DotHttpErrorManagerService, {
                handle: jest.fn()
            }),
            mockProvider(DotBrowsingService, {
                getSitesTreePath: jest.fn(() => of(TREE_SELECT_SITES_MOCK)),
                getCurrentSiteAsTreeNodeItem: jest.fn(() => of(TREE_SELECT_SITES_MOCK[0])),
                buildTreeByPaths: jest.fn(() =>
                    of({ node: TREE_SELECT_SITES_MOCK[0], tree: null })
                ),
                searchFolders: jest.fn(() =>
                    of({
                        folders: [],
                        pagination: { currentPage: 1, perPage: 40, totalEntries: 0 }
                    })
                )
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-host-folder-field [field]="field" [contentlet]="contentlet" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [HOST_FOLDER_TEXT_MOCK.variable]: new FormControl()
                    }),
                    field: HOST_FOLDER_TEXT_MOCK,
                    contentlet: createFakeContentlet({
                        [HOST_FOLDER_TEXT_MOCK.variable]: null
                    })
                }
            }
        );
        field = spectator.query(DotHostFolderFieldComponent);
        store = field.store;
        service = spectator.inject(DotBrowsingService);
        hostFormControl = spectator.hostComponent.formGroup.get(
            HOST_FOLDER_TEXT_MOCK.variable
        ) as FormControl;
        mockMatchMedia();
    });

    it('should create the component', () => {
        spectator.detectChanges();
        expect(store).toBeTruthy();
        expect(spectator.component).toBeTruthy();
    });

    it('should load sites into the store on init', () => {
        spectator.detectChanges();

        expect(service.getSitesTreePath).toHaveBeenCalled();
        expect(store.sites()).toBe(TREE_SELECT_SITES_MOCK);
    });

    it('should render the field trigger', () => {
        spectator.detectChanges();

        const trigger = spectator.query(byTestId('host-folder-trigger'));
        expect(trigger).toBeTruthy();
    });

    describe('Preselected value parity', () => {
        it('should select the root site when the persisted value is a site only', () => {
            const node = TREE_SELECT_SITES_MOCK[0];
            hostFormControl.setValue(node.label);
            spectator.detectChanges();

            expect(store.selectedSite()?.key).toBe(node.key);
            expect(store.confirmedNode()?.key).toBe(node.key);
            expect(hostFormControl.value).toBe(`${node.data.hostname}:/`);
        });

        it('should resolve and highlight a nested folder path via buildTreeByPaths', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            const nestedNode = {
                key: 'demo.dotcms.comapplicationapivtl',
                label: 'demo.dotcms.com/application/apivtl/',
                data: {
                    id: 'demo.dotcms.comapplicationapivtl',
                    hostname: site.data.hostname,
                    path: '/application/apivtl/',
                    type: 'folder' as const
                }
            };
            service.buildTreeByPaths.mockReturnValue(
                of({
                    node: nestedNode,
                    tree: {
                        path: '/',
                        folders: [nestedNode],
                        parent: {
                            hostName: site.data.hostname,
                            id: site.data.id,
                            path: '/',
                            addChildrenAllowed: true
                        }
                    }
                })
            );

            hostFormControl.setValue('//demo.dotcms.com/application/apivtl/');
            spectator.detectChanges();

            // The leading `//` is stripped before calling buildTreeByPaths so the folder
            // search API receives a well-formed `hostname/path/` value.
            expect(service.buildTreeByPaths).toHaveBeenCalledWith(
                'demo.dotcms.com',
                'demo.dotcms.com',
                '/application/apivtl/'
            );
            expect(store.selectedSite()?.key).toBe(site.key);
            expect(store.confirmedNode()?.key).toBe(nestedNode.key);
        });

        it('should keep the field functional when the persisted path references an unknown site', fakeAsync(() => {
            service.buildTreeByPaths.mockReturnValue(
                of({
                    node: {
                        key: 'unknown-node',
                        label: 'unknown-site.dotcms.com/nonexistent-folder/',
                        data: {
                            id: 'unknown-node',
                            hostname: 'unknown-site.dotcms.com',
                            path: '/nonexistent-folder/',
                            type: 'folder'
                        }
                    },
                    tree: {
                        path: '/',
                        folders: [],
                        parent: {
                            hostName: 'unknown-site.dotcms.com',
                            id: 'unknown-site-id',
                            path: '/',
                            addChildrenAllowed: true
                        }
                    }
                })
            );

            hostFormControl.setValue('unknown-site.dotcms.com/nonexistent-folder/');
            tick();
            spectator.detectChanges();

            expect(store.sitesStatus()).toBe(ComponentStatus.ERROR);
            expect(spectator.query(byTestId('host-folder-trigger'))).toBeTruthy();
            expect(field.$isDisabled()).toBe(false);
        }));
    });

    describe('Staged commit through the UI', () => {
        it('should only propagate the value to the form control after commit', () => {
            spectator.detectChanges();

            const site = TREE_SELECT_SITES_MOCK[1];
            field.onSiteSelect(site);
            expect(hostFormControl.value).not.toBe(`${site.data.hostname}:/`);

            field.onSelect();
            spectator.detectChanges();

            expect(hostFormControl.value).toBe(`${site.data.hostname}:/`);
        });
    });

    describe('Disabled State Management', () => {
        it('should sync disabled state from the main form control to the field', () => {
            spectator.detectChanges();
            expect(field.$isDisabled()).toBe(false);

            hostFormControl.disable();
            spectator.detectChanges();

            expect(field.$isDisabled()).toBe(true);
        });

        it('should sync enabled state from the main form control to the field', () => {
            hostFormControl.disable();
            spectator.detectChanges();
            expect(field.$isDisabled()).toBe(true);

            hostFormControl.enable();
            spectator.detectChanges();

            expect(field.$isDisabled()).toBe(false);
        });
    });
});
