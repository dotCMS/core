import { TestBed } from '@angular/core/testing';

import { DotEventsService } from './dot-events.service';

describe('DotEventsService', () => {
    let dotEventsService: DotEventsService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotEventsService]
        });

        dotEventsService = TestBed.inject(DotEventsService);
    });
    it('should filter notifications based on event name', () => {
        let timesCalled = 0;
        let randomEvent = 0;
        dotEventsService.listen('test').subscribe(() => {
            timesCalled++;
        });
        dotEventsService.listen('randomEvent').subscribe(() => {
            randomEvent++;
        });

        dotEventsService.notify<number[]>('test', [1, 2, 3]);
        dotEventsService.notify('randomEvent');

        expect(timesCalled).toEqual(1);
        expect(randomEvent).toEqual(1);
    });

    it('should notify subscribers', () => {
        let numbersArray: number[] = [];
        dotEventsService.listen('test').subscribe((value) => {
            numbersArray = value.data as number[];
        });

        dotEventsService.notify<number[]>('test', [1, 2, 3]);

        expect(numbersArray).toEqual([1, 2, 3]);
    });
});
