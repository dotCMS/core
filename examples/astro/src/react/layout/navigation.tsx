function Navigation({ items, className }: {items: any[], className?: string}) {


  return (
    <nav className={className}>
      <ul className="flex space-x-4 text-white">
        <li>
          <a
            href="/"> {/* I NEED TO PRESERVE THE QUERY PARAMS*/}
            Home
          </a>
        </li>
        {items?.map((item) => (
          <li key={item.folder}>
            <a
              href={item.href} // I NEED TO PRESERVE THE QUERY PARAMS
              target={item.target}>
              {item.title}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
}

export default Navigation;
