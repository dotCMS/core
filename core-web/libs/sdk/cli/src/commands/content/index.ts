import { defineCommand } from 'citty';

import { deleteCommand } from './delete';
import { diffCommand } from './diff';
import { pullCommand } from './pull';
import { pushCommand } from './push';
import { statusCommand } from './status';

export const contentCommand = defineCommand({
    meta: {
        name: 'content',
        description: 'Pull, push, and manage contentlets'
    },
    subCommands: {
        pull: pullCommand,
        push: pushCommand,
        status: statusCommand,
        diff: diffCommand,
        delete: deleteCommand
    }
});
