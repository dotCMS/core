import { DotCMSContentType } from '@dotcms/dotcms-models';

export interface DotRelationship {
    archived: boolean;
    cardinality: number;
    categoryId: string;
    childRelationName: string;
    childRequired: boolean;
    childStructure: DotCMSContentType;
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
    parentStructure: DotCMSContentType;
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
