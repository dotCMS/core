import {
    checkFlagExperimentAlreadyChecked,
    getDataExperimentAttributes,
    getExperimentScriptTag,
    getFullUrl,
    getScriptDataAttributes
} from './utils';

import { EXPERIMENT_ALREADY_CHECKED_KEY, EXPERIMENT_SCRIPT_FILE_NAME } from '../constants';
import { LocationMock } from '../mocks/mock';

describe('Utility ', () => {
    describe('getExperimentScriptTag', () => {
        it('should throw an error if the experiment script is not found', () => {
            document.body.innerHTML = `<script src="other-script.js"></script>`;
            expect(() => getExperimentScriptTag()).toThrow('Experiment script not found');
        });

        it('should return the script element when the experiment script is found', () => {
            const experimentScriptUrl = 'http://example.com/' + EXPERIMENT_SCRIPT_FILE_NAME;

            document.body.innerHTML = `<script src="${experimentScriptUrl}"></script>`;

            const scriptTag = getExperimentScriptTag();

            expect(scriptTag).toBeDefined();
            expect(scriptTag.src).toBe(experimentScriptUrl);
        });
    });

    describe('getDataExperimentAttributes', () => {
        const location: Location = { ...LocationMock, href: 'http:/localhost/' };

        it('should return null and warn if data-experiment-api-key is not specified but script is present', () => {
            const experimentScriptUrl = 'http://example.com/' + EXPERIMENT_SCRIPT_FILE_NAME;

            document.body.innerHTML = `<script src="${experimentScriptUrl}"></script>`;

            try {
                getDataExperimentAttributes(location);
                expect('This should not be reached if an error is thrown').toBeNull();
            } catch (error) {
                expect(() => getDataExperimentAttributes(location)).toThrow(
                    'You need specify the `data-experiment-api-key`'
                );
            }
        });

        it('should return the experiment attributes if they are present', () => {
            const experimentScriptUrl = 'http://example.com/' + EXPERIMENT_SCRIPT_FILE_NAME;

            document.body.innerHTML = `<script src="${experimentScriptUrl}" data-experiment-api-key="testKey" data-experiment-server="http://localhost"></script>`;

            const attributes = getDataExperimentAttributes(location);

            expect(attributes).toEqual({
                apiKey: 'testKey',
                server: 'http://localhost',
                debug: false
            });
        });
    });

    describe('getScriptDataAttributes', () => {
        it('should return the experiment attributes if they are present', () => {
            const experimentScriptUrl = 'http://example.com/' + EXPERIMENT_SCRIPT_FILE_NAME;

            document.body.innerHTML = `<script src="${experimentScriptUrl}" data-experiment-api-key="testKey" data-experiment-server="http://localhost"></script>`;

            // eslint-disable-next-line no-restricted-globals
            const attributes = getScriptDataAttributes(location);

            expect(attributes).toEqual({
                apiKey: 'testKey',
                server: 'http://localhost',
                debug: false
            });
        });
    });

    describe('SessionStorage EXPERIMENT_ALREADY_CHECKED_KEY handle', () => {
        Object.defineProperty(window, 'sessionStorage', {
            value: {
                setItem: jest.fn(),
                getItem: jest.fn()
            },
            writable: true
        });

        describe('checkFlagExperimentAlreadyChecked', () => {
            const getItemMock = window.sessionStorage.getItem as jest.MockedFunction<
                typeof window.sessionStorage.getItem
            >;

            const testCases = [
                { value: '', expected: false, description: 'sessionStorage value is ""' },
                { value: 'true', expected: true, description: 'sessionStorage value is "true"' },
                { value: null, expected: false, description: 'sessionStorage value is null' }
            ];

            testCases.forEach(({ description, value, expected }) => {
                it(`returns ${expected} when ${description}`, () => {
                    getItemMock.mockReturnValue(value);

                    expect(checkFlagExperimentAlreadyChecked()).toBe(expected);
                    expect(getItemMock).toHaveBeenCalledWith(EXPERIMENT_ALREADY_CHECKED_KEY);
                });
            });
        });
    });

    describe('getFullUrl', () => {
        const href = 'http://localhost/';

        const location: Location = { ...LocationMock, href };

        it('should return null if absolutePath is null', () => {
            const result = getFullUrl(location, null);

            expect(result).toBeNull();
        });

        it('should return the same absolutePath if it is a full URL', () => {
            const absolutePath = href + '/test';

            const expectedUrl = absolutePath;

            const result = getFullUrl(location, absolutePath);

            expect(result).toBe(absolutePath);
            expect(result).toBe(expectedUrl);
        });

        it('should return a full URL if absolutePath is a relative path', () => {
            const absolutePath = '/test';

            const result = getFullUrl(location, absolutePath);

            expect(result).toBe(`${location.origin}${absolutePath}`);
        });
    });
});
