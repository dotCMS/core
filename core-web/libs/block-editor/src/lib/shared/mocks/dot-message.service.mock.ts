import { Injectable } from '@angular/core';

import { formatMessage } from '@dotcms/utils';

// Move all this translations to Language.properties
export const MessageServiceMock: Record<string, string> = {
    'block-editor.common.accept': 'Accept',
    'block-editor.common.delete': 'Delete',
    'block-editor.common.regenerate': 'Regenerate',
    'block-editor.common.pending': 'Pending',
    'block-editor.common.generate': 'Generate',
    'block-editor.common.input-prompt-required-error': 'The information provided is insufficient',

    'block-editor.extension.ai-content.ask-ai-to-write-something': 'Ask AI to write something',

    'block-editor.extension.ai-image.dialog-title': 'Generate AI Image',

    'block-editor.extension.ai-image.input-text.title':
        'Generate an AI image based on your input and requests.',
    'block-editor.extension.ai-image.input-text.placeholder':
        'Create a realistic image of a cow in the snow',
    'block-editor.extension.ai-image.input-text.tooltip':
        'Describe the type of image you want to generate.',

    'block-editor.extension.ai-image.auto-text.title':
        'Auto-Generate an Image based on the content created within the Block Editor.',
    'block-editor.extension.ai-image.auto-text.tooltip':
        'Describe the size, color palette,  style, mood, etc.',
    'block-editor.extension.ai-image.auto-text.placeholder':
        'E.g. 1200x800px, vibrant colors, impressionistic, adventurous.'
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
