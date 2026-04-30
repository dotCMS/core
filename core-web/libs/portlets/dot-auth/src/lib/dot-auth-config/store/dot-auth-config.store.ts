import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { computed, inject } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import { DotAuthService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DOT_AUTH_SYSTEM_HOST, DotAuthConfig, DotAuthUiProtocol } from '@dotcms/dotcms-models';

import {
    DEFAULT_CONFIG,
    applyOidcDiscovery,
    applyTrustedDiscovery,
    clone,
    equal,
    fromView,
    setPath,
    toHeadlessPayload,
    toPayload,
    toWellKnownUrl,
    validate,
    validateHeadless
} from './dot-auth-config.mappers';

type DotAuthConfigStatus = 'init' | 'loading' | 'loaded' | 'saving';

interface DotAuthConfigState {
    siteId: string;
    original: DotAuthConfig;
    draft: DotAuthConfig;
    configured: boolean;
    inherited: boolean;
    status: DotAuthConfigStatus;
    errors: Record<string, string>;
}

const initialState: DotAuthConfigState = {
    siteId: DOT_AUTH_SYSTEM_HOST,
    original: clone(DEFAULT_CONFIG),
    draft: clone(DEFAULT_CONFIG),
    configured: false,
    inherited: false,
    status: 'init',
    errors: {}
};

export const DotAuthConfigStore = signalStore(
    withState<DotAuthConfigState>(initialState),
    withComputed((store) => ({
        isSystem: computed(() => store.siteId() === DOT_AUTH_SYSTEM_HOST),
        ssoDirty: computed(() => {
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            const { headless: _o, ...origSso } = store.original();
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            const { headless: _d, ...draftSso } = store.draft();
            return !equal(origSso, draftSso);
        }),
        headlessDirty: computed(() => !equal(store.original().headless, store.draft().headless)),
        dirty: computed(() => !equal(store.original(), store.draft())),
        errorCount: computed(() => Object.keys(store.errors()).length)
    })),
    withMethods((store) => {
        const service = inject(DotAuthService);
        const httpErrorManager = inject(DotHttpErrorManagerService);

        function load(siteId: string): void {
            patchState(store, { siteId, status: 'loading', errors: {} });
            service
                .getConfig(siteId)
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'loaded' });
                        return EMPTY;
                    })
                )
                .subscribe((view) => {
                    const config = fromView(view);
                    patchState(store, {
                        original: config,
                        draft: clone(config),
                        configured: view.configured,
                        inherited: view.inherited,
                        status: 'loaded'
                    });
                });
        }

        function saveSso(): boolean {
            const draft = store.draft();
            if (draft.protocol === 'none') {
                return true;
            }
            const validation = validate(draft);
            if (Object.keys(validation).length) {
                patchState(store, { errors: validation });
                return false;
            }
            patchState(store, { status: 'saving', errors: {} });
            service
                .saveConfig(store.siteId(), toPayload(draft))
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'loaded' });
                        return EMPTY;
                    })
                )
                .subscribe(() => load(store.siteId()));
            return true;
        }

        function saveHeadless(): boolean {
            const validation = validateHeadless(store.draft());
            if (Object.keys(validation).length) {
                patchState(store, { errors: validation });
                return false;
            }
            patchState(store, { status: 'saving', errors: {} });
            service
                .saveHeadlessConfig(toHeadlessPayload(store.draft()))
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'loaded' });
                        return EMPTY;
                    })
                )
                .subscribe(() => load(store.siteId()));
            return true;
        }

        return {
            load,
            saveSso,
            saveHeadless,

            reset(): void {
                patchState(store, { draft: clone(store.original()), errors: {} });
            },

            clearOverride(): void {
                patchState(store, { status: 'saving' });
                service
                    .clearConfig(store.siteId())
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { status: 'loaded' });
                            return EMPTY;
                        })
                    )
                    .subscribe(() => load(store.siteId()));
            },

            setProtocol(protocol: DotAuthUiProtocol): void {
                patchState(store, {
                    draft: { ...store.draft(), protocol, ssoEnabled: protocol !== 'none' }
                });
            },

            update(path: string, value: unknown): void {
                patchState(store, { draft: setPath(store.draft(), path, value) });
            },

            addAllowedOrigin(): void {
                const draft = clone(store.draft());
                draft.headless.allowedOrigins.push('');
                patchState(store, { draft });
            },

            removeAllowedOrigin(index: number): void {
                const draft = clone(store.draft());
                draft.headless.allowedOrigins.splice(index, 1);
                patchState(store, { draft });
            },

            addTrustedIdp(): void {
                const draft = clone(store.draft());
                draft.headless.trustedIdps.push({
                    id: crypto.randomUUID?.() ?? String(Date.now()),
                    name: '',
                    enabled: true,
                    discoveryUrl: '',
                    discoveryStatus: 'idle',
                    issuer: '',
                    jwksUrl: '',
                    audience: '',
                    algs: ['RS256'],
                    claimEmail: 'email',
                    claimFirstName: 'given_name',
                    claimLastName: 'family_name',
                    claimGroups: 'groups',
                    autoProvision: true,
                    syncOnExchange: true,
                    defaultRoles: [],
                    roleBehavior: 'merge',
                    groupMappings: []
                });
                patchState(store, { draft });
            },

            removeTrustedIdp(index: number): void {
                const draft = clone(store.draft());
                draft.headless.trustedIdps.splice(index, 1);
                patchState(store, { draft });
            },

            runOidcDiscovery(path: 'oidc' | `headless.trustedIdps.${number}`): void {
                const draft = clone(store.draft());
                const rawUrl =
                    path === 'oidc'
                        ? draft.oidc.discoveryUrl
                        : draft.headless.trustedIdps[Number(path.split('.').pop())].discoveryUrl;
                if (!rawUrl) return;
                const discoveryUrl = toWellKnownUrl(rawUrl);
                patchState(store, {
                    draft: setPath(draft, `${path}.discoveryStatus`, 'loading')
                });
                service
                    .discoverOidc(discoveryUrl)
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, {
                                draft: setPath(store.draft(), `${path}.discoveryStatus`, 'error')
                            });
                            return EMPTY;
                        })
                    )
                    .subscribe((view) => {
                        patchState(store, {
                            draft:
                                path === 'oidc'
                                    ? applyOidcDiscovery(store.draft(), view)
                                    : applyTrustedDiscovery(store.draft(), path, view)
                        });
                    });
            },

            revokeAllSessionRefs(): void {
                service
                    .revokeAllSessionRefs()
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            return EMPTY;
                        })
                    )
                    .subscribe();
            }
        };
    })
);
