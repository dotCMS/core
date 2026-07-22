export * from './lib/edit-content.routes';
export * from './lib/components/dot-create-content-dialog/dot-create-content-dialog.component';
export * from './lib/fields/dot-edit-content-file-field/components/dot-binary-field-ce-bridge/dot-binary-field-ce-bridge.component';
export * from './lib/fields/dot-edit-content-file-field/components/dot-file-field/dot-file-field.component';
export * from './lib/fields/dot-edit-content-tag-field/components/tag-field/tag-field.component';
export * from './lib/models/dot-edit-content-dialog.interface';
export * from './lib/services/dot-edit-content.service';
export * from './lib/components/dot-edit-content-side-panel/dot-edit-content-side-panel.component';
export * from './lib/utils/functions.util';
export * from './lib/models/dot-edit-content-field.constant';

// Relationship "select existing content" picker — reused by Content Drive's relationship filter.
export { DotSelectExistingContentComponent } from './lib/fields/dot-edit-content-relationship-field/components/dot-select-existing-content/dot-select-existing-content.component';
export { FooterComponent as DotSelectExistingContentFooterComponent } from './lib/fields/dot-edit-content-relationship-field/components/dot-select-existing-content/components/footer/footer.component';
export { ExistingContentStore } from './lib/fields/dot-edit-content-relationship-field/components/dot-select-existing-content/store/existing-content.store';
export {
    getContentTypeIdFromRelationship,
    getSelectionModeByCardinality
} from './lib/fields/dot-edit-content-relationship-field/utils';
export type {
    InitLoadParams,
    SelectionMode
} from './lib/fields/dot-edit-content-relationship-field/models/relationship.models';
