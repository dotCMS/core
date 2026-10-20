import { experimentMarkup } from '../experiments/markup';

import type { DotCMSEventsExperimentPage } from '../models';
import type { ReactElement, ReactNode } from 'react';

export interface DotCMSExperimentProps {
    /**
     * The page asset, requested for the variant in the URL's `variantName`. The component reads
     * the experiment running on it and the variant the server rendered.
     */
    page: DotCMSEventsExperimentPage | null | undefined;
    /** Nonce for the inline style and script, on pages with a nonce-based Content-Security-Policy. */
    nonce?: string;
    /** Class for the element that wraps the content. */
    className?: string;
    /** What the experiment varies: the page's layout, or content the view renders itself. */
    children: ReactNode;
}

/**
 * Runs the page's experiment on its content. On a page that runs an experiment, it wraps the
 * content in an element marked with the experiment and the variant the server rendered, keeps
 * it hidden, and prints the script that decides the visitor's variant while the HTML loads.
 * `dotEvents` then shows the content, or the page is replaced with the assigned variant. On any
 * other page it renders the content alone.
 *
 * It has no hooks, so it also renders in a server component, which keeps the script builder
 * out of the client bundle.
 *
 * @param props - The page asset, an optional nonce and class, and the content
 * @returns The content, marked and hidden until its variant is decided on an experiment page
 *
 * @example
 * ```tsx
 * <DotCMSExperiment page={pageAsset}>
 *     <DotCMSLayoutBody page={pageAsset} components={pageComponents} />
 * </DotCMSExperiment>
 * ```
 */
export const DotCMSExperiment = ({
    page,
    nonce,
    className,
    children
}: DotCMSExperimentProps): ReactElement => {
    const markup = experimentMarkup(page);

    // A fragment, so the component always returns an element: its type reads plainly, and
    // JSX accepts it with every @types/react version
    if (!markup) {
        return <>{children}</>;
    }

    // Browsers hide a nonce from the DOM once they apply it, which hydration reads as a mismatch
    const nonceProps = nonce ? { nonce, suppressHydrationWarning: true } : {};

    return (
        <>
            <style {...nonceProps}>{markup.style}</style>
            {/* Runs while the HTML is parsed; React never runs it again on the client */}
            <script {...nonceProps} dangerouslySetInnerHTML={{ __html: markup.script }} />
            <div className={className} {...markup.attributes}>
                {children}
            </div>
        </>
    );
};
