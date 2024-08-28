import { Spectator, SpyObject, createComponentFactory } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';
import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { mockMatchMedia } from '@dotcms/utils-testing';

import { DotEditContentHostFolderFieldComponent } from './dot-edit-content-host-folder-field.component';
import { HostFolderFiledStore } from './store/host-folder-field.store';

import {
    TreeNodeItem,
    TreeNodeSelectItem
} from '../../models/dot-edit-content-host-folder-field.interface';
import { newFakeRxMethod, getRxMethodFake } from '../../utils/fake-rx-method';
import {
    HOST_FOLDER_TEXT_MOCK,
    TREE_SELECT_SITES_MOCK,
    TREE_SELECT_MOCK,
    createFormGroupDirectiveMock
} from '../../utils/mocks';

class MockHostFolderFiledStore {
    nodeSelected = signal<TreeNodeItem | null>(null);
    nodeExpaned = signal<TreeNodeSelectItem['node'] | null>(null);
    tree = signal<TreeNodeItem[]>([]);
    status = signal<'idle' | 'pending' | 'fulfilled' | { error: string }>('idle');

    iconClasses = signal(['']);

    loadSites = newFakeRxMethod();
    loadChildren = newFakeRxMethod();
    chooseNode = newFakeRxMethod();
}

type TypeMock = SpyObject<MockHostFolderFiledStore>;

describe('DotEditContentHostFolderFieldComponent', () => {
    let spectator: Spectator<DotEditContentHostFolderFieldComponent>;
    let component: DotEditContentHostFolderFieldComponent;
    let store: TypeMock;

    const createComponent = createComponentFactory({
        component: DotEditContentHostFolderFieldComponent,
        componentViewProviders: [
            { provide: ControlContainer, useValue: createFormGroupDirectiveMock() }
        ],
        componentProviders: [
            {
                provide: HostFolderFiledStore,
                useClass: MockHostFolderFiledStore
            }
        ],
        providers: [FormGroupDirective],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.setInput('field', { ...HOST_FOLDER_TEXT_MOCK });
        store = spectator.inject(HostFolderFiledStore, true) as unknown as TypeMock;
        component = spectator.component;
        component.formControl.setValue(null);
        mockMatchMedia();
    });

    it('should create the component', () => {
        spectator.detectChanges();
        expect(spectator.component).toBeTruthy();
        expect(store).toBeTruthy();
    });

    it('should show options', () => {
        store.tree.set(TREE_SELECT_SITES_MOCK);
        const spyloadSites = getRxMethodFake(store.loadSites);
        spectator.detectChanges();

        const options = component.store.tree();

        expect(options).toBe(TREE_SELECT_SITES_MOCK);
        expect(component.$treeSelect().options).toBe(TREE_SELECT_SITES_MOCK);
        expect(spyloadSites).toHaveBeenCalled();
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
        it('should show a root path', () => {
            store.tree.set(TREE_SELECT_SITES_MOCK);
            const nodeSelected = TREE_SELECT_SITES_MOCK[0];
            store.nodeSelected.set(nodeSelected);
            component.formControl.setValue(nodeSelected.key);
            spectator.detectChanges();
            expect(component.formControl.value).toBe('demo.dotcms.com');
            expect(component.pathControl.value.key).toBe(nodeSelected.key);
            expect(component.$treeSelect().value.label).toBe(nodeSelected.label);
        });
    });

    describe('The init value with the one levels', () => {
        it('should show a path selected', () => {
            store.tree.set(TREE_SELECT_MOCK);
            const nodeSelected = TREE_SELECT_MOCK[0].children[0];
            store.nodeSelected.set(nodeSelected);
            component.formControl.setValue(nodeSelected.label);
            spectator.detectChanges();

            expect(component.formControl.value).toBe('demo.dotcms.com/level1/');
            expect(component.pathControl.value.key).toBe(nodeSelected.key);
            expect(component.$treeSelect().value.label).toBe(nodeSelected.label);
        });
    });

    describe('The init value with the two levels', () => {
        it('should show a path selected', () => {
            store.tree.set(TREE_SELECT_MOCK);
            const nodeSelected = TREE_SELECT_MOCK[0].children[0].children[0];
            store.nodeSelected.set(nodeSelected);
            component.formControl.setValue(nodeSelected.label);
            spectator.detectChanges();

            expect(component.formControl.value).toBe('demo.dotcms.com/level1/child1/');
            expect(component.pathControl.value.key).toBe(nodeSelected.key);
            expect(component.$treeSelect().value.label).toBe(nodeSelected.label);
        });
    });
});
