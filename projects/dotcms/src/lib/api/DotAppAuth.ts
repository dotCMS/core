import fetch from 'node-fetch';

export interface DotAppAuthLoginParams {
    user: string;
    password: string;
    expirationDays: string;
    host: string;
}

export class DotAppAuth {
    isLogin() {
        return document.cookie.split('access_token').length > 1;
    }

    logout() {
        return fetch('v1/logout', {
            method: 'PUT'
        });
    }

    login({ user, password, expirationDays, host }: DotAppAuthLoginParams): Promise<any> {
        return fetch(`${host || ''}/api/v1/authentication/api-token`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                user: user,
                password: password,
                expirationDays: expirationDays || 10
            })
        });
    }
}
