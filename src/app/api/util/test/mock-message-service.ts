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

    constructor(private messages: MessageConfig) {

    }

    public getMessages(keys: string[]): Observable<any> {
        let resp = {};
        keys.forEach(key => resp[key] = this.messages[key]);
        return Observable.of(resp);
    }
}

interface MessageConfig {
    [propName: string]: string;
}