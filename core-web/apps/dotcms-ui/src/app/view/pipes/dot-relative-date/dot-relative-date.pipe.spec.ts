import { fakeAsync, tick } from '@angular/core/testing';

import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import { DotcmsConfigServiceMock } from '@dotcms/utils-testing';

import { DotRelativeDatePipe } from './dot-relative-date.pipe';

const MILLISECONDS_IN_A_DAY = 86400000;

const EIGHT_DAYS = MILLISECONDS_IN_A_DAY * 8;

const TWO_DAYS = MILLISECONDS_IN_A_DAY * 2;

const ONE_DAY = MILLISECONDS_IN_A_DAY * 1;

const DATE = '2020/12/3';
const DATE_AND_TIME = '2020/12/3, 10:08 PM';

describe('DotRelativeDatePipe', () => {
    let formatDateService: DotFormatDateService;
    let pipe: DotRelativeDatePipe;

    beforeEach(() => {
        formatDateService = new DotFormatDateService(
            new DotcmsConfigServiceMock() as unknown as DotcmsConfigService
        );

        pipe = new DotRelativeDatePipe(formatDateService as unknown as DotFormatDateService);
    });

    describe('relative', () => {
        it('should set relative date', fakeAsync(() => {
            const date = new Date();

            tick(1000);

            expect(pipe.transform(date.getTime())).toEqual('1 second ago');
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

        it('should call dotFormatService.getUTC 3 times when its not a timestamp', () => {
            const date = new Date();

            spyOn(formatDateService, 'getUTC').and.callThrough();

            pipe.transform(date.getTime()); // This is milliseconds not a timestamp

            expect(formatDateService.getUTC).toHaveBeenCalledTimes(3);
        });

        it('should call dotFormatService.getUTC just once when its a timestamp and more than 7 days', fakeAsync(() => {
            const date = DATE_AND_TIME; // This is from 2020 so it's more than 7 days

            spyOn(formatDateService, 'getUTC').and.callThrough();

            pipe.transform(date); // This is a hardcoded timestamp

            expect(formatDateService.getUTC).toHaveBeenCalledTimes(1); // It gets called once in this workflow
        }));

        it('should call dotFormatService.getUTC twice when its a timestamp and less than 7 days', fakeAsync(() => {
            const date = new Date().toUTCString(); // This is now so it's less than 7 days

            spyOn(formatDateService, 'getUTC').and.callThrough();

            pipe.transform(date); // This is a hardcoded timestamp

            expect(formatDateService.getUTC).toHaveBeenCalledTimes(2); // It gets called twice in this workflow
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
            const N_DAYS_MILLISECONDS = (N_DAYS + 1) * MILLISECONDS_IN_A_DAY;

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
