import { parseDataForAnalytics } from './parser';

import { IsUserIncludedResponse, LocationMock } from '../mocks/mock';
import { AssignedExperiments, ExperimentParsed } from '../models';

const assignedExperiments: AssignedExperiments = IsUserIncludedResponse.entity;
const experimentMock = IsUserIncludedResponse.entity.experiments[0];

describe('parseDataForAnalytics', () => {
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
        const parsedData: ExperimentParsed = parseDataForAnalytics(assignedExperiments, location);

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
        const parsedData: ExperimentParsed = parseDataForAnalytics(assignedExperiments, location);

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
        const parsedData: ExperimentParsed = parseDataForAnalytics(assignedExperiments, location);

        expect(parsedData).toStrictEqual(expectedExperimentsParsed);
    });
});
