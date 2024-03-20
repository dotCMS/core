import { parseData, parseDataForAnalytics } from './parser';

import {
    CURRENT_TIMESTAMP,
    IsUserIncludedResponse,
    IsUserIncludedResponseStored,
    LocationMock,
    TIME_15_DAYS_MILLISECONDS
} from '../mocks/mock';
import { AssignedExperiments, Experiment, ExperimentParsed } from '../models';

const assignedExperiments: AssignedExperiments = IsUserIncludedResponse.entity;
const experimentMock = IsUserIncludedResponse.entity.experiments[0];

describe('Parsers', () => {
    describe('parseData For Analytics', () => {
        it('returns `isExperimentPage` true and `isTargetPage` false when location is /blog', () => {
            const expectedURL = 'http://localhost/blog';

            const expectedExperimentsParsed: ExperimentParsed = {
                href: expectedURL,
                experiments: [
                    {
                        experiment: experimentMock.id,
                        runningId: experimentMock.runningId,
                        variant: experimentMock.variant.name,
                        lookBackWindow: experimentMock.lookBackWindow.value,
                        isExperimentPage: true,
                        isTargetPage: false
                    }
                ]
            };

            const location: Location = { ...LocationMock, href: expectedURL };
            const parsedData: ExperimentParsed = parseDataForAnalytics(
                assignedExperiments,
                location
            );

            expect(parsedData).toStrictEqual(expectedExperimentsParsed);
        });

        it('returns `isExperimentPage` false and `isTargetPage` true when location is /destinations', () => {
            const expectedURL = 'http://localhost/destinations';
            const expectedExperimentsParsed: ExperimentParsed = {
                href: expectedURL,
                experiments: [
                    {
                        experiment: experimentMock.id,
                        runningId: experimentMock.runningId,
                        variant: experimentMock.variant.name,
                        lookBackWindow: experimentMock.lookBackWindow.value,
                        isExperimentPage: false,
                        isTargetPage: true
                    }
                ]
            };

            const location: Location = { ...LocationMock, href: expectedURL };
            const parsedData: ExperimentParsed = parseDataForAnalytics(
                assignedExperiments,
                location
            );

            expect(parsedData).toStrictEqual(expectedExperimentsParsed);
        });

        it('returns `isExperimentPage` false and `isTargetPage` false when location is /other-url', () => {
            const expectedURL = 'http://localhost/other-url';
            const expectedExperimentsParsed: ExperimentParsed = {
                href: expectedURL,
                experiments: [
                    {
                        experiment: experimentMock.id,
                        runningId: experimentMock.runningId,
                        variant: experimentMock.variant.name,
                        lookBackWindow: experimentMock.lookBackWindow.value,
                        isExperimentPage: false,
                        isTargetPage: false
                    }
                ]
            };

            const location: Location = { ...LocationMock, href: expectedURL };
            const parsedData: ExperimentParsed = parseDataForAnalytics(
                assignedExperiments,
                location
            );

            expect(parsedData).toStrictEqual(expectedExperimentsParsed);
        });
    });

    describe('parseData For Store', () => {
        const oldDataFromIndexDB: AssignedExperiments = IsUserIncludedResponseStored;

        const newExperimentId = '22222-22222-22222-22222-22222';
        const primaryExperiment: Experiment = IsUserIncludedResponse.entity.experiments[0];

        const newExperimentData: Experiment = { ...primaryExperiment, id: newExperimentId };

        const newDataFromEndpoint: AssignedExperiments = {
            excludedExperimentIds: ['11111-11111-11111-11111-11111'],
            experiments: [newExperimentData],
            includedExperimentIds: [newExperimentId]
        };

        const mockNow = jest.spyOn(Date, 'now');
        mockNow.mockImplementation(() => CURRENT_TIMESTAMP);

        beforeEach(() => {
            jest.clearAllMocks();
        });

        it('should handle case where only NEW data is available', () => {
            let experiment = newDataFromEndpoint.experiments[0];
            experiment = {
                ...experiment,
                lookBackWindow: {
                    ...experiment.lookBackWindow,
                    expireTime: CURRENT_TIMESTAMP + experiment.lookBackWindow.expireMillis
                }
            };
            const expectedResult: AssignedExperiments = {
                ...newDataFromEndpoint,
                excludedExperimentIds: [],
                experiments: [experiment]
            };
            const result = parseData(newDataFromEndpoint, null);

            expect(result).toStrictEqual(expectedResult);
        });

        it('should handle case where only OLD data is available', () => {
            const expectedResult: AssignedExperiments = {
                ...oldDataFromIndexDB
            };
            const result = parseData(null, oldDataFromIndexDB);

            expect(result).toStrictEqual(expectedResult);
        });

        it('should handle case where both OLD and NEW data are available', () => {
            let newExperiment = newDataFromEndpoint.experiments[0];
            newExperiment = {
                ...newExperiment,
                lookBackWindow: {
                    ...newExperiment.lookBackWindow,
                    expireTime: CURRENT_TIMESTAMP + newExperiment.lookBackWindow.expireMillis
                }
            };

            const oldExperimentData = oldDataFromIndexDB.experiments[0];
            const expectedResult: AssignedExperiments = {
                excludedExperimentIds: [],
                experiments: [newExperiment, oldExperimentData],
                includedExperimentIds: [newExperimentData.id, oldExperimentData.id]
            };
            const result = parseData(newDataFromEndpoint, oldDataFromIndexDB);

            expect(result).toStrictEqual(expectedResult);
        });

        it('should remove from stored experiment the experiments expired', () => {
            const now15Days = CURRENT_TIMESTAMP + TIME_15_DAYS_MILLISECONDS;
            mockNow.mockImplementation(() => now15Days);

            const expectedResult: AssignedExperiments = {
                excludedExperimentIds: [],
                experiments: [],
                includedExperimentIds: []
            };
            const result = parseData(null, oldDataFromIndexDB);

            expect(result).toStrictEqual(expectedResult);
        });
    });
});
