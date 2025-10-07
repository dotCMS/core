import { useEffect, useState } from "react";

import { useEditableDotCMSPage } from "@dotcms/react";

import { dotCMSClient } from "@/dotcms-integration";
import { useDebounce } from "@/hooks";
import { BlogCard } from "@/components/ui";
import Header from "@/components/common/Header";
import type { DotCMSCustomPageResponse } from "@/types/page.model";
import type { DotCMSNavigationItem } from "@dotcms/types";

export function BlogListingPage({ pageResponse }: { pageResponse: DotCMSCustomPageResponse }) {
    const { content } = useEditableDotCMSPage<DotCMSCustomPageResponse>(pageResponse);
    const [searchQuery, setSearchQuery] = useState("");
    const [filteredBlogs, setFilteredBlogs] = useState<any[]>([]);
    const debouncedSearchQuery = useDebounce(searchQuery, 500)
    const navigation = content?.navigation;

    useEffect(() => {
        const allBlogs = content?.blogs || [];

        if (!debouncedSearchQuery.length) {
            setFilteredBlogs(allBlogs);
            return;
        }

        dotCMSClient.content
            .getCollection("Blog")
            .limit(3)
            .query((qb) => qb.field("title").equals(`${debouncedSearchQuery}*`))
            .sortBy([
                {
                    field: "Blog.postingDate",
                    order: "desc",
                },
            ])
            .then(({ contentlets }: { contentlets: any[] }) => {
                setFilteredBlogs(contentlets);
            });
    }, [debouncedSearchQuery, content?.blogs]);

    return (
        <div className="flex flex-col gap-6 min-h-screen bg-slate-50">
            <Header navigation={navigation as DotCMSNavigationItem} />
            <main className="container mx-auto px-4 py-8">
                <div className="flex flex-col gap-4 mb-8">
                    <h1 className="text-4xl font-bold text-center">
                        Travel Blog
                    </h1>
                    <p className="text-gray-600 text-center">
                        Get inspired to experience the world. Our writers will
                        give you their first-hand stories and recommendations
                        that will inspire, excite you, and help you make the
                        best decisions for planning your next adventure.
                    </p>
                </div>

                <SearchBar
                    searchQuery={searchQuery}
                    setSearchQuery={setSearchQuery}
                />

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {filteredBlogs.map((blog) => (
                        <BlogCard key={blog.identifier} blog={blog} />
                    ))}
                </div>

                {filteredBlogs.length === 0 && (
                    <div className="text-center py-8">
                        <p className="text-gray-500">
                            No blogs found matching your search criteria.
                        </p>
                    </div>
                )}
            </main>
            <footer className="bg-slate-50 text-slate-900 py-4">
                <div className="container mx-auto px-4">
                    <p className="text-center">
                        &copy; {new Date().getFullYear()} TravelLux. All rights
                        reserved.
                    </p>
                </div>
            </footer>
        </div>
    );
}

const SearchBar = ({ searchQuery, setSearchQuery }: { searchQuery: string, setSearchQuery: (query: string) => void }) => {
    return (
        <div className="mb-8">
            <div className="relative">
                <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
                    <svg
                        className="w-4 h-4 text-gray-500"
                        xmlns="http://www.w3.org/2000/svg"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                    >
                        <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                        />
                    </svg>
                </div>
                <input
                    type="search"
                    className="block w-full p-4 pl-10 text-sm text-gray-900 border border-gray-300 rounded-lg bg-white focus:ring-violet-800 focus:border-violet-800 outline-hidden"
                    placeholder="Search blogs..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                />
            </div>
        </div>
    );
};
