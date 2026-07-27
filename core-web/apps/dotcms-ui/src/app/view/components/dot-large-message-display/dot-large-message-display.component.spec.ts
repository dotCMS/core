import { createComponentFactory, Spectator } from '@openng/spectator/jest';
import { Subject } from 'rxjs';

import { fakeAsync, tick } from '@angular/core/testing';

import { DotEventsSocket } from '@dotcms/data-access';

import { DotLargeMessageDisplayComponent } from './dot-large-message-display.component';

import { DotParseHtmlService } from '../../../api/services/dot-parse-html/dot-parse-html.service';

describe('DotLargeMessageDisplayComponent', () => {
    let spectator: Spectator<DotLargeMessageDisplayComponent>;
    const largeMessageSubject = new Subject<unknown>();
    const mockDotEventsSocket = {
        on: jest.fn().mockReturnValue(largeMessageSubject.asObservable())
    };

    const createComponent = createComponentFactory({
        component: DotLargeMessageDisplayComponent,
        detectChanges: false,
        imports: [],
        providers: [
            { provide: DotEventsSocket, useValue: mockDotEventsSocket },
            DotParseHtmlService
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create DotLargeMessageDisplayComponent', fakeAsync(() => {
        spectator.fixture.detectChanges(false); // run ngOnInit so component subscribes
        largeMessageSubject.next({
            title: 'title Test',
            height: '200',
            width: '1000',
            body: 'Hello World',
            code: { lang: 'eng', content: 'codeTest' }
        });
        spectator.fixture.detectChanges(false);
        tick();

        expect(spectator.component.messages[0].title).toBe('title Test');
        expect(spectator.component.messages[0].width).toBe('1000');
        expect(spectator.component.messages[0].height).toBe('200');
        expect(spectator.component.getMessageVisibility(spectator.component.messages[0])).toBe(
            true
        );
        expect(spectator.component.messages[0].code?.content).toBe('codeTest');
        expect(mockDotEventsSocket.on).toHaveBeenCalledWith('LARGE_MESSAGE');

        tick(0);
        spectator.fixture.detectChanges(false);
        expect(spectator.component.messages[0].body).toBe('Hello World');
    }));

    it('should render script tag from body', fakeAsync(() => {
        spectator.fixture.detectChanges(false);
        largeMessageSubject.next({
            title: 'title Test',
            body: '<h1>Hello World</h1><script>console.log("abc")</script>'
        });
        spectator.fixture.detectChanges(false);
        tick(0);

        expect(spectator.component.messages.length).toBe(1);
        expect(spectator.component.messages[0].body).toBe(
            '<h1>Hello World</h1><script>console.log("abc")</script>'
        );
    }));

    it('should render script tag from script property', fakeAsync(() => {
        spectator.fixture.detectChanges(false);
        largeMessageSubject.next({
            title: 'title Test',
            body: '<h1>Hello World</h1><script>console.log("abc")</script>',
            script: 'console.log("script from prop")'
        });
        spectator.fixture.detectChanges(false);
        tick(0);

        expect(spectator.component.messages.length).toBe(1);
        expect(spectator.component.messages[0].body).toBe(
            '<h1>Hello World</h1><script>console.log("abc")</script>'
        );
        expect(spectator.component.messages[0].script).toBe('console.log("script from prop")');
    }));

    it('should remove dialog when it is close', fakeAsync(() => {
        spectator.fixture.detectChanges(false);
        largeMessageSubject.next({
            title: 'title Test',
            body: '<h1>Hello World</h1><script>console.log("abc")</script>',
            script: 'console.log("script from prop")'
        });
        spectator.fixture.detectChanges(false);
        tick(0);

        expect(spectator.component.messages.length).toBe(1);
        const message = spectator.component.messages[0];
        spectator.component.onVisibilityChange(message, false);
        spectator.fixture.detectChanges(false);

        expect(spectator.component.messages.length).toBe(0);
    }));

    it('should set default height and width', fakeAsync(() => {
        spectator.fixture.detectChanges(false);
        largeMessageSubject.next({
            title: 'title Test',
            body: 'bodyTest',
            code: { lang: 'eng', content: 'codeTest' }
        });
        spectator.fixture.detectChanges(false);
        tick(0);

        const message = spectator.component.messages[0];
        expect(message.width).toBeUndefined();
        expect(message.height).toBeUndefined();
        expect(spectator.component.messages[0].title).toBe('title Test');
        expect(spectator.component.messages.length).toBe(1);
    }));

    it('should show two dialogs', fakeAsync(() => {
        spectator.fixture.detectChanges(false);
        largeMessageSubject.next({
            title: 'title Test',
            body: 'bodyTest',
            code: { lang: 'eng', content: 'codeTest' }
        });
        spectator.fixture.detectChanges(false);
        tick(0);

        expect(spectator.component.messages.length).toBe(1);

        largeMessageSubject.next({
            title: 'title Test 2',
            body: 'bodyTest 2',
            code: { lang: 'eng', content: 'codeTest 2' }
        });
        spectator.fixture.detectChanges(false);
        tick(0);

        expect(spectator.component.messages.length).toBe(2);
    }));
});
