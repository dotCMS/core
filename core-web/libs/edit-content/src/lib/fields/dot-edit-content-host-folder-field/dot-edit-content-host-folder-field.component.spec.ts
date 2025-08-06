import { createFakeEvent } from '@ngneat/spectator';
import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { fakeAsync, tick } from '@angular/core/testing';
import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { mockMatchMedia } from '@dotcms/utils-testing';

import { DotEditContentHostFolderFieldComponent } from './dot-edit-content-host-folder-field.component';
import { HostFolderFiledStore } from './store/host-folder-field.store';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import {
    HOST_FOLDER_TEXT_MOCK,
    TREE_SELECT_SITES_MOCK,
    TREE_SELECT_MOCK,
    createFormGroupDirectiveMock
} from '../../utils/mocks';

describe('DotEditContentHostFolderFieldComponent', () => {
    let spectator: Spectator<DotEditContentHostFolderFieldComponent>;
    let store: InstanceType<typeof HostFolderFiledStore>;

    const createComponent = createComponentFactory({
        component: DotEditContentHostFolderFieldComponent,
        componentViewProviders: [
            { provide: ControlContainer, useValue: createFormGroupDirectiveMock() }
        ],
        componentProviders: [HostFolderFiledStore],
        providers: [
            FormGroupDirective,
            mockProvider(DotEditContentService, {
                getSitesTreePath: jest.fn(() => of(TREE_SELECT_SITES_MOCK))
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.setInput('field', { ...HOST_FOLDER_TEXT_MOCK });
        store = spectator.inject(HostFolderFiledStore, true);
        spectator.component.formControl.setValue(null);
        mockMatchMedia();
    });

    it('should create the component', () => {
        spectator.detectChanges();
        expect(spectator.component).toBeTruthy();
        expect(store).toBeTruthy();
    });

    it('should show options', () => {
        const loadSitesSpy = jest.spyOn(store, 'loadSites');

        spectator.component.ngOnInit();

        expect(store.tree()).toBe(TREE_SELECT_SITES_MOCK);
        expect(spectator.component.$treeSelect().options).toBe(TREE_SELECT_SITES_MOCK);
        expect(loadSitesSpy).toHaveBeenCalled();
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
            spectator.component.formControl.setValue(nodeSelected.key);
            spectator.detectChanges();

            tick(50);
            spectator.detectChanges();

            store.chooseNode({
                originalEvent: createFakeEvent('click'),
                node: nodeSelected
            });

            tick(50);
            spectator.detectChanges();

            expect(spectator.component.formControl.value).toBe('demo.dotcms.com:/');
            expect(spectator.component.pathControl.value.key).toBe(nodeSelected.key);
            expect(spectator.component.$treeSelect().value.label).toBe(nodeSelected.label);
        }));

        it('should show a path selected with the two levels', fakeAsync(() => {
            const nodeSelected = TREE_SELECT_MOCK[0].children[0].children[0];
            spectator.component.formControl.setValue(nodeSelected.label);
            spectator.detectChanges();

            tick(50);
            spectator.detectChanges();

            store.chooseNode({
                originalEvent: createFakeEvent('click'),
                node: nodeSelected
            });

            tick(50);
            spectator.detectChanges();

            expect(spectator.component.formControl.value).toBe('demo.dotcms.com:/level1/child1/');
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
            spectator.component.formControl.disable();
            tick(50);

            // Path control should be disabled automatically
            expect(spectator.component.pathControl.disabled).toBe(true);
        }));

        it('should sync enabled state from main form control to path control', fakeAsync(() => {
            spectator.detectChanges();
            tick(50);

            // Start with disabled controls
            spectator.component.formControl.disable();
            tick(50);
            expect(spectator.component.pathControl.disabled).toBe(true);

            // Enable the main form control
            spectator.component.formControl.enable();
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
            spectator.component.formControl.disable();
            tick(50);
            spectator.detectChanges();

            // The pathControl should be disabled, which automatically disables the TreeSelect
            expect(spectator.component.pathControl.disabled).toBe(true);
        }));
    });
});
