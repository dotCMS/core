import { ContentTypeView } from './content-type-view.model';

export interface StructureTypeView {
    name: string;
    label: string;
    /** Null when the base type has no content types — the endpoint sends null, not an empty array. */
    types: ContentTypeView[] | null;
}
