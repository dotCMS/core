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
        it('should have an empty hiddenFields set', () => {
            expect(store.hiddenFields()).toEqual(new Set());
        });
    });

    describe('setFieldVisibility', () => {
        it('should add a field to hiddenFields when visible is false', () => {
            store.setFieldVisibility('myField', false);
            expect(store.hiddenFields().has('myField')).toBe(true);
        });

        it('should remove a field from hiddenFields when visible is true', () => {
            store.setFieldVisibility('myField', false);
            expect(store.hiddenFields().has('myField')).toBe(true);

            store.setFieldVisibility('myField', true);
            expect(store.hiddenFields().has('myField')).toBe(false);
        });

        it('should handle multiple fields independently', () => {
            store.setFieldVisibility('fieldA', false);
            store.setFieldVisibility('fieldB', false);

            expect(store.hiddenFields().has('fieldA')).toBe(true);
            expect(store.hiddenFields().has('fieldB')).toBe(true);

            store.setFieldVisibility('fieldA', true);

            expect(store.hiddenFields().has('fieldA')).toBe(false);
            expect(store.hiddenFields().has('fieldB')).toBe(true);
        });

        it('should be a no-op when hiding an already hidden field', () => {
            store.setFieldVisibility('myField', false);
            const sizeAfterFirst = store.hiddenFields().size;

            store.setFieldVisibility('myField', false);
            expect(store.hiddenFields().size).toBe(sizeAfterFirst);
        });

        it('should be a no-op when showing an already visible field', () => {
            store.setFieldVisibility('myField', true);
            expect(store.hiddenFields().size).toBe(0);
        });
    });

    describe('isFieldHidden', () => {
        it('should return false for a visible field', () => {
            expect(store.isFieldHidden('myField')).toBe(false);
        });

        it('should return true for a hidden field', () => {
            store.setFieldVisibility('myField', false);
            expect(store.isFieldHidden('myField')).toBe(true);
        });

        it('should return false after showing a previously hidden field', () => {
            store.setFieldVisibility('myField', false);
            store.setFieldVisibility('myField', true);
            expect(store.isFieldHidden('myField')).toBe(false);
        });
    });

    describe('resetFieldVisibility', () => {
        it('should clear all hidden fields', () => {
            store.setFieldVisibility('fieldA', false);
            store.setFieldVisibility('fieldB', false);
            expect(store.hiddenFields().size).toBe(2);

            store.resetFieldVisibility();
            expect(store.hiddenFields().size).toBe(0);
        });

        it('should make isFieldHidden return false for all fields after reset', () => {
            store.setFieldVisibility('fieldA', false);
            store.setFieldVisibility('fieldB', false);

            store.resetFieldVisibility();

            expect(store.isFieldHidden('fieldA')).toBe(false);
            expect(store.isFieldHidden('fieldB')).toBe(false);
        });
    });
});
