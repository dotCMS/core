import { Observable, of } from 'rxjs';

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
 *       messageServiceMock.get('Structure-Name');
 * </code>
 * Return:
 * {
 *      'Structure-Name': 'Content Type Name',
 * }
 * @export
 * @class MockDotMessageService
 */
export class MockDotMessageService {
    constructor(private messages: MessageConfig) {}

    init(): Observable<void> {
        return of(void 0);
    }

    invalidatePendingSession(): void {
        // The mock has no pending requests.
    }

    get(key: string, ...args: string[]): string {
        return this.messages[key]
            ? args.length
                ? this.formatMessage(this.messages[key], args)
                : this.messages[key]
            : key;
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
