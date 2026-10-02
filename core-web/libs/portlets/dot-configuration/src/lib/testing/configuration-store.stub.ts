import { signal } from '@angular/core';

import { DotLoginLanguage, DotSystemTimezone } from '@dotcms/dotcms-models';

import { FAKE_ADMIN_LOCALES, createFakeCompanyConfiguration } from './fake-company-configuration';

import {
    DotConfigurationDraft,
    DotConfigurationErrors,
    toDraft
} from '../store/dot-configuration.mappers';

/**
 * Writable stand-in for `DotConfigurationStore`, for component specs. Signals can be set per
 * test; methods are spies.
 */
export const createConfigurationStoreStub = () => ({
    draft: signal<DotConfigurationDraft | null>(toDraft(createFakeCompanyConfiguration())),
    errors: signal<DotConfigurationErrors>({}),
    saving: signal(false),
    keyDigest: signal(createFakeCompanyConfiguration().keyDigest ?? ''),
    regeneratingKey: signal(false),
    adminLocales: signal<DotLoginLanguage[]>(FAKE_ADMIN_LOCALES),
    timezones: signal<DotSystemTimezone[]>([
        { id: 'UTC', label: 'Coordinated Universal Time (UTC)', offset: 0 },
        { id: 'Europe/Madrid', label: 'Central European Time (Europe/Madrid)', offset: 3600000 }
    ]),
    patchBranding: vi.fn(),
    patchLocale: vi.fn(),
    setAuthType: vi.fn()
});

export type DotConfigurationStoreStub = ReturnType<typeof createConfigurationStoreStub>;
