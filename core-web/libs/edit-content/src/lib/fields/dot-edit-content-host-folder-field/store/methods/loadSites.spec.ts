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
