import { DotLoginUserSystemInformation } from '@models/dot-login/dot-login-user-system-information.model';

/**
 * Interface that wrap all data needed to display the login page.
 *
 * @interface
 */
export interface DotLoginInformation {
    i18nMessagesMap: { [key: string]: string };
    entity: DotLoginUserSystemInformation;
}
