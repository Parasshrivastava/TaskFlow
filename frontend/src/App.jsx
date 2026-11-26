import { useEffect, useState } from 'react'

export default function App() {
  const [status, setStatus] = useState('loading...')  // React state: re-renders when it changes

  useEffect(() => {                                    // runs once, after the first render
    fetch('/api/public/ping')                          // relative URL: goes through the proxy
      .then(res => res.json())
      .then(data => setStatus(data.status))
      .catch(() => setStatus('backend unreachable'))
  }, [])                                               // [] = no dependencies, run once

  return (
    <main style={{ fontFamily: 'sans-serif', padding: 32 }}>
      <h1>TaskFlow</h1>
      <p>Backend says: <b>{status}</b></p>
    </main>
  )
}
