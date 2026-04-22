import * as React from "react";
import { createRoot } from "react-dom/client";
import { App } from "./App";
import "./theme.css";

const el = document.getElementById("NativeTarget");
if (!el) throw new Error("NativeTarget not found");
createRoot(el).render(<App />);
