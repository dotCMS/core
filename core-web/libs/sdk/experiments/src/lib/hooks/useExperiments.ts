import { useContext, useEffect } from 'react';

import { isInsideEditor } from '@dotcms/client';

import DotExperimentsContext from '../contexts/DotExperimentsContext';
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
export const useExperiments = (): void => {
    const experimentContext = useContext(DotExperimentsContext);

    /**
     * This `useEffect` hook is responsible for tracking location changes when not inside an editor environment, and invoking the
     * `locationChanged` method from `experimentContext` with current location and custom redirection function.
     *
     */
    useEffect(() => {
        if (!experimentContext || typeof document === 'undefined') {
            return;
        }

        const insideEditor = isInsideEditor();

        if (!insideEditor) {
            const location = typeof window !== 'undefined' ? window.location : undefined;

            if (experimentContext && location) {
                experimentContext.locationChanged(location, experimentContext.customRedirectFn);
            }
        }
    }, [experimentContext]);

    /**
     * This effect sets a click handler on the document.
     * It captures click events and redirects to a new URL if the clicked anchor has a variant assigned,
     * additional removing the experiment query param from the URL.
     */
    useEffect(() => {
        if (!experimentContext) {
            return;
        }

        const customClickHandler = (event: MouseEvent) => {
            let target = event.target as HTMLAnchorElement;

            if (target.tagName.toLowerCase() !== 'a') {
                if (target.parentElement && target.parentElement.tagName.toLowerCase() === 'a') {
                    target = target.parentElement as HTMLAnchorElement;
                } else {
                    return;
                }
            }

            const clickedHref = target.getAttribute('href');

            if (clickedHref) {
                const modifiedUrl = new URL(clickedHref, experimentContext.location.href);

                // Remove the experiment query param from the URL
                modifiedUrl.searchParams.delete(EXPERIMENT_QUERY_PARAM_KEY);

                event.preventDefault();

                // Get the variant from the href of the clicked anchor
                const variant = experimentContext.getVariantFromHref(clickedHref);

                if (variant && variant.name !== EXPERIMENT_DEFAULT_VARIANT_NAME) {
                    // Set the experiment query param in the URL if the variant is not the default one
                    modifiedUrl.searchParams.set(EXPERIMENT_QUERY_PARAM_KEY, variant.name);
                }

                // Redirect to the new URL using the custom redirect function
                experimentContext.customRedirectFn(modifiedUrl.toString());
            }
        };

        // Register the click handler to all elements in the document
        document.addEventListener('click', customClickHandler);

        return () => {
            // Remove the click handler when the component is unmounted
            document.removeEventListener('click', customClickHandler);
        };
    }, [experimentContext]);
};
