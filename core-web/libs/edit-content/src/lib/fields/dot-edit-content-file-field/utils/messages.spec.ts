import { getUiMessage, UiMessageMap } from './messages';

import { MESSAGES_TYPES } from '../models';

describe('getUiMessage function', () => {
    it('should return the correct uiMessage for a valid key', () => {
        const key: MESSAGES_TYPES = 'DEFAULT';
        const expectedUiMessage = UiMessageMap[key];
        expect(getUiMessage(key)).toEqual(expectedUiMessage);
    });

    it('should throw an error for an invalid key', () => {
        const key = 'INVALID_KEY' as MESSAGES_TYPES;
        expect(() => getUiMessage(key)).toThrowError(`Key ${key} not found in UiMessageMap`);
    });

    it('should throw an error for a null or undefined key', () => {
        const key = null as unknown as MESSAGES_TYPES;
        expect(() => getUiMessage(key)).toThrowError(`Key ${key} not found in UiMessageMap`);
        const key2 = undefined as unknown as MESSAGES_TYPES;
        expect(() => getUiMessage(key2)).toThrowError(`Key ${key2} not found in UiMessageMap`);
    });
});
