"use client";

import { useState } from "react";

// Function to filter out unwanted properties from data
const filterData = (data) => {
    const {
        __icon__,
        archived,
        baseType,
        contentTypeIcon,
        creationDate,
        folder,
        hasLiveVersion,
        hasTitleImage,
        hostName,
        languageId,
        live,
        locked,
        modDate,
        modUser,
        modUserName,
        owner,
        ownerName,
        publishDate,
        publishUser,
        publishUserName,
        shortyId,
        sortOrder,
        stInode,
        titleImage,
        url,
        variant,
        working,
        ...filteredData
    } = data;
    return filteredData;
};

/**
 * DummyContentlet is a development utility component that displays the data model
 * for DotCMS content types. It renders a formatted JSON view of the content,
 * filtering out system properties to focus on the actual content structure.
 *
 * Use this component as:
 * 1. A reference to understand the content model structure
 * 2. A starting point for developing custom components
 * 3. A debugging tool during development
 *
 * The component automatically handles any content type and displays its properties
 * in an interactive card with copy functionality.
 *
 * @param {Object} props
 * @param {Object} props.data - The raw content data from DotCMS
 * @returns {JSX.Element} A card displaying the filtered content structure
 */
export default function DummyContentlet({ data }) {
    const filteredData = filterData(data);
    const [copied, setCopied] = useState(false);

    const formattedJSON = JSON.stringify(filteredData, null, 2);

    const copyToClipboard = () => {
        navigator.clipboard.writeText(formattedJSON);
        setCopied(true);
        setTimeout(() => setCopied(false), 1500);
    };

    return (
        <>
            <div className="m-4 bg-white rounded-lg border border-gray-200">
                <div className="flex justify-between px-4 py-5">
                    <h3 className="font-mono text-lg font-medium leading-6 text-gray-900">
                        &lt;{filteredData.contentType}/&gt;
                    </h3>
                    <div className="flex relative gap-2 items-center">
                        <button
                            className={`px-3 py-1 text-sm font-medium rounded-md transition-colors border ${
                                copied
                                    ? "text-white bg-blue-500 border-blue-500"
                                    : "text-gray-700 bg-white border-gray-200 hover:text-blue-900 hover:bg-blue-100 hover:border-blue-100"
                            }`}
                            onClick={copyToClipboard}
                        >
                            {copied ? "Copied" : "Copy"}
                        </button>
                        <div className="relative group">
                            <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5 text-gray-400 cursor-pointer hover:text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 16v-4m0-4h.01M12 2C6.477 2 2 6.477 2 12s4.477 10 10 10 10-4.477 10-10S17.523 2 12 2z" />
                            </svg>
                            <div className="hidden absolute right-0 top-full z-10 p-2 mt-2 w-80 text-sm text-white bg-gray-800 rounded shadow-lg group-hover:block">
                                This component displays the raw content structure from DotCMS. To replace check <code className="text-green-500">src/components/my-page.js</code> for the <code className="text-green-500">components</code> object.
                            </div>
                        </div>
                    </div>
                </div>
                <div className="px-4 pb-5">
                    <div className="relative">
                        <pre className="bg-gray-100 p-4 rounded-lg overflow-auto h-[300px] text-sm font-mono">
                            <code>{formattedJSON}</code>
                        </pre>
                    </div>
                </div>
            </div>
        </>
    );
}
