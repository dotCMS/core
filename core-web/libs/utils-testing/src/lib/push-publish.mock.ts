import { of as observableOf } from 'rxjs';

import { Injectable } from '@angular/core';

@Injectable()
export class MockPushPublishService {
    getEnvironments() {
        return observableOf([
            {
                id: '123',
                name: 'Environment 1'
            },
            {
                id: '456',
                name: 'Environment 2'
            }
        ]);
    }
}
