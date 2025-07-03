export function NotFound() {
  return (
    <div className="bg-slate-100 min-h-dvh w-full flex justify-center items-center">
      <section>
        <div className="py-8 px-4 mx-auto max-w-5xl lg:py-16 lg:px-6">
          <div className="mx-auto max-w-2xl text-center">
            <h1 className="mb-4 text-7xl tracking-tight font-extrabold lg:text-9xl text-primary-600">
              404
            </h1>
            <p className="mb-4 text-3xl tracking-tight font-bold text-gray-900 md:text-4xl">
              Something&apos;s missing.
            </p>
            <p className="mb-4 text-lg font-light text-gray-500">
              Sorry, we can&apos;t find that page. You&apos;ll find lots to
              explore on the home page.
            </p>
            <a href={"/"}>
              <div className="inline-flex text-white bg-blue-600 hover:bg-blue-800 focus:ring-4 focus:outline-none focus:ring-blue-300 font-medium rounded-lg text-sm px-5 py-2.5 text-center my-4">
                Return Home
              </div>
            </a>
          </div>
        </div>
      </section>
    </div>
  );
}
