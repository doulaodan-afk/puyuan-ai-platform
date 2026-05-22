const config = {
  projectName: 'merchant-miniapp',
  date: '2026-5-20',
  designWidth: 750,
  deviceRatio: {
    640: 2.34 / 2,
    750: 1,
    828: 1.81 / 2,
    375: 2 / 1
  },
  sourceRoot: 'src',
  outputRoot: 'dist',
  plugins: [],
  defineConstants: {},
  copy: {
    patterns: [
      {
        from: 'src/images',
        to: 'dist/images',
        ignore: ['*.md']
      }
    ],
    options: {}
  },
  framework: 'vue3',
  compiler: {
    type: 'webpack5',
    prebundle: {
      enable: false
    }
  },
  cache: {
    enable: false
  },
  mini: {
    postcss: {
      pxtransform: {
        enable: true,
        config: {}
      },
      url: {
        enable: true,
        config: {
          limit: 1024
        }
      },
      cssModules: {
        enable: false,
        config: {
          namingPattern: 'module',
          generateScopedName: '[name]__[local]___[hash:base64:5]'
        }
      }
    },
    webpackChain(chain) {
      chain.resolve.alias
        .set('@', path.resolve(__dirname, '..', 'src'))
    }
  },
  h5: {
    publicPath: '/',
    staticDirectory: 'static',
    esnextModules: ['nutui-uniapp'],
    postcss: {
      autoprefixer: {
        enable: true,
        config: {}
      },
      cssModules: {
        enable: false,
        config: {
          namingPattern: 'module',
          generateScopedName: '[name]__[local]___[hash:base64:5]'
        }
      }
    },
    devServer: {
      port: 10086
    },
    webpackChain(chain) {
      chain.resolve.alias
        .set('@', path.resolve(__dirname, '..', 'src'))
    }
  }
};

module.exports = config;

const path = require('path');
