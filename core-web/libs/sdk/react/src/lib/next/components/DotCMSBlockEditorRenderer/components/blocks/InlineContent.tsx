import { BlockEditorNode } from '@dotcms/types';

interface InlineContentProps {
    node: BlockEditorNode;
}

const INLINE_CONTENT_NO_DATA_MESSAGE =
    '[DotCMSBlockEditorRenderer]: No data provided for an inline content reference (dotInlineContent). If the error persists, please contact the DotCMS support team.';

/**
 * Default renderer for an inline contentlet reference (`dotInlineContent`).
 *
 * Renders the referenced contentlet's title inline. When a front-end URL is available on the
 * hydrated `attrs.data` (`urlMap` for URL-mapped content, or `url` for pages) it renders a link;
 * otherwise it falls back to a plain inline label. Consumers can fully override this output by
 * passing `customRenderers={{ dotInlineContent: MyComponent }}` — handled upstream in
 * {@link BlockEditorBlock} by the `customRenderers[node.type]` lookup.
 */
export const InlineContent = ({ node }: InlineContentProps) => {
    const data = node.attrs?.['data'];

    if (!data) {
        console.error(INLINE_CONTENT_NO_DATA_MESSAGE);

        return null;
    }

    const title = data.title || data.identifier || '';
    const url = data.urlMap ?? data.url ?? null;

    if (!url) {
        return <span className="dot-inline-content">{title}</span>;
    }

    return (
        <a className="dot-inline-content" href={url}>
            {title}
        </a>
    );
};
