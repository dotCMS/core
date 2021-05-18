import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotGlobalMessageComponent } from './dot-global-message.component';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotIconModule } from '@dotcms/ui';
import { DotSpinnerModule } from '@dotcms/ui';
import { By } from '@angular/platform-browser';

describe('DotGlobalMessageComponent', () => {
    let component: DotGlobalMessageComponent;
    let fixture: ComponentFixture<DotGlobalMessageComponent>;
    let dotEventsService: DotEventsService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotGlobalMessageComponent],
            providers: [DotEventsService],
            imports: [DotIconModule, DotSpinnerModule]
        }).compileComponents();

        fixture = TestBed.createComponent(DotGlobalMessageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        dotEventsService = TestBed.get(DotEventsService);
    });

    it('should set the value of the message with the corresponding icon and life time', () => {
        dotEventsService.notify('dot-global-message', {
            value: 'test',
            type: 'loading',
            life: 3000
        });
        expect(component.message).toEqual({
            value: 'test',
            type: 'loading',
            life: 3000
        });
    });

    it('should show dotSpinner for events type loading', () => {
        dotEventsService.notify('dot-global-message', { value: 'test', type: 'loading' });
        fixture.detectChanges();
        const dotSpinner = fixture.debugElement.query(By.css('dot-spinner'));
        const dotIcon = fixture.debugElement.query(By.css('dot-icon'));

        expect(dotSpinner).toBeDefined();
        expect(dotIcon).toBeNull();
    });

    it('should show dotIcon for any event type expect loading', () => {
        dotEventsService.notify('dot-global-message', { value: 'test' });
        fixture.detectChanges();
        const dotSpinner = fixture.debugElement.query(By.css('dot-spinner'));
        const dotIcon = fixture.debugElement.query(By.css('dot-icon'));

        expect(dotSpinner).toBeNull();
        expect(dotIcon).toBeDefined();
    });

    it('should set visibility to false after 10 ms', (done) => {
        dotEventsService.notify('dot-global-message', { value: 'test', life: 10 });
        expect(component.classes).toContain('dot-global-message--visible');
        // TODO: Find a way to get rid of timeouts.
        setTimeout(() => {
            expect(component.classes).toEqual(' ');
            done();
        }, 50);
    });
});
