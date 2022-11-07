const isCi = process.env.CI !== undefined;
if (!isCi) {
    require('husky').install('core-web/.husky');
}
