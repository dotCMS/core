import { createPipeFactory, mockProvider, SpectatorPipe } from '@ngneat/spectator';

import { DotFormatDateService } from '@dotcms/ui';

import { DotTimestampToDatePipe } from './dot-timestamp-to-date.pipe';

const TIMESTAMP_MOCK = 1698789866;
const EXPECTED_DATE_MOCK = '10/29/2023, 12:43 PM';
describe('DotTimestampPipe ', () => {
    let spectator: SpectatorPipe<DotTimestampToDatePipe>;

    const createPipe = createPipeFactory({
        pipe: DotTimestampToDatePipe,
        providers: [
            DotFormatDateService,
            mockProvider(DotFormatDateService, { getDateFromTimestamp: () => EXPECTED_DATE_MOCK })
        ]
    });

    it('should transform the timestamp using getDateFromTimestamp to date format', () => {
        spectator = createPipe(`<div>{{ timestamp |  dotTimestampToDate }}</div>`, {
            hostProps: {
                TIMESTAMP_MOCK
            }
        });

        expect(spectator.element).toHaveText(EXPECTED_DATE_MOCK);
    });
});
