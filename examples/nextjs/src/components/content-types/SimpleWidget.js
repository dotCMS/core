export function SimpleWidget({ title }) {
    return (
        <div className="relative z-10 p-6 -mt-24 w-full bg-white rounded-lg shadow-lg lg:mx-auto lg:max-w-5xl">
            <h2 className="mb-6 text-3xl font-bold text-gray-800">{title}</h2>
            <form className="flex flex-wrap gap-4 items-end">
                <div className="flex-grow min-w-[200px]">
                    <label htmlFor="destination" className="block mb-1 text-sm font-medium text-gray-700">Destination</label>
                    <div className="relative">
                        <select id="destination" className="py-2 pr-10 pl-3 w-full text-base bg-white rounded-md border border-gray-300 appearance-none focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm">
                            <option>Colorado & The Rockies</option>
                            <option>Yellowstone National Park</option>
                            <option>California Coast</option>
                            <option>New York City</option>
                            <option>Florida Keys</option>
                            <option>Grand Canyon</option>
                            <option>Hawaii Islands</option>
                            <option>Alaska Wilderness</option>
                            <option>New Orleans</option>
                            <option>Washington D.C.</option>
                        </select>
                        <div className="flex absolute inset-y-0 right-0 items-center px-2 text-gray-700 pointer-events-none">
                            <svg className="w-5 h-5 fill-current" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20">
                                <path d="M9.293 12.95l.707.707L15.657 8l-1.414-1.414L10 10.828 5.757 6.586 4.343 8z" />
                            </svg>
                        </div>
                    </div>
                </div>
                <div className="flex-shrink-0 w-40">
                    <label htmlFor="start-date" className="block mb-1 text-sm font-medium text-gray-700">Start date</label>
                    <input type="date" id="start-date" className="py-2 pr-3 pl-3 w-full text-base rounded-md border border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" placeholder="mm/dd/yyyy" />
                </div>
                <div className="flex-shrink-0 w-40">
                    <label htmlFor="end-date" className="block mb-1 text-sm font-medium text-gray-700">End date</label>
                    <input type="date" id="end-date" className="py-2 pr-3 pl-3 w-full text-base rounded-md border border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" placeholder="mm/dd/yyyy" />
                </div>
                <div className="flex-shrink-0">
                    <button type="submit" className="flex justify-center px-8 py-2 text-sm font-medium text-white bg-orange-600 rounded-md border border-transparent shadow-sm hover:bg-orange-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-orange-500">
                        BOOK NOW
                    </button>
                </div>
            </form>
        </div>
    );
}
