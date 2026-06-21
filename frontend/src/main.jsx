/*
 * =========================================================
 * MODULE 5: REACT ENTRY POINT  (main.jsx)
 * =========================================================
 *
 * This file is the bridge between plain HTML and React.
 *
 * React.StrictMode:
 *   Wraps your app in development-mode checks:
 *   - Warns about deprecated APIs
 *   - Detects unexpected side effects
 *   - Runs effects twice (in dev) to catch bugs
 *   Has NO effect in production builds.
 */

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'

// Find the <div id="root"> in index.html and take control of it
const rootElement = document.getElementById('root')

// createRoot is React 18's way to create the root renderer
// (replaces the old ReactDOM.render() from React 17)
const root = createRoot(rootElement)

// Render our <App /> component into the root element
root.render(
  <StrictMode>
    <App />
  </StrictMode>
)
