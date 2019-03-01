import fetch from 'node-fetch';
import { DotCMSAuthorizationLoginParams, DotCMSError } from '../models';

function getErrorMessage(data: {[key: string]: any}) {
    if (data.errors) {
        return data.errors[0].message;
    }

    if (data.error) {
        return data.error;
    }
}

export class DotApiAuthorization {
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

    getToken(params: DotCMSAuthorizationLoginParams): Promise<Response> {
        const { user, password, expirationDays, host } = params;

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
        }).then(async (res: Response) => {
            const data = await res.json();

            if (res.status === 200) {
                return data.entity.token;
            }

            throw <DotCMSError>{
                message: getErrorMessage(data),
                status: res.status
            };
        });
    }
}
