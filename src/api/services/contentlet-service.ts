import {Inject, Injectable} from '@angular/core';
import {CoreWebService} from './core-web-service';
import {RequestMethod} from '@angular/http';
import {ResponseView} from './response-view';
import {Observable} from 'rxjs/Rx';
import {ApiRoot} from '../persistence/ApiRoot';
import {Http} from '@angular/http';

@Injectable()
export class ContentletService extends CoreWebService {

    constructor(apiRoot: ApiRoot, http: Http ) {
        super(apiRoot, http);
    }

    public getContentTypes(): Observable<StructureTypeView[]> {
        return this.requestView({
            method: RequestMethod.Get,
            url: 'v1/content/types'
        }).pluck('entity');
    }
}

export interface ContentTypeView {
    'type': StructureType;
    'name': string;
    'inode': string;
    'action': string;
}

export interface StructureTypeView {
    'name': string;
    'label': string;
    'types': ContentTypeView[];
}

export enum StructureType {
    CONTENT,
    HTMLPAGE,
    FILEASSET,
    WIDGET,
    PERSONA,
}
