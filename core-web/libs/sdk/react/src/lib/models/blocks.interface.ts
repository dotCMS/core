import { Contentlet } from '../components/BlockEditorRenderer/blocks/Contentlet';

export interface Block {
    content: ContentNode[];
}

export interface Mark {
    type: string;
    attrs: Record<string, string>;
}

export interface ContentNode {
    type: string;
    content: ContentNode[];
    attrs?: Record<string, string> & { data: DotContentProps | Contentlet };
    marks?: Mark[];
    text?: string;
}

export interface DotContentProps {
    title: string;
    baseType: string;
    inode: string;
    archived: boolean;
    working: boolean;
    locked: boolean;
    contentType: string;
    live: boolean;
    identifier: string;
    image: string;
    imageContentAsset: string;
    urlTitle: string;
    url: string;
    titleImage: string;
    urlMap: string;
    hasLiveVersion: boolean;
    hasTitleImage: boolean;
    sortOrder: number;
    modUser: string;
    __icon__: string;
    contentTypeIcon: string;
    language: string;
    description: string;
    shortDescription: string;
    salePrice: string;
    retailPrice: string;
    mimeType: string;
    thumbnail?: string;
}

export interface BlockProps {
    children: React.ReactNode;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type CustomRenderer = Record<string, React.FC<any>>;

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type DotAssetProps = { data: DotContentProps; [key: string]: any };
