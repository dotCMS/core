import { BlockEditorNode } from '@dotcms/types';

import { UnknownContentType } from './UnknownContentType';

import { useIsDevMode } from '../../../../hooks/useIsDevMode';
import { CustomRenderer } from '../../DotCMSBlockEditorRenderer';

interface DotContentProps {
    customRenderers: CustomRenderer;
    node: BlockEditorNode;
}

const DOT_CONTENT_NO_DATA_MESSAGE =
    '[DotCMSBlockEditorRenderer]: No data provided for Contentlet Block. Try to add a contentlet to the block editor. If the error persists, please contact the DotCMS support team.';

const DOT_CONTENT_NO_MATCHING_COMPONENT_MESSAGE = (contentType: string) =>
    `[DotCMSBlockEditorRenderer]: No matching component found for content type: ${contentType}. Provide a custom renderer for this content type to fix this error.`;

/**
 * Renders a DotContent component.
 *
 * @param {DotContentProps} props - The props for the DotContent component.
 * @returns {JSX.Element} The rendered DotContent component.
 */
export const DotContent = ({ customRenderers, node }: DotContentProps) => {
    const isDevMode = useIsDevMode();
    const attrs = node?.attrs || {};
    const data = attrs.data;

    if (!data) {
        console.error(DOT_CONTENT_NO_DATA_MESSAGE);

        return null;
    }

    const contentType = data.contentType || 'Unknown Content Type';
    const Component = customRenderers[contentType];

    // In dev mode, show a helpful message for unknown content types
    if (isDevMode && !Component) {
        return <UnknownContentType contentType={contentType} />;
    }

    // In production, use default component if no matching component found
    if (!Component) {
        console.warn(DOT_CONTENT_NO_MATCHING_COMPONENT_MESSAGE(contentType));

        return null;
    }

    return <Component {...node} />;
};
