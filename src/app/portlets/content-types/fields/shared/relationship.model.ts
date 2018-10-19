import { ContentType } from '@portlets/content-types/shared/content-type.model';

export interface Relationship {
    archived: boolean;
    cardinality: number;
    categoryId: string;
    childRelationName: string;
    childRequired: boolean;
    childStructure: ContentType;
    childStructureInode: string;
    fixed: boolean;
    identifier: string;
    inode: string;
    live: boolean;
    locked: boolean;
    modDate: number;
    modUser: string;
    new: boolean;
    owner: string;
    parentRelationName: string;
    parentRequired: boolean;
    parentStructure: ContentType;
    parentStructureInode: string;
    permissionId: string;
    permissionType: string;
    relationTypeValue: string;
    title: string;
    type: string;
    versionId: null;
    versionType: string;
    working: boolean;
}
