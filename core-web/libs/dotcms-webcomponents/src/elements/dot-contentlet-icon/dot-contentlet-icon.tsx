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
    doc: { icon: doc },
    docx: { icon: doc },
    odt: { icon: doc },
    ott: { icon: doc },
    odm: { icon: doc },

    // Spreadsheet
    csv: { icon: doc },
    numbers: { icon: doc },
    wks: { icon: doc },
    xls: { icon: doc },
    xlsx: { icon: doc },
    ods: { icon: doc },
    ots: { icon: doc },

    // Presentation
    keynote: { icon: doc },
    ppt: { icon: doc },
    pptx: { icon: doc },
    odp: { icon: doc },
    otp: { icon: doc },

    // PDF Files
    pdf: { icon: doc },

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
    vtl: { icon: code },
    js: { icon: code },
    jsx: { icon: code },
    esm: { icon: code },
    ts: { icon: code },
    tsx: { icon: code },
    html: { icon: code },
    scss: { icon: code },
    sass: { icon: code },
    less: { icon: code },
    css: { icon: code },

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
            If it contains "Icon" (with uppercase) we extract the extension by removing "Icon",
            otherwise we're assuming that is coming as a material icon name.
            This is the only way we have to differentiate between new and legacy icons without passing an extra attribute.
        */
        this.ext =
            this.icon && this.icon.includes('Icon') && this.icon !== 'Icon'
                ? this.icon.replace('Icon', '')
                : '';
    }

    render() {
        let icon: string;
        if (this.ext) {
            // Icon has "Icon" suffix, look it up in the map
            icon = this.getIconName().icon;
        } else if (this.icon) {
            // Icon is a direct material icon name
            icon = this.icon;
        } else {
            // No icon provided, use fallback
            icon = 'insert_drive_file';
        }

        return (
            <Host>
                {/* Icon's size in Card View is 96, lower than that the label won't be displayed  */}
                {icon === 'insert_drive_file' && parseInt(this.size.replace('px', ''), 10) >= 96 ? (
                    <span>{this.ext}</span>
                ) : null}
                <mwc-icon style={{ '--mdc-icon-size': this.size, color: 'var(--gray-700)' }}>
                    {icon}
                </mwc-icon>
            </Host>
        );
    }

    private getIconName(): {
        icon: string;
    } {
        return map[this.ext] || map['ukn'];
    }
}
