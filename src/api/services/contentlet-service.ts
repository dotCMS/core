import {Inject, Injectable} from '@angular/core';
import {CoreWebService} from './core-web-service';
import {RequestMethod} from '@angular/http';
import {ResponseView} from './response-view';
import {Observable} from 'rxjs/Rx';
import {ApiRoot} from '../persistence/ApiRoot';
import {Http} from '@angular/http';
import {LoginService} from './login-service';
import {Subject} from 'rxjs/Subject';
import {DotcmsEventsService} from './dotcms-events-service';

@Injectable()
export class ContentletService extends CoreWebService {
    private structureTypeView: StructureTypeView[];
    private _structureTypeView$: Subject<StructureTypeView[]> = new Subject<StructureTypeView[]>();

    constructor(apiRoot: ApiRoot, http: Http, loginService: LoginService, dotcmsEventsService: DotcmsEventsService) {
        super(apiRoot, http);

        loginService.watchUser(this.loadContentTypes.bind(this));

        dotcmsEventsService.subscribeTo('SAVE_BASE_CONTENT_TYPE').pluck('data').subscribe( contentTypeView => {
            console.log('contentTypeView', contentTypeView);
            let structureTypeView: StructureTypeView = this.getStructureTypeView(contentTypeView.type);
            structureTypeView.types.push(contentTypeView);
            this._structureTypeView$.next(this.structureTypeView);
        });

        dotcmsEventsService.subscribeTo('UPDATE_BASE_CONTENT_TYPE').pluck('data').subscribe( contentTypeViewUpdated => {
            let structureTypeView: StructureTypeView = this.getStructureTypeView(contentTypeViewUpdated.type);

            structureTypeView.types = structureTypeView.types.map(
                contentTypeView => contentTypeView.inode === contentTypeViewUpdated.inode ? contentTypeViewUpdated : contentTypeView);

            this._structureTypeView$.next(this.structureTypeView);
        });

        dotcmsEventsService.subscribeTo('DELETE_BASE_CONTENT_TYPE').pluck('data').subscribe( contentTypeViewRemoved => {
            let structureTypeView: StructureTypeView = this.getStructureTypeView(contentTypeViewRemoved.type);
            structureTypeView.types = structureTypeView.types.filter(
                contentTypeView => contentTypeView.inode !== contentTypeViewRemoved.inode);
            this._structureTypeView$.next(this.structureTypeView);
        });
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
                this.structureTypeView = structureTypeView;
                this._structureTypeView$.next(structureTypeView);
            }
        );
    }

    private getStructureTypeView(type: string): StructureTypeView {
        return this.structureTypeView.filter(structureTypeView => structureTypeView.name === type)[0];
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
