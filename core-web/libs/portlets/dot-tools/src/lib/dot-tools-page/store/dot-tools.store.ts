import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { EMPTY, Observable, forkJoin } from 'rxjs';

import { computed, inject } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

import { CATALOG_INITIAL_LIMIT, CATALOG_LOAD_MORE_STEP } from '../../constants/dot-tools.constants';
import {
    DotToolsCatalogEntry,
    DotToolsSection,
    DotToolsSectionForm,
    DotToolsToolForm
} from '../../models/dot-tools.models';
import { DotToolsService } from '../../services/dot-tools.service';

type DotToolsStatus = 'init' | 'loading' | 'loaded' | 'error';

interface DotToolsState {
    sections: DotToolsSection[];
    catalog: DotToolsCatalogEntry[];
    selectedSectionId: string | null;
    catalogFilter: string;
    catalogLimit: number;
    status: DotToolsStatus;
}

const initialState: DotToolsState = {
    sections: [],
    catalog: [],
    selectedSectionId: null,
    catalogFilter: '',
    catalogLimit: CATALOG_INITIAL_LIMIT,
    status: 'init'
};

export const DotToolsStore = signalStore(
    withState<DotToolsState>(initialState),
    withComputed((store) => {
        const selectedSection = computed(() => {
            const id = store.selectedSectionId();
            if (!id) {
                return null;
            }

            return store.sections().find((section) => section.id === id) ?? null;
        });

        const catalogById = computed(() => {
            const map = new Map<string, DotToolsCatalogEntry>();
            for (const entry of store.catalog()) {
                map.set(entry.id, entry);
            }

            return map;
        });

        // Tools resolved for the selected section, in the stored `portletIds`
        // order. Any id the catalog has not fetched yet is skipped rather than
        // rendered as a broken row — the design assumes the catalog is loaded
        // in parallel with sections and the two arrive together.
        const selectedSectionTools = computed<DotToolsCatalogEntry[]>(() => {
            const section = selectedSection();
            if (!section) {
                return [];
            }

            const lookup = catalogById();

            return section.portletIds
                .map((id) => lookup.get(id))
                .filter((entry): entry is DotToolsCatalogEntry => entry !== undefined);
        });

        // Client-side filter over the catalog, case-insensitive on title.
        const filteredCatalog = computed<DotToolsCatalogEntry[]>(() => {
            const term = store.catalogFilter().trim().toLowerCase();
            if (!term) {
                return store.catalog();
            }

            return store.catalog().filter((entry) => entry.title.toLowerCase().includes(term));
        });

        const paginatedCatalog = computed<DotToolsCatalogEntry[]>(() =>
            filteredCatalog().slice(0, store.catalogLimit())
        );

        const hasMoreCatalog = computed(() => filteredCatalog().length > store.catalogLimit());

        // Portlet ids checked in the Available Tools list — the intersection of
        // the selected section's ids with the catalog. Rendered as a Set so the
        // row template can O(1) test membership.
        const selectedSectionToolIds = computed<Set<string>>(() => {
            const section = selectedSection();

            return new Set(section?.portletIds ?? []);
        });

        // For the tool-delete confirmation body: how many sections reference
        // this tool. Client-derived; no backend endpoint needed.
        const toolSectionCount = computed(
            () => (toolId: string) =>
                store.sections().filter((section) => section.portletIds.includes(toolId)).length
        );

        return {
            selectedSection,
            selectedSectionTools,
            filteredCatalog,
            paginatedCatalog,
            hasMoreCatalog,
            selectedSectionToolIds,
            toolSectionCount
        };
    }),
    withMethods((store) => {
        const toolsService = inject(DotToolsService);
        const httpErrorManager = inject(DotHttpErrorManagerService);

        function loadAll() {
            patchState(store, { status: 'loading' });

            forkJoin({
                sections: toolsService.getSections(),
                catalog: toolsService.getCatalog()
            })
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'error' });

                        return EMPTY;
                    })
                )
                .subscribe(({ sections, catalog }) => {
                    const ordered = [...sections].sort((a, b) => a.tabOrder - b.tabOrder);
                    patchState(store, {
                        sections: ordered,
                        catalog,
                        selectedSectionId: store.selectedSectionId() ?? ordered[0]?.id ?? null,
                        status: 'loaded'
                    });
                });
        }

        // Wraps a mutating call: sets loading, catches error → loaded, calls
        // the caller's success handler with the API result. Keeps the store
        // methods below focused on the shape of the patch, not the plumbing.
        function runMutation<T>(source$: Observable<T>, onSuccess: (result: T) => void) {
            patchState(store, { status: 'loading' });
            source$
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'loaded' });

                        return EMPTY;
                    })
                )
                .subscribe((result) => onSuccess(result));
        }

        function replaceSectionTools(sectionId: string, portletIds: string[]) {
            runMutation(toolsService.setSectionTools(sectionId, portletIds), () => {
                patchState(store, {
                    sections: store
                        .sections()
                        .map((section) =>
                            section.id === sectionId ? { ...section, portletIds } : section
                        ),
                    status: 'loaded'
                });
            });
        }

        return {
            loadAll,

            selectSection(id: string) {
                patchState(store, {
                    selectedSectionId: id,
                    catalogLimit: CATALOG_INITIAL_LIMIT
                });
            },

            setCatalogFilter(catalogFilter: string) {
                patchState(store, {
                    catalogFilter,
                    catalogLimit: CATALOG_INITIAL_LIMIT
                });
            },

            loadMoreCatalog() {
                patchState(store, {
                    catalogLimit: store.catalogLimit() + CATALOG_LOAD_MORE_STEP
                });
            },

            createSection(form: DotToolsSectionForm) {
                runMutation(toolsService.createSection(form), (created) => {
                    const nextTabOrder = store.sections().length;
                    const section: DotToolsSection = { ...created, tabOrder: nextTabOrder };
                    patchState(store, {
                        sections: [...store.sections(), section],
                        selectedSectionId: section.id,
                        status: 'loaded'
                    });
                });
            },

            updateSection(id: string, form: DotToolsSectionForm) {
                runMutation(toolsService.updateSection(id, form), (updated) => {
                    patchState(store, {
                        sections: store
                            .sections()
                            .map((section) =>
                                section.id === id
                                    ? { ...section, name: updated.name, icon: updated.icon }
                                    : section
                            ),
                        status: 'loaded'
                    });
                });
            },

            deleteSection(id: string) {
                runMutation(toolsService.deleteSection(id), () => {
                    const rest = store.sections().filter((section) => section.id !== id);
                    const nextSelectedId =
                        store.selectedSectionId() === id
                            ? (rest[0]?.id ?? null)
                            : store.selectedSectionId();
                    patchState(store, {
                        sections: rest,
                        selectedSectionId: nextSelectedId,
                        status: 'loaded'
                    });
                });
            },

            reorderSections(orderedIds: string[]) {
                const bySection = new Map(
                    store.sections().map((section) => [section.id, section] as const)
                );
                const reordered = orderedIds
                    .map((id, index) => {
                        const section = bySection.get(id);

                        return section ? { ...section, tabOrder: index } : null;
                    })
                    .filter((section): section is DotToolsSection => section !== null);

                // Optimistic patch first — the API confirmation just resets status.
                patchState(store, { sections: reordered });

                runMutation(toolsService.reorderSections(orderedIds), () => {
                    patchState(store, { status: 'loaded' });
                });
            },

            reorderSelectedSectionTools(portletIds: string[]) {
                const sectionId = store.selectedSectionId();
                if (!sectionId) {
                    return;
                }
                replaceSectionTools(sectionId, portletIds);
            },

            toggleToolInSelectedSection(toolId: string) {
                const section = store.selectedSection();
                if (!section) {
                    return;
                }
                const isChecked = section.portletIds.includes(toolId);
                const nextPortletIds = isChecked
                    ? section.portletIds.filter((id) => id !== toolId)
                    : [...section.portletIds, toolId];
                replaceSectionTools(section.id, nextPortletIds);
            },

            removeToolFromSelectedSection(toolId: string) {
                const section = store.selectedSection();
                if (!section) {
                    return;
                }
                replaceSectionTools(
                    section.id,
                    section.portletIds.filter((id) => id !== toolId)
                );
            },

            createCustomTool(form: DotToolsToolForm) {
                runMutation(toolsService.createCustomTool(form), (created) => {
                    const catalog = [...store.catalog(), created].sort((a, b) =>
                        a.title.localeCompare(b.title)
                    );
                    patchState(store, { catalog, status: 'loaded' });
                });
            },

            updateCustomTool(form: DotToolsToolForm) {
                runMutation(toolsService.updateCustomTool(form), (updated) => {
                    const catalog = store
                        .catalog()
                        .map((entry) => (entry.id === updated.id ? updated : entry))
                        .sort((a, b) => a.title.localeCompare(b.title));
                    patchState(store, { catalog, status: 'loaded' });
                });
            },

            deleteCustomTool(id: string) {
                runMutation(toolsService.deleteCustomTool(id), () => {
                    patchState(store, {
                        catalog: store.catalog().filter((entry) => entry.id !== id),
                        // The tool disappears from every section that referenced
                        // it — mirrors what DELETE /v1/portlet/portletId/{id}
                        // does today on the backend.
                        sections: store.sections().map((section) => ({
                            ...section,
                            portletIds: section.portletIds.filter((portletId) => portletId !== id)
                        })),
                        status: 'loaded'
                    });
                });
            }
        };
    }),
    withHooks((store) => ({
        onInit() {
            store.loadAll();
        }
    }))
);
