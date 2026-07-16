import { GridStackWidget } from 'gridstack';
import { DDElementHost } from 'gridstack/dist/dd-element';

import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    HostListener,
    Input,
    inject
} from '@angular/core';

@Component({
    selector: 'dotcms-add-widget',
    templateUrl: './add-widget.component.html',
    styleUrls: ['./add-widget.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddWidgetComponent implements AfterViewInit {
    private el = inject(ElementRef);

    @Input() label = 'Add Widget';
    @Input() icon = '';
    @Input() gridstackOptions: GridStackWidget;

    protected imageError = false;

    ngAfterViewInit(): void {
        this.el.nativeElement.gridstackNode = {
            ...this.gridstackOptions
        };
    }

    @HostListener('mousedown')
    setGridOptions(): void {
        /*
            GS = GrisStack

            This code is a hack made to override all the gridstack logic on "dropover",
            that is basically when you hover an element on a row.

            This does not interfere with the logic when moving from a row to another, that has another workflow.

            Basically, we need to set the initial size for our widgets and
            what's happening is that GS cleans the node where it takes the initial options if it takes it from attributes
            and the values are the same as the defaults like: gs-w="1", gs-h="1" or gs-x="0", gs-y="0".

            So, after it deletes this attributes (now our node doesn't have any options on attributes), it will calculate
            all the sizes based on the client size and the button size, so it can occupy the same space as the button itself.

            That behavior makes GS initialize our widgets with arbitrary sizes calculated under the hood,
            so we need to override it.

            And the way to override is to set the gridstackNode property on the element,
            so GS will take the options from there instead of the attributes.

            But, what's happening again, is that GS is cleaning the gridstackNode property as well when the drop is made.

            So we need to set it again on the mousedown event, so it will be set after GS cleans it.

            Also see the method fixGridstackNodeOnMouseLeave in template-builder.component.ts, that fixes the case
            when you leave a row and GS cleans the gridstackNode property.
        */

        this.el.nativeElement.gridstackNode = {
            ...this.gridstackOptions
        };
    }

    get nativeElement(): DDElementHost {
        return this.el.nativeElement;
    }
}
