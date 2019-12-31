const path = require('path');
const slsw = require('serverless-webpack');
const outputDirectory = path.join(__dirname, '.webpack');

module.exports = {
  entry: slsw.lib.entries,
  resolve: {
    extensions: [
      '.js',
      '.json',
      '.ts',
      '.tsx'
    ]
  },
  output: {
    libraryTarget: 'commonjs2',
    path: outputDirectory,
    filename: '[name].js'
  },
  target: 'node',
  module: {
    rules: [
      {
        test: /\.ts(x?)$/,
        use: [
          {
            loader: 'ts-loader'
          }
        ]
      }
    ]
  },
  plugins: [],
  node: {
    __dirname: false
  }
};
