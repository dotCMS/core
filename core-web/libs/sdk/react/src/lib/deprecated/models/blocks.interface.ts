/**
 * Props for a contentlet inside the Block Editor
 *
 * @export
 * @interface DotContentletProps
 */
export interface DotContentletProps {
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

/**
 * Props for a block inside the Block Editor Component
 *
 * @export
 * @interface BlockProps
 */
export interface BlockProps {
    children: React.ReactNode;
}
