export * from './lib/edit-content.routes';
export * from './lib/fields/dot-edit-content-file-field/components/dot-binary-field-ce-bridge/dot-binary-field-ce-bridge.component';
export * from './lib/fields/dot-edit-content-file-field/components/dot-file-field/dot-file-field.component';
export * from './lib/fields/dot-edit-content-tag-field/components/tag-field/tag-field.component';
export * from './lib/services/dot-edit-content.service';
// The Edit Content side panel's content, loaded on demand. Surfaces open content through
// DotEditContentOpenerService below, which picks this panel or the legacy one.
export { loadDotEditContentSidePanel } from './lib/components/dot-edit-content-side-panel/load-dot-edit-content-side-panel';

// Open any content in the side panel, in whichever editor its type uses (#38303): one request,
// one opener, the same five events from either editor.
export * from './lib/models/dot-edit-content-request.model';
export * from './lib/models/dot-edit-content-panel-events.model';
export * from './lib/models/dot-open-content-event.model';
export { DotEditContentOpenerService } from './lib/opener/dot-edit-content-opener.service';
export type { DotEditContentOpenOptions } from './lib/opener/dot-edit-content-opener.service';
export { injectEditContentPanel } from './lib/opener/inject-edit-content-panel';
export type {
    DotEditContentPanelBinding,
    DotEditContentPanelHandle
} from './lib/opener/inject-edit-content-panel';
// The legacy editor and any other legacy admin screen as side panel content, loaded on demand.
// Remove with the legacy editor.
export { loadDotLegacyEditorSidePanel } from './lib/components/dot-legacy-editor-side-panel/load-dot-legacy-editor-side-panel';
export { loadDotLegacyScreenSidePanel } from './lib/components/dot-legacy-screen-side-panel/load-dot-legacy-screen-side-panel';
export type {
    DotLegacyScreenEvent,
    DotLegacyScreenRequest
} from './lib/models/dot-legacy-screen.model';
// Edit Code: a file's source in Monaco, in a side panel, loaded on demand. Opened from Content
// Drive's row menu and from the page editor's VTL menu, both deciding with `toSourceEditorRequest`.
export { loadDotSourceEditorSidePanel } from './lib/components/dot-source-editor-side-panel/load-dot-source-editor-side-panel';
export type { DotSourceEditorRequest } from './lib/models/dot-source-editor.model';
export { toSourceEditorRequest } from './lib/utils/source-editor.util';
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

// Lets Content Drive take over "switch to the old editor" and a load error in its own panel
// (#37759).
// Remove with the legacy editor.
export { EDIT_CONTENT_NAVIGATION_OVERRIDE } from './lib/models/edit-content-navigation-override';
export type { EditContentNavigationOverride } from './lib/models/edit-content-navigation-override';
export type { EditContentIdentity } from './lib/services/host/edit-content-host.model';
