import {
    DEFAULT_PAGE,
    DEFAULT_PAGINATION,
    DEFAULT_SORT,
    DEFAULT_TREE_EXPANDED,
    SYSTEM_HOST
} from './constants';
import { DotContentDriveState, DotContentDriveStatus } from './models';

/**
 * A complete `DotContentDriveState`, for specs that install a store feature in isolation.
 *
 * **Complete on purpose.** A partial object cast to the state type compiles today and rots quietly:
 * `withActionExecution.spec.ts` already records that the sibling feature specs hand-rolled their own
 * and went stale — still carrying a field the state had dropped, still missing one it had gained —
 * and stayed green throughout, because the test runner transpiles without typechecking. Sourcing the
 * defaults from `shared/constants` and returning the whole shape is what keeps that from happening
 * a third time.
 *
 * Override only what the test is actually about:
 *
 * ```ts
 * const state = buildContentDriveState({ items: [folderRow('/old-a/')] });
 * ```
 *
 * **Two fields differ from the store's own `initialState`.** That object seeds `currentSite` and
 * `path` with `undefined` to mean "not resolved yet", which the state type does not actually admit
 * — both are declared non-optional. The lie is pre-existing and predates this file, but repeating
 * it here would put it on new lines, where the strict gate rightly rejects it. A mounted site and
 * an empty path are also the better default for a feature spec, which is testing behaviour after
 * the portlet has settled. A spec that genuinely needs the pre-mount shape overrides it and owns
 * the cast itself.
 */
export function buildContentDriveState(
    overrides: Partial<DotContentDriveState> = {}
): DotContentDriveState {
    return {
        currentSite: SYSTEM_HOST,
        path: '',
        filters: {},
        items: [],
        selectedItems: [],
        status: DotContentDriveStatus.LOADING,
        pagination: DEFAULT_PAGINATION,
        sort: DEFAULT_SORT,
        isTreeExpanded: DEFAULT_TREE_EXPANDED,
        isTreeForceCollapsed: false,
        pages: [DEFAULT_PAGE],
        userSearchableFields: [],
        userSearchableActive: [],
        userSearchableFieldsLoaded: false,
        showInListFields: [],
        languages: [],
        defaultLanguageId: undefined,
        defaultLanguageLoaded: false,
        currentUserIsAdmin: false,
        ...overrides
    };
}
