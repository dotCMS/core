import { Injectable } from '@angular/core';

import { formatMessage } from '@dotcms/utils';

// Move all this translations to Language.properties
export const MessageServiceMock: Record<string, string> = {
    'block-editor.common.accept': 'Accept',
    'block-editor.common.delete': 'Delete',
    'block-editor.common.regenerate': 'Regenerate',
    'block-editor.common.pending': 'Pending',
    'block-editor.extension.ai-content.ask-ai-to-write-something': 'Ask AI to write something'
};

@Injectable()
export class DotMessageServiceMock {
    init() {
        // fake init
    }
    get(key: string, ...args: string[]): string {
        return MessageServiceMock[key]
            ? args.length
                ? formatMessage(MessageServiceMock[key], args)
                : MessageServiceMock[key]
            : key;
    }
}
