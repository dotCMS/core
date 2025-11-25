import { Experiment, ExperimentEvent, IsUserIncludedApiResponse } from '../models';

/**
 * Represents the response object for the IsUserIncluded API.
 * @typedef {Object} IsUserIncludedResponse
 * @property {IsUserIncludedApiResponse} entity - The response entity.
 * @property {string[]} errors - Array of error messages, if any.
 * @property {Object} i18nMessagesMap - Map of internationalization messages.
 * @property {string[]} messages - Array of additional messages, if any.
 */
export const IsUserIncludedResponse: IsUserIncludedApiResponse = {
    entity: {
        excludedExperimentIds: [],
        excludedExperimentIdsEnded: [],
        experiments: [
            {
                id: '11111-11111-11111-11111-11111',
                lookBackWindow: {
                    expireMillis: 1209600000,
                    value: 'AAAAAAAAAA'
                },
                name: 'Exp1',
                pageUrl: '/blog/index',
                regexs: {
                    isExperimentPage:
                        '^(http|https):\\/\\/(localhost|127.0.0.1|\\b(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,})(:\\d{1,5})?\\/blog(\\/index|\\/)?(\\/?\\?.*)?$',
                    isTargetPage: '.*destinations.*'
                },
                runningId: '1111111-22222222',
                variant: {
                    name: 'variant-1',
                    url: '/blog/index?variantName=variant-1'
                }
            }
        ],
        includedExperimentIds: ['11111-11111-11111-11111-11111']
    },
    errors: [],
    i18nMessagesMap: {},
    messages: []
};

export const NewIsUserIncludedResponse: IsUserIncludedApiResponse = {
    entity: {
        excludedExperimentIds: ['11111-11111-11111-11111-11111'],
        excludedExperimentIdsEnded: ['11111-11111-11111-11111-11111'],
        experiments: [
            {
                id: '222222-222222-222222-222222-222222',
                lookBackWindow: {
                    expireMillis: 1209600000,
                    value: 'BBBBBBBBBBBBBB'
                },
                name: 'Exp2',
                pageUrl: '/destinations/index',
                regexs: {
                    isExperimentPage:
                        '^(http|https):\\/\\/(localhost|127.0.0.1|\\b(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,})(:\\d{1,5})?\\/destinations(\\/index|\\/)?(\\/?\\?.*)?$',
                    isTargetPage: null
                },
                runningId: '33333333-3333333333',
                variant: {
                    name: 'variant-1',
                    url: '/blog/index?variantName=variant-1'
                }
            }
        ],
        includedExperimentIds: ['222222-222222-222222-222222-222222']
    },
    errors: [],
    i18nMessagesMap: {},
    messages: []
};

export const After15DaysIsUserIncludedResponse: IsUserIncludedApiResponse = {
    entity: {
        excludedExperimentIds: ['222222-222222-222222-222222-222222'],
        excludedExperimentIdsEnded: [],
        experiments: [],
        includedExperimentIds: []
    },
    errors: [],
    i18nMessagesMap: {},
    messages: []
};

export const NoExperimentsIsUserIncludedResponse: IsUserIncludedApiResponse = {
    entity: {
        excludedExperimentIds: [],
        excludedExperimentIdsEnded: [],
        experiments: [],
        includedExperimentIds: []
    },
    errors: [],
    i18nMessagesMap: {},
    messages: []
};

export const MOCK_CURRENT_TIMESTAMP = 1704096000000;

export const TIME_15_DAYS_MILLISECONDS = 1296000 * 1000;

export const TIME_5_DAYS_MILLISECONDS = 432000 * 1000;

export const MockDataStoredIndexDB: Experiment[] = [
    {
        ...IsUserIncludedResponse.entity.experiments[0],
        lookBackWindow: {
            ...IsUserIncludedResponse.entity.experiments[0].lookBackWindow,
            // Added expireTime to the lookBackWindow
            expireTime:
                MOCK_CURRENT_TIMESTAMP +
                IsUserIncludedResponse.entity.experiments[0].lookBackWindow.expireMillis
        }
    }
];

export const MockDataStoredIndexDBNew: Experiment[] = [
    {
        ...NewIsUserIncludedResponse.entity.experiments[0],
        lookBackWindow: {
            ...NewIsUserIncludedResponse.entity.experiments[0].lookBackWindow,
            // Added expireTime to the lookBackWindow
            expireTime:
                // 2nd request, 5 days later, so expireTime is 5 days from MOCK_CURRENT_TIMESTAMP
                MOCK_CURRENT_TIMESTAMP +
                TIME_5_DAYS_MILLISECONDS +
                NewIsUserIncludedResponse.entity.experiments[0].lookBackWindow.expireMillis
        }
    }
];

// Final data to be stored in IndexedDB only the last, the first will be removed by was ended from `excludedExperimentIdsEnded`
export const MockDataStoredIndexDBWithNew: Experiment[] = [...MockDataStoredIndexDBNew];

// Mock Store after 15 days
export const MockDataStoredIndexDBWithNew15DaysLater: Experiment[] = [...MockDataStoredIndexDBNew];

/**
 * Represents an event that indicates the expected experiments parsed from a response to send to Analytics.
 *
 * @typedef {Object} ExpectedExperimentsParsedEvent
 * @property {ExperimentEvent[]} experiments - An array of experiment events.
 */
export const ExpectedExperimentsParsedEvent: ExperimentEvent[] = [
    {
        experiment: IsUserIncludedResponse.entity.experiments[0].id,
        runningId: IsUserIncludedResponse.entity.experiments[0].runningId,
        variant: IsUserIncludedResponse.entity.experiments[0].variant.name,
        lookBackWindow: IsUserIncludedResponse.entity.experiments[0].lookBackWindow.value,
        isExperimentPage: false,
        isTargetPage: false
    }
];

/**
 * Represents a mock location object.
 *
 * @typedef {Object} LocationMock
 * @property {string} href - The complete URL.
 *
 */
export const LocationMock: Location = {
    hash: '',
    host: '',
    hostname: '',
    href: 'http://localhost/blog',
    origin: '',
    pathname: '',
    port: '',
    protocol: '',
    search: '',
    assign: () => {
        //
    },
    replace: () => {
        //
    },
    reload: () => {
        //
    },
    toString: () => {
        return '';
    },
    ancestorOrigins: {} as DOMStringList
};

const store: { [key: string]: string } = {};

export const sessionStorageMock = {
    getItem: function (key: string): string | null {
        return store[key] || null;
    },
    setItem: function (key: string, value: string) {
        store[key] = value;
    },
    removeItem: function (key: string) {
        delete store[key];
    },
    clear: function () {
        Object.keys(store).forEach((key) => delete store[key]);
    },
    key: function (index: number) {
        return Object.keys(store)[index] || null;
    },
    get length() {
        return Object.keys(store).length;
    }
};
