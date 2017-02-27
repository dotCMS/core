import {Injectable} from '@angular/core';
import {CONSTANTS} from '../../view/constants';
import {HttpRequestUtils} from './httpRequestUtils';

const DEV_MODE_PARAM = 'devMode';

/**
 * Encapsulates generic configuration, such as the dev mode, etc.
 * @author jsanca
 */
@Injectable()
export class Config {

    private httpRequestUtils: HttpRequestUtils;

    constructor() {
        this.httpRequestUtils = new HttpRequestUtils();
    }

    /**
     * Determine if angular is running in a Production way
     * @returns {boolean}
     */
    isProduction(): boolean {

        let runningMode: string = CONSTANTS.ENV;
        let devMode: string = this.httpRequestUtils.getQueryStringParam(DEV_MODE_PARAM);

        if (devMode) {
            console.log('Found a parameter in the url with a devMode: ', devMode);
            runningMode = devMode === 'on' ? CONSTANTS.DEV_MODE : CONSTANTS.PROD_MODE;
        }

        return runningMode === CONSTANTS.PROD_MODE;
    } // isProduction.
} // E:O:F:Config
