# JavaScript SDK for DotCMS API's

This library allows you to interact with DotCMS API's easily from the browser, nodejs and React Native. [Full Documentation](https://dotcms.github.io/core-web/dotcms/)

## Install

`npm install dotcms --save`

or

`yarn install dotcms`

## Usage

```javascript
import { initDotCMS } from 'dotcms';

const dotcms = initDotCMS({
    hostId: 'DOTCMS_SITE_IDENTIFIER',
    host: 'YOUR_DOTCMS_INSTANCE',
    token: 'YOUR AUTH TOKEN'
});

// Example
dotcms.page
    .get({
        url: '/about-us'
    })
    .then((data) => {
        console.log(data);
    })
    .catch((err) => {
        console.error(err.status, err.message);
    });
```

## Examples

### Next.js

Next.js gives you the best developer experience with all the features you need for production. [Read more](https://nextjs.org/)

#### Fetching data in the client

```javascript
import { useEffect, useState } from 'react';
import { initDotCMS } from 'dotcms';

const dotcms = initDotCMS({
    host: 'YOUR_DOTCMS_INSTANCE',
    token: 'YOUR AUTH TOKEN'
});

export default function Home() {
    const [state, setState] = useState(null);

    useEffect(async () => {
        const page = await dotcms.page.get({
            url: '/index'
        });
        setState(page);
    }, []);

    return state && <h1>{state.page.title}</h1>;
}
```

#### Fetching data in the server

```javascript
import { useEffect, useState } from 'react';
import { initDotCMS } from 'dotcms';

export default function Home(props) {
    return <h1>{props.page.title}</h1>;
}

export async function getServerSideProps(context) {
    const page = await dotcms.page.get({
        url: context.req.url // you can map the urls with dotcms
    });

    return {
        props: page // will be passed to the page component as props
    };
}
```

More about [data fetching](https://nextjs.org/docs/basic-features/data-fetching) in Nextjs.

## Running unit tests

Run `nx test dotcms` to execute the unit tests.

This library was generated with [Nx](https://nx.dev).
