module.exports = {
  extends: ['taro/react'],
  parser: '@typescript-eslint/parser',
  parserOptions: {
    ecmaFeatures: {
      jsx: true
    },
    useJSXTextNode: true,
    ecmaVersion: 2018,
    sourceType: 'module',
    project: './tsconfig.json'
  },
  plugins: ['@typescript-eslint'],
  rules: {
    'react/jsx-uses-react': 'off',
    'react/react-in-jsx-scope': 'off',
    '@typescript-eslint/ban-ts-ignore': 'off',
    '@typescript-eslint/ban-ts-comment': 'off',
    '@typescript-eslint/no-empty-function': 'off',
    '@typescript-eslint/no-explicit-any': 'off',
    '@typescript-eslint/ban-types': 'off',
    '@typescript-eslint/explicit-module-boundary-types': 'off',
    '@typescript-eslint/no-unused-vars': ['warn', {
      varsIgnorePattern: '^_',
      argsIgnorePattern: '^_'
    }],
    'no-unused-vars': ['warn', {
      varsIgnorePattern: '^_',
      argsIgnorePattern: '^_'
    }]
  }
};
