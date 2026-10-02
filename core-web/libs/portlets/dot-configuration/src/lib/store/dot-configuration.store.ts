import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { EMPTY, Observable, forkJoin, from, throwError } from 'rxjs';

import { computed, inject } from '@angular/core';

import { catchError, concatMap, finalize, take, tap } from 'rxjs/operators';

import {
    DotCompanyConfigurationService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCompanyAuthType,
    DotCompanyConfiguration,
    DotLoginLanguage,
    DotMessageSeverity,
    DotMessageType
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import {
    DOT_CONFIGURATION_DEFAULTS,
    DotConfigurationBranding,
    DotConfigurationDraft,
    DotConfigurationLocale,
    DotConfigurationSection,
    SECTION_SAVE_ORDER,
    dirtySections,
    toBrandingForm,
    toDraft,
    toLocaleForm,
    validate
} from './dot-configuration.mappers';

/** What the action bar reports about the form. */
export const DotConfigurationSaveState = {
    SAVED: 'saved',
    UNSAVED: 'unsaved',
    SAVING: 'saving',
    FAILED: 'failed'
} as const;

export type DotConfigurationSaveState =
    (typeof DotConfigurationSaveState)[keyof typeof DotConfigurationSaveState];

interface DotConfigurationState {
    status: ComponentStatus;
    /** Values last stored on the server; `null` until the first load succeeds. */
    original: DotConfigurationDraft | null;
    draft: DotConfigurationDraft | null;
    keyDigest: string;
    regeneratingKey: boolean;
    adminLocales: DotLoginLanguage[];
    /** Section whose save failed last; cleared when a new save starts. */
    failedSection: DotConfigurationSection | null;
}

const initialState: DotConfigurationState = {
    status: ComponentStatus.INIT,
    original: null,
    draft: null,
    keyDigest: '',
    regeneratingKey: false,
    adminLocales: [],
    failedSection: null
};

/**
 * State of the Configuration page: the configuration as last stored on the server, the edits
 * made since, and the single Save that sends every modified section.
 *
 * Provided by the page shell, so every card on the page shares one instance.
 */
export const DotConfigurationStore = signalStore(
    withState<DotConfigurationState>(initialState),
    withComputed((store, globalStore = inject(GlobalStore)) => {
        const dirtyBySection = computed(() => {
            const original = store.original();
            const draft = store.draft();

            return original && draft
                ? dirtySections(original, draft)
                : { branding: false, authentication: false, locale: false };
        });
        const dirty = computed(() => Object.values(dirtyBySection()).some(Boolean));
        const errors = computed(() => {
            const draft = store.draft();

            return draft ? validate(draft) : {};
        });
        const hasErrors = computed(() => Object.keys(errors()).length > 0);

        return {
            dirtyBySection,
            dirty,
            errors,
            hasErrors,
            /** True while Save Changes is running; the form is read-only meanwhile. */
            saving: computed(() => store.status() === ComponentStatus.SAVING),
            canSave: computed(
                () => dirty() && !hasErrors() && store.status() !== ComponentStatus.SAVING
            ),
            saveState: computed<DotConfigurationSaveState>(() => {
                if (store.status() === ComponentStatus.SAVING) {
                    return DotConfigurationSaveState.SAVING;
                }

                if (store.failedSection() && dirty()) {
                    return DotConfigurationSaveState.FAILED;
                }

                return dirty()
                    ? DotConfigurationSaveState.UNSAVED
                    : DotConfigurationSaveState.SAVED;
            }),
            timezones: globalStore.systemTimezones,
            systemTimezone: globalStore.systemTimezone
        };
    }),
    withMethods((store) => {
        const service = inject(DotCompanyConfigurationService);
        const httpErrorManager = inject(DotHttpErrorManagerService);
        const messageDisplayService = inject(DotMessageDisplayService);
        const dotMessageService = inject(DotMessageService);

        function updateDraft(patch: (draft: DotConfigurationDraft) => DotConfigurationDraft): void {
            const draft = store.draft();

            if (draft) {
                patchState(store, { draft: patch(draft) });
            }
        }

        function saveRequest(
            section: DotConfigurationSection,
            draft: DotConfigurationDraft
        ): Observable<DotCompanyConfiguration> {
            switch (section) {
                case DotConfigurationSection.BRANDING:
                    return service.saveBranding(toBrandingForm(draft.branding));
                case DotConfigurationSection.AUTHENTICATION:
                    return service.saveAuthType(draft.authType);
                case DotConfigurationSection.LOCALE:
                    return service.saveLocale(toLocaleForm(draft.locale));
            }
        }

        /**
         * Takes the server's answer as the new stored state, and replaces the saved section of
         * the draft with what the server actually kept (it may normalize values, e.g. derive
         * `mx`). Sections not saved yet keep their edits.
         */
        function applySaved(section: DotConfigurationSection, view: DotCompanyConfiguration): void {
            const saved = toDraft(view);
            const draft = store.draft() ?? saved;

            patchState(store, {
                original: saved,
                keyDigest: view.keyDigest ?? store.keyDigest(),
                draft: {
                    branding:
                        section === DotConfigurationSection.BRANDING
                            ? saved.branding
                            : draft.branding,
                    authType:
                        section === DotConfigurationSection.AUTHENTICATION
                            ? saved.authType
                            : draft.authType,
                    locale: section === DotConfigurationSection.LOCALE ? saved.locale : draft.locale
                }
            });
        }

        return {
            /** Loads the configuration and the admin UI locales the language can be set to. */
            load(): void {
                patchState(store, { status: ComponentStatus.LOADING, failedSection: null });

                forkJoin({
                    configuration: service.getConfiguration(),
                    adminLocales: service.getAdminLocales()
                })
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { status: ComponentStatus.ERROR });

                            return EMPTY;
                        })
                    )
                    .subscribe(({ configuration, adminLocales }) => {
                        const draft = toDraft(configuration);

                        patchState(store, {
                            status: ComponentStatus.LOADED,
                            original: draft,
                            draft,
                            keyDigest: configuration.keyDigest ?? '',
                            adminLocales
                        });
                    });
            },

            /** Edits branding and outbound email values in the form. Nothing is saved. */
            patchBranding(patch: Partial<DotConfigurationBranding>): void {
                updateDraft((draft) => ({ ...draft, branding: { ...draft.branding, ...patch } }));
            },

            /** Edits the authentication type in the form. Nothing is saved. */
            setAuthType(authType: DotCompanyAuthType): void {
                updateDraft((draft) => ({ ...draft, authType }));
            },

            /** Edits the language or time zone in the form. Nothing is saved. */
            patchLocale(patch: Partial<DotConfigurationLocale>): void {
                updateDraft((draft) => ({ ...draft, locale: { ...draft.locale, ...patch } }));
            },

            /** Throws away every edit and goes back to the values last stored on the server. */
            discard(): void {
                patchState(store, { draft: store.original(), failedSection: null });
            },

            /**
             * Puts the default colors and login background back in the form. Nothing is saved
             * until Save Changes; logos are not touched.
             */
            restoreDefaults(): void {
                updateDraft((draft) => ({
                    ...draft,
                    branding: { ...draft.branding, ...DOT_CONFIGURATION_DEFAULTS }
                }));
            },

            /**
             * Sends every modified section, one after another in {@link SECTION_SAVE_ORDER}.
             * They are never sent in parallel: branding and authentication each rewrite the whole
             * company record, so concurrent saves could overwrite each other. The first failure
             * stops the sequence; the sections already saved stay saved, and the failed one and
             * the rest keep their edits.
             */
            save(): void {
                const draft = store.draft();

                if (!draft || !store.canSave()) {
                    return;
                }

                const sections = SECTION_SAVE_ORDER.filter(
                    (section) => store.dirtyBySection()[section]
                );

                patchState(store, { status: ComponentStatus.SAVING, failedSection: null });

                from(sections)
                    .pipe(
                        concatMap((section) =>
                            saveRequest(section, store.draft() ?? draft).pipe(
                                tap((view) => applySaved(section, view)),
                                catchError((error) => {
                                    httpErrorManager.handle(error);
                                    patchState(store, { failedSection: section });

                                    return throwError(() => error);
                                })
                            )
                        ),
                        catchError(() => EMPTY),
                        finalize(() => patchState(store, { status: ComponentStatus.LOADED }))
                    )
                    .subscribe();
            },

            /**
             * Replaces the company key right away; not part of Save Changes. The caller must have
             * confirmed it: Apps secrets are reset and push-publishing endpoint keys are
             * re-encrypted.
             */
            regenerateKey(): void {
                patchState(store, { regeneratingKey: true });

                service
                    .regenerateKey()
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);

                            return EMPTY;
                        }),
                        finalize(() => patchState(store, { regeneratingKey: false }))
                    )
                    .subscribe((keyDigest) => {
                        patchState(store, { keyDigest });
                        messageDisplayService.push({
                            life: 5000,
                            severity: DotMessageSeverity.SUCCESS,
                            message: dotMessageService.get('configuration.regenerate-key.success'),
                            type: DotMessageType.SIMPLE_MESSAGE
                        });
                    });
            }
        };
    }),
    withHooks({
        onInit(store) {
            store.load();
        }
    })
);
