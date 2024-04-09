// Update the location of the DotExperiments instance when it changes
import { useContext, useEffect } from 'react';

import { isInsideEditor } from '@dotcms/client';

import DotExperimentsContext from './DotExperimentsContext';

import { EXPERIMENT_DEFAULT_VARIANT_NAME, EXPERIMENT_QUERY_PARAM_KEY } from '../shared/constants';

/**
 * `useExperiments` is a custom hook for updating the location of the DotExperiments instance when it changes.
 *
 * @returns {void}
 */
export const useExperiments = (): void => {
    // retrieve experiment context
    const experimentContext = useContext(DotExperimentsContext);
    /**
     * This effect adds a change listener to the experiment context, specifically updating the location.
     * It doesn't have a meaningful return; it primarily triggers side-effects.
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
     * It captures click events and redirects to a new URL if the clicked anchor has a variant assigned.
     */
    useEffect(() => {
        if (!experimentContext) {
            return;
        }

        const customClickHandler = (event: MouseEvent) => {
            const target = event.target as HTMLAnchorElement;
            // Check if the clicked element is an anchor
            if (target.tagName.toLowerCase() === 'a') {
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
