import { enUS, es } from 'date-fns/locale';

import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { DotFormatDateService, DotMessageService } from '@dotcms/data-access';
import { DotcmsConfigService, LoginService } from '@dotcms/dotcms-js';
import { DotcmsConfigServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotRelativeDatePipe } from './dot-relative-date.pipe';

@Component({
    imports: [DotRelativeDatePipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: '<span data-testid="timestamp">{{ date | dotRelativeDate }}</span>'
})
class RelativeDateHostComponent {
    readonly date = Date.now();
}

const ONE_DAY = 86400000;

const EIGHT_DAYS = ONE_DAY * 8;

const DATE = '2020/12/3';
const DATE_AND_TIME = '2020/12/3, 10:08 PM';

const getDateAndTimeFormat = (date: Date): string => {
    return date.toLocaleDateString('en-US', {
        month: '2-digit',
        day: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        hour12: true
    });
};

describe('DotRelativeDatePipe', () => {
    let formatDateService: DotFormatDateService;
    let pipe: DotRelativeDatePipe;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                {
                    provide: LoginService,
                    useValue: { currentUserLanguageId: 'en-US' }
                },
                {
                    provide: DotcmsConfigService,
                    useClass: DotcmsConfigServiceMock
                },
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({
                        'relative.date.now': 'Now',
                        'relative.date.minute.ago': '1 minute ago',
                        'relative.date.minutes.ago': '{0} minutes ago',
                        'relative.date.hours.ago': '{0} hours ago'
                    })
                },
                DotFormatDateService,
                DotRelativeDatePipe
            ]
        });

        formatDateService = TestBed.inject(DotFormatDateService);
        pipe = TestBed.inject(DotRelativeDatePipe);
    });

    describe('bundle recovery in an OnPush view', () => {
        const revision = signal(0);
        const version = signal<string | null>(null);
        const language = signal<string | null>(null);
        let messages: Record<string, string>;

        beforeEach(() => {
            revision.set(0);
            version.set(null);
            language.set(null);
            messages = {};
            Object.assign(TestBed.inject(DotMessageService), {
                loadedBundleRevision: revision.asReadonly(),
                loadedBundleVersion: version.asReadonly(),
                loadedBundleLanguage: language.asReadonly(),
                get: (key: string) => messages[key] ?? key
            });
        });

        it('should recover unchanged timestamps after a late bundle install and retain tracking on cache hits', () => {
            const fixture = TestBed.createComponent(RelativeDateHostComponent);
            fixture.detectChanges();
            const text = () =>
                fixture.nativeElement.querySelector('[data-testid="timestamp"]').textContent;
            expect(text()).toBe('relative.date.now');

            messages = { 'relative.date.now': 'Now' };
            language.set('en');
            version.set('build-1');
            fixture.detectChanges();
            expect(text()).toBe('Now');

            fixture.changeDetectorRef.markForCheck();
            fixture.detectChanges();
            messages = { 'relative.date.now': 'Recovered now' };
            version.set('build-2');
            fixture.detectChanges();
            expect(text()).toBe('Recovered now');
        });

        it('refreshes an unchanged timestamp when the same-version bundle is replaced', () => {
            version.set('same-build');
            language.set('en');
            messages = { 'relative.date.now': 'Now' };
            const fixture = TestBed.createComponent(RelativeDateHostComponent);
            fixture.detectChanges();
            messages = { 'relative.date.now': 'Fresh now' };
            revision.update((value) => value + 1);
            fixture.detectChanges();
            expect(
                fixture.nativeElement.querySelector('[data-testid="timestamp"]').textContent
            ).toBe('Fresh now');
        });

        it('should update for a language change with the same bundle version', () => {
            version.set('same-build');
            language.set('en');
            messages = { 'relative.date.now': 'Now' };
            const fixture = TestBed.createComponent(RelativeDateHostComponent);
            fixture.detectChanges();

            messages = { 'relative.date.now': 'Ahora' };
            language.set('es');
            fixture.detectChanges();
            expect(
                fixture.nativeElement.querySelector('[data-testid="timestamp"]').textContent
            ).toBe('Ahora');
        });
    });

    it('should cache date calculations until inputs, bundle identity or date locale change', () => {
        const calculate = vi.spyOn(formatDateService, 'differenceInCalendarDays');
        const date = Date.now() - ONE_DAY * 2;
        formatDateService.localeOptions = { locale: enUS };
        expect(pipe.transform(date)).toBe('2 days ago');
        pipe.transform(date);
        expect(calculate).toHaveBeenCalledTimes(1);

        formatDateService.localeOptions = { locale: es };
        expect(pipe.transform(date)).toBe('hace 2 días');
        expect(calculate).toHaveBeenCalledTimes(2);
        pipe.transform(date, 'yyyy', 0);
        expect(calculate).toHaveBeenCalledTimes(3);
    });

    describe('relative', () => {
        it('should set `now` is the time is null', () => {
            expect(pipe.transform(null)).toEqual('Now');
        });

        it('should set relative date', () => {
            const date = new Date();
            expect(pipe.transform(date.getTime())).toEqual('Now');
        });

        it('should return relative date even if it is hardcoded date', () => {
            const date = new Date();
            date.setDate(date.getDate() - 2);
            const dateFormat = getDateAndTimeFormat(date);

            expect(pipe.transform(dateFormat)).toEqual('2 days ago');
        });

        it('should return relative date even if it is hardcoded date with time', () => {
            const date = new Date();
            date.setDate(date.getDate() - 1);
            const dateAndTime = getDateAndTimeFormat(date);
            expect(pipe.transform(dateAndTime)).toEqual('1 day ago');
        });

        it('should return minutes ago for time less than 2 hours', () => {
            const date = new Date();
            date.setMinutes(date.getMinutes() - 30);
            expect(pipe.transform(date.getTime())).toEqual('30 minutes ago');
        });

        it('should return hours ago for time less than 24 hours', () => {
            const date = new Date();
            date.setHours(date.getHours() - 5);
            expect(pipe.transform(date.getTime())).toEqual('5 hours ago');
        });
    });

    describe('format date', () => {
        it('should return formated date after 7 days', fakeAsync(() => {
            const date = new Date();

            tick(EIGHT_DAYS);

            // The pipe converts millisecond inputs to UTC before formatting,
            // so the expected value must use the same UTC-shifted date.
            // Comparing against `date.toLocaleDateString` directly would be
            // flaky around midnight in any non-UTC timezone.
            expect(pipe.transform(date.getTime())).toEqual(
                formatDateService.getUTC(date).toLocaleDateString('en-US', {
                    month: '2-digit',
                    day: '2-digit',
                    year: 'numeric'
                })
            );
        }));

        it('should return formated date even if it is hardcoded date', fakeAsync(() => {
            const date = DATE;

            tick(EIGHT_DAYS);

            expect(pipe.transform(date)).toEqual('12/03/2020');
        }));

        it('should return formated date even if it is hardcoded date with time', fakeAsync(() => {
            const date = DATE_AND_TIME;

            tick(EIGHT_DAYS);

            expect(pipe.transform(date)).toEqual('12/03/2020');
        }));

        it('should return formated date after N days when timeStampAfter is less than N days', () => {
            const date = new Date();
            const N_DAYS = Math.round(Math.random() * 10);
            const timeStampAfter = N_DAYS - 1;

            date.setDate(date.getDate() - N_DAYS); //Set the date to N days before
            // Match the pipe's UTC-shift semantics in the expected value
            // so the assertion isn't timezone-flaky around midnight.
            expect(pipe.transform(date.getTime(), 'MM/dd/yyyy', timeStampAfter)).toEqual(
                formatDateService.getUTC(date).toLocaleDateString('en-US', {
                    month: '2-digit',
                    day: '2-digit',
                    year: 'numeric'
                })
            );
        });

        it('should return formated date with specified format', fakeAsync(() => {
            const date = new Date();

            const format = 'MM/dd/yyyy, hh:mm aa';

            tick(EIGHT_DAYS);

            expect(pipe.transform(date.getTime(), format)).toEqual(
                formatDateService.getUTC(date).toLocaleDateString('en-US', {
                    month: '2-digit',
                    day: '2-digit',
                    year: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                    hour12: true
                })
            );
        }));
    });
});
