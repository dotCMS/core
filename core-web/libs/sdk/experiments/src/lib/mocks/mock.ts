import { AssignedExperiments, ExperimentEvent, IsUserIncludedApiResponse } from '../models';

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
        experiments: [
            {
                id: '11111-11111-11111-11111-11111',
                lookBackWindow: {
                    expireMillis: 1209600000,
                    value: 'Q5KtspItPxWJYKVniq6A'
                },
                name: 'Exp1',
                pageUrl: '/blog/index',
                regexs: {
                    isExperimentPage:
                        '^(http|https):\\/\\/(localhost|127.0.0.1|\\b(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,})(:\\d{1,5})?\\/blog(\\/index|\\/)?(\\/?\\?.*)?$',
                    isTargetPage: '.*destinations.*'
                },
                runningId: '1f467fb0-cb3e-49c3-8b5d-126a86c95462',
                variant: {
                    name: 'dotexperiment-d5f1eb69a0-variant-1',
                    url: '/blog/index?variantName=dotexperiment-d5f1eb69a0-variant-1'
                }
            }
        ],
        includedExperimentIds: ['11111-11111-11111-11111-11111']
    },
    errors: [],
    i18nMessagesMap: {},
    messages: []
};

export const CURRENT_TIMESTAMP = 1630629253956;

export const TIME_15_DAYS_MILLISECONDS = 15 * 24 * 60 * 60 * 1000;

let storedExperiment = IsUserIncludedResponse.entity.experiments[0];
storedExperiment = {
    ...storedExperiment,
    lookBackWindow: {
        ...storedExperiment.lookBackWindow,
        expireTime: CURRENT_TIMESTAMP + storedExperiment.lookBackWindow.expireMillis - 86400000
    }
};
export const IsUserIncludedResponseStored: AssignedExperiments = {
    ...IsUserIncludedResponse.entity,
    experiments: [storedExperiment]
};

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
