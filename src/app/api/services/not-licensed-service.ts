import {Injectable} from '@angular/core';
import {RoutingService} from './routing-service';
import {CoreWebService} from './core-web-service';
import { DotRouterService } from './dot-router-service';
import { HttpCode } from '../util/http-code';

@Injectable()
export class NotLicensedService {
    constructor(private routingService: RoutingService, private coreWebService: CoreWebService,
                private router: DotRouterService) {
    }

    public init(): void {
        this.coreWebService.subscribeTo(HttpCode.FORBIDDEN).subscribe(res => this.router.goToNotLicensed());
    }
}