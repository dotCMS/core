import { patchState, signalStore, withState, withMethods, withComputed } from '@ngrx/signals';

import { computed } from '@angular/core';

export interface GlobalState {
    user: { name: string; email: string } | null;
}

const initialState: GlobalState = {
    user: null
};

export const GlobalStore = signalStore(
    { providedIn: 'root' },
    withState(initialState),
    withMethods((store) => ({
        login(user: { name: string; email: string }) {
            patchState(store, { user });
        }
    })),
    withComputed(({ user }) => ({
        isLoggedIn: computed(() => user() != null)
    }))
);
