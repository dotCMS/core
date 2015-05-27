export default (urls, elem) => {
  elem.innerHTML += urls.map(url => `<img src="${url}">`).join("\n")
}