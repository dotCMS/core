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

/** How often the time badge refreshes. It shows minutes, so once a minute is enough. */
const CLOCK_TICK_MS = 60_000;

const FALLBACK_TIME_ZONE = 'UTC';

interface SelectOption {
    label: string;
    value: string;
}

/**
 * Current time in a time zone, with the zone's short name: `16:20 GMT+2`, `09:20 CDT`.
 *
 * @param now - Moment to show.
 * @param timeZoneId - Java time zone ID, as listed by the server.
 * @param rawOffset - The zone's offset from UTC in milliseconds, without daylight saving.
 * @returns The formatted time. A legacy Java ID the browser does not recognize is shown with
 * its raw offset applied and its ID as the name, since daylight saving cannot be known for it.
 */
export function formatTimeInZone(now: Date, timeZoneId: string, rawOffset: number): string {
    try {
        return new Intl.DateTimeFormat('en-US', {
            hour: '2-digit',
            minute: '2-digit',
            hourCycle: 'h23',
            timeZone: timeZoneId,
            timeZoneName: 'short'
        }).format(now);
    } catch {
        const shifted = new Date(now.getTime() + rawOffset);
        const time = new Intl.DateTimeFormat('en-US', {
            hour: '2-digit',
            minute: '2-digit',
            hourCycle: 'h23',
            timeZone: 'UTC'
        }).format(shifted);

        return `${time} ${timeZoneId}`;
    }
}

/**
 * Locale card: the language and time zone used when a user has not set their own, plus a badge
 * with the current time in the time zone selected in the form, updated as the selection changes.
 */
@Component({
    selector: 'dot-configuration-locale',
    imports: [FormsModule, SelectModule, TagModule, DotMessagePipe, DotConfigurationCardComponent],
    templateUrl: './dot-configuration-locale.component.html',
    styleUrls: ['./dot-configuration-locale.component.scss']
})
export class DotConfigurationLocaleComponent {
    protected readonly store = inject(DotConfigurationStore);

    readonly #now = toSignal(
        interval(CLOCK_TICK_MS).pipe(
            startWith(0),
            map(() => new Date())
        ),
        { initialValue: new Date() }
    );

    protected readonly $zoneTime = computed(() => {
        const timeZoneId = this.store.draft()?.locale.timeZoneId || FALLBACK_TIME_ZONE;
        const rawOffset =
            this.store.timezones().find((timezone) => timezone.id === timeZoneId)?.offset ?? 0;

        return formatTimeInZone(this.#now(), timeZoneId, rawOffset);
    });

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
