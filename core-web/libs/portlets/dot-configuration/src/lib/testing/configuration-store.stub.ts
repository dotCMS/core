import { signal } from '@angular/core';

import { DotLoginLanguage, DotSystemTimezone } from '@dotcms/dotcms-models';

import { FAKE_ADMIN_LOCALES, createFakeCompanyConfiguration } from './fake-company-configuration';

import {
    DotConfigurationDraft,
    DotConfigurationErrors,
    toDraft
} from '../store/dot-configuration.mappers';

/**
 * Writable signals standing in for `DotConfigurationStore` state, for component specs. Each spec
 * adds the store methods it needs as spies, so this file stays free of test-runner globals.
 */
export const createConfigurationStoreSignals = () => ({
    draft: signal<DotConfigurationDraft | null>(toDraft(createFakeCompanyConfiguration())),
    errors: signal<DotConfigurationErrors>({}),
    saving: signal(false),
    keyDigest: signal(createFakeCompanyConfiguration().keyDigest ?? ''),
    regeneratingKey: signal(false),
    adminLocales: signal<DotLoginLanguage[]>(FAKE_ADMIN_LOCALES),
    timezones: signal<DotSystemTimezone[]>([
        { id: 'UTC', label: 'Coordinated Universal Time (UTC)', offset: 0 },
        { id: 'Europe/Madrid', label: 'Central European Time (Europe/Madrid)', offset: 3600000 }
    ])
});
