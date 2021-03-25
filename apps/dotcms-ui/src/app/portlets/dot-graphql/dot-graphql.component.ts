import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { renderPlaygroundPage } from 'graphql-playground-html';

@Component({
    selector: 'dot-dot-graphql',
    templateUrl: './dot-graphql.component.html',
    styleUrls: ['./dot-graphql.component.scss']
})
export class DotGraphqlComponent {
    private iframe: ElementRef<HTMLIFrameElement>;

    @ViewChild('iframe') set content(content: ElementRef<HTMLIFrameElement>) {
        if (content) {
            this.iframe = content;
            this.writeDocument();
        }
    }

    private API_URL = '/api/v1/graphql';

    constructor() {}

    private writeDocument(): void {
        const doc = this.iframe.nativeElement.contentWindow.document;
        doc.open();
        doc.write(
            renderPlaygroundPage({
                endpoint: this.API_URL
            })
        );
        doc.close();
    }
}
