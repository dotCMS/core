import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

const AUDIO = 'audiotrack';
const DOC = 'insert_drive_file';
const CODE = 'insert_drive_file';
const IMAGE = 'image';
const VIDEO = 'videocam';
const FONT = 'font_download';

const ICON_MAP: {
    [key: string]: {
        icon: string;
        color?: string;
    };
} = {
    // Misc
    page: { icon: 'web' },
    gear: { icon: 'settings' },
    content: { icon: 'library_books' },
    form: { icon: 'format_list_bulleted' },
    persona: { icon: 'person' },
    ukn: { icon: DOC },
    folder: { icon: 'folder' },

    // Text
    doc: { icon: DOC, color: '#2E8AED' },
    docx: { icon: DOC, color: '#2E8AED' },
    odt: { icon: DOC, color: '#2E8AED' },
    ott: { icon: DOC, color: '#2E8AED' },
    odm: { icon: DOC, color: '#2E8AED' },

    // Spreadsheet
    csv: { icon: DOC, color: '#1AAA6B' },
    numbers: { icon: DOC, color: '#1AAA6B' },
    wks: { icon: DOC, color: '#1AAA6B' },
    xls: { icon: DOC, color: '#1AAA6B' },
    xlsx: { icon: DOC, color: '#1AAA6B' },
    ods: { icon: DOC, color: '#1AAA6B' },
    ots: { icon: DOC, color: '#1AAA6B' },

    // Presentation
    keynote: { icon: DOC, color: '#F7C000' },
    ppt: { icon: DOC, color: '#F7C000' },
    pptx: { icon: DOC, color: '#F7C000' },
    odp: { icon: DOC, color: '#F7C000' },
    otp: { icon: DOC, color: '#F7C000' },

    // PDF Files
    pdf: { icon: DOC, color: '#F15B44' },

    // Video files
    asf: { icon: VIDEO },
    avi: { icon: VIDEO },
    mov: { icon: VIDEO },
    mp4: { icon: VIDEO },
    mpg: { icon: VIDEO },
    ogg: { icon: VIDEO },
    ogv: { icon: VIDEO },
    rm: { icon: VIDEO },
    vob: { icon: VIDEO },

    // Image Files
    bmp: { icon: IMAGE },
    image: { icon: IMAGE },
    jpeg: { icon: IMAGE },
    jpg: { icon: IMAGE },
    pct: { icon: IMAGE },
    png: { icon: IMAGE },
    gif: { icon: IMAGE },
    webp: { icon: IMAGE },
    svg: { icon: IMAGE },
    ico: { icon: IMAGE },

    // Audio
    aac: { icon: AUDIO },
    aif: { icon: AUDIO },
    iff: { icon: AUDIO },
    m3u: { icon: AUDIO },
    mid: { icon: AUDIO },
    mp3: { icon: AUDIO },
    mpa: { icon: AUDIO },
    ra: { icon: AUDIO },
    wav: { icon: AUDIO },
    wma: { icon: AUDIO },

    // Code
    vtl: { icon: CODE, color: 'var(--color-main)' },
    js: { icon: CODE, color: '#EBB131' },
    jsx: { icon: CODE, color: '#EBB131' },
    esm: { icon: CODE, color: '#EBB131' },
    ts: { icon: CODE, color: '#EBB131' },
    tsx: { icon: CODE, color: '#EBB131' },
    html: { icon: CODE, color: '#ED6832' },
    scss: { icon: CODE, color: '#2587C5' },
    sass: { icon: CODE, color: '#2587C5' },
    less: { icon: CODE, color: '#2587C5' },
    css: { icon: CODE, color: '#2587C5' },

    // Font
    otf: { icon: FONT },
    ttf: { icon: FONT },
    ttc: { icon: FONT },
    fnt: { icon: FONT },
    woff: { icon: FONT },
    woff2: { icon: FONT },
    eot: { icon: FONT }
};

@Component({
    selector: 'dot-contentlet-icon-ng',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './dot-contentlet-icon.component.html',
    styleUrl: './dot-contentlet-icon.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentletIconComponent {
    @Input() set contentlet(value: DotCMSContentlet) {
        this._contentlet = value;
        this.icon = this.getContentletIcon();
    }
    @Input() size = 24;

    private icon: string;
    private _contentlet: DotCMSContentlet;

    private get ext(): string {
        return this.icon.match('Icon') ? this.icon.replace('Icon', '') : '';
    }

    getIconName(): string {
        if (this.ext) {
            return this.getIconFromMap().icon;
        }

        return this.icon;
    }

    getIconColor(): string {
        if (this.ext) {
            return this.getIconFromMap().color || '#444';
        }

        return '#444';
    }

    private getIconFromMap(): { icon: string; color?: string } {
        return ICON_MAP[this.ext] || ICON_MAP['ukn'];
    }

    /**
     * Get icon for the contentlet
     */
    private getContentletIcon(): string {
        if (!this._contentlet) {
            return '';
        }

        if (this._contentlet.__icon__) {
            return this._contentlet.__icon__;
        }

        return this._contentlet.contentTypeIcon || '';
    }
}
