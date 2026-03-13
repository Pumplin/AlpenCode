/**
 * AI 分析 SSE 接口
 * EventSource 不支持自定义 header，token 通过 query param 传递
 * 开发环境走 /api 代理（vite proxy），生产环境直连后端
 */

function getApiBase(): string {
  // 开发环境：走 vite proxy /api 前缀
  if (import.meta.env.DEV) {
    return '/api';
  }
  return import.meta.env.VITE_API_BASE_URL || '';
}

/**
 * 创建 AI 分析 SSE 连接
 * @param submitId 提交记录 ID
 * @param onMessage 每个 token 回调
 * @param onDone 完成回调
 * @param onError 错误回调
 * @returns 关闭函数
 */
export function createAiAnalysisStream(
  submitId: number,
  onMessage: (token: string) => void,
  onDone: () => void,
  onError: (err: Event) => void,
): () => void {
  const token = localStorage.getItem('ac_token') || '';
  // Sa-Token 存储时带 Bearer 前缀，传参时去掉前缀
  const rawToken = token.startsWith('Bearer ') ? token.slice(7) : token;
  const url = `${getApiBase()}/ac/ai/analyze/${submitId}?token=${encodeURIComponent(rawToken)}`;

  const es = new EventSource(url);

  es.onmessage = (e) => {
    if (e.data === '[DONE]') {
      es.close();
      onDone();
      return;
    }
    onMessage(e.data);
  };

  es.onerror = (e) => {
    es.close();
    onError(e);
  };

  // 返回关闭函数，供组件 unmount 时调用
  return () => es.close();
}
