import { NgComponentOutlet } from "@angular/common";
import { Component, Input } from "@angular/core";

@Component({
    selector: 'dotcms-block-editor-renderer-bold',
    standalone: true,
    template: `
        <strong>
            {{text}}
        </strong>
    `
})
export class DotCMSBlockEditorRendererBoldComponent {
    @Input() text!: string;
 }

@Component({
    selector: 'dotcms-block-editor-renderer-italic',
    standalone: true,
    template: `
        <em>
            {{text}}
        </em>
    `
})
export class DotCMSBlockEditorRendererItalicComponent { 
    @Input() text!: string;
}

@Component({
    selector: 'dotcms-block-editor-renderer-strike',
    standalone: true,
    template: `
        <s>
            {{text}}
        </s>
    `
})
export class DotCMSBlockEditorRendererStrikeComponent { 
    @Input() text!: string;
}

@Component({
    selector: 'dotcms-block-editor-renderer-underline',
    standalone: true,
    template: `
        <u>
            {{text}}
        </u>
    `
})
export class DotCMSBlockEditorRendererUnderlineComponent { 
    @Input() text!: string;
}

@Component({
    selector: 'dotcms-block-editor-renderer-link',
    standalone: true,
    template: `
        <a [attr.href]="attrs['href']" [attr.target]="attrs['target']" [attr.rel]="attrs['rel']">
            {{text}}
        </a>
    `
})
export class DotCMSBlockEditorRendererLinkComponent {
    @Input() attrs!: Record<string, string>;
    @Input() text!: string;
}



@Component({
    selector: 'dotcms-block-editor-renderer-superscript',
    standalone: true,
    template: `
        <sup>
            {{text}}
        </sup>
    `
})
export class DotCMSBlockEditorRendererSuperscriptComponent {
    @Input() text!: string;
}

@Component({
    selector: 'dotcms-block-editor-renderer-subscript',
    standalone: true,
    template: `
        <sub>
            {{text}}
        </sub>
    `
})
export class DotCMSBlockEditorRendererSubscriptComponent {
    @Input() text!: string;
}

// Paragraph

@Component({
    selector: 'dotcms-block-editor-renderer-paragraph',
    standalone: true,
    template: `
        <p>
            <ng-content />
        </p>
    `
})
export class DotCMSBlockEditorRendererParagraphComponent { }

// Heading

@Component({
    selector: 'dotcms-block-editor-renderer-heading',
    standalone: true,
    template: `
        @switch (level) {
            @case('1') {
                <h1>
                    <ng-content />
                </h1>
            }
            @case('2') {
                <h2>
                    <ng-content />
                </h2>
            }
            @case('3') {
                <h3>
                    <ng-content />
                </h3>
            }
            @case('4') {
                <h4>
                    <ng-content />
                </h4>
            }
            @case('5') {
                <h5>
                    <ng-content />
                </h5>
            }
            @case('6') {
                <h6>
                    <ng-content />
                </h6>
            }
            @default {
                <h1>
                    <ng-content />
                </h1>
            }
        }
    `,
    imports: []
})
export class DotCMSBlockEditorRendererHeadingComponent {
    @Input() level!: string;

    ngOnInit(){
        console.log(this.level);
    }
}




const NodeMarks = {
    link: DotCMSBlockEditorRendererLinkComponent,
    bold: DotCMSBlockEditorRendererBoldComponent,
    underline: DotCMSBlockEditorRendererUnderlineComponent,
    italic: DotCMSBlockEditorRendererItalicComponent,
    strike: DotCMSBlockEditorRendererStrikeComponent,
    superscript: DotCMSBlockEditorRendererSuperscriptComponent,
    subscript: DotCMSBlockEditorRendererSubscriptComponent,
}

interface TextBlockProps {
    marks?: Array<{type: string; attrs: Record<string, unknown>}>;
    text?: string;
}

@Component({
    selector: 'dotcms-block-editor-renderer-text',
    standalone: true,
    imports: [NgComponentOutlet],
    template: `
        @if (component) {
            <ng-container *ngComponentOutlet="component; inputs: componentInputs" />
        } @else {
            {{text}}
        }
    `
})
export class DotCMSBlockEditorRendererTextComponent {
    @Input() marks: TextBlockProps['marks'] = [];
    @Input() text = '';

    protected get component() {
        const mark = this.marks?.[0] || { type: '', attrs: {} };

        return NodeMarks[mark.type as keyof typeof NodeMarks];
    }

    protected get componentInputs() {
        const mark = this.marks?.[0] || { type: '', attrs: {} };

        if (['link'].includes(mark.type)) {
            const attrs = {...mark.attrs};
            if (attrs['class']) {
                attrs['className'] = attrs['class'];
                delete attrs['class'];
            }

            return { text: this.text, attrs };
        }

        return { text: this.text };
    }

    protected get remainingMarks() {
        return this.marks?.slice(1);
    }
}