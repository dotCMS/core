import { IsUserIncludedApiResponse } from '../models';

export const IsUserIncludedResponse: IsUserIncludedApiResponse = {
    entity: {
        excludedExperimentIds: [],
        experiments: [
            {
                id: 'd5f1eb69-a03a-479a-af4c-8da3b9eaacf8',
                lookBackWindow: {
                    expireMillis: 1209600000,
                    value: 'Q5KtspItPxWJYKVniq6A'
                },
                name: 'Exp1',
                pageUrl: '/blog/index',
                regexs: {
                    isExperimentPage:
                        '^(http|https):\\/\\/(localhost|127.0.0.1|\\b(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,})(:\\d{1,5})?\\/blog(\\/index|\\/)?(\\/?\\?.*)?$',
                    isTargetPage: null
                },
                runningId: '1f467fb0-cb3e-49c3-8b5d-126a86c95462',
                variant: {
                    name: 'dotexperiment-d5f1eb69a0-variant-1',
                    url: '/blog/index?variantName=dotexperiment-d5f1eb69a0-variant-1'
                }
            }
        ],
        includedExperimentIds: ['d5f1eb69-a03a-479a-af4c-8da3b9eaacf8']
    },
    errors: [],
    i18nMessagesMap: {},
    messages: []
};
