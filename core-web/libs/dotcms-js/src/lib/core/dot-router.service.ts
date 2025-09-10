import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
/**
 * Router service with common routing methods
 *
 * @export
 * @class DotRouterService
 * @deprecated Please use @services/dot-router/dot-router.service
 */
@Injectable()
export class DotRouterService {
    private router = inject(Router);

    public goToMain(): void {
        this.router.navigate(['/c']);
    }

    public goToLogin(parameters?: any): void {
        this.router.navigate(['/public/login'], parameters);
    }

    public goToURL(url: string): void {
        this.router.navigate([url]);
    }

    public isPublicUrl(url: string): boolean {
        return url.startsWith('/public');
    }

    public isFromCoreUrl(url: string): boolean {
        return url.startsWith('/fromCore');
    }

    public isRootUrl(url: string): boolean {
        return url === '/';
    }

    public gotoPortlet(portletId: string): void {
        this.router.navigate([`c/${portletId.replace(' ', '_')}`]);
    }

    public goToForgotPassword(): void {
        this.router.navigate(['/public/forgotPassword']);
    }

    public goToNotLicensed(): void {
        this.router.navigate(['c/notLicensed']);
    }
}
