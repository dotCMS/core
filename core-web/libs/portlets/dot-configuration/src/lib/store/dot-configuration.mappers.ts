import {
    DotCompanyAuthType,
    DotCompanyBrandingForm,
    DotCompanyConfiguration,
    DotCompanyLocaleForm
} from '@dotcms/dotcms-models';

/**
 * The page saves through three independent endpoints. They are listed in the order they are
 * sent: branding first, because it is the largest form and the most likely to be rejected.
 */
export const DotConfigurationSection = {
    BRANDING: 'branding',
    AUTHENTICATION: 'authentication',
    LOCALE: 'locale'
} as const;

export type DotConfigurationSection =
    (typeof DotConfigurationSection)[keyof typeof DotConfigurationSection];

export const SECTION_SAVE_ORDER: readonly DotConfigurationSection[] = [
    DotConfigurationSection.BRANDING,
    DotConfigurationSection.AUTHENTICATION,
    DotConfigurationSection.LOCALE
];

/** Fields saved through `PUT /branding`: the Branding and Outbound Communication cards. */
export interface DotConfigurationBranding {
    portalURL: string;
    emailAddress: string;
    mx: string;
    primaryColor: string;
    secondaryColor: string;
    backgroundColor: string;
    backgroundImage: string;
    loginScreenLogo: string;
    navBarLogo: string;
}

export interface DotConfigurationLocale {
    languageId: string;
    timeZoneId: string;
}

/** What the page edits. Empty strings stand for values the server returns as `null`. */
export interface DotConfigurationDraft {
    branding: DotConfigurationBranding;
    authType: DotCompanyAuthType;
    locale: DotConfigurationLocale;
}

export type DotConfigurationField =
    | keyof DotConfigurationBranding
    | keyof DotConfigurationLocale
    | 'authType';

/** Field name → i18n key of the message explaining why the value is rejected. */
export type DotConfigurationErrors = Partial<Record<DotConfigurationField, string>>;

/**
 * Values Restore Defaults puts back in the form. They are the ones dotCMS ships in its starter
 * data. Logos are left alone: the starter logo is an asset that may not exist on this instance.
 */
export const DOT_CONFIGURATION_DEFAULTS: Pick<
    DotConfigurationBranding,
    'primaryColor' | 'secondaryColor' | 'backgroundColor' | 'backgroundImage'
> = {
    primaryColor: '#576be8',
    secondaryColor: '#2f3e6c',
    backgroundColor: '#1b3359',
    backgroundImage: '/html/images/backgrounds/bg-11.jpg'
};

const HEX_COLOR = /^#([0-9a-f]{3}|[0-9a-f]{4}|[0-9a-f]{6}|[0-9a-f]{8})$/i;

// Either `Name <address>` or a bare address. Written without nested quantifiers so it runs in
// linear time; the server's own parser is the final word.
const SENDER_EMAIL = /^(?:[^<>]*<[^\s<>@]+@[^\s<>@]+>|[^\s<>@]+@[^\s<>@]+)$/;

const ASSET_PATH = /^\/dA\//;

const BUNDLED_BACKGROUND = /^\/html\/images\/backgrounds\/bg-(?:[1-9]|1[01])\.jpg$/;

/**
 * Builds the editable draft from the configuration the server returned.
 *
 * @param view - Configuration as returned by the API.
 * @returns The draft, with `null` values turned into empty strings.
 */
export function toDraft(view: DotCompanyConfiguration): DotConfigurationDraft {
    return {
        branding: {
            portalURL: view.portalURL ?? '',
            emailAddress: view.emailAddress ?? '',
            mx: view.mx ?? '',
            primaryColor: view.primaryColor ?? '',
            secondaryColor: view.secondaryColor ?? '',
            backgroundColor: view.backgroundColor ?? '',
            backgroundImage: view.backgroundImage ?? '',
            loginScreenLogo: view.loginScreenLogo ?? '',
            navBarLogo: view.navBarLogo ?? ''
        },
        authType: view.authType,
        locale: {
            languageId: view.languageId ?? '',
            timeZoneId: view.timeZoneId ?? ''
        }
    };
}

/**
 * Builds the body of `PUT /branding`. Every field is sent, including the ones the page does not
 * show, because the endpoint clears whatever is left out.
 *
 * @param branding - Branding values from the draft.
 * @returns The request body; empty optional values are omitted so the server stores them blank.
 */
export function toBrandingForm(branding: DotConfigurationBranding): DotCompanyBrandingForm {
    const optional = (value: string) => (value ? value : undefined);

    return {
        portalURL: branding.portalURL,
        emailAddress: branding.emailAddress,
        mx: optional(branding.mx),
        primaryColor: branding.primaryColor,
        secondaryColor: branding.secondaryColor,
        backgroundColor: optional(branding.backgroundColor),
        backgroundImage: optional(branding.backgroundImage),
        loginScreenLogo: optional(branding.loginScreenLogo),
        navBarLogo: optional(branding.navBarLogo)
    };
}

/**
 * Builds the body of `PUT /locale`.
 *
 * @param locale - Locale values from the draft.
 * @returns The request body.
 */
export function toLocaleForm(locale: DotConfigurationLocale): DotCompanyLocaleForm {
    return { languageId: locale.languageId, timeZoneId: locale.timeZoneId };
}

/**
 * Tells which sections of the draft differ from what the server last stored.
 *
 * @param original - Values last stored on the server.
 * @param draft - Values currently in the form.
 * @returns One flag per section.
 */
export function dirtySections(
    original: DotConfigurationDraft,
    draft: DotConfigurationDraft
): Record<DotConfigurationSection, boolean> {
    return {
        branding: !shallowEqual(original.branding, draft.branding),
        authentication: original.authType !== draft.authType,
        locale: !shallowEqual(original.locale, draft.locale)
    };
}

/**
 * Checks every field the page lets the user edit.
 *
 * @param draft - Values currently in the form.
 * @returns The rejected fields, each with the i18n key of its message. Empty when valid.
 */
export function validate(draft: DotConfigurationDraft): DotConfigurationErrors {
    const errors: DotConfigurationErrors = {};
    const { branding, locale } = draft;

    if (!HEX_COLOR.test(branding.primaryColor)) {
        errors.primaryColor = 'configuration.validation.color';
    }

    if (!HEX_COLOR.test(branding.secondaryColor)) {
        errors.secondaryColor = 'configuration.validation.color';
    }

    if (!branding.portalURL.trim()) {
        errors.portalURL = 'configuration.validation.required';
    } else if (/[<>]/.test(branding.portalURL)) {
        errors.portalURL = 'configuration.validation.portal-url';
    }

    if (!SENDER_EMAIL.test(branding.emailAddress.trim())) {
        errors.emailAddress = 'configuration.validation.email';
    }

    if (!branding.loginScreenLogo) {
        errors.loginScreenLogo = 'configuration.validation.required';
    } else if (!ASSET_PATH.test(branding.loginScreenLogo)) {
        errors.loginScreenLogo = 'configuration.validation.asset-path';
    }

    if (
        branding.backgroundImage &&
        !ASSET_PATH.test(branding.backgroundImage) &&
        !BUNDLED_BACKGROUND.test(branding.backgroundImage)
    ) {
        errors.backgroundImage = 'configuration.validation.asset-path';
    }

    if (branding.navBarLogo && !ASSET_PATH.test(branding.navBarLogo)) {
        errors.navBarLogo = 'configuration.validation.asset-path';
    }

    if (!locale.languageId) {
        errors.languageId = 'configuration.validation.required';
    }

    if (!locale.timeZoneId) {
        errors.timeZoneId = 'configuration.validation.required';
    }

    return errors;
}

function shallowEqual<T extends object>(a: T, b: T): boolean {
    const keys = Object.keys(a) as (keyof T)[];

    return keys.every((key) => a[key] === b[key]);
}
