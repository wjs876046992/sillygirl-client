import { create } from 'zustand'

interface Service {
  id: string
  name: string
  baseUrl: string
  token: string
}

interface PluginInfo {
  name: string
  description: string
  enabled: boolean
}

interface AppActions {
  addService: (service: Service) => void
  removeService: (id: string) => void
  setCurrentService: (id: string) => void
  setPlugins: (plugins: PluginInfo[]) => void
}

interface AppState {
  services: Service[]
  currentServiceId: string | null
  currentService: Service | null
  currentUserPlugins: PluginInfo[]
  actions: AppActions
}

export const useAppStore = create<AppState>((set, get) => ({
  services: [],
  currentServiceId: null,
  currentService: null,
  currentUserPlugins: [],
  actions: {
    addService: (service: Service) =>
      set((state) => ({
        services: [...state.services, service],
        currentServiceId: state.currentServiceId || service.id,
        currentService: service,
      })),
    removeService: (id: string) =>
      set((state) => {
        const newServices = state.services.filter((s) => s.id !== id)
        return {
          services: newServices,
          currentServiceId:
            state.currentServiceId === id
              ? newServices[0]?.id || null
              : state.currentServiceId,
          currentService:
            state.currentServiceId === id
              ? newServices[0] || null
              : state.currentService,
        }
      }),
    setCurrentService: (id: string) =>
      set((state) => {
        const service = state.services.find((s) => s.id === id)
        return {
          currentServiceId: id,
          currentService: service || null,
        }
      }),
    setPlugins: (plugins: PluginInfo[]) => set({ currentUserPlugins: plugins }),
  },
}))
