import { patchState, signalStore, withState, withMethods, withComputed } from '@ngrx/signals';

import { computed } from '@angular/core';

/**
 * Represents the global application state.
 *
 * This interface defines the structure of the global state that is shared
 * across the entire application. It contains user authentication information
 * and can be extended to include other global state properties.
 */
export interface GlobalState {
    /**
     * The currently authenticated user information.
     *
     * Contains the user's name and email address. Set to `null` when no user
     * is authenticated.
     */
    user: { name: string; email: string } | null;
}

/**
 * The initial state for the global store.
 *
 * This object represents the default state when the application starts,
 * with no user authenticated.
 */
const initialState: GlobalState = {
    user: null
};

/**
 * Global application store using NgRx Signals.
 *
 * This store manages global application state including user authentication.
 * It provides reactive state management with computed values and methods
 * for state updates.
 *
 * @example
 * ```typescript
 * // Inject the store in a component
 * private readonly globalStore = inject(GlobalStore);
 *
 * // Access state
 * const user = this.globalStore.user();
 * const isLoggedIn = this.globalStore.isLoggedIn();
 *
 * // Update state
 * this.globalStore.login({ name: 'John Doe', email: 'john@example.com' });
 * ```
 */
export const GlobalStore = signalStore(
    { providedIn: 'root' },
    withState(initialState),
    withMethods((store) => ({
        /**
         * Authenticates a user and updates the global state.
         *
         * This method sets the user information in the global store,
         * effectively logging the user into the application.
         *
         * @param user - The user information to authenticate
         * @param user.name - The user's display name
         * @param user.email - The user's email address
         *
         * @example
         * ```typescript
         * this.globalStore.login({
         *   name: 'John Doe',
         *   email: 'john@example.com'
         * });
         * ```
         */
        login(user: { name: string; email: string }) {
            patchState(store, { user });
        }
    })),
    withComputed(({ user }) => ({
        /**
         * Computed signal indicating whether a user is currently authenticated.
         *
         * Returns `true` if a user is logged in (user is not null),
         * `false` otherwise.
         *
         * @returns A boolean signal indicating authentication status
         *
         * @example
         * ```typescript
         * // In template
         * @if (globalStore.isLoggedIn()) {
         *   <span>Welcome, {{ globalStore.user()?.name }}!</span>
         * } @else {
         *   <button>Login</button>
         * }
         * ```
         */
        isLoggedIn: computed(() => user() != null)
    }))
);
