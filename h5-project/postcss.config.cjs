module.exports = {
  plugins: {
    "@tailwindcss/postcss": {},
    autoprefixer: {},
    "postcss-px-to-viewport-8-plugin": {
      unitToConvert: "px",
      viewportWidth: 750,
      unitPrecision: 5,
      propList: ["*"],
      viewportUnit: "vw",
      fontViewportUnit: "vw",
      selectorBlackList: [".ignore-vw"],
      minPixelValue: 1,
      mediaQuery: true,
      replace: true,
      exclude: [/node_modules\/vant/],
    },
  },
};
