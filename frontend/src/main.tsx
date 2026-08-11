import React from 'react'
import ReactDOM from 'react-dom/client'
import { Provider } from 'react-redux'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { store } from './store'
import { ThemeProvider } from './components/ThemeProvider'
import App from './App'
import './index.css'
import './components/agent/agent.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: false,
    },
  },
})

// --- AWS ECS Wake-up Trigger ---
const wakeUrl = import.meta.env.VITE_AWS_WAKE_URL
if (wakeUrl) {
  // Use a fire-and-forget self-executing function to avoid blocking React
  ;(async () => {
    try {
      console.log('[AWS Wake] Requesting backend startup')
      const response = await fetch(wakeUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        // Only sending a generic wake payload. No secrets exposed.
        body: JSON.stringify({ action: 'wake' })
      })
      if (response.ok) {
        console.log('[AWS Wake] Backend startup requested')
      } else {
        console.warn(`[AWS Wake] Wake request returned status: ${response.status}`)
      }
    } catch (error) {
      console.error('[AWS Wake] Wake request failed', error)
    }
  })()
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <App />
        </ThemeProvider>
      </QueryClientProvider>
    </Provider>
  </React.StrictMode>,
)
