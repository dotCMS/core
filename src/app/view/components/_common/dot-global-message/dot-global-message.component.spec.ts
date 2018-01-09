import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotGlobalMessageComponent } from './dot-global-message.component';
import { DotEventsService } from '../../../../api/services/dot-events/dot-events.service';

describe('DotGlobalMessageComponent', () => {
    let component: DotGlobalMessageComponent;
    let fixture: ComponentFixture<DotGlobalMessageComponent>;
    let dotEventsService: DotEventsService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotGlobalMessageComponent],
            providers: [DotEventsService]
        }).compileComponents();

        fixture = TestBed.createComponent(DotGlobalMessageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        dotEventsService = TestBed.get(DotEventsService);
    });

    it('should set the value of the message with the corresponding icon and life time ', () => {
        dotEventsService.notify('dot-global-message', { value: 'test', type: 'loading', life: 3000 });
        expect(component.message).toEqual({ value: 'test', type: 'fa fa-circle-o-notch fa-spin', life: 3000 });
    });

    it('should set visibility to false after 10 ms', () => {
        dotEventsService.notify('dot-global-message', { value: 'test', life: 10 });
        // TODO: Find a way to get rid of timeouts.
        setTimeout(() => {
            expect(component.visibility).toEqual(false);
        }, 90);
    });
});
