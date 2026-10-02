/**
 * How users identify themselves on the login form. Stored on the company record.
 */
export const DotCompanyAuthType = {
    EMAIL_ADDRESS: 'emailAddress',
    USER_ID: 'userId'
} as const;

export type DotCompanyAuthType = (typeof DotCompanyAuthType)[keyof typeof DotCompanyAuthType];

/**
 * Company-wide configuration as returned by `GET /api/v1/configuration/branding` and by every
 * company configuration `PUT`, mirroring the backend `CompanyConfigView`.
 *
 * `languageId` and `timeZoneId` belong to the system default user, not to the company record;
 * the backend reads them from there. Image fields come back as `null` unless the stored value is
 * a `/dA/...` asset path.
 */
export interface DotCompanyConfiguration {
    companyId: string;
    companyName: string;
    portalURL: string;
    /** Sender in `Name <address>` form, e.g. `dotCMS Website <website@dotcms.com>`. */
    emailAddress: string;
    /** Mail domain; derived from `emailAddress` by the backend when saved blank. */
    mx: string;
    primaryColor: string | null;
    secondaryColor: string | null;
    /** Login page background color. Not shown in the UI, but must be sent back on every save. */
    backgroundColor: string | null;
    backgroundImage: string | null;
    loginScreenLogo: string | null;
    /** Admin top bar logo override. Enterprise only; the backend drops it on Community. */
    navBarLogo: string | null;
    authType: DotCompanyAuthType;
    /** SHA-256 digest of the company key. Only returned to CMS Administrators. */
    keyDigest: string | null;
    /** Admin UI locale of the default user, e.g. `en_US`. */
    languageId: string | null;
    /** Java time zone id of the default user, e.g. `America/New_York`. */
    timeZoneId: string | null;
}

/**
 * Body of `PUT /api/v1/configuration/branding`.
 *
 * The endpoint is a full replace: an optional field left empty or omitted is cleared on the
 * server, so callers must send back every value they did not change.
 */
export interface DotCompanyBrandingForm {
    portalURL: string;
    emailAddress: string;
    mx?: string;
    primaryColor: string;
    secondaryColor: string;
    backgroundColor?: string;
    backgroundImage?: string;
    loginScreenLogo?: string;
    navBarLogo?: string;
}

/**
 * Body of `PUT /api/v1/configuration/locale`.
 */
export interface DotCompanyLocaleForm {
    languageId: string;
    timeZoneId: string;
}
