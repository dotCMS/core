import { groupLinkRuns, resolveEmoji, type EmojiWarnScope } from '@dotcms/client/internal';
import { BlockEditorNode } from '@dotcms/types';
import { BlockEditorDefaultBlocks } from '@dotcms/types/internal';
import { getUVEState } from '@dotcms/uve';

import { DotCMSAudio } from './blocks/Audio';
import { BlockQuote, CodeBlock } from './blocks/Code';
import { DotContent } from './blocks/DotContent';
import { GridBlock } from './blocks/GridBlock';
import { DotCMSImage } from './blocks/Image';
import { BulletList, ListItem, OrderedList } from './blocks/Lists';
import { TableRenderer } from './blocks/Table';
import { Heading, Link, Paragraph, TextBlock } from './blocks/Texts';
import { DotCMSVideo } from './blocks/Video';

import { CustomRenderer } from '../DotCMSBlockEditorRenderer';

interface BlockEditorBlockProps {
    content: BlockEditorNode[] | undefined;
    customRenderers?: CustomRenderer;
    isDevMode?: boolean;
}

/**
 * Renders a block editor item based on the provided content and custom renderers.
 *
 * @param content - The content nodes to render.
 * @param customRenderers - Optional custom renderers for specific node types.
 * @returns The rendered block editor item.
 */
export const BlockEditorBlock = ({
    content,
    customRenderers,
    isDevMode
}: BlockEditorBlockProps) => {
    if (!content) {
        return null;
    }

    // One scope per render so a document repeating an unresolvable emoji warns once, not once
    // per occurrence.
    const warned: EmojiWarnScope = new Set<string>();

    /**
     * Group siblings into link runs before rendering (#37340).
     *
     * Rendering node-by-node emits one `<a>` per text node, so a link split by a legacy `emoji`
     * node became two anchors: two tab stops, two screen-reader entries, one fragmented
     * announcement. A run renders as a single `<a>` wrapping its nodes, with the `link` mark
     * stripped from the children so the anchor is not nested inside itself.
     */
    return groupLinkRuns(content).flatMap((group, groupIndex) => {
        const rendered = group.nodes.map((node, index) =>
            renderNode(
                group.link
                    ? { ...node, marks: node.marks?.filter((mark) => mark.type !== 'link') }
                    : node,
                `${groupIndex}-${index}`
            )
        );

        if (!group.link) {
            return rendered;
        }

        return [
            <Link key={`link-${groupIndex}`} type={group.link.type} attrs={group.link.attrs}>
                {rendered}
            </Link>
        ];
    });

    function renderNode(node: BlockEditorNode, key: string) {
        const CustomRendererComponent = customRenderers?.[node.type];

        if (CustomRendererComponent) {
            return (
                <CustomRendererComponent key={key} node={node}>
                    <BlockEditorBlock
                        content={node.content}
                        customRenderers={customRenderers}
                        isDevMode={isDevMode}
                    />
                </CustomRendererComponent>
            );
        }

        switch (node.type) {
            case BlockEditorDefaultBlocks.PARAGRAPH:
                return (
                    <Paragraph key={key} node={node}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                            isDevMode={isDevMode}
                        />
                    </Paragraph>
                );

            case BlockEditorDefaultBlocks.HEADING:
                return (
                    <Heading key={key} node={node}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                            isDevMode={isDevMode}
                        />
                    </Heading>
                );

            case BlockEditorDefaultBlocks.TEXT:
                return <TextBlock key={key} {...node} />;

            // Legacy content only — the editor no longer creates `emoji` nodes (#37340). The
            // node stores a shortcode, never the character, so without this branch it fell
            // through to UnknownBlock and rendered nothing on the live site.
            case BlockEditorDefaultBlocks.EMOJI:
                return <span key={key}>{resolveEmoji(node, warned)}</span>;

            case BlockEditorDefaultBlocks.BULLET_LIST:
                return (
                    <BulletList key={key}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                            isDevMode={isDevMode}
                        />
                    </BulletList>
                );

            case BlockEditorDefaultBlocks.ORDERED_LIST:
                return (
                    <OrderedList key={key}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                            isDevMode={isDevMode}
                        />
                    </OrderedList>
                );

            case BlockEditorDefaultBlocks.LIST_ITEM:
                return (
                    <ListItem key={key}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                            isDevMode={isDevMode}
                        />
                    </ListItem>
                );

            case BlockEditorDefaultBlocks.BLOCK_QUOTE:
                return (
                    <BlockQuote key={key}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                            isDevMode={isDevMode}
                        />
                    </BlockQuote>
                );

            case BlockEditorDefaultBlocks.CODE_BLOCK:
                return (
                    <CodeBlock key={key} node={node}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                            isDevMode={isDevMode}
                        />
                    </CodeBlock>
                );

            case BlockEditorDefaultBlocks.HARDBREAK:
                return <br key={key} />;

            case BlockEditorDefaultBlocks.HORIZONTAL_RULE:
                return <hr key={key} />;

            case BlockEditorDefaultBlocks.DOT_IMAGE:
                return <DotCMSImage key={key} node={node} />;

            case BlockEditorDefaultBlocks.DOT_VIDEO:
                return <DotCMSVideo key={key} node={node} />;

            case BlockEditorDefaultBlocks.DOT_AUDIO:
                return <DotCMSAudio key={key} node={node} />;

            case BlockEditorDefaultBlocks.TABLE:
                return (
                    <TableRenderer
                        key={key}
                        content={node.content ?? []}
                        attrs={node.attrs}
                        blockEditorItem={BlockEditorBlock}
                    />
                );

            case BlockEditorDefaultBlocks.GRID_BLOCK:
                return (
                    <GridBlock
                        key={key}
                        node={node}
                        blockEditorBlock={BlockEditorBlock}
                        customRenderers={customRenderers}
                    />
                );

            case BlockEditorDefaultBlocks.DOT_CONTENT:
                return (
                    <DotContent
                        key={key}
                        customRenderers={customRenderers as CustomRenderer}
                        node={node}
                        isDevMode={isDevMode}
                    />
                );

            default:
                return <UnknownBlock key={key} node={node} />;
        }
    }
};

/**
 * Renders an unknown block type with a warning message in development mode.
 *
 * @param node - The block editor node to render.
 * @returns The rendered block or null if in production mode.
 */
const UnknownBlock = ({ node }: { node: BlockEditorNode }) => {
    const style = {
        backgroundColor: '#fff5f5',
        color: '#333',
        padding: '1rem',
        borderRadius: '0.5rem',
        marginBottom: '1rem',
        marginTop: '1rem',
        border: '1px solid #fc8181'
    };

    if (getUVEState()) {
        return (
            <div style={style}>
                <strong style={{ color: '#c53030' }}>Warning:</strong> The block type{' '}
                <strong>{node.type}</strong> is not recognized. Please check your{' '}
                <a
                    href="https://dev.dotcms.com/docs/block-editor"
                    target="_blank"
                    rel="noopener noreferrer">
                    configuration
                </a>{' '}
                or contact support for assistance.
            </div>
        );
    }

    return null;
};
