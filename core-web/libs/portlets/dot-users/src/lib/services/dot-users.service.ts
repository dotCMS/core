import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSAPIResponse } from '@dotcms/dotcms-models';

/**
 * Row shape returned by GET /api/v1/users/filter — mirrors the fields
 * populated by com.liferay.portal.model.User#toMap() on the backend.
 * Kept intentionally partial: only the fields consumed by the list view
 * are declared.
 */
export interface DotUserListItem {
    userId: string;
    id: string;
    firstName: string;
    lastName: string;
    fullName: string;
    name: string;
    emailAddress: string;
    gravitar: string;
    active: boolean;
    admin: boolean;
    backendUser: boolean;
    frontendUser: boolean;
    hasConsoleAccess: boolean;
    lastLoginDate: number | null;
    lastLoginIP: string | null;
    failedLoginAttempts: number | null;
}

/**
 * Full user payload returned by GET /api/v1/users/{userId}. Includes
 * every field User#toMap() populates — additional info, birthday, etc.
 * — which are absent from the list response. `additionalInfo` is a
 * free-form map keyed by whatever the caller decided to store; the
 * dialog treats `prefix`, `suffix`, `title`, `company`, `website` as
 * conventional keys.
 */
export interface DotUserDetail extends DotUserListItem {
    birthday: string | null;
    middleName: string | null;
    nickname: string | null;
    languageId: string | null;
    timeZoneId: string | null;
    male: boolean | null;
    female: boolean | null;
    additionalInfo: Record<string, unknown> | null;
    createDate: number | null;
    modificationDate: number | null;
}

/**
 * Payload accepted by both POST /api/v1/users (create) and PUT
 * /api/v1/users (update). Mirrors com.dotcms.rest.api.v1.user.UserForm.
 *
 * Contract notes:
 * - Create requires `password`; update leaves it optional and skips the
 *   password mutation when omitted.
 * - A non-empty `roles` list REPLACES the user's role membership
 *   entirely (see UserResource#processRoles). The FE always sends the
 *   full role list echoed back from `DotRolesService.getForUser`
 *   with Access-toggle deltas applied, so membership is preserved on
 *   every save — omitting `roles` (the alternative "don't touch" mode)
 *   is not used by this UI.
 */
export interface DotUserFormPayload {
    userId?: string;
    firstName: string;
    lastName: string;
    email: string;
    active: boolean;
    password?: string;
    middleName?: string;
    nickName?: string;
    birthday?: string;
    languageId?: string;
    timeZoneId?: string;
    male?: boolean;
    additionalInfo?: Record<string, unknown>;
    roles?: string[];
}

interface DotUserDetailResponse {
    entity: {
        userId: string;
        user: DotUserDetail;
        roleId: string;
    };
}

interface DotUserUpdateResponse {
    entity: {
        userId: string;
        user: DotUserDetail;
        roleId?: string;
    };
}

interface DotToolgroupStateResponse {
    entity: {
        message?: boolean;
    };
}

/**
 * Row shape returned by GET /api/v1/apitoken/{userId}/tokens. Mirrors
 * `com.dotcms.auth.providers.jwt.beans.ApiToken`. Dates are epoch
 * milliseconds (`revokedDate` is null when not revoked). The three
 * boolean flags reflect derived state on the backend: `valid = !expired
 * && !revoked && after-notBefore`.
 */
export interface DotApiToken {
    id: string;
    userId: string;
    requestingUserId: string;
    requestingIp: string | null;
    issuer: string | null;
    subject: string | null;
    tokenType: string | null;
    claims: { label?: string } & Record<string, unknown>;
    allowNetwork: string | null;
    issueDate: number;
    expiresDate: number;
    revokedDate: number | null;
    modificationDate: number;
    valid: boolean;
    expired: boolean;
    revoked: boolean;
}

/**
 * Payload accepted by POST /api/v1/apitoken. `expirationSeconds` is
 * the TTL in seconds computed from the user-picked expiration date.
 * `network` is a CIDR block ("0.0.0.0/0" = any). Free-form `claims`
 * are stored verbatim; the UI only sets `label`.
 */
export interface DotApiTokenCreatePayload {
    userId: string;
    expirationSeconds: number;
    network?: string;
    claims?: { label?: string } & Record<string, unknown>;
}

/**
 * Response envelope for POST /api/v1/apitoken. The `jwt` field is the
 * initial signed value returned alongside the created token record;
 * the caller can re-mint an equivalent JWT later via `getApiTokenJwt`,
 * so this is not a one-shot secret — it's just the convenient first
 * one so the UI doesn't need a second round-trip.
 */
export interface DotApiTokenCreateResult {
    jwt: string;
    token: DotApiToken;
}

interface DotApiTokensListResponse {
    entity: { tokens: DotApiToken[] };
}

interface DotApiTokenCreateResponse {
    entity: { jwt: string; token: DotApiToken };
}

export interface DotUsersPaginatedParams {
    filter?: string;
    page?: number;
    perPage?: number;
    orderBy?: string;
    direction?: 'ASC' | 'DESC';
    includeAnonymous?: boolean;
    includeDefault?: boolean;
    /**
     * System role key to constrain the result set to users who hold that role
     * (e.g. `DOTCMS_BACK_END_USER`, `DOTCMS_FRONT_END_USER`). The backend
     * accepts it repeatably or as a comma-separated list; the FE currently
     * uses a single value from the "Filter by" chip.
     */
    roleKey?: string;
}

@Injectable({ providedIn: 'root' })
export class DotUsersService {
    readonly #http = inject(HttpClient);

    getUsersPaginated(
        params: DotUsersPaginatedParams
    ): Observable<DotCMSAPIResponse<DotUserListItem[]>> {
        const paramMap: Array<[string, string | undefined]> = [
            ['query', params.filter],
            ['page', params.page?.toString()],
            ['per_page', params.perPage?.toString()],
            ['orderby', params.orderBy],
            ['direction', params.direction],
            ['includeanonymous', params.includeAnonymous ? 'true' : undefined],
            ['includedefault', params.includeDefault ? 'true' : undefined],
            ['roleKey', params.roleKey]
        ];

        const httpParams = paramMap.reduce(
            (acc, [key, value]) => (value ? acc.set(key, value) : acc),
            new HttpParams()
        );

        return this.#http.get<DotCMSAPIResponse<DotUserListItem[]>>('/api/v1/users/filter', {
            params: httpParams
        });
    }

    deleteUser(userId: string, replacementUserId?: string): Observable<unknown> {
        let params = new HttpParams();
        if (replacementUserId) {
            params = params.set('replacementUserId', replacementUserId);
        }

        return this.#http.delete(`/api/v1/users/${encodeURIComponent(userId)}`, { params });
    }

    /**
     * Fetches the full user payload used to hydrate the edit dialog.
     * The list response omits `additionalInfo`, `birthday`, and other
     * profile-only fields, so the dialog must re-fetch even when it
     * already has a `DotUserListItem` from the row click.
     */
    getUser(userId: string): Observable<DotUserDetail> {
        return this.#http
            .get<DotUserDetailResponse>(`/api/v1/users/${encodeURIComponent(userId)}`)
            .pipe(map((response) => response.entity.user));
    }

    createUser(payload: DotUserFormPayload): Observable<DotUserDetail> {
        return this.#http
            .post<DotUserUpdateResponse>('/api/v1/users', payload)
            .pipe(map((response) => response.entity.user));
    }

    updateUser(payload: DotUserFormPayload): Observable<DotUserDetail> {
        return this.#http
            .put<DotUserUpdateResponse>('/api/v1/users', payload)
            .pipe(map((response) => response.entity.user));
    }

    /**
     * Reads whether the `gettingstarted` layout is assigned to a user.
     * The legacy admin UI ties the "Show Getting Started" checkbox to
     * this same layout via `_addtouser` / `_removefromuser`; we mirror
     * that semantic instead of introducing a new field.
     */
    getGettingStartedState(userId: string): Observable<boolean> {
        return this.#http
            .get<DotToolgroupStateResponse>('/api/v1/toolgroups/gettingstarted/_userHasLayout', {
                params: new HttpParams().set('userid', userId)
            })
            .pipe(map((response) => !!response.entity?.message));
    }

    /**
     * Adds or removes the `gettingstarted` layout on the target user.
     * Independent of `PUT /api/v1/users` — mirrors the two legacy
     * endpoints used by view_users_js_inc.jsp.
     */
    setGettingStarted(userId: string, enabled: boolean): Observable<unknown> {
        const url = enabled
            ? '/api/v1/toolgroups/gettingstarted/_addtouser'
            : '/api/v1/toolgroups/gettingstarted/_removefromuser';

        return this.#http.put(url, null, {
            params: new HttpParams().set('userid', userId)
        });
    }

    /**
     * Lists every API token owned by a user. `showRevoked=false` (the
     * default) filters revoked entries server-side; the tab flips this
     * when the user checks "Show revoked/expired".
     */
    getApiTokens(userId: string, showRevoked: boolean): Observable<DotApiToken[]> {
        return this.#http
            .get<DotApiTokensListResponse>(
                `/api/v1/apitoken/${encodeURIComponent(userId)}/tokens`,
                { params: new HttpParams().set('showRevoked', String(showRevoked)) }
            )
            .pipe(map((response) => response.entity?.tokens ?? []));
    }

    /**
     * Mints a new API token for the target user. The JWT string in the
     * response is a convenience — the tab surfaces it inline so the
     * admin doesn't need a second call — but any later reveal minted
     * via `getApiTokenJwt` signs an equivalent JWT over the same
     * record. Listings only return metadata.
     */
    createApiToken(payload: DotApiTokenCreatePayload): Observable<DotApiTokenCreateResult> {
        return this.#http
            .post<DotApiTokenCreateResponse>('/api/v1/apitoken', payload)
            .pipe(map((response) => response.entity));
    }

    /**
     * Mints a fresh JWT for an existing token id. Stateless: the token
     * record itself is unchanged and previously-issued JWTs stay valid
     * until the token is revoked or expires — revoke is the only
     * mechanism that kills a leaked JWT. Backend refuses this on
     * revoked/expired tokens (400).
     */
    getApiTokenJwt(tokenId: string): Observable<string> {
        return this.#http
            .get<{ entity: { jwt: string } }>(`/api/v1/apitoken/${encodeURIComponent(tokenId)}/jwt`)
            .pipe(
                map((response) => {
                    const jwt = response.entity?.jwt;
                    // Throwing here surfaces a malformed response
                    // through the tab's httpErrorManager instead of
                    // leaving the reveal dialog spinning on `''`.
                    if (typeof jwt !== 'string' || jwt.length === 0) {
                        throw new Error('Malformed JWT response');
                    }

                    return jwt;
                })
            );
    }

    /**
     * Soft-revokes a token. The row stays in the list (with
     * `revoked=true`, `revokedDate` populated) so an admin can prove
     * when it was disabled; a subsequent DELETE is required to purge.
     */
    revokeApiToken(tokenId: string): Observable<unknown> {
        return this.#http.put(`/api/v1/apitoken/${encodeURIComponent(tokenId)}/revoke`, null);
    }

    /**
     * Hard-deletes a token. No UI surface yet — the current tab shows
     * a static "Revoked" pill for inactive rows instead of a Delete
     * button. Left in place so the wiring is ready when that follow-up
     * lands.
     */
    deleteApiToken(tokenId: string): Observable<unknown> {
        return this.#http.delete(`/api/v1/apitoken/${encodeURIComponent(tokenId)}`);
    }
}
