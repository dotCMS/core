import {Injectable} from '@angular/core';
import {RoutingService} from './routing-service';
import {CoreWebService} from './core-web-service';
import {RequestMethod, Http} from '@angular/http';
import {DotRouterService} from './dot-router-service';

@Injectable()
export class NotLicensedService {
    constructor(private routingService: RoutingService, private coreWebService: CoreWebService,
                private router: DotRouterService) {
    }

    public init(): void {
        this.coreWebService.subscribeTo(403).subscribe(res => this.router.goToNotLicensed());
    }
}

