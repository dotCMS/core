import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { EMPTY, Observable, forkJoin, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';

import { catchError, take, tap } from 'rxjs/operators';

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
    /**
     * Monotonic counter bumped on every successful section-tool mutation
     * (toggle, remove, drag-reorder). Lets the page component acknowledge
     * the save with a status toast without the store owning any UI
     * concern. `0` is the "nothing has happened yet" sentinel so the
     * component's effect skips the initial read.
     */
    toolsSavedAt: number;
}

const initialState: DotToolsState = {
    sections: [],
    catalog: [],
    selectedSectionId: null,
    catalogFilter: '',
    catalogLimit: CATALOG_INITIAL_LIMIT,
    status: 'init',
    toolsSavedAt: 0
};

/** Server truth is the source of `tabOrder`; the read never guarantees order. */
function sortByTabOrder(sections: DotToolsSection[]): DotToolsSection[] {
    return [...sections].sort((a, b) => a.tabOrder - b.tabOrder);
}

function isNotFound(error: unknown): boolean {
    return error instanceof HttpErrorResponse && error.status === 404;
}

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
        // order. If the catalog hasn't loaded yet we fall back to the
        // portletTitles the section itself carries — /v1/layouts sends the
        // localized title for each tool inline so this list can always render.
        const selectedSectionTools = computed<DotToolsCatalogEntry[]>(() => {
            const section = selectedSection();
            if (!section) {
                return [];
            }

            const lookup = catalogById();

            return section.portletIds.map((id, index) => {
                const catalogEntry = lookup.get(id);
                if (catalogEntry) {
                    return catalogEntry;
                }

                return {
                    id,
                    title: section.portletTitles[index] ?? id,
                    isCustom: false
                };
            });
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

        const selectedSectionToolIds = computed<Set<string>>(() => {
            const section = selectedSection();

            return new Set(section?.portletIds ?? []);
        });

        const toolSectionCount = computed(
            () => (toolId: string) =>
                store.sections().filter((section) => section.portletIds.includes(toolId)).length
        );

        const showLoading = computed(
            () => store.status() === 'loading' && store.sections().length === 0
        );
        const showError = computed(() => store.status() === 'error');
        const showEmptySections = computed(
            () => store.status() === 'loaded' && store.sections().length === 0
        );

        return {
            selectedSection,
            selectedSectionTools,
            filteredCatalog,
            paginatedCatalog,
            hasMoreCatalog,
            selectedSectionToolIds,
            toolSectionCount,
            showLoading,
            showError,
            showEmptySections
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
                        // The initial load has its own error UX
                        // (`showError` renders a retry card in the sections
                        // panel). A 404 reaching this handler would also
                        // surface as the global "This URL does not exist"
                        // modal and cover that card, so 404s stay out of
                        // the global handler here. Everything else routes
                        // through it so real backend problems still toast.
                        if (!isNotFound(error)) {
                            httpErrorManager.handle(error);
                        }
                        patchState(store, { status: 'error' });

                        return EMPTY;
                    })
                )
                .subscribe(({ sections, catalog }) => {
                    const ordered = sortByTabOrder(sections);
                    patchState(store, {
                        sections: ordered,
                        catalog,
                        selectedSectionId: store.selectedSectionId() ?? ordered[0]?.id ?? null,
                        status: 'loaded'
                    });
                });
        }

        // Fire-and-forget mutations route errors through the global handler.
        // The write path where a specific error needs inline handling
        // (section create/update) returns an Observable instead; onError
        // lets callers roll back optimistic patches before status flips.
        function runMutation<T>(
            source$: Observable<T>,
            onSuccess: (result: T) => void,
            onError?: (error: unknown) => void
        ) {
            patchState(store, { status: 'loading' });
            source$
                .pipe(
                    take(1),
                    catchError((error) => {
                        onError?.(error);
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'loaded' });

                        return EMPTY;
                    })
                )
                .subscribe((result) => onSuccess(result));
        }

        function commitSectionList(sections: DotToolsSection[]) {
            const ordered = sortByTabOrder(sections);
            const selectedStillExists = ordered.some(
                (section) => section.id === store.selectedSectionId()
            );
            patchState(store, {
                sections: ordered,
                selectedSectionId: selectedStillExists
                    ? store.selectedSectionId()
                    : (ordered[0]?.id ?? null),
                status: 'loaded'
            });
        }

        // Optimistic single-section patch for the three tool-mutation
        // paths (toggle, remove, drag-reorder). Patching before the
        // request means a second quick click reads the pending list
        // instead of the server-confirmed one — without this, two fast
        // checkbox clicks race: the second request computes a
        // portletIds without the first tool and the full-replace drops
        // it. Snapshot the whole sections array so a failure restores
        // every section (ordering is immaterial, the previous array
        // itself is the rollback value).
        function replaceSectionTools(sectionId: string, portletIds: string[]) {
            const snapshot = store.sections();
            patchState(store, {
                sections: snapshot.map((section) => {
                    if (section.id !== sectionId) {
                        return section;
                    }
                    const titleFor = (id: string): string => {
                        const prevIndex = section.portletIds.indexOf(id);
                        if (prevIndex !== -1) {
                            return section.portletTitles[prevIndex] ?? id;
                        }

                        return catalogTitleFor(id);
                    };

                    return {
                        ...section,
                        portletIds,
                        portletTitles: portletIds.map(titleFor)
                    };
                })
            });

            runMutation(
                toolsService.setSectionTools(sectionId, portletIds),
                (sections) => {
                    commitSectionList(sections);
                    patchState(store, { toolsSavedAt: store.toolsSavedAt() + 1 });
                },
                () => patchState(store, { sections: snapshot })
            );
        }

        function catalogTitleFor(id: string): string {
            return store.catalog().find((entry) => entry.id === id)?.title ?? id;
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

            /**
             * Returns an observable so the section dialog can render duplicate-name
             * (400) inline instead of the global error toast. The store still
             * commits state on success; errors reset status to loaded and re-throw.
             */
            createSection(form: DotToolsSectionForm): Observable<DotToolsSection> {
                patchState(store, { status: 'loading' });

                return toolsService.createSection(form).pipe(
                    take(1),
                    tap((created) => {
                        patchState(store, {
                            sections: [...store.sections(), created],
                            selectedSectionId: created.id,
                            status: 'loaded'
                        });
                    }),
                    catchError((error) => {
                        patchState(store, { status: 'loaded' });

                        return throwError(() => error);
                    })
                );
            },

            updateSection(id: string, form: DotToolsSectionForm): Observable<DotToolsSection> {
                patchState(store, { status: 'loading' });

                return toolsService.updateSection(id, form).pipe(
                    take(1),
                    tap((updated) => {
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
                    }),
                    catchError((error) => {
                        patchState(store, { status: 'loaded' });

                        return throwError(() => error);
                    })
                );
            },

            deleteSection(id: string) {
                runMutation(toolsService.deleteSection(id), (sections) => {
                    commitSectionList(sections);
                });
            },

            reorderSections(orderedIds: string[]) {
                // Optimistic patch: the server returns the full list on
                // success and commitSectionList replaces state. On failure
                // we restore the pre-reorder sections — otherwise the UI
                // keeps an order that was never saved until next reload.
                const snapshot = store.sections();
                const bySection = new Map(
                    snapshot.map((section) => [section.id, section] as const)
                );
                const optimistic = orderedIds
                    .map((id, index) => {
                        const section = bySection.get(id);

                        return section ? { ...section, tabOrder: index } : null;
                    })
                    .filter((section): section is DotToolsSection => section !== null);

                patchState(store, { sections: optimistic });

                runMutation(
                    toolsService.reorderSections(orderedIds),
                    (sections) => commitSectionList(sections),
                    () => patchState(store, { sections: snapshot })
                );
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

            /**
             * Returns an Observable so the tool dialog can render the
             * server's rejection (e.g. duplicate portletId) inline
             * instead of behind the global toast. Mirrors createSection.
             */
            createCustomTool(form: DotToolsToolForm): Observable<DotToolsCatalogEntry> {
                patchState(store, { status: 'loading' });

                return toolsService.createCustomTool(form).pipe(
                    take(1),
                    tap((created) => {
                        const catalog = [...store.catalog(), created].sort((a, b) =>
                            a.title.localeCompare(b.title)
                        );
                        patchState(store, { catalog, status: 'loaded' });
                    }),
                    catchError((error) => {
                        patchState(store, { status: 'loaded' });

                        return throwError(() => error);
                    })
                );
            },

            updateCustomTool(form: DotToolsToolForm): Observable<DotToolsCatalogEntry> {
                patchState(store, { status: 'loading' });

                return toolsService.updateCustomTool(form).pipe(
                    take(1),
                    tap((updated) => {
                        const catalog = store
                            .catalog()
                            .map((entry) => (entry.id === updated.id ? updated : entry))
                            .sort((a, b) => a.title.localeCompare(b.title));
                        patchState(store, { catalog, status: 'loaded' });
                    }),
                    catchError((error) => {
                        patchState(store, { status: 'loaded' });

                        return throwError(() => error);
                    })
                );
            },

            deleteCustomTool(id: string) {
                runMutation(toolsService.deleteCustomTool(id), () => {
                    patchState(store, {
                        catalog: store.catalog().filter((entry) => entry.id !== id),
                        sections: store.sections().map((section) => {
                            const dropAt = section.portletIds.indexOf(id);
                            if (dropAt === -1) {
                                return section;
                            }

                            return {
                                ...section,
                                portletIds: section.portletIds.filter(
                                    (portletId) => portletId !== id
                                ),
                                portletTitles: section.portletTitles.filter(
                                    (_title, index) => index !== dropAt
                                )
                            };
                        }),
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
