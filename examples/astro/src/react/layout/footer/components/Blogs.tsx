import RecommendedCard from "@components/react/RecommendedCard";

export default function Blogs({ blogs }: { blogs: any }) {
    if (!blogs?.length) return null;

    return (
        <div className="flex flex-col">
            <h2 className="text-2xl font-bold mb-7 text-white">
                Latest Blog Posts
            </h2>
            <div className="flex flex-col gap-5">
                {blogs.map((blog: any) => (
                    <RecommendedCard key={blog.identifier} contentlet={blog} />
                ))}
            </div>
        </div>
    );
}
