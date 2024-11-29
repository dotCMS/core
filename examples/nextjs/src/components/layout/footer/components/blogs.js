import Contentlets from '@/components/shared/contentlets';

export default function Blogs({ blogs }) {
    return (
        <div className="flex flex-col">
            <h2 className="text-2xl font-bold mb-7 text-black">Latest Blog Posts</h2>
            {!!blogs.length && <Contentlets contentlets={blogs} />}
        </div>
    );
}
