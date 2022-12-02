import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable()
export class MockDotHttpErrorManagerService {
    public handle(): Observable<unknown> {
        return null;
    }
}
