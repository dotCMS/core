import { parseData, parseDataForAnalytics } from './parser';

import {
    IsUserIncludedResponse,
    LocationMock,
    MOCK_CURRENT_TIMESTAMP,
    MockDataStoredIndexDB,
    MockDataStoredIndexDBWithNew,
    MockDataStoredIndexDBWithNew15DaysLater,
    NewIsUserIncludedResponse,
    TIME_15_DAYS_MILLISECONDS,
    TIME_5_DAYS_MILLISECONDS
} from '../mocks/mock';
import { Experiment, ExperimentParsed, FetchExperiments } from '../models';

const assignedExperiments: Experiment[] = IsUserIncludedResponse.entity.experiments;

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
        const mockNow = jest.spyOn(Date, 'now');

        mockNow.mockImplementation(() => MOCK_CURRENT_TIMESTAMP);

        beforeEach(() => {
            jest.clearAllMocks();
        });

        it('should handle case where only NEW data is available', () => {
            // First request, expire in now + experiment.lookBackWindow.expireMillis
            const newData: FetchExperiments = {
                experiments: IsUserIncludedResponse.entity.experiments,
                excludedExperimentIdsEnded: []
            };

            const dataFromIndexDB: Experiment[] | undefined = undefined;

            const parsedData = parseData(newData, dataFromIndexDB);

            expect(parsedData).toStrictEqual(MockDataStoredIndexDB);
            expect(newData.experiments.length).toBe(parsedData.length);
        });

        it('should handle case where only OLD data is available', () => {
            //No new request, not touch anything if not expired

            const newData: FetchExperiments = {
                experiments: [],
                excludedExperimentIdsEnded: []
            };

            const dataFromIndexDB: Experiment[] | undefined = MockDataStoredIndexDB;

            const parsedData = parseData(newData, dataFromIndexDB);

            expect(parsedData).toStrictEqual(MockDataStoredIndexDB);
            expect(MockDataStoredIndexDB.length).toBe(parsedData.length);
        });

        it('should handle case where both OLD and NEW data are available', () => {
            //new request, stored data + new data. No delete anything only 5 days passed, 2 to store

            const nowPlus5Days = MOCK_CURRENT_TIMESTAMP + TIME_5_DAYS_MILLISECONDS;

            mockNow.mockImplementation(() => nowPlus5Days);

            const newData: FetchExperiments = {
                experiments: NewIsUserIncludedResponse.entity.experiments,
                excludedExperimentIdsEnded: [
                    ...NewIsUserIncludedResponse.entity.excludedExperimentIdsEnded
                ]
            };

            const dataFromIndexDB: Experiment[] | undefined = MockDataStoredIndexDB;

            const parsedData = parseData(newData, dataFromIndexDB);

            expect(parsedData).toStrictEqual(MockDataStoredIndexDBWithNew);

            expect(parsedData.length).toBe(MockDataStoredIndexDBWithNew.length);
        });

        it('should remove from stored experiment the experiments expired', () => {
            // no new request, 15 days later, so expireTime is 15 days from MOCK_CURRENT_TIMESTAMP
            // 1st experiment expired, so only 1 to store
            const now15Days = MOCK_CURRENT_TIMESTAMP + TIME_15_DAYS_MILLISECONDS;

            mockNow.mockImplementation(() => now15Days);

            const newData: FetchExperiments = {
                experiments: [],
                excludedExperimentIdsEnded: []
            };

            const dataFromIndexDB: Experiment[] | undefined = MockDataStoredIndexDBWithNew;

            const parsedData = parseData(newData, dataFromIndexDB);

            expect(parsedData.length).toBe(MockDataStoredIndexDBWithNew15DaysLater.length);
            expect(parsedData).toStrictEqual(MockDataStoredIndexDBWithNew15DaysLater);
        });
    });
});
