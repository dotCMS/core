import { createPipeFactory, SpectatorPipe } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotMessagePipe } from './dot-message.pipe';

describe('DotMessagePipe', () => {
    let spectator: SpectatorPipe<DotMessagePipe>;

    const mockMessages = {
        'apps.search.placeholder': 'Search',
        'apps.best': 'Test {0} {1}'
    };

    const messageServiceMock = new MockDotMessageService(mockMessages);

    const createPipe = createPipeFactory({
        pipe: DotMessagePipe,
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
    });

    it('should return empty string if param is undefined', () => {
        spectator = createPipe(`{{ value | dm }}`, {
            hostProps: { value: undefined }
        });
        expect(spectator.element.textContent).toBe('');
    });

    it('should return message requested', () => {
        spectator = createPipe(`{{ value | dm }}`, {
            hostProps: { value: 'apps.search.placeholder' }
        });
        expect(spectator.element.textContent).toBe('Search');
    });

    it('should return message requested with replaced values', () => {
        spectator = createPipe(`{{ value | dm:args }}`, {
            hostProps: { value: 'apps.best', args: ['Costa Rica', 'Panama'] }
        });
        expect(spectator.element.textContent).toBe('Test Costa Rica Panama');
    });

    it('should return key if message not found', () => {
        spectator = createPipe(`{{ value | dm }}`, {
            hostProps: { value: 'no.label.found' }
        });
        expect(spectator.element.textContent).toBe('no.label.found');
    });
});
