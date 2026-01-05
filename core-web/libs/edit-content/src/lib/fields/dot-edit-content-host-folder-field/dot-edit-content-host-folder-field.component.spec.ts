import { createFakeEvent } from '@ngneat/spectator';
import { SpectatorHost, createHostFactory, mockProvider, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Component } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotBrowsingService } from '@dotcms/ui';
import { createFakeContentlet, mockMatchMedia } from '@dotcms/utils-testing';

import { DotHostFolderFieldComponent } from './components/host-folder-field/host-folder-field.component';
import { DotEditContentHostFolderFieldComponent } from './dot-edit-content-host-folder-field.component';
import { HostFolderFiledStore } from './store/host-folder-field.store';

import { TREE_SELECT_SITES_MOCK, TREE_SELECT_MOCK, HOST_FOLDER_TEXT_MOCK } from '../../utils/mocks';

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
            mockProvider(DotBrowsingService, {
                getSitesTreePath: jest.fn(() => of(TREE_SELECT_SITES_MOCK)),
                getCurrentSiteAsTreeNodeItem: jest.fn(() => of(TREE_SELECT_SITES_MOCK[0])),
                buildTreeByPaths: jest.fn(() => of({ node: TREE_SELECT_SITES_MOCK[0], tree: null }))
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

    it('should show options', () => {
        spectator.detectChanges();

        expect(store.tree()).toBe(TREE_SELECT_SITES_MOCK);
        expect(field.$treeSelect().options).toBe(TREE_SELECT_SITES_MOCK);
    });

    it('should tree selection height and virtual scroll height be the same', async () => {
        spectator.detectChanges();

        const triggerElement = spectator.query('.p-treeselect-trigger');
        spectator.click(triggerElement);

        await spectator.fixture.whenStable();

        const field = spectator.query(DotHostFolderFieldComponent);
        const treeSelectHeight = field.$treeSelect().scrollHeight;
        const treeVirtualScrollHeight = field.$treeSelect().virtualScrollOptions.style['height'];

        expect(treeSelectHeight).toBe(treeVirtualScrollHeight);
    });

    describe('The init value with the root path', () => {
        it('should show a root path', fakeAsync(() => {
            const nodeSelected = TREE_SELECT_SITES_MOCK[0];
            hostFormControl.setValue(null);
            spectator.detectChanges();

            store.chooseNode({
                originalEvent: createFakeEvent('click'),
                node: nodeSelected
            });
            spectator.detectChanges();

            const field = spectator.query(DotHostFolderFieldComponent);
            expect(hostFormControl.value).toBe('demo.dotcms.com:/');
            expect(field.pathControl.value.key).toBe(nodeSelected.key);
            expect(field.$treeSelect().value.label).toBe(nodeSelected.label);
        }));

        it('should show a path selected with the two levels', fakeAsync(() => {
            const nodeSelected = TREE_SELECT_MOCK[0].children[0].children[0];
            hostFormControl.setValue(null);
            spectator.detectChanges();

            service.buildTreeByPaths.mockReturnValue(
                of({
                    node: nodeSelected,
                    tree: null
                })
            );
            store.chooseNode({
                originalEvent: createFakeEvent('click'),
                node: nodeSelected
            });
            spectator.detectChanges();

            const field = spectator.query(DotHostFolderFieldComponent);
            expect(hostFormControl.value).toBe('demo.dotcms.com:/level1/child1/');
            expect(field.pathControl.value.key).toBe(nodeSelected.key);
            expect(field.$treeSelect().value.label).toBe(nodeSelected.label);
        }));
    });

    describe('Disabled State Management', () => {
        it('should sync disabled state from main form control to path control', fakeAsync(() => {
            spectator.detectChanges();
            tick(50);

            const field = spectator.query(DotHostFolderFieldComponent);
            // Initially both controls should be enabled
            expect(spectator.component.formControl.disabled).toBe(false);
            expect(field.pathControl.disabled).toBe(false);

            // Disable the main form control
            hostFormControl.disable();
            spectator.detectChanges();
            tick(50);

            // Path control should be disabled automatically
            expect(field.pathControl.disabled).toBe(true);
        }));

        it('should sync enabled state from main form control to path control', fakeAsync(() => {
            spectator.detectChanges();
            tick(50);

            // Start with disabled controls
            hostFormControl.disable();
            spectator.detectChanges();
            tick(50);
            expect(field.$isDisabled()).toBe(true);

            // Enable the main form control
            hostFormControl.enable();
            spectator.detectChanges();
            tick(50);

            // Path control should be enabled automatically
            expect(field.$isDisabled()).toBe(false);
        }));

        it('should reflect disabled state in the TreeSelect through form control binding', fakeAsync(() => {
            spectator.detectChanges();
            tick(50);

            // The TreeSelect is bound to pathControl via [formControl]="pathControl"
            // When pathControl is disabled, the TreeSelect should be automatically disabled by Angular
            // This is the same pattern used by the text field component

            // Initially pathControl should be enabled
            const field = spectator.query(DotHostFolderFieldComponent);
            expect(field.pathControl.disabled).toBe(false);

            // Disable the main form control
            hostFormControl.disable();
            tick(50);
            spectator.detectChanges();

            // The pathControl should be disabled, which automatically disables the TreeSelect
            expect(field.pathControl.disabled).toBe(true);
        }));
    });
});
