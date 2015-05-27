export default (posts) => {
  return posts
    //.filter(post => post.data.over_18)
    .map(post => post.data.url)
    .filter(url => /gifv$/.exec(url))
    .map(url => url.replace(/v$/, ''))

}