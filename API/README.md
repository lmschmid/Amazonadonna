# React-Typescript-Express-Webpack-Starter

A boilderplate for using Express with Typescript. The code uses tsc to transpile and nodemon to reload the server on changes.

## Getting started

First run `npm install`

-   To run the server in a dev environment, run `npm start` to have `tsc` transpile and watch your server files and nodemon starts the express server on `http://localhost:3000/`

## Overview

`Server.ts` serves that html file and any other APIs/routes you may want to add. tsc is used to transpile the back-end typescipt into javascript, and nodemon is used to serve the `./dist/server.js` file that is generated from tsc.

To build the files, run `npm run build` which will transpile `server.ts` to `server.js`, which can be ran with `node ./dist/server.js`. (Or run `npm run start:prod` to do this for you, with the `PROD` env variable set to `true`)

## Debugging in VS Code

-   Run `npm start` in a terminal
-   Run the `Debug` configuartion from the debug menu, add breakpoints where needed
-   Use any chrome window to navigate to `http://localhost:3000/`

## Tsconfig files

There are 2 tsconfig files, `tsonfig.backend.json` which extend `tsconfig.json` as the base configuration. The `tslint` extension (see below) only looks for `tsconfig.json` (as of the time of writting this there is no config option to change that), so all of the linting is done based off of what is in `tsconfig.json`.

## Dev Tooling

The following is a list of optional dev tooling that I included to help my workflow:

-   TSLint: A linter for Typescript. My linting settings are checked into the repo under `tslint.json`. (Can either be run with `npm run lint` or use the VSCode extention for in editior linting)
-   Prettier: A formatter to autostyle my code. You need the `prettier` VSCode extention. Prettier uses `.prettierrc.json` to set autoformatting style. Make sure your prettier seetings match your tslint settings, for example, `tslint.json` is set up to never allow semicolons unless necessary, so my `.prettierrc.json` auto removes semicolons unless necessary.
