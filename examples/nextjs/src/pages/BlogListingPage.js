"use client";

import { useState } from "react";

import { useIsEditMode } from "@/hooks/isEditMode";
import { useEditableDotCMSPage } from "@dotcms/react/next";
import { editContentlet } from "@dotcms/uve";

import Image from "next/image";
import Header from "@/components/header";

export function BlogListingPage(pageResponse) {
    const { content } = useEditableDotCMSPage(pageResponse);
    const [searchQuery, setSearchQuery] = useState("");
    const allBlogs = content.blogs || [];

    const filteredBlogs = searchQuery
        ? allBlogs.filter((blog) =>
              blog.title.toLowerCase().includes(searchQuery.toLowerCase())
          )
        : allBlogs;

    return (
        <div className="flex flex-col gap-6 min-h-screen bg-slate-50">
            <Header />
            <main className="container mx-auto px-4 py-8">
                <div className="flex flex-col gap-4 mb-8">
                    <h1 className="text-4xl font-bold text-center">Travel Blog</h1>
                    <p className="text-gray-600 text-center">Get inspired to experience the world. Our writers will give you their first-hand stories and recommendations that will inspire, excite you, and help you make the best decisions for planning your next adventure.</p>
                </div>

                <SearchBar searchQuery={searchQuery} setSearchQuery={setSearchQuery} />

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
                        &copy; {new Date().getFullYear()} TravelLux. All rights reserved.
                    </p>
                </div>
            </footer>
        </div>
    );
}

const SearchBar = ({ searchQuery, setSearchQuery }) => {
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
                    className="block w-full p-4 pl-10 text-sm text-gray-900 border border-gray-300 rounded-lg bg-white focus:ring-blue-500 focus:border-blue-500 outline-none"
                    placeholder="Search blogs..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                />
            </div>
        </div>
    );
};

const BlogCard = ({ blog }) => {
    const {
        title,
        identifier,
        image,
        urlMap,
        inode,
        modDate,
        urlTitle,
        teaser,
    } = blog;
    const author = blog.author?.[0];

    const dateFormatOptions = {
        year: "numeric",
        month: "long",
        day: "numeric",
    };

    const isEditMode = useIsEditMode();

    return (
        <div className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow duration-300 relative flex flex-col h-full">
            {isEditMode && (
                <button
                    onClick={() => editContentlet(blog)}
                    className="absolute top-2 right-2 z-10 bg-blue-500 text-white rounded-md py-2 px-4 shadow-md hover:bg-blue-600"
                >
                    Edit
                </button>
            )}

            <div className="relative h-48 w-full">
                {image ? (
                    <Image
                        src={inode}
                        alt={urlTitle || title}
                        fill={true}
                        className="object-cover"
                    />
                ) : (
                    <div className="absolute inset-0 bg-gray-200 flex items-center justify-center">
                        <span className="text-gray-400">No image</span>
                    </div>
                )}
            </div>

            <div className="p-4 flex flex-col flex-grow">
                <h3 className="text-lg font-bold mb-2 hover:text-blue-600">
                    <a href={urlMap}>{title}</a>
                </h3>

                {teaser && (
                    <p className="text-gray-600 text-sm mb-3 line-clamp-2">
                        {teaser}
                    </p>
                )}

                <div className="flex justify-between items-center mt-auto pt-3 border-t border-gray-100">
                    {author && (
                        <div className="text-sm text-gray-700">
                            {author.firstName && author.lastName
                                ? `${author.firstName} ${author.lastName}`
                                : "Unknown Author"}
                        </div>
                    )}

                    <time className="text-sm text-gray-500">
                        {new Date(modDate).toLocaleDateString(
                            "en-US",
                            dateFormatOptions
                        )}
                    </time>
                </div>
            </div>
        </div>
    );
};
