"use client";

import dynamic from "next/dynamic";
import Splash from "./Splash";

const App = dynamic(() => import("./App"), { ssr: false, loading: () => <Splash /> });

export default function ClientRoot() {
  return <App />;
}
