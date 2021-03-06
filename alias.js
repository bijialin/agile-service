const path = require('path');

function pathForWebpack(alias) {
  Object.keys(alias).forEach((key) => {
    // eslint-disable-next-line no-param-reassign
    alias[key] = path.resolve(__dirname, alias[key]);
  });
  return alias;
}
const alias = {
  '@': ('./react'),
};
module.exports = {
  gulp: alias,
  webpack: pathForWebpack({ ...alias }),
};
