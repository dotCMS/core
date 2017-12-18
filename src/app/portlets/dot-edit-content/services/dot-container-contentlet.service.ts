import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';

@Injectable()
export class DotContainerContentletService {
    constructor() {}

    getContentletToContainer(containerId: string, contentletId: string): Observable<string> {
        return Observable.of(`
            <script type="text/javascript" src="//cdn.jsdelivr.net/npm/lodash@4.17.4/lodash.min.js"></script>
            <script type="text/javascript">
                alert('Javascript src and code works ' + _.compact([0, 1, false, 2, '', 3]).toString());
            </script>
            <h3>New endpoint</h3>
            <p>This is a response from the new endpoint, so yaaaas!</p>
        `).delay(1000);
    }
}
