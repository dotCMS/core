import { DotUserDetail, DotUserListItem } from '../services/dot-users.service';

/**
 * Baseline `DotUserListItem` for tests. Kept exhaustive so callers can
 * override only the fields under test without repeating the boilerplate.
 * Callers should always spread the return value and mutate — the factory
 * returns a fresh object on every call so tests never share state.
 *
 * Lives under `lib/testing/` (not `@dotcms/utils-testing`) because
 * `DotUserListItem` / `DotUserDetail` are declared in this portlet's
 * service. Move to the shared testing lib once those types graduate to
 * `@dotcms/dotcms-models`.
 */
export function createFakeUser(overrides: Partial<DotUserListItem> = {}): DotUserListItem {
    return {
        userId: 'user-42',
        id: 'user-42',
        firstName: 'Jane',
        lastName: 'Doe',
        fullName: 'Jane Doe',
        name: 'Jane Doe',
        emailAddress: 'jane@dotcms.com',
        gravitar: '',
        active: true,
        admin: false,
        backendUser: true,
        frontendUser: false,
        hasConsoleAccess: true,
        lastLoginDate: null,
        lastLoginIP: null,
        failedLoginAttempts: 0,
        ...overrides
    };
}

/**
 * Baseline `DotUserDetail` — the list-item baseline extended with the
 * detail-only fields (birthday, additionalInfo, etc.). Detail-only
 * overrides pass through; list-item fields ride the shared baseline via
 * {@link createFakeUser}.
 */
export function createFakeUserDetail(overrides: Partial<DotUserDetail> = {}): DotUserDetail {
    return {
        ...createFakeUser(overrides),
        birthday: null,
        middleName: null,
        nickname: null,
        languageId: 'en-US',
        timeZoneId: null,
        male: null,
        female: null,
        additionalInfo: null,
        createDate: null,
        modificationDate: null,
        ...overrides
    };
}
