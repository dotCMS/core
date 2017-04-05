import {ApiRoot} from '../persistence/ApiRoot';
import {CoreWebService} from './core-web-service';
import {Http} from '@angular/http';
import {Injectable} from '@angular/core';
import {LoginService} from './login-service';
import {Observable} from 'rxjs/Rx';
import {RequestMethod} from '@angular/http';
import {Subject} from 'rxjs/Subject';
import {DotcmsEventsService} from './dotcms-events-service';

@Injectable()
export class ContentletService {
    private structureTypeView: StructureTypeView[];
    private _structureTypeView$: Subject<StructureTypeView[]> = new Subject<StructureTypeView[]>();

    constructor(loginService: LoginService, dotcmsEventsService: DotcmsEventsService,
                private coreWebService: CoreWebService) {

        loginService.watchUser(this.loadContentTypes.bind(this));

        dotcmsEventsService.subscribeTo('SAVE_BASE_CONTENT_TYPE').pluck('data').subscribe( data => {
            let contentTypeView: ContentTypeView = <ContentTypeView> data;
            let structureTypeView: StructureTypeView = this.getStructureTypeView(contentTypeView.type);
            structureTypeView.types.push(contentTypeView);
            this._structureTypeView$.next(this.structureTypeView);
        });

        dotcmsEventsService.subscribeTo('UPDATE_BASE_CONTENT_TYPE').pluck('data').subscribe( data => {
            let contentTypeViewUpdated: ContentTypeView = <ContentTypeView> data;
            let structureTypeView: StructureTypeView = this.getStructureTypeView(contentTypeViewUpdated.type);

            structureTypeView.types = structureTypeView.types.map(
                contentTypeView => contentTypeView.inode === contentTypeViewUpdated.inode ? contentTypeViewUpdated : contentTypeView);

            this._structureTypeView$.next(this.structureTypeView);
        });

        dotcmsEventsService.subscribeTo('DELETE_BASE_CONTENT_TYPE').pluck('data').subscribe( data => {
            let contentTypeViewRemoved = <ContentTypeView> data;
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
        this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: 'v1/contenttype/basetypes'
        }).pluck('entity').subscribe(
            structureTypeView => {
                this.structureTypeView = <StructureTypeView[]> structureTypeView;
                this._structureTypeView$.next(<StructureTypeView[]> structureTypeView);
            }
        );
    }

    private getStructureTypeView(type: StructureType): StructureTypeView {
        return this.structureTypeView.filter(structureTypeView => structureTypeView.name === type.toString())[0];
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
