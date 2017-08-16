import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RoutingPrivateAuthService } from '../../api/services/routing-private-auth-service';
import { RuleEngineContainer } from './rule-engine.container';

const ruleEngineRoutes: Routes = [
    {
        canActivate: [RoutingPrivateAuthService],
        component: RuleEngineContainer,
        path: '',
    }
];

@NgModule({
    exports: [
        RouterModule
    ],
    imports: [
        RouterModule.forChild(ruleEngineRoutes)
    ]
})
export class RuleEngineRoutingModule { }
