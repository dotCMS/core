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

    public getMessages(_keys: string[]): Observable<any> {
        return Observable.of(this.messages);
    }

    get messageMap$(): Observable<any> {
        return Observable.of(this.messages);
    }

    get(key: string, ...args: string[]): string {
        if (args.length) {
            return this.messages[key] ? this.formatMessage(this.messages[key], args) : key;
        } else {
            return this.messages[key] || key;
        }
    }

    private formatMessage(message: string, args: string[]): string {
        return message.replace(/{(\d+)}/g, (match, number) => {
            return typeof args[number] !== 'undefined' ? args[number] : match;
        });
    }
}

interface MessageConfig {
    [propName: string]: string;
}
