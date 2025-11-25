import { jest } from '@jest/globals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';

import { tap } from 'rxjs/operators';

export const FAKE_RX_METHOD = Symbol('FAKE_RX_METHOD');

export function newFakeRxMethod() {
    const f = jest.fn();
    const r = rxMethod(tap((x) => f(x)));
    r[FAKE_RX_METHOD] = f;

    return r;
}

export function getRxMethodFake(rxMethod) {
    return rxMethod[FAKE_RX_METHOD];
}
