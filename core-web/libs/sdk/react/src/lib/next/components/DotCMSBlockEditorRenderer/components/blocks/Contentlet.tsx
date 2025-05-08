import { BlockEditorNode } from '@dotcms/types';

import { useIsDevMode } from '../../../../hooks/useIsDevMode';
import { CustomRenderer } from '../../DotCMSBlockEditorRenderer';

interface DotContentProps {
    customRenderers: CustomRenderer;
    node: BlockEditorNode;
}

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
        console.error('DotContent: No data provided');

        return null;
    }

    const contentType = data.contentType || 'Unknown Content Type';
    const Component = customRenderers[contentType];

    // In dev mode, show a helpful message for unknown content types
    if (isDevMode && !Component) {
        return <div>Unknown ContentType: {contentType}</div>;
    }

    // In production, use default component if no matching component found
    if (!Component) {
        console.error('DotContent: No matching component found for content type', contentType);

        return null;
    }

    return <Component {...node} />;
};
