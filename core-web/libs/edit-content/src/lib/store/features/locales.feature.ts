import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, effect, inject, untracked } from '@angular/core';
import { Router } from '@angular/router';

import { DialogService } from 'primeng/dynamicdialog';

import { filter, switchMap, take } from 'rxjs/operators';

import {
    DotContentletService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService
} from '@dotcms/data-access';
import { ComponentStatus, DotCMSContentlet, DotLanguage } from '@dotcms/dotcms-models';
import { DotEditContentSidebarUntranslatedLocaleComponent } from '@dotcms/edit-content/components/dot-edit-content-sidebar/components/dot-edit-content-sidebar-untranslated-locale/dot-edit-content-sidebar-untranslated-locale.component';

import { ContentState } from './content.feature';
import { FormState } from './form.feature';
import { WorkflowState } from './workflow.feature';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { EditContentRootState } from '../../store/edit-content.store';
import { parseCurrentActions } from '../../utils/workflows.utils';

export interface LocalesState {
    locales: DotLanguage[] | null;
    systemDefaultLocale: DotLanguage | null;
    currentLocale: DotLanguage | null;
    currentIdentifier: string | null;
    localesStatus: {
        status: ComponentStatus;
        error: string;
    };
}

export const localesInitialState: LocalesState = {
    locales: null,
    systemDefaultLocale: null,
    currentLocale: null,
    currentIdentifier: null,
    localesStatus: {
        status: ComponentStatus.INIT,
        error: ''
    }
};

export function withLocales() {
    return signalStoreFeature(
        { state: type<ContentState & FormState & WorkflowState & EditContentRootState>() },
        withState(localesInitialState),
        withComputed((store) => ({
            /**
             * Computed property that indicates whether the locales are currently being loaded.
             *
             * @param state The current state of the locales feature.
             * @returns `true` if the locales are being loaded, `false` otherwise.
             */
            isLoadingLocales: computed(
                () => store.localesStatus().status === ComponentStatus.LOADING
            ),

            isCopyingLocale: computed(() => store.initialContentletState() === 'copy')
        })),
        withMethods(
            (
                store,
                dotContentletService = inject(DotContentletService),
                dotEditContentService = inject(DotEditContentService),
                dotLanguagesService = inject(DotLanguagesService),
                dotHttpErrorManagerService = inject(DotHttpErrorManagerService),
                dotMessageService = inject(DotMessageService),
                dialogService = inject(DialogService),
                router = inject(Router)
            ) => ({
                /**
                 * Loads the locales based on a content identifier and updates the state accordingly.
                 *
                 * @param {DotCMSContentlet} contentlet - The contentlet to load the locales for.
                 */
                loadContentLocales: rxMethod<DotCMSContentlet>(
                    pipe(
                        switchMap((contentlet) => {
                            patchState(store, {
                                localesStatus: { status: ComponentStatus.LOADING, error: '' }
                            });

                            return forkJoin({
                                locales: dotContentletService.getLanguages(contentlet.identifier),
                                systemDefaultLocale: dotLanguagesService.getDefault()
                            }).pipe(
                                take(1),
                                tapResponse({
                                    next: ({ locales, systemDefaultLocale }) => {
                                        patchState(store, {
                                            locales,
                                            systemDefaultLocale,
                                            currentLocale: locales.find(
                                                (x) => x.id === contentlet?.languageId
                                            ),
                                            currentIdentifier: contentlet?.identifier,
                                            localesStatus: {
                                                status: ComponentStatus.LOADED,
                                                error: ''
                                            }
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        dotHttpErrorManagerService.handle(error);
                                        patchState(store, {
                                            localesStatus: {
                                                status: ComponentStatus.ERROR,
                                                error: error.message
                                            }
                                        });
                                    }
                                })
                            );
                        })
                    )
                ),

                /**
                 * Loads the system locales and updates the state accordingly.
                 *  This method is used when there is no contentlet in the store.
                 *  meaning that the user is creating a new content.
                 */
                loadSystemLocales: rxMethod<void>(
                    pipe(
                        switchMap(() => {
                            patchState(store, {
                                localesStatus: { status: ComponentStatus.LOADING, error: '' }
                            });

                            return dotLanguagesService.get().pipe(
                                take(1),
                                tapResponse({
                                    next: (locales: DotLanguage[]) => {
                                        const defaultLocale = locales.find(
                                            (locale) => locale.defaultLanguage
                                        );
                                        patchState(store, {
                                            locales,
                                            systemDefaultLocale: defaultLocale,
                                            currentLocale: defaultLocale,
                                            localesStatus: {
                                                status: ComponentStatus.LOADED,
                                                error: ''
                                            }
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        dotHttpErrorManagerService.handle(error);
                                        patchState(store, {
                                            localesStatus: {
                                                status: ComponentStatus.ERROR,
                                                error: error.message
                                            }
                                        });
                                    }
                                })
                            );
                        })
                    )
                ),

                /**
                 * Switches the locale and updates the state accordingly.
                 *
                 * @param {DotLanguage} locale - The locale to switch to.
                 */
                switchLocale: rxMethod<DotLanguage>(
                    pipe(
                        switchMap((locale: DotLanguage) => {
                            /**
                             * Checks if the locale is translated. If it is, retrieves the content
                             * by its identifier and locale id and navigates to the content page by inode.
                             */
                            if (locale.translated) {
                                return dotEditContentService
                                    .getContentById(store.currentIdentifier(), locale.id)
                                    .pipe(
                                        tapResponse({
                                            next: (contentlet) => {
                                                router.navigate(['/content', contentlet.inode], {
                                                    replaceUrl: true,
                                                    queryParamsHandling: 'preserve'
                                                });
                                            },
                                            error: (error: HttpErrorResponse) => {
                                                dotHttpErrorManagerService.handle(error);
                                                patchState(store, {
                                                    localesStatus: {
                                                        status: ComponentStatus.ERROR,
                                                        error: error.message
                                                    }
                                                });
                                            }
                                        })
                                    );
                            } else {
                                const ref = dialogService.open(
                                    DotEditContentSidebarUntranslatedLocaleComponent,
                                    {
                                        header: dotMessageService.get(
                                            'edit.content.sidebar.locales.untranslated.locale'
                                        ),
                                        width: '35rem',
                                        data: {
                                            currentLocale: store.currentLocale()
                                        },
                                        modal: true
                                    }
                                );

                                ref.onClose
                                    .pipe(
                                        take(1),
                                        filter((value) => value)
                                    )
                                    .subscribe((copyType) => {
                                        const schemes = store.schemes();
                                        const schemeIds = Object.keys(schemes);
                                        // If we have only one scheme, we set it as the default one
                                        const defaultSchemeId =
                                            schemeIds.length === 1 ? schemeIds[0] : null;
                                        // Parse the actions as an object with the schemeId as the key
                                        const parsedCurrentActions = parseCurrentActions(
                                            schemes[defaultSchemeId]?.actions || []
                                        );

                                        patchState(store, {
                                            currentLocale: locale,
                                            currentSchemeId: defaultSchemeId,
                                            currentContentActions: parsedCurrentActions,
                                            state: ComponentStatus.LOADED,
                                            currentIdentifier: store.contentlet()?.identifier,
                                            initialContentletState: 'copy',
                                            error: null,
                                            formValues: null,
                                            contentlet:
                                                copyType === 'populate' ? store.contentlet() : null
                                        });
                                    });

                                return of(null);
                            }
                        })
                    )
                )
            })
        ),
        withHooks({
            onInit(store) {
                /**
                 * Effect that loads content locales or system locales based on the current state.
                 *
                 * This effect checks if there is a contentlet or content type in the store. If a contentlet is present,
                 * it loads the content locales using the contentlet's identifier.
                 * If a contentType is present but no Contentlet ( new Content ), it loads the system locales.
                 */
                effect(() => {
                    const contentlet = store.contentlet();
                    const contentType = store.contentType();

                    untracked(() => {
                        if (store.initialContentletState() !== 'copy') {
                            if (contentlet) {
                                store.loadContentLocales(contentlet);
                            } else if (contentType) {
                                store.loadSystemLocales();
                            }
                        }
                    });
                });
            }
        })
    );
}
