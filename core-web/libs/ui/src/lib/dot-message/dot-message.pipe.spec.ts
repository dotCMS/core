import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotMessagePipe } from './dot-message.pipe';

describe('DotMessagePipe', () => {
    let messageServiceMock: MockDotMessageService;
    let pipe: DotMessagePipe;
    beforeEach(() => {
        messageServiceMock = new MockDotMessageService({
            'apps.search.placeholder': 'Search',
            'apps.best': 'Test {0} {1}'
        });
        pipe = new DotMessagePipe(messageServiceMock as unknown as DotMessageService);
    });

    it('should return empty string param is undefined', () => {
        expect(pipe.transform(undefined)).toEqual('');
    });

    it('should return message requested', () => {
        expect(pipe.transform('apps.search.placeholder')).toEqual('Search');
    });

    it('should return message requested with replaced values', () => {
        expect(pipe.transform('apps.best', ['Costa Rica', 'Panama'])).toEqual(
            'Test Costa Rica Panama'
        );
    });

    it('should return key if message not found', () => {
        expect(pipe.transform('no.label.found')).toEqual('no.label.found');
    });
});
