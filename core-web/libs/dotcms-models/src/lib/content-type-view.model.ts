import { StructureType } from './structure-type.model';

export interface ContentTypeView {
    type: StructureType;
    name: string;
    inode: string;
    action: string;
    variable: string;
}
