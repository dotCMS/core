import { Response, Request } from '@angular/http';
import { HttpCode } from '../util/http-code';

export const NETWORK_CONNECTION_ERROR = 1;
export const UNKNOWN_RESPONSE_ERROR = 2;
export const SERVER_RESPONSE_ERROR = 3;
export const CLIENTS_ONLY_MESSAGES = {
  1: 'Could not connect to server.'
};

export class CwError {
  constructor(public code: number, public message: string, public request?: Request,
  public response?: Response, public source?: any) {}
}

export interface ResponseError {
  response: Response;
  msg: string;
}

// tslint:disable-next-line:only-arrow-functions
export function isSuccess(resp: Response): boolean {
  return resp.status > 199 && resp.status < 300;
}

// tslint:disable-next-line:only-arrow-functions
export function hasContent(resp: Response): boolean {
  return isSuccess(resp) && resp.status !== HttpCode.NO_CONTENT;
}