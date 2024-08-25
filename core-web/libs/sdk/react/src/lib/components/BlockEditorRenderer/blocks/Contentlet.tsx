import { ContentNode, CustomRenderer } from '../../../models/blocks.interface';

// Replace this when we have a types lib
export interface Contentlet {
    hostName: string;
    modDate: string;
    publishDate: string;
    title: string;
    baseType: string;
    inode: string;
    archived: boolean;
    ownerName: string;
    host: string;
    working: boolean;
    locked: boolean;
    stInode: string;
    contentType: string;
    live: boolean;
    owner: string;
    identifier: string;
    publishUserName: string;
    publishUser: string;
    languageId: number;
    creationDate: string;
    url: string;
    titleImage: string;
    modUserName: string;
    hasLiveVersion: boolean;
    folder: string;
    hasTitleImage: boolean;
    sortOrder: number;
    modUser: string;
    __icon__: string;
    contentTypeIcon: string;
    variant: string;
}

type DotContentProps = ContentNode & {
    customRenderers?: CustomRenderer;
};

/**
 * Renders a DotContent component.
 *
 * @param {DotContentProps} props - The props for the DotContent component.
 * @returns {JSX.Element} The rendered DotContent component.
 */
export const DotContent = (props: DotContentProps) => {
    const { attrs, customRenderers } = props;
    const DefaultContent = () => <div>Unknown Content Type</div>;
    const data = attrs?.data as unknown as Contentlet;

    const Component = customRenderers?.[data.contentType] ?? DefaultContent;

    if (!data) {
        console.error('DotContent: No data provided');
    }

    return <Component {...data} />;
};
