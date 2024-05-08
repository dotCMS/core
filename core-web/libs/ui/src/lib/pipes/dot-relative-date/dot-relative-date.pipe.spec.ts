import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { DotFormatDateService, DotMessageService } from '@dotcms/data-access';
import { DotcmsConfigService, LoginService } from '@dotcms/dotcms-js';
import { DotRelativeDatePipe } from '@dotcms/ui';
import { DotcmsConfigServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

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

        it('should return formated date after N days when timeStampAfter is less than N days', () => {
            const date = new Date();
            const N_DAYS = Math.round(Math.random() * 10);
            const timeStampAfter = N_DAYS - 1;

            date.setDate(date.getDate() - N_DAYS); //Set the date to N days before
            expect(pipe.transform(date.getTime(), 'MM/dd/yyyy', timeStampAfter)).toEqual(
                date.toLocaleDateString('en-US', {
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
