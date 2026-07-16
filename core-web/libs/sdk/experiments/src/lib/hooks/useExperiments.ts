import { useEffect } from 'react';

import { getUVEState } from '@dotcms/uve';

import { DotExperiments } from '../dot-experiments';
import { EXPERIMENT_DEFAULT_VARIANT_NAME, EXPERIMENT_QUERY_PARAM_KEY } from '../shared/constants';

/**
 * Custom hook `useExperiments`.
 *
 * This hook is designed to handle changes in the location of the current DotExperiments
 * instance and set a global click handler that redirects the application when an element with an
 * assigned variant is clicked.
 *
 * It also manages adding or removing the experimentation query parameter from the URL as
 * appropriate when a click occurs.
 *
 * @returns {void}
 */
export const useExperiments = (instance: DotExperiments | null): void => {
    /**
     * This `useEffect` hook is responsible for tracking location changes when not inside an editor environment, and invoking the
     * `locationChanged` method from `experimentContext` with current location and custom redirection function.
     *
     */
    useEffect(() => {
        if (!instance || typeof document === 'undefined') {
            return;
        }

        const insideEditor = getUVEState()?.mode;

        if (!insideEditor) {
            const location = typeof window !== 'undefined' ? window.location : undefined;

            if (instance && location) {
                instance.locationChanged(location, instance.customRedirectFn);
            }
        }
    }, [instance]);

    /**
     * This effect sets a click handler on the document.
     * It captures click events and redirects to a new URL if the clicked anchor has a variant assigned,
     * additional removing the experiment query param from the URL.
     */
    useEffect(() => {
        if (!instance) {
            return;
        }

        const customClickHandler = (event: MouseEvent) => {
            const target = (event.target as HTMLElement).closest('a');

            if (!target) {
                return;
            }

            const clickedHref = target.getAttribute('href');

            if (clickedHref) {
                const modifiedUrl = new URL(clickedHref, instance.location.href);

                // Remove the experiment query param from the URL
                modifiedUrl.searchParams.delete(EXPERIMENT_QUERY_PARAM_KEY);

                event.preventDefault();

                // Get the variant from the href of the clicked anchor
                const variant = instance.getVariantFromHref(clickedHref);

                if (variant && variant.name !== EXPERIMENT_DEFAULT_VARIANT_NAME) {
                    // Set the experiment query param in the URL if the variant is not the default one
                    modifiedUrl.searchParams.set(EXPERIMENT_QUERY_PARAM_KEY, variant.name);
                }

                // Redirect to the new URL using the custom redirect function
                instance.customRedirectFn(modifiedUrl.toString());
            }
        };

        // Register the click handler to all elements in the document
        document.addEventListener('click', customClickHandler);

        return () => {
            // Remove the click handler when the component is unmounted
            document.removeEventListener('click', customClickHandler);
        };
    }, [instance]);
};
