import { ContentTypeView } from './content-type-view.model';

export interface StructureTypeView {
    name: string;
    label: string;
    types: ContentTypeView[];
}
