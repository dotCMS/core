import { interval } from 'rxjs';

import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';

import { map, startWith } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { DotConfigurationCardComponent } from '../../components/dot-configuration-card/dot-configuration-card.component';
import { DotConfigurationStore } from '../../store/dot-configuration.store';

/** How often the server-time badge refreshes. It shows minutes, so once a minute is enough. */
const CLOCK_TICK_MS = 60_000;

const UTC_TIME = new Intl.DateTimeFormat('en-GB', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    timeZone: 'UTC'
});

interface SelectOption {
    label: string;
    value: string;
}

/**
 * Locale card: the language and time zone used when a user has not set their own, plus a badge
 * with the current time in UTC. No endpoint returns the server clock, so the badge formats the
 * browser clock; both run against the same UTC.
 */
@Component({
    selector: 'dot-configuration-locale',
    imports: [FormsModule, SelectModule, TagModule, DotMessagePipe, DotConfigurationCardComponent],
    templateUrl: './dot-configuration-locale.component.html',
    styleUrls: ['./dot-configuration-locale.component.scss']
})
export class DotConfigurationLocaleComponent {
    protected readonly store = inject(DotConfigurationStore);

    protected readonly $utcTime = toSignal(
        interval(CLOCK_TICK_MS).pipe(
            startWith(0),
            map(() => UTC_TIME.format(new Date()))
        ),
        { initialValue: UTC_TIME.format(new Date()) }
    );

    protected readonly $languageOptions = computed<SelectOption[]>(() =>
        this.store.adminLocales().map((locale) => ({
            label: locale.displayName,
            value: `${locale.language}_${locale.country}`
        }))
    );

    protected readonly $timeZoneOptions = computed<SelectOption[]>(() =>
        this.store.timezones().map((timezone) => ({ label: timezone.label, value: timezone.id }))
    );

    protected readonly $locale = computed(() => this.store.draft()?.locale);

    onLanguageChange(languageId: string): void {
        this.store.patchLocale({ languageId });
    }

    onTimeZoneChange(timeZoneId: string): void {
        this.store.patchLocale({ timeZoneId });
    }
}
