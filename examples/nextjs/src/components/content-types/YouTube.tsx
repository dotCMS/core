interface YouTubeContent {
    id?: string;
    title?: string;
    author?: string;
    length?: string;
    thumbnailLarge?: string;
}

type YouTubeProps = YouTubeContent & {
    content?: YouTubeContent;
};

export default function YouTube(props: YouTubeProps) {
    const content = props.content || props;
    if (!content || !content.id) return null;

    const { id: videoId, title, author, length } = content;

    return (
        <figure className="mx-auto w-full max-w-4xl overflow-hidden rounded-2xl border border-line bg-bg shadow-sm">
            <div className="aspect-video w-full bg-surface-2">
                <iframe
                    src={`https://www.youtube.com/embed/${videoId}`}
                    title={title}
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                    allowFullScreen
                    className="h-full w-full"
                />
            </div>

            <figcaption className="flex flex-col gap-1 p-5">
                <h3 className="font-display text-xl font-semibold text-ink">{title}</h3>
                <div className="flex items-center gap-2 text-sm text-muted">
                    {author && <span>{author}</span>}
                    {author && length && <span aria-hidden="true">•</span>}
                    {length && <span>{length}</span>}
                </div>
            </figcaption>
        </figure>
    );
}
