export interface WechatShareConfig {
  title: string;
  desc: string;
  link?: string;
  imgUrl?: string;
}

export interface WechatSignature {
  appId: string;
  timestamp: number;
  nonceStr: string;
  signature: string;
}

export interface WechatSetupOptions {
  signatureEndpoint: string;
  jsApiList?: string[];
  share: WechatShareConfig;
}

declare global {
  interface Window {
    wx?: {
      config(options: Record<string, unknown>): void;
      ready(callback: () => void): void;
      error(callback: (error: unknown) => void): void;
      updateAppMessageShareData?(options: WechatShareConfig): void;
      updateTimelineShareData?(options: WechatShareConfig): void;
      onMenuShareAppMessage?(options: WechatShareConfig): void;
      onMenuShareTimeline?(options: WechatShareConfig): void;
    };
  }
}

const DEFAULT_JS_APIS = ["updateAppMessageShareData", "updateTimelineShareData"];
const SDK_URL = "https://res.wx.qq.com/open/js/jweixin-1.6.0.js";

export async function setupWechatShare(options: WechatSetupOptions) {
  await loadWechatSdk();

  if (!window.wx) {
    throw new Error("WeChat JS-SDK failed to load");
  }

  const signature = await fetchSignature(options.signatureEndpoint);

  window.wx.config({
    debug: false,
    appId: signature.appId,
    timestamp: signature.timestamp,
    nonceStr: signature.nonceStr,
    signature: signature.signature,
    jsApiList: options.jsApiList ?? DEFAULT_JS_APIS,
  });

  window.wx.ready(() => {
    const share = {
      link: window.location.href,
      ...options.share,
    };

    window.wx?.updateAppMessageShareData?.(share);
    window.wx?.updateTimelineShareData?.(share);
    window.wx?.onMenuShareAppMessage?.(share);
    window.wx?.onMenuShareTimeline?.(share);
  });
}

function loadWechatSdk() {
  if (window.wx) return Promise.resolve();

  return new Promise<void>((resolve, reject) => {
    const existingScript = document.querySelector<HTMLScriptElement>(`script[src="${SDK_URL}"]`);
    if (existingScript) {
      existingScript.addEventListener("load", () => resolve(), { once: true });
      existingScript.addEventListener("error", reject, { once: true });
      return;
    }

    const script = document.createElement("script");
    script.src = SDK_URL;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = reject;
    document.head.appendChild(script);
  });
}

async function fetchSignature(endpoint: string): Promise<WechatSignature> {
  const url = new URL(endpoint, window.location.origin);
  url.searchParams.set("url", window.location.href.split("#")[0]);

  const response = await fetch(url.toString(), {
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch WeChat signature: ${response.status}`);
  }

  return response.json() as Promise<WechatSignature>;
}
