import { BlockEditorNode } from '@dotcms/types';
import { BlockEditorDefaultBlocks } from '@dotcms/types/internal';

import { BlockQuote, CodeBlock } from './blocks/Code';
import { DotContent } from './blocks/Contentlet';
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
        if (CustomRendererComponent) {
            return (
                <CustomRendererComponent key={`${node.type}-${index}`} content={node.content}>
                    <BlockEditorBlock content={node.content} customRenderers={customRenderers} />
                </CustomRendererComponent>
            );
        }

        switch (node.type) {
            case BlockEditorDefaultBlocks.PARAGRAPH:
                return (
                    <Paragraph key={`${node.type}-${index}`} node={node}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </Paragraph>
                );

            case BlockEditorDefaultBlocks.HEADING:
                return (
                    <Heading key={`${node.type}-${index}`} node={node}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </Heading>
                );

            case BlockEditorDefaultBlocks.TEXT:
                return <TextBlock key={`${node.type}-${index}`} {...node} />;

            case BlockEditorDefaultBlocks.BULLET_LIST:
                return (
                    <BulletList key={`${node.type}-${index}`}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </BulletList>
                );

            case BlockEditorDefaultBlocks.ORDERED_LIST:
                return (
                    <OrderedList key={`${node.type}-${index}`}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </OrderedList>
                );

            case BlockEditorDefaultBlocks.LIST_ITEM:
                return (
                    <ListItem key={`${node.type}-${index}`}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </ListItem>
                );

            case BlockEditorDefaultBlocks.BLOCK_QUOTE:
                return (
                    <BlockQuote key={`${node.type}-${index}`}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </BlockQuote>
                );

            case BlockEditorDefaultBlocks.CODE_BLOCK:
                return (
                    <CodeBlock key={`${node.type}-${index}`} node={node}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </CodeBlock>
                );

            case BlockEditorDefaultBlocks.HARDBREAK:
                return <br key={`${node.type}-${index}`} />;

            case BlockEditorDefaultBlocks.HORIZONTAL_RULE:
                return <hr key={`${node.type}-${index}`} />;

            case BlockEditorDefaultBlocks.DOT_IMAGE:
                return <DotCMSImage key={`${node.type}-${index}`} node={node} />;

            case BlockEditorDefaultBlocks.DOT_VIDEO:
                return <DotCMSVideo key={`${node.type}-${index}`} node={node} />;

            case BlockEditorDefaultBlocks.TABLE:
                return (
                    <TableRenderer
                        key={`${node.type}-${index}`}
                        content={node.content ?? []}
                        blockEditorItem={BlockEditorBlock}
                    />
                );

            case BlockEditorDefaultBlocks.DOT_CONTENT:
                return (
                    <DotContent
                        key={`${node.type}-${index}`}
                        customRenderers={customRenderers as CustomRenderer}
                        node={node}
                    />
                );

            default:
                return <div key={`${node.type}-${index}`}>Unknown Block Type {node.type}</div>;
        }
    });
};
