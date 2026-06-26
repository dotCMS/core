import RecommendedCard from "@/components/RecommendedCard";
import type { Blog } from "@/types/content";

interface BlogsProps {
    blogs?: Blog[];
}

export default function Blogs({ blogs }: BlogsProps) {
    if (!blogs?.length) return null;

    return (
        <div className="flex flex-col">
            <h2 className="mb-6 text-sm font-semibold uppercase tracking-wider text-bg/60">
                Latest stories
            </h2>
            <div className="flex flex-col gap-4">
                {blogs.map((blog) => (
                    <RecommendedCard key={blog.identifier} contentlet={blog} />
                ))}
            </div>
        </div>
    );
}
