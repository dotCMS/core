import { of, Observable, Subject } from 'rxjs';
import { Auth, User } from '@dotcms/dotcms-js';
import { DotLoginInformation } from '@dotcms/dotcms-models';

export const mockUser = () => {
    return {
        emailAddress: 'admin@dotcms.com',
        firstName: 'Admin',
        lastName: 'Admin',
        loggedInDate: 123456789,
        userId: '123',
        languageId: 'en_US'
    };
};

export const mockLoginFormResponse: DotLoginInformation = {
    entity: {
        serverId: '860173b0',
        levelName: 'COMMUNITY EDITION',
        version: '5.0.0',
        buildDateString: 'March 13, 2019',
        languages: [
            { language: 'en', country: 'US', displayName: 'English (United States)' },
            { language: 'es', country: 'ES', displayName: 'español (España)' },
            { language: 'it', country: 'IT', displayName: 'italiano (Italia)' },
            { language: 'fr', country: 'FR', displayName: 'français (France)' },
            { language: 'de', country: 'DE', displayName: 'Deutsch (Deutschland)' },
            { language: 'zh', country: 'CN', displayName: '中文 (中国)' },
            { language: 'nl', country: 'NL', displayName: 'Nederlands (Nederland)' },
            { language: 'ru', country: 'RU', displayName: 'русский (Россия)' },
            { language: 'fi', country: 'FI', displayName: 'suomi (Suomi)' }
        ],
        backgroundColor: '#3a3847',
        backgroundPicture: '/html/images/backgrounds/bg-11.jpg',
        logo: '/image/company_logo?img_id=dotcms.org&key=954201',
        authorizationType: 'emailAddress',
        currentLanguage: { language: 'en', country: 'US', displayName: 'English (United States)' },
        companyEmail: '@dotcms.com'
    },
    i18nMessagesMap: {
        cancel: 'Cancel',
        'sign-in': 'Sign In',
        'angular.login.component.community.licence.message':
            '<a href="https://dotcms.com/features" target="_blank">upgrade</a>',
        'email-address': 'Email Address',
        Server: 'Server',
        'a-new-password-has-been-sent-to-x': 'An Email with instructions has been sent to {0}.',
        'user-id': 'User ID',
        'remember-me': 'Remember Me',
        password: 'Password',
        'get-new-password': 'Recover Password',
        'error.form.mandatory': 'The field {0} is required',
        'reset-password-success': 'Your password has been successfully changed',
        'forgot-password': 'Forgot Password',
        'reset-password': 'Password Reset',
        'enter-password': 'Enter Password',
        're-enter-password': 'Confirm Password',
        'change-password': 'Change Password',
        'welcome-login': 'Welcome!',
        Logout: 'Logout',
        'message.successfully.logout': 'You are logout successfully, click the button to login',
        'reset-password-confirmation-do-not-match': 'password do not match'
    }
};

const mockUserWithRedirect: User = {
    ...mockUser(),
    editModeUrl: 'redirect/to'
};

export const mockAuth: Auth = {
    loginAsUser: mockUser(),
    user: mockUser()
};

export class LoginServiceMock {
    _auth: Subject<Auth> = new Subject();
    private watchUserFunc: (params: unknown) => void;

    constructor() {
        this._auth.next(mockAuth);
    }

    get auth(): Auth {
        return mockAuth;
    }

    get auth$(): Observable<Auth> {
        return this._auth;
    }

    setAuth(): void {
        /* */
    }

    loginAs(): Observable<boolean> {
        return of(true);
    }

    logoutAs(): Observable<boolean> {
        return of(true);
    }

    loginUser(): Observable<User> {
        return of(mockUserWithRedirect);
    }

    logOutUser(): Observable<User> {
        return of(null);
    }

    watchUser(func: (params: unknown) => void): void {
        this.watchUserFunc = func;
    }

    tiggerWatchUser(): void {
        this.watchUserFunc(mockAuth);
    }

    triggerNewAuth(auth: Auth) {
        this._auth.next(auth);
    }

    getLoginFormInfo(): Observable<DotLoginInformation> {
        return of({
            i18nMessagesMap: {
                'sign-in': 'Sign in'
            },
            entity: {
                authorizationType: '',
                backgroundColor: '',
                backgroundPicture: '',
                buildDateString: '',
                companyEmail: '',
                languages: [],
                levelName: '',
                logo: '',
                serverId: '',
                version: '',
                currentLanguage: {
                    country: 'US',
                    displayName: 'English (United States)',
                    language: 'en'
                }
            }
        });
    }

    recoverPassword(): Observable<string> {
        return of('email@dotcms.com');
    }

    changePassword(): Observable<string> {
        return of('email@dotcms.com');
    }

    getCurrentUser() {
        return of({
            email: 'admin@dotcms.com',
            givenName: 'Admin',
            loginAs: true,
            roleId: 'e7d4e34e-5127-45fc-8123-d48b62d510e3',
            surname: 'User',
            userId: 'dotcms.org.1'
        });
    }
}
