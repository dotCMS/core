import { Observable } from 'rxjs/Observable';

/**
 * Mock of DotMessageService.
 * How to use:
 * <code>
 *      let messageServiceMock = new MockDotMessageService({
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
 * @class MockDotMessageService
 */
export class MockDotMessageService {
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
