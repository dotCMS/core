import { Equals } from '../../../query-builder/lucene-syntax';
import { QueryBuilder } from '../../../query-builder/sdk-query-builder';

// Model to sort by fields
export type SortBy = {
    field: string;
    order: 'asc' | 'desc';
};

// Callback to build a query
export type BuildQuery = (qb: QueryBuilder) => Equals;

// Main fields of a Contentlet (Inherited from the Content Type)
export interface ContentTypeMainFields {
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

// The contentlet has the main fields and the custom fields of the content type
export type Contentlet<T> = T & ContentTypeMainFields;

// Response of the get collection method
export interface GetCollectionResponse<T> {
    contentlets: Contentlet<T>[];
    page: number;
    size: number;
    total: number;
    sortedBy?: SortBy[];
}

export interface GetCollectionRawResponse<T> {
    entity: {
        jsonObjectView: {
            contentlets: Contentlet<T>[];
        };
        resultsSize: number;
    };
}

export interface GetCollectionError {
    status: number;
    [key: string]: unknown;
}
