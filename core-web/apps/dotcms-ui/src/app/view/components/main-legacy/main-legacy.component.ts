import { Component, OnInit, ViewEncapsulation, inject } from '@angular/core';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'dot-main-component',
    styleUrls: ['./main-legacy.component.scss'],
    templateUrl: './main-legacy.component.html',
    standalone: false
})
export class MainComponentLegacyComponent implements OnInit {
    private dotCustomEventHandlerService = inject(DotCustomEventHandlerService);

    ngOnInit(): void {
        document.body.style.backgroundColor = '';
        document.body.style.backgroundImage = '';
    }

    /**
     * Handle the custom events emmited by the Create Contentlet
     *
     * @param CustomEvent $event
     * @memberof MainComponentLegacyComponent
     */
    onCustomEvent($event: CustomEvent): void {
        this.dotCustomEventHandlerService.handle($event);
    }
}
