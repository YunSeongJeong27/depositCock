import { useEffect, useState, type CSSProperties, type ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { copyText } from '../api'
import { SUPPORT_EMAIL } from '../texts/common'

/**
 * 문구 파일(src/texts)의 문장을 그린다. **굵게** 와 [글자](/경로) 링크(새 탭)만 바꾼다.
 * 링크는 우리 사이트 경로(/로 시작)만 받는다: 고객이 적은 값이 문장에 들어가도 바깥 주소 링크가 되지 않게
 */
export function Rich({ text, strongStyle }: { text: string; strongStyle?: CSSProperties }) {
  return (
    <>
      {text.split(/(\*\*[^*]+\*\*|\[[^\]]+\]\(\/[a-z][^)\s]*\))/).map((part, i) => {
        if (part.startsWith('**') && part.endsWith('**') && part.length > 4) return <b key={i} style={strongStyle}>{part.slice(2, -2)}</b>
        const link = /^\[([^\]]+)\]\((\/[a-z][^)\s]*)\)$/.exec(part)
        if (link) return <a key={i} href={link[2]} target="_blank" rel="noreferrer">{link[1]}</a>
        return part
      })}
    </>
  )
}

/** 입금콕 로고 (그림 + 글자). 그림은 탭 아이콘과 같은 public/logo.svg, 크기는 글자 크기를 따른다 */
export function Brand() {
  return (
    <span className="brand-logo">
      <img src="/logo.svg" alt="" />입금<em>콕</em>
    </span>
  )
}

/**
 * 모든 화면 맨 아래 (2026-11-09: 문의처를 넣으며 화면마다 따로 있던 아래쪽을 하나로).
 * 고객 주문 화면은 서비스 이름 + 개인정보처리방침 (이용약관은 판매자와의 약관이라 뺌), 나머지는 개인정보처리방침 + 이용약관
 */
export function SiteFooter() {
  const customerPage = useLocation().pathname.startsWith('/g/')
  return (
    <footer className="footer">
      {customerPage
        ? <><Brand /> · <Link to="/privacy">개인정보처리방침</Link></>
        : <><Link to="/privacy">개인정보처리방침</Link> · <Link to="/terms">이용약관</Link></>}
      <br />
      문의 : <a href={`mailto:${SUPPORT_EMAIL}`}>{SUPPORT_EMAIL}</a>
    </footer>
  )
}

export function ErrorBox({ message }: { message: string | null | undefined }) {
  if (!message) return null
  return <div className="error-box" style={{ whiteSpace: 'pre-line' }}>{message}</div>
}

export function CopyButton({ text, label = '복사' }: { text: string; label?: string }) {
  const [done, setDone] = useState(false)
  return (
    <button
      type="button"
      className="btn small"
      onClick={async () => {
        if (await copyText(text)) {
          setDone(true)
          setTimeout(() => setDone(false), 1500)
        }
      }}
    >
      {done ? '복사됨 ✓' : label}
    </button>
  )
}

export function Stat({ label, value, tone }: { label: string; value: ReactNode; tone?: 'ok' | 'warn' | 'danger' }) {
  // 숫자는 한 줄로 두므로 "1,620,000원"처럼 긴 값은 글자를 줄여 칸 안에 넣는다
  const length = typeof value === 'string' || typeof value === 'number' ? String(value).length : 0
  const size = length >= 10 ? ' xlong' : length >= 8 ? ' long' : ''
  return (
    <div className={`stat ${tone ?? ''}`}>
      <div className="stat-label">{label}</div>
      <div className={`stat-value${size}`}>{value}</div>
    </div>
  )
}

export function StatusBadge({ status, label }: { status: string; label: string }) {
  return <span className={`badge badge-${status}`}>{label}</span>
}

/** 폰 폭(640px 이하)인지. index.css 의 폰 카드 전환 기준과 같다 */
export function useIsMobile(): boolean {
  const query = '(max-width: 640px)'
  const [mobile, setMobile] = useState(() => typeof window !== 'undefined' && window.matchMedia(query).matches)
  useEffect(() => {
    const m = window.matchMedia(query)
    const onChange = () => setMobile(m.matches)
    m.addEventListener('change', onChange)
    return () => m.removeEventListener('change', onChange)
  }, [])
  return mobile
}
