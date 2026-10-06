import { Pipe, PipeTransform, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

import { toElapsedMessage } from './dot-network.utils';

/** Turns the seconds since a node's last heartbeat into "4 seconds ago", "6 minutes ago", etc. */
@Pipe({ name: 'dotNetworkElapsed' })
export class DotNetworkElapsedPipe implements PipeTransform {
    readonly #messageService = inject(DotMessageService);

    transform(seconds: number | null): string {
        const { key, args } = toElapsedMessage(seconds);

        return this.#messageService.get(key, ...args);
    }
}
