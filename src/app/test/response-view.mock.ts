import { ResponseView } from 'dotcms-js/core/util/response-view';
import { ResponseOptions, Response } from '@angular/http';

export const mockResponseView = (status, url?) => new ResponseView(new Response(new ResponseOptions({
    body: {},
    status: status,
    headers: null,
    url: url || '/test/test'
})));
