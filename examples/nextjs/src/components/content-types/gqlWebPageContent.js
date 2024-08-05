function GqlWebPageContent({ body }) {
    return (
        <div className="prose lg:prose-xl prose-a:text-blue-600 prose-h3:mb-0 prose-h3:text-purple-700 prose-h2:text-yellow-800 prose-h2:text-4xl prose-h2:tracking-tight prose-h3:tracking-tighter" dangerouslySetInnerHTML={{ __html: body }}>
        </div>
    );
}

export default GqlWebPageContent;
