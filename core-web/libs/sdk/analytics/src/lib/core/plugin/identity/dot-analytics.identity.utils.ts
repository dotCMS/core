import { SESSION_UTM_KEY } from '../../shared/constants';
import { DotCMSEventUtmData } from '../../shared/models';
import { safeSessionStorage } from '../../shared/utils/dot-analytics.utils';

/**
 * Compares UTM parameters to detect campaign changes.
 * Only checks significant parameters: source, medium, and campaign.
 * @internal This function is for internal use only.
 * @param currentUTM - Current UTM parameters in DotCMS format
 * @returns True if UTM parameters have changed, false otherwise
 */
export const hasUTMChanged = (currentUTM: DotCMSEventUtmData): boolean => {
    try {
        const storedUTM = safeSessionStorage.getItem(SESSION_UTM_KEY);
        if (!storedUTM) {
            safeSessionStorage.setItem(SESSION_UTM_KEY, JSON.stringify(currentUTM));

            return false;
        }

        const previousUTM = JSON.parse(storedUTM);
        const significantParams: (keyof DotCMSEventUtmData)[] = ['source', 'medium', 'campaign'];

        for (const param of significantParams) {
            if (currentUTM[param] !== previousUTM[param]) {
                safeSessionStorage.setItem(SESSION_UTM_KEY, JSON.stringify(currentUTM));

                return true;
            }
        }

        return false;
    } catch {
        return false;
    }
};
