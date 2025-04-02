import { Blocks } from '../../../models/blocks.interface';
import { ContentNode, CustomRenderer } from '../../../models/content-node.interface';
import { BlockQuote, CodeBlock } from '../blocks/Code';
import { DotContent } from '../blocks/Contentlet';
import { DotCMSImage } from '../blocks/Image';
import { BulletList, ListItem, OrderedList } from '../blocks/Lists';
import { TableRenderer } from '../blocks/Table';
import { Heading, Paragraph, TextBlock } from '../blocks/Texts';
import { DotCMSVideo } from '../blocks/Video';

/**
 * Renders a block editor item based on the provided content and custom renderers.
 *
 * @param content - The content nodes to render.
 * @param customRenderers - Optional custom renderers for specific node types.
 * @returns The rendered block editor item.
 */
export const BlockEditorBlock = ({
    content,
    customRenderers
}: {
    content: ContentNode[];
    customRenderers?: CustomRenderer;
}) => {
    return content?.map((node: ContentNode, index) => {
        const CustomRendererComponent = customRenderers?.[node.type];
        if (CustomRendererComponent) {
            return (
                <CustomRendererComponent
                    key={`${node.type}-${index}`}
                    {...node}
                    content={node.content}>
                    <BlockEditorBlock content={node.content} customRenderers={customRenderers} />
                </CustomRendererComponent>
            );
        }

        switch (node.type) {
            case Blocks.PARAGRAPH:
                return (
                    <Paragraph key={`${node.type}-${index}`} {...node}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </Paragraph>
                );

            case Blocks.HEADING:
                return (
                    <Heading key={`${node.type}-${index}`} {...node}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </Heading>
                );

            case Blocks.TEXT:
                return <TextBlock key={`${node.type}-${index}`} {...node} />;

            case Blocks.BULLET_LIST:
                return (
                    <BulletList key={`${node.type}-${index}`}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </BulletList>
                );

            case Blocks.ORDERED_LIST:
                return (
                    <OrderedList key={`${node.type}-${index}`}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </OrderedList>
                );

            case Blocks.LIST_ITEM:
                return (
                    <ListItem key={`${node.type}-${index}`}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </ListItem>
                );

            case Blocks.BLOCK_QUOTE:
                return (
                    <BlockQuote key={`${node.type}-${index}`}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </BlockQuote>
                );

            case Blocks.CODE_BLOCK:
                return (
                    <CodeBlock key={`${node.type}-${index}`} {...node}>
                        <BlockEditorBlock
                            content={node.content}
                            customRenderers={customRenderers}
                        />
                    </CodeBlock>
                );

            case Blocks.HARDBREAK:
                return <br key={`${node.type}-${index}`} />;

            case Blocks.HORIZONTAL_RULE:
                return <hr key={`${node.type}-${index}`} />;

            case Blocks.DOT_IMAGE:
                return <DotCMSImage key={`${node.type}-${index}`} {...node} />;

            case Blocks.DOT_VIDEO:
                return <DotCMSVideo key={`${node.type}-${index}`} {...node} />;

            case Blocks.TABLE:
                return (
                    <TableRenderer
                        key={`${node.type}-${index}`}
                        content={node.content}
                        blockEditorItem={BlockEditorBlock}
                    />
                );

            case Blocks.DOT_CONTENT:
                return (
                    <DotContent
                        key={`${node.type}-${index}`}
                        {...node}
                        customRenderers={customRenderers}
                    />
                );

            default:
                return <div key={`${node.type}-${index}`}>Unknown Block Type {node.type}</div>;
        }
    });
};
