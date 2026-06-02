export default defineAppConfig({
  pages: [
    'pages/index/index',
    'pages/login/index',
    'pages/plugins/index',
    'pages/ai-image/index',
    'pages/ai-script/index',
    'pages/ai-translate/index',
    'pages/account/index',
    'pages/recharge/index',
    'pages/recharge/orders/index',
    'pages/ledger/index',
    'pages/billing/index',
    'pages/agreement/user/index',
    'pages/agreement/privacy/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#ffffff',
    navigationBarTitleText: '濮院毛衫 AI 平台',
    navigationBarTextStyle: 'black',
    backgroundColor: '#f5f5f5'
  },
  tabBar: {
    color: '#999999',
    selectedColor: '#667eea',
    backgroundColor: '#ffffff',
    borderStyle: 'black',
    list: [
      {
        pagePath: 'pages/index/index',
        text: '工作台'
      },
      {
        pagePath: 'pages/plugins/index',
        text: '插件'
      },
      {
        pagePath: 'pages/account/index',
        text: '账户'
      }
    ]
  },
  darkmode: true,
  themeLocation: 'theme.json'
});
