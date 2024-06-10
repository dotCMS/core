import useLatest from "@/components/hooks/useLatest";
import Contentlets from "@/components/shared/contentlets";

export default function Blogs() {
    const blogs = useLatest("Blog");

    return (
        <div className="flex flex-col">
            <h2 className="text-2xl font-bold mb-7">Latest Blog Posts</h2>
            {blogs.length && <Contentlets contentlets={blogs} />}
        </div>
    );
}
