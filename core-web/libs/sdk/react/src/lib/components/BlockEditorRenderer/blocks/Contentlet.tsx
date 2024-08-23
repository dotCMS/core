import { CustomRenderer } from '../../../models/blocks.interface';

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

export const DotContent = (props: { data: Contentlet; customRenderers?: CustomRenderer }) => {
    const { data, customRenderers } = props;
    const DefaultContent = () => <div>Unknown Content Type</div>;

    const Component = customRenderers?.[data.contentType] ?? DefaultContent;

    return <Component {...data} />;
};
