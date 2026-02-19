export default function YouTube(props) {
    const content = props.content || props;
    if (!content || !content.id) return null;

    const videoId = content.id;
    const title = content.title;
    const author = content.author;
    const length = content.length;
    const thumbnail = content.thumbnailLarge;

    return (
        <div className="max-w-4xl mx-auto p-4 border border-gray-200 rounded-lg mb-8">
            <div className="aspect-w-16 aspect-h-9 mb-4">
                <iframe
                    src={`https://www.youtube.com/embed/${videoId}`}
                    title={title}
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                    allowFullScreen
                    className="w-full h-full rounded-lg shadow-lg"
                />
            </div>

            <div className="space-y-2">
                <h1 className="text-2xl font-bold text-gray-900">{title}</h1>
                <div className="flex items-center space-x-4 text-sm text-gray-600">
                    <span>{author}</span>
                    <span>•</span>
                    <span>{length}</span>
                </div>
            </div>
        </div>
    );
}
