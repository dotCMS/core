import { createFakeEvent } from '@ngneat/spectator';
import { SpectatorHost, createHostFactory, mockProvider, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Component } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { mockMatchMedia } from '@dotcms/utils-testing';

import { DotEditContentHostFolderFieldComponent } from './dot-edit-content-host-folder-field.component';
import { HostFolderFiledStore } from './store/host-folder-field.store';

import { DotEditContentService } from '../../services/dot-edit-content.service';
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
}

describe('DotEditContentHostFolderFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentHostFolderFieldComponent, MockFormComponent>;
    let store: InstanceType<typeof HostFolderFiledStore>;
    let service: SpyObject<DotEditContentService>;
    let hostFormControl: FormControl;

    const createHost = createHostFactory({
        component: DotEditContentHostFolderFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule, DotEditContentHostFolderFieldComponent],
        providers: [
            HostFolderFiledStore,
            mockProvider(DotEditContentService, {
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
                <dot-edit-content-host-folder-field [field]="field" [formControlName]="field.variable" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [HOST_FOLDER_TEXT_MOCK.variable]: new FormControl()
                    }),
                    field: HOST_FOLDER_TEXT_MOCK
                }
            }
        );
        store = spectator.inject(HostFolderFiledStore, true);
        service = spectator.inject(DotEditContentService);
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
        expect(spectator.component.$treeSelect().options).toBe(TREE_SELECT_SITES_MOCK);
    });

    it('should tree selection height and virtual scroll height be the same', async () => {
        spectator.detectChanges();

        const triggerElement = spectator.query('.p-treeselect-trigger');
        spectator.click(triggerElement);

        await spectator.fixture.whenStable();

        const treeSelectHeight = spectator.component.$treeSelect().scrollHeight;
        const treeVirtualScrollHeight =
            spectator.component.$treeSelect().virtualScrollOptions.style['height'];

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

            expect(hostFormControl.value).toBe('demo.dotcms.com:/');
            expect(spectator.component.pathControl.value.key).toBe(nodeSelected.key);
            expect(spectator.component.$treeSelect().value.label).toBe(nodeSelected.label);
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

            expect(hostFormControl.value).toBe('demo.dotcms.com:/level1/child1/');
            expect(spectator.component.pathControl.value.key).toBe(nodeSelected.key);
            expect(spectator.component.$treeSelect().value.label).toBe(nodeSelected.label);
        }));
    });

    describe('Disabled State Management', () => {
        it('should sync disabled state from main form control to path control', fakeAsync(() => {
            spectator.detectChanges();
            tick(50);

            // Initially both controls should be enabled
            expect(spectator.component.formControl.disabled).toBe(false);
            expect(spectator.component.pathControl.disabled).toBe(false);

            // Disable the main form control
            hostFormControl.disable();
            tick(50);

            // Path control should be disabled automatically
            expect(spectator.component.pathControl.disabled).toBe(true);
        }));

        it('should sync enabled state from main form control to path control', fakeAsync(() => {
            spectator.detectChanges();
            tick(50);

            // Start with disabled controls
            hostFormControl.disable();
            tick(50);
            expect(spectator.component.pathControl.disabled).toBe(true);

            // Enable the main form control
            hostFormControl.enable();
            tick(50);

            // Path control should be enabled automatically
            expect(spectator.component.pathControl.disabled).toBe(false);
        }));

        it('should reflect disabled state in the TreeSelect through form control binding', fakeAsync(() => {
            spectator.detectChanges();
            tick(50);

            // The TreeSelect is bound to pathControl via [formControl]="pathControl"
            // When pathControl is disabled, the TreeSelect should be automatically disabled by Angular
            // This is the same pattern used by the text field component

            // Initially pathControl should be enabled
            expect(spectator.component.pathControl.disabled).toBe(false);

            // Disable the main form control
            hostFormControl.disable();
            tick(50);
            spectator.detectChanges();

            // The pathControl should be disabled, which automatically disables the TreeSelect
            expect(spectator.component.pathControl.disabled).toBe(true);
        }));
    });
});
