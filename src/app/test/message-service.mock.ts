import { Observable } from 'rxjs/Observable';

/**
 * Mock of MessageService.
 * How to use:
 * <code>
 *      let messageServiceMock = new MockMessageService({
 *           'Description': 'Description',
 *           'Entries': 'Entries',
 *           'Structure-Name': 'Content Type Name',
 *           'Variable': 'Variable Name'
 *       });
 *       messageServiceMock.getMessages(['Structure-Name', 'Description']);
 * </code>
 * Return:
 * {
 *      'Description': 'Description',
 *      'Structure-Name': 'Content Type Name',
 * }
 * @export
 * @class MockMessageService
 */
export class MockMessageService {
    constructor(private messages: MessageConfig) {}

    public getMessages(keys: string[]): Observable<any> {
        return Observable.of(this.messages);
    }

    get messageMap$(): Observable<any> {
        return Observable.of(this.messages);
    }

    get(key: string): string {
        return this.messages[key];
    }
}

interface MessageConfig {
    [propName: string]: string;
}
