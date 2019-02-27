import fetch from 'node-fetch';
import { DotCMSAuthLoginParams } from '../models';


export class DotAppAuth {
    isLogin(): boolean {
        return document.cookie.split('access_token').length > 1;
    }

    logout(): Promise<string> {
        return fetch('v1/logout', {
            method: 'PUT'
        })
            .then((data: Response) => data.json())
            .then((data: { [key: string]: any }) => data.entity);
    }

    getToken({ user, password, expirationDays, host }: DotCMSAuthLoginParams): Promise<string> {
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
        })
            .then((data: Response) => data.json())
            .then((data: { [key: string]: any }) => data.entity.token);
    }
}
