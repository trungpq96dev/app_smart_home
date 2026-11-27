import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { Eye, EyeOff, Lock, ShieldCheck } from 'lucide-react';
import { SH, tint } from '../theme/colors.js';

export type WebAccessMode = 'disabled' | 'lan' | 'cookie' | 'unauthenticated';

interface WebAccessContextValue {
  mode: WebAccessMode;
  logout: () => Promise<void>;
}

const WebAccessContext = createContext<WebAccessContextValue>({
  mode: 'disabled',
  logout: async () => {},
});

export function useWebAccess(): WebAccessContextValue {
  return useContext(WebAccessContext);
}

export function WebAccessGate({ children }: { children: React.ReactNode }) {
  const [checking, setChecking] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);
  const [mode, setMode] = useState<WebAccessMode>('unauthenticated');

  const [password, setPassword] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const checkSession = useCallback(async () => {
    try {
      const resp = await fetch('/api/auth/session', { credentials: 'same-origin' });
      if (!resp.ok) {
        setAuthenticated(false);
        setMode('unauthenticated');
        return;
      }
      const data = (await resp.json()) as {
        required?: boolean;
        authenticated?: boolean;
        mode?: WebAccessMode;
      };
      setAuthenticated(Boolean(data.authenticated));
      setMode(data.mode || (data.authenticated ? 'disabled' : 'unauthenticated'));
    } catch {
      // Neu mang tam thoi rot khi dang mo app, giu nguyen man hinh dang nhap de thu lai
      setAuthenticated(false);
    } finally {
      setChecking(false);
    }
  }, []);

  useEffect(() => {
    void checkSession();
  }, [checkSession]);

  const logout = useCallback(async () => {
    try {
      await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
    } catch {
      // ignore
    }
    setPassword('');
    setError(null);
    setChecking(true);
    await checkSession();
  }, [checkSession]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!password.trim() || submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      const resp = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify({ password: password.trim() }),
      });
      const data = (await resp.json().catch(() => ({}))) as { ok?: boolean; error?: string };
      if (!resp.ok || !data.ok) {
        setError(data.error || 'Mật khẩu không đúng.');
        return;
      }
      setPassword('');
      setAuthenticated(true);
      setMode('cookie');
    } catch {
      setError('Không kết nối được tới máy chủ SmartHome. Kiểm tra sóng 4G/WiFi.');
    } finally {
      setSubmitting(false);
    }
  };

  if (checking) {
    return (
      <div className="flex h-dvh min-h-screen w-full flex-col items-center justify-center bg-[#0B1220] text-[#E6EDF7] px-4">
        <img
          src="/logo_sh.png"
          alt="SmartHome"
          className="h-14 w-14 rounded-2xl border border-[#253459] shadow-lg mb-3"
        />
        <div className="text-xs font-medium text-[#8A9BB8] animate-pulse">
          Đang kiểm tra phiên bảo mật…
        </div>
      </div>
    );
  }

  if (!authenticated) {
    return (
      <div className="flex h-dvh min-h-screen w-full flex-col items-center justify-center bg-[#0B1220] text-[#E6EDF7] px-4 pt-[env(safe-area-inset-top)] pb-[env(safe-area-inset-bottom)]">
        <div
          className="w-full max-w-sm rounded-3xl p-6 shadow-2xl space-y-5"
          style={{
            backgroundColor: SH.Card,
            border: `1px solid ${SH.Border}`,
            boxShadow: `0 20px 50px -15px rgba(0,0,0,0.7), 0 0 32px -12px ${tint(SH.Blue, 0.3)}`,
          }}
        >
          <div className="flex flex-col items-center text-center space-y-2">
            <img
              src="/logo_sh.png"
              alt="SmartHome"
              className="h-16 w-16 rounded-2xl border border-[#253459] object-cover shadow-md"
            />
            <h1 className="text-lg font-bold tracking-tight text-[#E6EDF7]">SmartHome</h1>
            <div
              className="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-[11px] font-semibold"
              style={{
                backgroundColor: tint(SH.Green, 0.14),
                border: `1px solid ${tint(SH.Green, 0.35)}`,
                color: SH.Green,
              }}
            >
              <ShieldCheck size={13} />
              <span>Truy cập từ xa bảo mật (4G / HTTPS)</span>
            </div>
            <p className="text-xs text-[#8A9BB8] leading-relaxed pt-0.5">
              Nhập mật khẩu nhà thông minh để xem Camera 24/24 và điều khiển thiết bị.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-3.5">
            {/* Trường username ẩn giúp iOS Safari / FaceID / iCloud Keychain tự nhớ mật khẩu */}
            <input
              type="text"
              name="username"
              autoComplete="username"
              defaultValue="smarthome"
              tabIndex={-1}
              aria-hidden="true"
              className="sr-only"
            />

            <div>
              <label className="block text-xs font-medium text-[#8A9BB8] mb-1.5">
                Mật khẩu truy cập từ xa
              </label>
              <div className="relative flex items-center">
                <span className="pointer-events-none absolute left-3.5 text-[#64748B]">
                  <Lock size={15} />
                </span>
                <input
                  type={showPass ? 'text' : 'password'}
                  name="password"
                  autoComplete="current-password"
                  autoFocus
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    if (error) setError(null);
                  }}
                  placeholder="Nhập mật khẩu…"
                  className="w-full rounded-xl border border-[#253459] bg-[#0E1627] pl-10 pr-11 py-2.5 text-sm text-[#E6EDF7] outline-none focus:border-[#4F8BFF] font-mono-num"
                />
                <button
                  type="button"
                  onClick={() => setShowPass((s) => !s)}
                  className="absolute right-2.5 rounded-lg p-1.5 text-[#8A9BB8] hover:text-white cursor-pointer"
                  title={showPass ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showPass ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            {error && (
              <div className="rounded-xl border border-[#EF4444]/45 bg-[#EF4444]/12 px-3 py-2 text-xs text-[#FCA5A5]">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={submitting || !password.trim()}
              className="w-full rounded-xl py-2.5 text-sm font-bold text-white transition-all enabled:hover:brightness-110 enabled:active:scale-[0.99] disabled:opacity-45 disabled:cursor-not-allowed cursor-pointer"
              style={{
                background: `linear-gradient(90deg, #2563EB 0%, ${SH.Blue} 100%)`,
                boxShadow: `0 8px 20px -6px ${tint(SH.Blue, 0.6)}`,
              }}
            >
              {submitting ? 'Đang xác thực…' : 'Đăng nhập vào nhà'}
            </button>
          </form>

          <div className="text-center text-[11px] text-[#5F6F8C]">
            Tự động giữ phiên đăng nhập 30 ngày trên iPhone / trình duyệt này.
          </div>
        </div>
      </div>
    );
  }

  return (
    <WebAccessContext.Provider value={{ mode, logout }}>
      {children}
    </WebAccessContext.Provider>
  );
}
