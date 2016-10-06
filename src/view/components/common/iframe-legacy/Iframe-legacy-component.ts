import {Component, ElementRef, ViewEncapsulation} from '@angular/core';
import {LoginService} from '../../../../api/services/login-service';
import {ActivatedRoute} from '@angular/router';
import {RoutingService} from '../../../../api/services/routing-service';
import {SafeResourceUrl, DomSanitizer} from '@angular/platform-browser';
import {SiteChangeListener} from '../../../../api/util/site-change-listener';
import {SiteService} from '../../../../api/services/site-service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    selector: 'dot-iframe',
    styleUrls: ['iframe-legacy-component.css'],
    templateUrl: ['iframe-legacy-component.html']
})
export class IframeLegacyComponent extends SiteChangeListener {
    iframe: SafeResourceUrl;
    iframeElement;
    private loadingInProgress: boolean = true;
    private currentId: string;

    constructor(private route: ActivatedRoute, private routingService: RoutingService, private siteService: SiteService,
                private sanitizer: DomSanitizer, private element: ElementRef, private loginService: LoginService) {
        super(siteService);
    }

    ngOnInit(): void {
        this.iframeElement = this.element.nativeElement.querySelector('iframe');

        if (this.routingService.currentMenu) {
            this.initComponent();
        }
        
        this.iframeElement.onload = () => this.loadingInProgress = false;
    }

    initComponent(): void {
        this.route.params.pluck<string>('id').subscribe(res => {
            this.currentId = res;
            this.iframe = this.loadURL(this.routingService.getPortletURL(this.currentId) + '&in_frame=true&frame=detailFrame');
        });
    }

    changeSiteReload(): void {
        if (this.iframeElement && this.iframeElement.contentWindow
            && this.currentId !== 'EXT_HOSTADMIN') {

            this.loadingInProgress = true;
            this.iframeElement.contentWindow.location.reload();
        }
    }

    loadURL(url: string): SafeResourceUrl {
        this.loadingInProgress = true;
        return this.sanitizer.bypassSecurityTrustResourceUrl(url);
    }

    /**
     * Validate if the iframe window is send to the login page after jsessionid expired
     * then logout the user from angular session
     */
    checkSessionExpired(): void {
        if (this.iframeElement && this.iframeElement.contentWindow) {
            let currentPath = this.iframeElement.contentWindow.location.pathname;

            if (currentPath.indexOf('/c/portal_public/login') !== -1) {
                this.loginService.logOutUser().subscribe(data => {
                }, (error) => {
                    console.log(error);
                });
            }
        }
    }
}
