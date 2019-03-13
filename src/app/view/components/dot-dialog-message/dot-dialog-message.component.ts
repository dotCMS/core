import {
    DotDialogMessageService,
    DotDialogMessageParams
} from './services/dot-dialog-message.service';
import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';

@Component({
    selector: 'dot-dialog-message',
    templateUrl: './dot-dialog-message.component.html',
    styleUrls: ['./dot-dialog-message.component.scss']
})
export class DotDialogMessageComponent implements OnInit {
    data$: Observable<DotDialogMessageParams>;

    constructor(public dotDialogMessageService: DotDialogMessageService) {}

    ngOnInit() {
        this.data$ = this.dotDialogMessageService.sub();
    }

    close() {
        this.dotDialogMessageService.push(null);
    }

    show(type: string): void {
        if (type === 'html') {
            this.dotDialogMessageService.push({
                title: 'Hello World',
                width: '600px',
                body: '<h1>Hello World</h1>'
            });
        } else {
            this.dotDialogMessageService.push({
                title: 'Velocity Error',
                width: '80vw',
                code: {
                    lang: 'stacktrace',
                    content:
`11:53:39.692  ERROR filters.CookiesFilter - Exception processing Cookies
org.apache.catalina.connector.ClientAbortException: java.io.IOException: Broken pipe
	at org.apache.catalina.connector.OutputBuffer.realWriteBytes(OutputBuffer.java:356) ~[catalina.jar:8.5.32]
	at org.apache.catalina.connector.OutputBuffer.appendByteArray(OutputBuffer.java:795) ~[catalina.jar:8.5.32]
	at org.apache.catalina.connector.OutputBuffer.append(OutputBuffer.java:724) ~[catalina.jar:8.5.32]
	at org.apache.catalina.connector.OutputBuffer.writeBytes(OutputBuffer.java:391) ~[catalina.jar:8.5.32]
	at org.apache.catalina.connector.OutputBuffer.write(OutputBuffer.java:369) ~[catalina.jar:8.5.32]
	at org.apache.catalina.connector.CoyoteOutputStream.write(CoyoteOutputStream.java:96) ~[catalina.jar:8.5.32]
	at org.apache.catalina.connector.CoyoteOutputStream.write(CoyoteOutputStream.java:89) ~[catalina.jar:8.5.32]
	at org.apache.catalina.servlets.DefaultServlet.serveResource(DefaultServlet.java:1007) ~[catalina.jar:8.5.32]
	at org.apache.catalina.servlets.DefaultServlet.doGet(DefaultServlet.java:438) ~[catalina.jar:8.5.32]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:635) ~[servlet-api.jar:?]
	at org.apache.catalina.servlets.DefaultServlet.service(DefaultServlet.java:418) ~[catalina.jar:8.5.32]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:742) ~[servlet-api.jar:?]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:231) ~[catalina.jar:8.5.32]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[catalina.jar:8.5.32]
	at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:52) ~[tomcat-websocket.jar:8.5.32]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[catalina.jar:8.5.32]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[catalina.jar:8.5.32]
	at com.dotmarketing.filters.CMSFilter.doFilterInternal(CMSFilter.java:199) ~[dotcms_5.0.0_fba456a.jar:?]
	at com.dotmarketing.filters.CMSFilter.doFilter(CMSFilter.java:56) ~[dotcms_5.0.0_fba456a.jar:?]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[catalina.jar:8.5.32]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[catalina.jar:8.5.32]
	at com.dotcms.filters.interceptor.AbstractWebInterceptorSupportFilter.doFilter(AbstractWebInterceptorSupportFilter.java:90) ~[dotcms_5.0.0_fba456a.jar:?]`
                }
            });
        }
    }
}
