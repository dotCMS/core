import { BlockEditorNode } from '@dotcms/types';
import { BlockEditorDefaultBlocks } from '@dotcms/types/internal';
import { getUVEState } from '@dotcms/uve';

import { BlockQuote, CodeBlock } from './blocks/Code';
import { DotContent } from './blocks/DotContent';
import { DotCMSImage } from './blocks/Image';
import { BulletList, ListItem, OrderedList } from './blocks/Lists';
import { TableRenderer } from './blocks/Table';
import { Heading, Paragraph, TextBlock } from './blocks/Texts';
import { DotCMSVideo } from './blocks/Video';

import { CustomRenderer } from '../DotCMSBlockEditorRenderer';

interface BlockEditorBlockProps {
    content: BlockEditorNode[] | undefined;
    customRenderers?: CustomRenderer;
}

/**
 * Renders a block editor item based on the provided content and custom renderers.
 *
 * @param content - The content nodes to render.
 * @param customRenderers - Optional custom renderers for specific node types.
 * @returns The rendered block editor item.
 */
export const BlockEditorBlock = ({ content, customRenderers }: BlockEditorBlockProps) => {
    if (!content) {
        return null;
    }

    return content?.map((node: BlockEditorNode, index) => {
        const CustomRendererComponent = customRenderers?.[node.type];
        const key = `${node.type}-${index}`;

        if (CustomRendererComponent) {
            return (
                <CustomRendererComponent key={key} content={node.content}>
                    <BlockEditorBlock content={node.content} customRenderers={customRenderers} />
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
                        />
                    </Paragraph>
                );

            case BlockEditorDefaultBlocks.HEADING:
                return (
                    <Heading key={key} node={node}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </Heading>
                );

            case BlockEditorDefaultBlocks.TEXT:
                return <TextBlock key={key} {...node} />;

            case BlockEditorDefaultBlocks.BULLET_LIST:
                return (
                    <BulletList key={key}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </BulletList>
                );

            case BlockEditorDefaultBlocks.ORDERED_LIST:
                return (
                    <OrderedList key={key}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </OrderedList>
                );

            case BlockEditorDefaultBlocks.LIST_ITEM:
                return (
                    <ListItem key={key}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </ListItem>
                );

            case BlockEditorDefaultBlocks.BLOCK_QUOTE:
                return (
                    <BlockQuote key={key}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </BlockQuote>
                );

            case BlockEditorDefaultBlocks.CODE_BLOCK:
                return (
                    <CodeBlock key={key} node={node}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
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

            case BlockEditorDefaultBlocks.TABLE:
                return (
                    <TableRenderer
                        key={key}
                        content={node.content ?? []}
                        blockEditorItem={BlockEditorBlock}
                    />
                );

            case BlockEditorDefaultBlocks.DOT_CONTENT:
                return (
                    <DotContent
                        key={key}
                        customRenderers={customRenderers as CustomRenderer}
                        node={node}
                    />
                );

            default:
                return <UnknownBlock key={key} node={node} />;
        }
    });
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
