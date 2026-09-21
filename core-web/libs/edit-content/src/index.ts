export * from './lib/edit-content.routes';
export * from './lib/components/dot-create-content-dialog/dot-create-content-dialog.component';
export * from './lib/fields/dot-edit-content-file-field/components/dot-binary-field-ce-bridge/dot-binary-field-ce-bridge.component';
export * from './lib/fields/dot-edit-content-file-field/components/dot-file-field/dot-file-field.component';
export * from './lib/fields/dot-edit-content-tag-field/components/tag-field/tag-field.component';
export * from './lib/models/dot-edit-content-dialog.interface';
export * from './lib/services/dot-edit-content.service';
export * from './lib/components/dot-edit-content-side-panel/dot-edit-content-side-panel.component';
export { DotSidePanelNavController } from './lib/services/dot-side-panel-nav.service';
export * from './lib/utils/functions.util';
export * from './lib/models/dot-edit-content-field.constant';

// Relationship "select existing content" picker — reused by Content Drive's relationship filter.
// Exported for Content Drive's DOT_RELATIONSHIP_PICKER provider: the shared field-filter chip
// cannot import this library directly (that would make the dependency circular and drag this
// library into the legacy custom-element bundle), so the portlet supplies the capability instead.
export { AddRelationshipsComponent } from './lib/fields/dot-edit-content-relationship-field/components/add-relationships/add-relationships.component';
export type {
    AddRelationshipsInput,
    AddRelationshipsResult
} from './lib/fields/dot-edit-content-relationship-field/components/add-relationships/models/add-relationships.models';
export {
    getContentTypeIdFromRelationship,
    getSelectionModeByCardinality
} from './lib/fields/dot-edit-content-relationship-field/utils';
export type {
    InitLoadParams,
    SelectionMode
} from './lib/fields/dot-edit-content-relationship-field/models/relationship.models';

// Site/folder picker — reused by Content Drive's Action Center to collect a bulk move target.
// Exported as the inner control rather than the `dot-edit-content-host-folder-field` wrapper: the
// wrapper is card chrome bound to a `DotCMSContentTypeField`, which a bulk move has no equivalent of.
export { DotHostFolderFieldComponent } from './lib/fields/dot-edit-content-host-folder-field/components/host-folder-field/host-folder-field.component';
