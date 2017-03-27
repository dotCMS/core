import _ from 'lodash';
import {ApiRoot} from '../persistence/ApiRoot';
import {CoreWebService} from './core-web-service';
import {FormatDateService} from './format-date-service';
import {Injectable} from '@angular/core';
import {LoginService, User} from './login-service';
import {Observable} from 'rxjs/Observable';
import {RequestMethod, Http} from '@angular/http';
import {Subject} from 'rxjs/Subject';

@Injectable()
export class MessageService {
    private _messageMap$: Subject<any> = new Subject<any>();
    private doMessageLoad;
    private i18nUrl: string;
    private lang: string;
    private messageKeys: String[];
    private messagesLoaded: any;

    constructor(loginService: LoginService, private formatDateService: FormatDateService,
                private coreWebService: CoreWebService) {

        // There are tons of components asking for messages at the same time, when messages are not loaded yet
        // instead of doing tons of request, we acumulate the keys every component is asking for and then do one
        // request with all of them. More info: https://lodash.com/docs/4.15.0#debounce
        this.doMessageLoad = _.debounce(this.requestMessages, 100);

        this.i18nUrl = 'v1/languages/i18n';
        this.lang = loginService.auth.user.languageId;
        this.messageKeys = [];
        this.messagesLoaded = {};
        this.setRelativeDateMessages();

        loginService.auth$.pluck('user').subscribe( (user: User) => {
            if (user && this.lang !== user.languageId) {
                this.messagesLoaded = {};
                this.messageKeys = [];
                this.lang = user.languageId;
                this.setRelativeDateMessages();
            }
        });
    }

    /**
     * Get the messages objects as an Observable
     * @returns {Observable<any>}
     */
    get messageMap$(): Observable<any> {
        return this._messageMap$.asObservable();
    }

    /**
     * Public method to get messages, will get from cache or the server.
     * @param keys
     * @returns {any}
     */
    public getMessages(keys: String[]): Observable<any> {
        return Observable.create(observer => {
            if (_.every(keys, _.partial(_.has, this.messagesLoaded))) {
                observer.next(_.pick(this.messagesLoaded, keys));
            } else {
                this.messageKeys = _.concat(this.messageKeys, _.difference(keys, this.messageKeys));
                this.doMessageLoad();
                let messageMapSub = this.messageMap$.subscribe(res => {
                    observer.next(_.pick(res, keys));
                    messageMapSub.unsubscribe();
                });
            }
        });
    }

    private setRelativeDateMessages(): string|void {
        let relativeDateKeys = [
            'relativetime.future',
            'relativetime.past',
            'relativetime.s',
            'relativetime.m',
            'relativetime.mm',
            'relativetime.h',
            'relativetime.hh',
            'relativetime.d',
            'relativetime.dd',
            'relativetime.M',
            'relativetime.MM',
            'relativetime.y',
            'relativetime.yy'
        ];
        this.getMessages(relativeDateKeys).subscribe(res => {
            let relativeDateMessages = _.mapKeys(res, function(value, key: string): string {
                return key.replace('relativetime.', '');
            });
            this.formatDateService.setLang(this.lang.split('_')[0], relativeDateMessages);
        });
    }

    /**
     * Do the request to the server to get messages
     */
    private requestMessages(): void {
        this.coreWebService.requestView({
            body: {
                messagesKey: this.messageKeys
            },
            method: RequestMethod.Post,
            url: this.i18nUrl,
        }).pluck('i18nMessagesMap').subscribe(messages => {
            this.messageKeys = [];
            this.messagesLoaded = Object.assign({}, this.messagesLoaded, messages);
            this._messageMap$.next(this.messagesLoaded);
        });
    }
}