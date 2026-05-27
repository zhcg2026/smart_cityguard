/**
 * 高德地图 JSAPI 2.0 按需加载（Key 与安全密钥见 .env.example）
 */
let loadPromise = null

export function loadAmapScript() {
  if (loadPromise) {
    return loadPromise
  }
  const key = import.meta.env.VITE_AMAP_KEY
  if (!key) {
    return Promise.reject(new Error('未配置 VITE_AMAP_KEY'))
  }
  const securityJsCode = import.meta.env.VITE_AMAP_SECURITY_JS_CODE
  if (typeof window !== 'undefined' && securityJsCode) {
    window._AMapSecurityConfig = { securityJsCode }
  }
  loadPromise = import('@amap/amap-jsapi-loader').then(({ default: AMapLoader }) =>
    AMapLoader.load({
      key,
      version: '2.0',
      plugins: ['AMap.Geocoder', 'AMap.Geolocation', 'AMap.Scale']
    })
  )
  return loadPromise
}

/** 运城市区默认中心（GCJ-02） */
export const DEFAULT_MAP_CENTER = [111.003957, 35.022778]
