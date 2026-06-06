/** 管理端高德地图（index.html 同步 script）初始化辅助 */

export async function waitForElementSize(el, maxFrames = 24) {
  if (!el) return false
  for (let i = 0; i < maxFrames; i++) {
    if (el.offsetHeight >= 10 && el.offsetWidth >= 10) return true
    await new Promise((resolve) => requestAnimationFrame(resolve))
  }
  return el.offsetHeight >= 10 && el.offsetWidth >= 10
}

export async function waitForGlobalAmap(maxAttempts = 30, intervalMs = 100) {
  for (let i = 0; i < maxAttempts; i++) {
    if (typeof window !== 'undefined' && typeof window.AMap !== 'undefined') {
      return true
    }
    await new Promise((resolve) => setTimeout(resolve, intervalMs))
  }
  return typeof window !== 'undefined' && typeof window.AMap !== 'undefined'
}
