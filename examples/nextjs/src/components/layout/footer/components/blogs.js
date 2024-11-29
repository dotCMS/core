import { useEffect, useState } from 'react';
import Contentlets from '@/components/shared/contentlets';
import { client } from '@/utils/dotcmsClient';

export default function Blogs({ blogs }) {
    return (
        <div className="flex flex-col">
            <h2 className="text-2xl font-bold mb-7 text-black">Latest Blog Posts</h2>
            {!!blogs.length && <Contentlets contentlets={blogs} />}
        </div>
    );
}
