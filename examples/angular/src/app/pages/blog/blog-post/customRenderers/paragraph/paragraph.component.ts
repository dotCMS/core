import { CommonModule } from "@angular/common";
import { Component, Input, signal } from "@angular/core";
import { ContentNode } from "@dotcms/uve/types";

@Component({
    selector: 'app-paragraph',
    standalone: true,
    imports: [CommonModule],
    template: `
    <p>
        {{ $text() }}
    </p>
    `,
    styles: `
        p {
            font-size: 16px;
            line-height: 1.5;
            color: blue;
        }
    `
})
export class ParagraphComponent {
    @Input() content!: ContentNode;

    protected $text = signal<string>('');

    ngOnInit() {

        if(!this.content.content) {   
            return
        }

        const [{ text }] = this.content.content;
        this.$text.set(text ?? '');
    }
} 