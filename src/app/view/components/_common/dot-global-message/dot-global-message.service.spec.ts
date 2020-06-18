import { TestBed } from '@angular/core/testing';

import { DotGlobalMessageService } from './dot-global-message.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotEvent } from '@models/dot-event/dot-event';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { Observable } from 'rxjs';

describe('DotGlobalMessageService', () => {
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotEventsService: DotEventsService;
    let listenerDotGlobalMessage: Observable<DotEvent>;

    const messageServiceMock = new MockDotMessageService({
        'dot.common.message.loading': 'Loading...',
        'dot.common.message.loaded': 'Loaded',
        'dot.common.message.error': 'Error'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotGlobalMessageService,
                DotMessageService,
                DotEventsService,
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        dotGlobalMessageService = TestBed.get(DotGlobalMessageService);
        dotEventsService = TestBed.get(DotEventsService);

        listenerDotGlobalMessage = dotEventsService.listen('dot-global-message');
    });

    it('should set the default DotGlobalMessage Object to the Display scenario', () => {
        dotGlobalMessageService.display();
        listenerDotGlobalMessage.subscribe((event: DotEvent) => {
            expect(event.data).toEqual({ value: 'Loaded', life: 3000 });
        });
    });

    it('should set a specific text for the Display scenario', () => {
        dotGlobalMessageService.display('test');
        listenerDotGlobalMessage.subscribe((event: DotEvent) => {
            expect(event.data).toEqual({ value: 'test', life: 3000 });
        });
    });

    it('should set the default DotGlobalMessage Object for the Loading scenario', () => {
        dotGlobalMessageService.loading();
        listenerDotGlobalMessage.subscribe((event: DotEvent) => {
            expect(event.data).toEqual({ value: 'Loading...', type: 'loading' });
        });
    });

    it('should set a specific text for the Loading scenario', () => {
        dotGlobalMessageService.loading('TEST');
        listenerDotGlobalMessage.subscribe((event: DotEvent) => {
            expect(event.data).toEqual({ value: 'TEST', type: 'loading' });
        });
    });

    it('should set the default DotGlobalMessage Object for the Error scenario', () => {
        dotGlobalMessageService.error();
        listenerDotGlobalMessage.subscribe((event: DotEvent) => {
            expect(event.data).toEqual({ value: 'Error', life: 3000 });
        });
    });

    it('should set a specific text for the Error scenario', () => {
        dotGlobalMessageService.error('test error');
        listenerDotGlobalMessage.subscribe((event: DotEvent) => {
            expect(event.data).toEqual({ value: 'test error', life: 3000 });
        });
    });
});
