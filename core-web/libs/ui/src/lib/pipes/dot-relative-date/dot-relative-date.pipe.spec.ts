import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { DotFormatDateService, DotMessageService } from '@dotcms/data-access';
import { DotcmsConfigService, LoginService } from '@dotcms/dotcms-js';
import { DotRelativeDatePipe } from '@dotcms/ui';
import { DotcmsConfigServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

const ONE_DAY = 86400000;

const TWO_DAYS = ONE_DAY * 2;

const EIGHT_DAYS = ONE_DAY * 8;

const DATE = '2020/12/3';
const DATE_AND_TIME = '2020/12/3, 10:08 PM';

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
                    useValue: new MockDotMessageService({ new: 'New' })
                },
                DotFormatDateService,
                DotRelativeDatePipe
            ]
        });

        formatDateService = TestBed.inject(DotFormatDateService);
        pipe = TestBed.inject(DotRelativeDatePipe);
    });

    describe('relative', () => {
        it('should set `now` is the time is null', fakeAsync(() => {
            expect(pipe.transform(null)).toEqual('Now');
        }));

        it('should set relative date', fakeAsync(() => {
            const date = new Date();
            expect(pipe.transform(date.getTime())).toEqual('Now');
        }));

        it('should return relative date even if it is hardcoded date', fakeAsync(() => {
            const date = DATE;
            jasmine.clock().mockDate(new Date(DATE));

            jasmine.clock().tick(TWO_DAYS);

            expect(pipe.transform(date)).toEqual('2 days ago');
        }));

        it('should return relative date even if it is hardcoded date with time', fakeAsync(() => {
            const date = DATE_AND_TIME;

            jasmine.clock().mockDate(new Date(DATE_AND_TIME));

            jasmine.clock().tick(ONE_DAY);

            expect(pipe.transform(date)).toEqual('1 day ago');
        }));
    });

    describe('format date', () => {
        it('should return formated date after 7 days', fakeAsync(() => {
            const date = new Date();

            tick(EIGHT_DAYS);

            expect(pipe.transform(date.getTime())).toEqual(
                date.toLocaleDateString('en-US', {
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

        it('should return formated date after N days', fakeAsync(() => {
            const date = new Date();

            const N_DAYS = Math.round(Math.random() * 10);

            // Plus one because we need to go over N Days
            const N_DAYS_MILLISECONDS = (N_DAYS + 1) * ONE_DAY;

            tick(N_DAYS_MILLISECONDS);

            expect(pipe.transform(date.getTime(), 'MM/dd/yyyy', N_DAYS)).toEqual(
                date.toLocaleDateString('en-US', {
                    month: '2-digit',
                    day: '2-digit',
                    year: 'numeric'
                })
            );
        }));

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
