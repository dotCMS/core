import {
    getDataExperimentAttributes,
    getExperimentScriptTag,
    getScriptDataAttributes
} from './utils';

import { EXPERIMENT_SCRIPT_FILE_NAME } from '../constants';

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
    it('should return null and warn if data-experiment-api-key is not specified but script is present', () => {
        const consoleWarnSpy = jest.spyOn(console, 'warn');
        const experimentScriptUrl = 'http://example.com/' + EXPERIMENT_SCRIPT_FILE_NAME;
        document.body.innerHTML = `<script src="${experimentScriptUrl}"></script>`;

        const attributes = getDataExperimentAttributes();

        expect(attributes).toBeNull();
        expect(consoleWarnSpy).toHaveBeenCalledWith(
            '[dotCMS Experiments] You need specify the `data-experiment-api-key`'
        );
    });

    it('should return the experiment attributes if they are present', () => {
        const experimentScriptUrl = 'http://example.com/' + EXPERIMENT_SCRIPT_FILE_NAME;
        document.body.innerHTML = `<script src="${experimentScriptUrl}" data-experiment-api-key="testKey" data-experiment-server="http://localhost"></script>`;
        const attributes = getDataExperimentAttributes();
        expect(attributes).toEqual({
            'api-key': 'testKey',
            server: 'http://localhost',
            debug: false
        });
    });
});

describe('getScriptDataAttributes', () => {
    it('should return the experiment attributes if they are present', () => {
        const experimentScriptUrl = 'http://example.com/' + EXPERIMENT_SCRIPT_FILE_NAME;
        document.body.innerHTML = `<script src="${experimentScriptUrl}" data-experiment-api-key="testKey" data-experiment-server="http://localhost"></script>`;

        const attributes = getScriptDataAttributes();
        expect(attributes).toEqual({
            'api-key': 'testKey',
            server: 'http://localhost',
            debug: false
        });
    });
});
