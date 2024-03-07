import { jitsuClient } from '@jitsu/sdk-js';

export function testExternalLibrary(): string {
    const client = jitsuClient({
        key: '123',
        tracking_host: 'https://dotmatics.jitsu.com',
        log_level: 'DEBUG'
    });

    console.log('client', client);

    return 'test-external-library';
}
