import { Component, h, Prop, Host } from '@stencil/core';
import '@material/mwc-icon';

const audio = 'audiotrack';
const doc = 'insert_drive_file';
const code = 'insert_drive_file';
const image = 'image';
const video = 'videocam';
const font = 'font_download';

const map: {
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
    ukn: { icon: doc },
    folder: { icon: 'folder' },

    // Text
    doc: { icon: doc, color: '#2E8AED' },
    docx: { icon: doc, color: '#2E8AED' },
    odt: { icon: doc, color: '#2E8AED' },
    ott: { icon: doc, color: '#2E8AED' },
    odm: { icon: doc, color: '#2E8AED' },

    // Spreadsheet
    csv: { icon: doc, color: '#1AAA6B' },
    numbers: { icon: doc, color: '#1AAA6B' },
    wks: { icon: doc, color: '#1AAA6B' },
    xls: { icon: doc, color: '#1AAA6B' },
    xlsx: { icon: doc, color: '#1AAA6B' },
    ods: { icon: doc, color: '#1AAA6B' },
    ots: { icon: doc, color: '#1AAA6B' },

    // Presentation
    keynote: { icon: doc, color: '#F7C000' },
    ppt: { icon: doc, color: '#F7C000' },
    pptx: { icon: doc, color: '#F7C000' },
    odp: { icon: doc, color: '#F7C000' },
    otp: { icon: doc, color: '#F7C000' },

    // PDF Files
    pdf: { icon: doc, color: '#F15B44' },

    // Video files
    asf: { icon: video },
    avi: { icon: video },
    mov: { icon: video },
    mp4: { icon: video },
    mpg: { icon: video },
    ogg: { icon: video },
    ogv: { icon: video },
    rm: { icon: video },
    vob: { icon: video },

    // Image Files
    bmp: { icon: image },
    image: { icon: image },
    jpeg: { icon: image },
    jpg: { icon: image },
    pct: { icon: image },
    png: { icon: image },
    gif: { icon: image },
    webp: { icon: image },
    svg: { icon: image },
    ico: { icon: image },

    // Audio
    aac: { icon: audio },
    aif: { icon: audio },
    iff: { icon: audio },
    m3u: { icon: audio },
    mid: { icon: audio },
    mp3: { icon: audio },
    mpa: { icon: audio },
    ra: { icon: audio },
    wav: { icon: audio },
    wma: { icon: audio },

    // Code
    vtl: { icon: code, color: 'var(--color-main)' },
    js: { icon: code, color: '#EBB131' },
    jsx: { icon: code, color: '#EBB131' },
    esm: { icon: code, color: '#EBB131' },
    ts: { icon: code, color: '#EBB131' },
    tsx: { icon: code, color: '#EBB131' },
    html: { icon: code, color: '#ED6832' },
    scss: { icon: code, color: '#2587C5' },
    sass: { icon: code, color: '#2587C5' },
    less: { icon: code, color: '#2587C5' },
    css: { icon: code, color: '#2587C5' },

    // Font
    otf: { icon: font },
    ttf: { icon: font },
    ttc: { icon: font },
    fnt: { icon: font },
    woff: { icon: font },
    woff2: { icon: font },
    eot: { icon: font }
};

/**
 * Represent a mapping of legacy icons if DotCMS
 *
 * @export
 * @class DotFileIcon
 */
@Component({
    tag: 'dot-contentlet-icon',
    styleUrl: 'dot-contentlet-icon.scss',
    shadow: true
})
export class DotContentletIcon {
    @Prop({ reflect: true })
    icon = '';

    @Prop({ reflect: true })
    size = '';

    private ext: string;

    componentWillRender() {
        /*
            If it doesn't contain "Icon" (with uppercase) we're assuming that is coming to a material icon name,
            which is the only way we have to differentiate between new and legacy icons without passing an extra attribute.
        */
        this.ext = this.icon.match('Icon') ? this.icon.replace('Icon', '') : '';
    }

    render() {
        const { icon, color } = this.ext ? this.getIconName() : { icon: this.icon, color: '' };

        return (
            <Host>
                {/* Icon's size in Card View is 96, lower than that the label won't be displayed  */}
                {icon === 'insert_drive_file' && parseInt(this.size.replace('px', ''), 10) >= 96 ? (
                    <span>{this.ext}</span>
                ) : null}
                <mwc-icon style={{ '--mdc-icon-size': this.size, color: color || '#444' }}>
                    {icon}
                </mwc-icon>
            </Host>
        );
    }

    private getIconName(): {
        icon: string;
        color?: string;
    } {
        return map[this.ext] || map['ukn'];
    }
}
