import { Observable } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotEvent, DotGlobalMessage } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotGlobalMessageService } from './dot-global-message.service';

import { DotMessageService } from '../dot-alert-confirm/dot-alert-confirm.service';
import { DotEventsService } from '../dot-events/dot-events.service';

xdescribe('DotGlobalMessageService', () => {
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotEventsService: DotEventsService;
    let listenerDotGlobalMessage: Observable<DotEvent<DotGlobalMessage>>;

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

    it('should set the default DotGlobalMessage Object to the Display scenario', (done) => {
        dotGlobalMessageService.display();
        listenerDotGlobalMessage.subscribe((event: DotEvent<DotGlobalMessage>) => {
            expect(event.data).toEqual({ value: 'Loaded', life: 3000 });
            done();
        });
    });

    it('should set a specific text for the Display scenario', (done) => {
        dotGlobalMessageService.display('test');
        listenerDotGlobalMessage.subscribe((event: DotEvent<DotGlobalMessage>) => {
            expect(event.data).toEqual({ value: 'test', life: 3000 });
            done();
        });
    });

    it('should set a specific time for the Custom Display scenario', (done) => {
        dotGlobalMessageService.customDisplay('test', 1000);
        listenerDotGlobalMessage.subscribe((event: DotEvent<DotGlobalMessage>) => {
            expect(event.data).toEqual({ value: 'test', life: 1000 });
            done();
        });
    });

    it('should set the default DotGlobalMessage Object for the Loading scenario', (done) => {
        dotGlobalMessageService.loading();
        listenerDotGlobalMessage.subscribe((event: DotEvent<DotGlobalMessage>) => {
            expect(event.data).toEqual({ value: 'Loading...', type: 'loading' });
            done();
        });
    });

    it('should set a specific text for the Loading scenario', () => {
        dotGlobalMessageService.loading('TEST');
        listenerDotGlobalMessage.subscribe((event: DotEvent<DotGlobalMessage>) => {
            expect(event.data).toEqual({ value: 'TEST', type: 'loading' });
        });
    });

    it('should set the default DotGlobalMessage Object for the Error scenario', (done) => {
        dotGlobalMessageService.error();
        listenerDotGlobalMessage.subscribe((event: DotEvent<DotGlobalMessage>) => {
            expect(event.data).toEqual({ value: 'Error', life: 3000 });
            done();
        });
    });

    it('should set a specific text for the Error scenario', (done) => {
        dotGlobalMessageService.error('test error');
        listenerDotGlobalMessage.subscribe((event: DotEvent<DotGlobalMessage>) => {
            expect(event.data).toEqual({ value: 'test error', life: 3000 });
            done();
        });
    });
});
