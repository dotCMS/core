import { SpyObject, createSpyObject } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { loadSites, SYSTEM_HOST_NAME } from './loadSites';

import { DotEditContentService } from '../../../../services/dot-edit-content.service';
import { TREE_SELECT_SITES_MOCK } from '../../../../utils/mocks';
import { initialState } from '../host-folder-field.store';

describe('rxMethod: loadSites', () => {
    let Store = signalStore(withState(initialState));
    let store: InstanceType<typeof Store>;
    let service: SpyObject<DotEditContentService>;

    beforeEach(() => {
        service = createSpyObject(DotEditContentService);
        Store = signalStore(withState(initialState));
        store = new Store();
    });

    describe('System Host isRequired', () => {
        it('should include System Host when isRequired is false.', () => {
            service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
            const rxMethod = TestBed.runInInjectionContext(() => loadSites(store, service));
            const props = {
                path: '',
                isRequired: false
            };
            rxMethod(props);
            const hasSystemHost = store.tree().some((item) => item.label === SYSTEM_HOST_NAME);
            expect(hasSystemHost).toBe(true);
        });

        it('should not include System Host when isRequired is true.', () => {
            service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
            const rxMethod = TestBed.runInInjectionContext(() => loadSites(store, service));
            const props = {
                path: '',
                isRequired: true
            };
            rxMethod(props);
            const hasSystemHost = store.tree().some((item) => item.label === SYSTEM_HOST_NAME);
            expect(hasSystemHost).toBe(false);
        });
    });

    describe('when path is not empty', () => {
        it('should select the node if the path is not empty and not required', () => {
            const node = TREE_SELECT_SITES_MOCK[0];
            service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
            const rxMethod = TestBed.runInInjectionContext(() => loadSites(store, service));
            const props = {
                path: node.label,
                isRequired: false
            };
            rxMethod(props);
            expect(service.getCurrentSite).not.toHaveBeenCalled();
            expect(store.nodeSelected().key).toBe(node.key);
        });

        it('should select the node if the path is not empty and is required', () => {
            const node = TREE_SELECT_SITES_MOCK[0];
            service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
            const rxMethod = TestBed.runInInjectionContext(() => loadSites(store, service));
            const props = {
                path: node.label,
                isRequired: true
            };
            rxMethod(props);
            expect(service.getCurrentSite).not.toHaveBeenCalled();
            expect(store.nodeSelected().key).toBe(node.key);
        });
    });

    describe('when path is empty', () => {
        it('should select System Host if the path is not empty and not required', () => {
            service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
            const rxMethod = TestBed.runInInjectionContext(() => loadSites(store, service));
            const props = {
                path: null,
                isRequired: false
            };
            rxMethod(props);
            expect(service.getCurrentSite).not.toHaveBeenCalled();
            expect(store.nodeSelected().label).toBe(SYSTEM_HOST_NAME);
        });

        it('should select current site if the path is not empty and is required', () => {
            const hostNode = TREE_SELECT_SITES_MOCK[1];
            service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
            service.getCurrentSite.mockReturnValue(of(hostNode));
            const rxMethod = TestBed.runInInjectionContext(() => loadSites(store, service));
            const props = {
                path: null,
                isRequired: true
            };
            rxMethod(props);
            expect(service.getCurrentSite).toHaveBeenCalled();
            expect(store.nodeSelected().label).toBe(hostNode.label);
        });
    });
});
