import {Inject, Injectable} from '@angular/core';
import {CoreWebService} from './core-web-service';
import {RequestMethod} from '@angular/http';
import {ResponseView} from './response-view';
import {Observable} from 'rxjs/Rx';
import {ApiRoot} from '../persistence/ApiRoot';
import {Http} from '@angular/http';
import {LoginService} from './login-service';
import {Subject} from 'rxjs/Subject';

@Injectable()
export class ContentletService extends CoreWebService {

    private _structureTypeView$: Subject<StructureTypeView[]> = new Subject<StructureTypeView[]>();

    constructor(apiRoot: ApiRoot, http: Http, loginService: LoginService) {
        super(apiRoot, http);

        loginService.watchUser(this.loadContentTypes.bind(this));
        loginService.logoutAs().subscribe( () => this.loadContentTypes());
    }

    get structureTypeView$(): Observable<StructureTypeView[]>{
        return this._structureTypeView$.asObservable();
    }

    private loadContentTypes(): void {
        return this.requestView({
            method: RequestMethod.Get,
            url: 'v1/content/types'
        }).pluck('entity').subscribe(
            structureTypeView => {
                this._structureTypeView$.next(structureTypeView);
            }
        );
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
