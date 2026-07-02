/* eslint-disable @typescript-eslint/no-explicit-any */
import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

setupZoneTestEnv({
    errorOnUnknownElements: true,
    errorOnUnknownProperties: true
});

if (!global.structuredClone) {
    global.structuredClone = (obj: any) => JSON.parse(JSON.stringify(obj));
}
