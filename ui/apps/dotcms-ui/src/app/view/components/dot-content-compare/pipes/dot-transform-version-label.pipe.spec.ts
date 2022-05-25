import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CoreWebService, CoreWebServiceMock, DotcmsConfigService } from '@dotcms/dotcms-js';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { of } from 'rxjs';
import { dotContentCompareTableDataMock } from '../components/dot-content-compare-table/dot-content-compare-table.component.spec';
import { DotTransformVersionLabelPipe } from './dot-transform-version-label.pipe';
import { formatInTimeZone } from 'date-fns-tz';

describe('DotTransformVersionLabelPipe', () => {
    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotMessageService,
                DotFormatDateService,
                {
                    provide: CoreWebService,
                    useClass: CoreWebServiceMock
                },
                {
                    provide: DotcmsConfigService,
                    useValue: {
                        getSystemTimeZone: () =>
                            of({
                                id: 'CST',
                                label: 'Central Standard Time',
                                offset: -36000
                            })
                    }
                }
            ]
        });
    });

    it('transform label with modUserName, using date older than 7 days (no relative date transform)', () => {
        const dotMessageService: DotMessageService = TestBed.inject(DotMessageService);
        const dotFormatDateService: DotFormatDateService = TestBed.inject(DotFormatDateService);

        const pipe = new DotTransformVersionLabelPipe(dotFormatDateService, dotMessageService);
        expect(pipe.transform(dotContentCompareTableDataMock.working)).toEqual(
            '12/15/2021 - 02:56 PM by Admin User'
        );
    });

    // TODO find a way to make datetimes match using timezones in remote server
    xit('transform label with modUserName, using date within 7 days (relative date transform)', () => {
        const dotMessageService: DotMessageService = TestBed.inject(DotMessageService);
        const dotFormatDateService: DotFormatDateService = TestBed.inject(DotFormatDateService);
        const currentDay = formatInTimeZone(new Date(), 'CST', 'MM/dd/YYY HH:mm:ssXXX');
        let relativeExpected = dotFormatDateService.getRelative(
            new Date(currentDay).getTime().toString()
        );

        const pipe = new DotTransformVersionLabelPipe(dotFormatDateService, dotMessageService);
        let pipeResult = pipe.transform({
            ...dotContentCompareTableDataMock.working,
            modDate: `${currentDay}`
        });

        // this is needed due to race conditions
        relativeExpected = relativeExpected.replace('1 second', '0 seconds');
        pipeResult = pipeResult.replace('1 second', '0 seconds');

        expect(pipeResult).toEqual(`${relativeExpected} by Admin User`);
    });
});
