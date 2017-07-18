
import { Field } from '../fields';

export * from './content-types.component';

export interface ContentType {
    clazz: string;
    defaultType: boolean;
    description?: string;
    detailPage?: string;
    fields?: Array<Field>;
    fixed: boolean;
    folder: string;
    host: string;
    iDate?: Date;
    id?: string;
    modDate?: Date;
    name: string;
    owner: string;
    system: boolean;
    urlMapPattern?: string;
    variable?: string;
    workflow?: string;
}

export const CONTENT_TYPE_INITIAL_DATA: ContentType = {
    clazz: null,
    defaultType: false,
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: null,
    name: null,
    owner: null,
    system: false
};
