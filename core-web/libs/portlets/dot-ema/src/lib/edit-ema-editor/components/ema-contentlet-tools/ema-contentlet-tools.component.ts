import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { ContentletArea } from '../ema-page-dropzone/ema-page-dropzone.component';

@Component({
    selector: 'dot-ema-contentlet-tools',
    standalone: true,
    imports: [CommonModule, ButtonModule, MenuModule],
    templateUrl: './ema-contentlet-tools.component.html',
    styleUrls: ['./ema-contentlet-tools.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmaContentletToolsComponent {
    items = [
        {
            label: 'Content',
            icon: 'pi pi-refresh',
            command: () => {
                // eslint-disable-next-line no-console
                console.log('Content');
            }
        },
        {
            label: 'Form',
            icon: 'pi pi-book',
            command: () => {
                // eslint-disable-next-line no-console
                console.log('Form');
            }
        },
        {
            label: 'Widget',
            icon: 'pi pi-cog',
            command: () => {
                // eslint-disable-next-line no-console
                console.log('Widget');
            }
        }
    ];

    @Input() contentlet: ContentletArea;

    getPosition(): Record<string, string> {
        return {
            left: `${this.contentlet.x}px`,
            top: `${this.contentlet.y}px`,
            width: `${this.contentlet.width}px`,
            height: `${this.contentlet.height}px`
        };
    }

    getTopButtonPosition(): Record<string, string> {
        const buttonWidth = 40;
        const buttonHeight = 40;
        const contentletCenterX = this.contentlet.x + this.contentlet.width / 2;
        const buttonLeft = contentletCenterX - buttonWidth / 2;
        const buttonTop = this.contentlet.y - buttonHeight / 2;

        return {
            position: 'absolute',
            left: `${buttonLeft}px`,
            top: `${buttonTop}px`,
            zIndex: '1'
        };
    }

    getBottomButtonPosition(): Record<string, string> {
        const buttonWidth = 40;
        const buttonHeight = 40;
        const contentletCenterX = this.contentlet.x + this.contentlet.width / 2;
        const buttonLeft = contentletCenterX - buttonWidth / 2;
        const buttonTop = this.contentlet.y + this.contentlet.height - buttonHeight / 2;

        return {
            position: 'absolute',
            left: `${buttonLeft}px`,
            top: `${buttonTop}px`,
            zIndex: '1'
        };
    }
}
