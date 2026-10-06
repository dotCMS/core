import { BlockEditorNode } from '@dotcms/types';

interface DotCMSAudioProps {
    src: string;
    mimeType: string;
}

/**
 * Renders an audio component for displaying audio files. Mirrors {@link DotCMSVideo}
 * but without poster/width/height, which are not meaningful for audio.
 *
 * @param props - The properties for the audio component.
 * @returns The rendered audio component.
 */
export const DotCMSAudio = ({ node }: { node: BlockEditorNode }) => {
    const { src, mimeType } = node.attrs as DotCMSAudioProps;

    return (
        <audio controls preload="metadata">
            <source src={src} type={mimeType} />
            Your browser does not support the <code>audio</code> element.
        </audio>
    );
};
