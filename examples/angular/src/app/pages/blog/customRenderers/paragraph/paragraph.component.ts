import { CommonModule } from "@angular/common";
import { Component, Input } from "@angular/core";

@Component({
    selector: 'app-paragraph',
    standalone: true,
    imports: [CommonModule],
    template: `
    <p>
        <!-- {{text}} -->
         Custom Renderer
    </p>
    `,
    styles: `
        p {
            font-size: 16px;
            line-height: 1.5;
            color: red;
        }
    `
})
export class ParagraphComponent {
    // @Input() text: string = '';
    
} 