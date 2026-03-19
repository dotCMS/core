/* eslint-disable @typescript-eslint/no-explicit-any */
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, signalStoreFeature, withState } from '@ngrx/signals';

import { withFieldVisibility } from './field-visibility.feature';

import { initialRootState } from '../../edit-content.store';

describe('FieldVisibilityFeature', () => {
    let spectator: SpectatorService<any>;
    let store: any;

    const withTest = () => signalStoreFeature(withState({ ...initialRootState }));

    const createStore = createServiceFactory({
        service: signalStore(withTest(), withFieldVisibility())
    });

    beforeEach(() => {
        spectator = createStore();
        store = spectator.service;
    });

    describe('initial state', () => {
        it('should have an empty hiddenFields record', () => {
            expect(store.hiddenFields()).toEqual({});
        });
    });

    describe('setFieldVisibility', () => {
        it('should add a field to hiddenFields when visible is false', () => {
            store.setFieldVisibility('myField', false);
            expect(store.hiddenFields()['myField']).toBe(true);
        });

        it('should remove a field from hiddenFields when visible is true', () => {
            store.setFieldVisibility('myField', false);
            expect(store.hiddenFields()['myField']).toBe(true);

            store.setFieldVisibility('myField', true);
            expect(store.hiddenFields()['myField']).toBeUndefined();
        });

        it('should handle multiple fields independently', () => {
            store.setFieldVisibility('fieldA', false);
            store.setFieldVisibility('fieldB', false);

            expect(store.hiddenFields()['fieldA']).toBe(true);
            expect(store.hiddenFields()['fieldB']).toBe(true);

            store.setFieldVisibility('fieldA', true);

            expect(store.hiddenFields()['fieldA']).toBeUndefined();
            expect(store.hiddenFields()['fieldB']).toBe(true);
        });

        it('should be a no-op when hiding an already hidden field', () => {
            store.setFieldVisibility('myField', false);
            const keysAfterFirst = Object.keys(store.hiddenFields()).length;

            store.setFieldVisibility('myField', false);
            expect(Object.keys(store.hiddenFields()).length).toBe(keysAfterFirst);
        });

        it('should be a no-op when showing an already visible field', () => {
            store.setFieldVisibility('myField', true);
            expect(Object.keys(store.hiddenFields()).length).toBe(0);
        });
    });
});
