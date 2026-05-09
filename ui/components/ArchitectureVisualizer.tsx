'use client';
import React, { useEffect, useState, CSSProperties } from 'react';

// ── Bread Stack Logo SVG ──
function CrustLogo({ size = 36 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="crust-top" x1="10" y1="8" x2="54" y2="38" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#f5c56b" />
          <stop offset="50%" stopColor="#e09832" />
          <stop offset="100%" stopColor="#c67a1c" />
        </linearGradient>
        <linearGradient id="crust-bot" x1="8" y1="28" x2="52" y2="56" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#d4872a" />
          <stop offset="50%" stopColor="#b5651d" />
          <stop offset="100%" stopColor="#8b4513" />
        </linearGradient>
        <linearGradient id="crust-inner" x1="18" y1="14" x2="46" y2="32" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#fce8b2" />
          <stop offset="100%" stopColor="#f0d08c" />
        </linearGradient>
      </defs>
      {/* Bottom slice */}
      <path d="M8 38 C8 30, 14 26, 20 26 L46 26 C52 26, 58 30, 58 36 L58 44 C58 50, 54 54, 48 54 L16 54 C10 54, 8 48, 8 44 Z" fill="url(#crust-bot)" />
      <path d="M14 34 C14 31, 18 29, 22 29 L44 29 C48 29, 52 31, 52 34 L52 40 C52 43, 49 46, 45 46 L19 46 C15 46, 14 43, 14 40 Z" fill="#daa06d" opacity="0.5" />
      {/* Top slice — offset up-left */}
      <path d="M6 24 C6 16, 12 10, 18 10 L44 10 C50 10, 56 14, 56 22 L56 30 C56 36, 52 40, 46 40 L14 40 C8 40, 6 34, 6 30 Z" fill="url(#crust-top)" />
      {/* Inner bread face */}
      <path d="M12 22 C12 17, 16 13, 22 13 L42 13 C48 13, 52 17, 52 22 L52 28 C52 33, 48 36, 42 36 L18 36 C13 36, 12 32, 12 28 Z" fill="url(#crust-inner)" />
      {/* Texture lines on inner bread */}
      <path d="M18 19 Q26 17, 38 20" stroke="#e8c87a" strokeWidth="1" fill="none" opacity="0.6" />
      <path d="M20 23 Q30 21, 42 24" stroke="#e8c87a" strokeWidth="1" fill="none" opacity="0.5" />
      <path d="M16 27 Q28 25, 44 28" stroke="#e8c87a" strokeWidth="1" fill="none" opacity="0.4" />
      <path d="M22 31 Q32 29, 40 32" stroke="#e8c87a" strokeWidth="0.8" fill="none" opacity="0.3" />
    </svg>
  );
}

const MERMAID = `flowchart TD
    subgraph C["Clients"]
        U["Next.js UI :3000"]
        A["Android App"]
        M["MCP Server"]
    end
    subgraph S["Spring Boot :8080"]
        G["/graphql DGS"]
        Q["Queries"]
        MU["Mutations"]
    end
    subgraph SV["Services"]
        CM["MenuCommandService"]
        R["OutboxRelay @Scheduled"]
        CO["MenuAuditConsumer"]
    end
    subgraph E["External"]
        K["Apache Kafka"]
        GE["Gemini 2.0 Flash"]
    end
    subgraph D["PostgreSQL"]
        MV[("menu_version")]
        T[("category/item")]
        O[("outbox_event")]
        AR[("audit_result")]
    end
    U -->|POST /graphql| G
    A -->|POST /graphql| G
    M -->|Direct SQL| MV
    G --> Q
    G --> MU
    MU --> CM
    Q -->|EntityGraph| MV
    Q --> AR
    CM -->|UPDATE| MV
    CM -->|INSERT| O
    R -->|poll| O
    R -->|send| K
    R -->|UPDATE| O
    K --> CO
    CO -->|fetch| MV
    CO -->|generate| GE
    GE -->|risk JSON| CO
    CO -->|INSERT| AR
    MV -.- T`;

// ── CSS Helpers ──
const font = "'DM Sans',system-ui,sans-serif";
const mono = "'JetBrains Mono','Fira Code',monospace";
const grad1 = 'linear-gradient(135deg,#0f172a 0%,#1e293b 50%,#334155 100%)';
const grad2 = 'linear-gradient(135deg,#3b82f6,#8b5cf6)';
const grad3 = 'linear-gradient(135deg,#f97316,#ef4444)';
const shadow = '0 1px 3px rgba(0,0,0,0.06),0 1px 2px rgba(0,0,0,0.04)';
const shadowLg = '0 10px 25px rgba(0,0,0,0.08),0 4px 10px rgba(0,0,0,0.04)';

const FEATURES = [
  { icon:'⚡', title:'Event-Driven Pipeline', desc:'Menu mutations trigger an async Kafka pipeline. The Outbox pattern guarantees at-least-once delivery while keeping the main API latency under 5ms. Spring Kafka handles partitioning and consumer group coordination.', tag:'SPRING KAFKA' },
  { icon:'🧠', title:'AI-Powered Auditing', desc:'Every published menu version is analyzed by Gemini 2.0 Flash via LangChain4j. The AI identifies revenue leakage, tax compliance gaps, and pricing anomalies — returning structured risk assessments in real-time.', tag:'LANGCHAIN4J' },
  { icon:'🛡️', title:'Graceful Degradation', desc:'If the Gemini API fails, the system degrades gracefully. A fallback ChatLanguageModel bean logs the skip and keeps the menu pipeline running. No external failure can crash your core business logic.', tag:'RESILIENCE' },
  { icon:'📊', title:'Netflix DGS GraphQL', desc:'A unified GraphQL API surface powered by Netflix DGS. Two queries (getActiveMenu, getMenuRisks) and two mutations (publishMenu, revertToClean) serve all three client platforms from a single endpoint.', tag:'GRAPHQL' },
  { icon:'📱', title:'Multi-Platform Clients', desc:'Next.js admin dashboard for operators, Android app with Jetpack Compose + Apollo Kotlin for guests, and an MCP Server for Claude AI integration — all consuming the same GraphQL schema.', tag:'MULTI-CLIENT' },
  { icon:'🗄️', title:'Transactional Outbox', desc:'Menu publishes and outbox events are written in the same @Transactional boundary. A scheduled relay polls pending events and forwards them to Kafka, ensuring zero message loss even during crashes.', tag:'OUTBOX PATTERN' },
];

const BOTTLENECKS = [
  { id:'B1', sev:'CRITICAL', color:'#ef4444', title:'Blocking .get() in Scheduler', desc:'kafkaTemplate.send().get() freezes the scheduler thread per event. 50 pending = 50× Kafka RTT of blocking.', file:'OutboxRelay.kt:18', fix:'Use fire-and-forget with async CompletableFuture callbacks.' },
  { id:'B2', sev:'HIGH', color:'#f97316', title:'Fixed 5s Polling Without Backoff', desc:'@Scheduled(fixedDelay=5000) runs unconditionally. Wastes connections at night, can\'t drain bursts fast enough.', file:'OutboxRelay.kt:13', fix:'Switch to @TransactionalEventListener for immediate dispatch.' },
  { id:'B3', sev:'CRITICAL', color:'#ef4444', title:'Gemini Blocks Kafka Consumer', desc:'chatLanguageModel.generate() has no timeout. A 10s Gemini response freezes the consumer partition entirely.', file:'MenuAuditConsumer.kt:46', fix:'Wrap in CompletableFuture with deadline, ack Kafka message early.' },
  { id:'B4', sev:'CRITICAL', color:'#ef4444', title:'Revert Leaves No Active Menu', desc:'revertMenuVersion only marks current=REVERTED. No version gets promoted. getActiveMenu returns null.', file:'MenuCommandService.kt:52', fix:'Atomically promote the previous PUBLISHED or DRAFT version.' },
  { id:'B5', sev:'MEDIUM', color:'#eab308', title:'4-Level Cartesian Product', desc:'EntityGraph with 4 OneToMany joins generates 750+ row ResultSets before Hibernate deduplication.', file:'Repositories.kt', fix:'Use @BatchSize on child collections or paginate at category level.' },
  { id:'B6', sev:'HIGH', color:'#f97316', title:'Silent DLT Drop After Retries', desc:'After 4 @RetryableTopic retries, messages hit the DLT with no handler. Audits silently vanish.', file:'MenuAuditConsumer.kt:71', fix:'Add @DltHandler to write sentinel error records and alert operators.' },
];

const TIMELINE = [
  { t:'t = 0ms', label:'publishMenu mutation fires', sub:'GraphQL → MenuCommandService', color:'#3b82f6' },
  { t:'t ~ 1ms', label:'Outbox event persisted', sub:'Same @Transactional boundary as menu update', color:'#6366f1' },
  { t:'t ≤ 5s', label:'OutboxRelay polls database', sub:'@Scheduled(fixedDelay = 5000)', color:'#f59e0b' },
  { t:'t + Δ', label:'Kafka message delivered', sub:'topic: menu.version.published', color:'#8b5cf6' },
  { t:'t + Δ', label:'Gemini AI generates audit', sub:'LangChain4j blocking generate() call', color:'#ec4899' },
  { t:'final', label:'Risk results saved to DB', sub:'menu_audit_result table', color:'#10b981' },
];

const STACK = [
  { name:'Spring Boot 3.4', cat:'API Framework', icon:'⚡' },
  { name:'Kotlin 2.1', cat:'Language', icon:'🟣' },
  { name:'PostgreSQL 15', cat:'Database', icon:'🗄️' },
  { name:'Apache Kafka', cat:'Event Bus', icon:'📡' },
  { name:'Gemini 2.0 Flash', cat:'AI Engine', icon:'🧠' },
  { name:'Netflix DGS', cat:'GraphQL', icon:'🔗' },
  { name:'Next.js 15', cat:'Admin UI', icon:'🖥️' },
  { name:'Jetpack Compose', cat:'Mobile', icon:'📱' },
];

const LAYERS = [
  { name:'Client Layer', tech:'Next.js · Android · MCP Server', bg:'#eff6ff', border:'#93c5fd', icon:'📱' },
  { name:'API Gateway', tech:'Netflix DGS GraphQL · /graphql', bg:'#f5f3ff', border:'#c4b5fd', icon:'🔗' },
  { name:'Service Layer', tech:'MenuCommandService · OutboxRelay', bg:'#eef2ff', border:'#a5b4fc', icon:'⚙️' },
  { name:'Event Bus', tech:'Apache Kafka · menu.version.published', bg:'#fffbeb', border:'#fcd34d', icon:'📡' },
  { name:'AI Pipeline', tech:'LangChain4j → Gemini 2.0 Flash', bg:'#fdf2f8', border:'#f9a8d4', icon:'🧠' },
  { name:'Data Layer', tech:'PostgreSQL · Flyway · JPA EntityGraph', bg:'#ecfdf5', border:'#6ee7b7', icon:'🗄️' },
];

// ── Component ──
export default function ArchitectureVisualizer() {
  const [svg, setSvg] = useState<string|null>(null);
  const [err, setErr] = useState(false);
  const [scrollY, setScrollY] = useState(0);

  useEffect(() => {
    const onScroll = () => setScrollY(window.scrollY);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    let alive = true;
    const go = async () => {
      const m = (window as any).mermaid;
      if (!m || !alive) return;
      try {
        m.initialize({ startOnLoad:false, theme:'default', flowchart:{ curve:'basis', padding:16, nodeSpacing:50, rankSpacing:55 }, themeVariables:{ primaryColor:'#dbeafe', primaryTextColor:'#1e293b', primaryBorderColor:'#93c5fd', lineColor:'#94a3b8', secondaryColor:'#f1f5f9', fontSize:'12px' }});
        const id = 'mmd'+Math.random().toString(36).slice(2,8);
        const { svg:s } = await m.render(id, MERMAID);
        if (alive) setSvg(s.replace('<svg ','<svg style="width:100%;height:auto;max-height:600px;" '));
      } catch(e) { console.error(e); if(alive) setErr(true); }
    };
    if ((window as any).mermaid) go();
    else { const s=document.createElement('script'); s.src='https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js'; s.async=true; s.onload=()=>go(); s.onerror=()=>alive&&setErr(true); document.head.appendChild(s); }
    return ()=>{ alive=false; };
  }, []);

  const sec: CSSProperties = { maxWidth:1200, margin:'0 auto', padding:'0 32px' };

  return (
    <div style={{ fontFamily:font, background:'#fff', color:'#1e293b', overflowX:'hidden' }}>
      {/* NAV */}
      <nav style={{ position:'sticky', top:0, zIndex:99, background: scrollY>50?'rgba(255,255,255,0.95)':'transparent', backdropFilter:scrollY>50?'blur(12px)':'none', borderBottom: scrollY>50?'1px solid #e2e8f0':'none', transition:'all 0.3s ease', padding:'14px 0' }}>
        <div style={{ ...sec, display:'flex', alignItems:'center', justifyContent:'space-between' }}>
          <div style={{ display:'flex', alignItems:'center', gap:12 }}>
            <CrustLogo size={36} />
            <span style={{ fontSize:18, fontWeight:800, color:'#0f172a', letterSpacing:'-0.03em' }}>Crust</span>
          </div>
          <div style={{ display:'flex', gap:28, fontSize:13, fontWeight:500, color:'#64748b' }}>
            {['Overview','Architecture','Pipeline','Audit'].map(l=><a key={l} href={'#'+l.toLowerCase()} style={{ textDecoration:'none', color:'inherit', cursor:'pointer' }}>{l}</a>)}
          </div>
          <div style={{ display:'flex', gap:8 }}>
            <span style={{ fontSize:10, fontWeight:700, color:'#10b981', background:'#ecfdf5', border:'1px solid #a7f3d0', borderRadius:20, padding:'5px 14px' }}>● LIVE</span>
            <span style={{ fontSize:10, fontWeight:700, color:'#ef4444', background:'#fef2f2', border:'1px solid #fecaca', borderRadius:20, padding:'5px 14px' }}>6 ISSUES</span>
          </div>
        </div>
      </nav>

      {/* HERO */}
      <section id="overview" style={{ background:grad1, padding:'100px 0 80px', position:'relative', overflow:'hidden' }}>
        <div style={{ position:'absolute', top:0, left:0, right:0, bottom:0, background:'radial-gradient(ellipse at 30% 20%, rgba(59,130,246,0.15) 0%, transparent 60%), radial-gradient(ellipse at 70% 80%, rgba(139,92,246,0.1) 0%, transparent 60%)' }} />
        <div style={{ ...sec, position:'relative', zIndex:1, display:'grid', gridTemplateColumns:'1fr 380px', gap:48, alignItems:'center' }}>
          <div>
            <div style={{ display:'inline-block', fontSize:11, fontWeight:700, color:'#60a5fa', background:'rgba(59,130,246,0.1)', border:'1px solid rgba(59,130,246,0.2)', borderRadius:20, padding:'5px 16px', marginBottom:24, letterSpacing:'0.08em' }}>ENTERPRISE ARCHITECTURE DOCUMENTATION</div>
            <h1 style={{ fontSize:52, fontWeight:900, color:'#f8fafc', lineHeight:1.1, letterSpacing:'-0.04em', margin:'0 0 20px' }}>AI-Powered Menu Intelligence Platform</h1>
            <p style={{ fontSize:18, color:'#94a3b8', lineHeight:1.7, maxWidth:600, margin:'0 0 40px' }}>Event-driven architecture with real-time Gemini AI auditing, transactional outbox guarantees, and multi-platform GraphQL delivery — built for restaurant operators who can't afford revenue leakage.</p>
            <div style={{ display:'flex', gap:16 }}>
              {[{n:'< 5ms',l:'API Latency'},{n:'3',l:'Client Platforms'},{n:'4',l:'Retry Attempts'},{n:'6',l:'Risk Categories'}].map(s=>(
                <div key={s.l} style={{ background:'rgba(255,255,255,0.05)', border:'1px solid rgba(255,255,255,0.1)', borderRadius:12, padding:'16px 24px', backdropFilter:'blur(8px)' }}>
                  <p style={{ margin:0, fontSize:28, fontWeight:800, color:'#f8fafc', fontFamily:mono }}>{s.n}</p>
                  <p style={{ margin:'4px 0 0', fontSize:11, color:'#64748b', fontWeight:500 }}>{s.l}</p>
                </div>
              ))}
            </div>
          </div>
          {/* Mini Flow Diagram */}
          <div style={{ background:'rgba(255,255,255,0.04)', border:'1px solid rgba(255,255,255,0.08)', borderRadius:20, padding:'28px 24px', backdropFilter:'blur(12px)' }}>
            <p style={{ margin:'0 0 20px', fontSize:10, fontWeight:700, color:'#60a5fa', letterSpacing:'0.1em', textAlign:'center' }}>APPLICATION FLOW</p>
            {[
              { icon:'📱', label:'Client Request', sub:'Next.js · Android · MCP', color:'#3b82f6' },
              { icon:'🔗', label:'GraphQL API', sub:'Netflix DGS /graphql', color:'#6366f1' },
              { icon:'⚙️', label:'Command Service', sub:'@Transactional publish', color:'#8b5cf6' },
              { icon:'📮', label:'Outbox → Kafka', sub:'At-least-once delivery', color:'#f59e0b' },
              { icon:'🧠', label:'Gemini AI Audit', sub:'Revenue risk analysis', color:'#ec4899' },
              { icon:'✅', label:'Results Persisted', sub:'menu_audit_result table', color:'#10b981' },
            ].map((step, i) => (
              <React.Fragment key={i}>
                <div style={{ display:'flex', alignItems:'center', gap:12, padding:'10px 14px', borderRadius:10, background:`${step.color}10`, border:`1px solid ${step.color}25` }}>
                  <span style={{ fontSize:18, width:36, height:36, borderRadius:10, background:`${step.color}18`, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>{step.icon}</span>
                  <div style={{ flex:1, minWidth:0 }}>
                    <p style={{ margin:0, fontSize:12, fontWeight:700, color:'#f1f5f9' }}>{step.label}</p>
                    <p style={{ margin:0, fontSize:10, color:'#94a3b8' }}>{step.sub}</p>
                  </div>
                  <span style={{ fontSize:9, fontWeight:700, color:step.color, fontFamily:mono }}>{String(i+1).padStart(2,'0')}</span>
                </div>
                {i < 5 && (
                  <div style={{ display:'flex', justifyContent:'center', padding:'2px 0' }}>
                    <div style={{ width:0, height:0, borderLeft:'5px solid transparent', borderRight:'5px solid transparent', borderTop:`6px solid ${step.color}50` }} />
                  </div>
                )}
              </React.Fragment>
            ))}
          </div>
        </div>
      </section>

      {/* TECH STACK STRIP */}
      <section style={{ borderBottom:'1px solid #e2e8f0', background:'#fafbfc', padding:'20px 0' }}>
        <div style={{ ...sec, display:'flex', alignItems:'center', justifyContent:'space-between' }}>
          <span style={{ fontSize:10, fontWeight:700, color:'#94a3b8', letterSpacing:'0.1em' }}>POWERED BY</span>
          <div style={{ display:'flex', gap:24 }}>
            {STACK.map(s=>(
              <div key={s.name} style={{ display:'flex', alignItems:'center', gap:6, fontSize:12, color:'#64748b' }}>
                <span style={{ fontSize:14 }}>{s.icon}</span>
                <span style={{ fontWeight:600, color:'#334155' }}>{s.name}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* FEATURES */}
      <section style={{ padding:'80px 0', background:'#fff' }}>
        <div style={sec}>
          <div style={{ textAlign:'center', marginBottom:56 }}>
            <p style={{ fontSize:11, fontWeight:700, color:'#3b82f6', letterSpacing:'0.1em', margin:'0 0 12px' }}>CORE CAPABILITIES</p>
            <h2 style={{ fontSize:36, fontWeight:800, color:'#0f172a', letterSpacing:'-0.03em', margin:'0 0 16px' }}>Built for Production Reliability</h2>
            <p style={{ fontSize:15, color:'#64748b', maxWidth:520, margin:'0 auto', lineHeight:1.7 }}>Six architectural pillars that ensure your menu operations never fail, your revenue is always protected, and your AI insights arrive in seconds.</p>
          </div>
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr 1fr', gap:20 }}>
            {FEATURES.map(f=>(
              <div key={f.title} style={{ background:'#fff', border:'1px solid #e2e8f0', borderRadius:16, padding:'28px 24px', boxShadow:shadow, transition:'all 0.2s', cursor:'default' }}
                onMouseEnter={e=>{(e.currentTarget as HTMLElement).style.boxShadow=shadowLg;(e.currentTarget as HTMLElement).style.transform='translateY(-2px)';}}
                onMouseLeave={e=>{(e.currentTarget as HTMLElement).style.boxShadow=shadow;(e.currentTarget as HTMLElement).style.transform='translateY(0)';}}
              >
                <div style={{ display:'flex', alignItems:'center', gap:10, marginBottom:16 }}>
                  <span style={{ fontSize:24, width:44, height:44, background:'#f1f5f9', borderRadius:12, display:'flex', alignItems:'center', justifyContent:'center' }}>{f.icon}</span>
                  <span style={{ fontSize:9, fontWeight:700, color:'#3b82f6', background:'#eff6ff', borderRadius:4, padding:'3px 8px', letterSpacing:'0.06em' }}>{f.tag}</span>
                </div>
                <h3 style={{ fontSize:16, fontWeight:700, color:'#0f172a', margin:'0 0 8px' }}>{f.title}</h3>
                <p style={{ fontSize:13, color:'#64748b', lineHeight:1.65, margin:0 }}>{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ARCHITECTURE DIAGRAM */}
      <section id="architecture" style={{ padding:'80px 0', background:'#f8fafc', borderTop:'1px solid #e2e8f0' }}>
        <div style={sec}>
          <div style={{ display:'grid', gridTemplateColumns:'1fr 340px', gap:32, alignItems:'start' }}>
            <div>
              <p style={{ fontSize:11, fontWeight:700, color:'#8b5cf6', letterSpacing:'0.1em', margin:'0 0 12px' }}>SYSTEM ARCHITECTURE</p>
              <h2 style={{ fontSize:32, fontWeight:800, color:'#0f172a', letterSpacing:'-0.03em', margin:'0 0 16px' }}>End-to-End Data Flow</h2>
              <p style={{ fontSize:14, color:'#64748b', lineHeight:1.7, margin:'0 0 28px' }}>From GraphQL mutation to AI-generated risk assessment — every step is observable, retriable, and designed for graceful degradation.</p>
              <div style={{ background:'#fff', border:'1px solid #e2e8f0', borderRadius:16, boxShadow:shadowLg, overflow:'hidden' }}>
                <div style={{ padding:'14px 20px', borderBottom:'1px solid #e2e8f0', display:'flex', alignItems:'center', justifyContent:'space-between' }}>
                  <div style={{ display:'flex', alignItems:'center', gap:8 }}>
                    <span>🔀</span><span style={{ fontSize:13, fontWeight:700, color:'#0f172a' }}>System Flow Diagram</span>
                  </div>
                  <span style={{ fontSize:10, color:'#94a3b8', fontFamily:mono }}>Mermaid.js</span>
                </div>
                <div style={{ padding:24, minHeight:350, display:'flex', alignItems:'center', justifyContent:'center', background:'#fafbfc' }}>
                  {err ? <div style={{ textAlign:'center', color:'#94a3b8' }}><p style={{ fontSize:36 }}>⚠️</p><p style={{ fontSize:13, fontWeight:600 }}>Diagram render failed</p></div>
                  : !svg ? <div style={{ textAlign:'center', color:'#94a3b8' }}><p style={{ fontSize:13 }}>Loading diagram…</p></div>
                  : <div style={{ width:'100%' }} dangerouslySetInnerHTML={{ __html:svg }} />}
                </div>
              </div>
            </div>

            {/* Layers sidebar */}
            <div>
              <p style={{ fontSize:11, fontWeight:700, color:'#64748b', letterSpacing:'0.1em', margin:'0 0 16px' }}>ARCHITECTURE LAYERS</p>
              <div style={{ display:'flex', flexDirection:'column', gap:4 }}>
                {LAYERS.map((l,i)=>(
                  <React.Fragment key={l.name}>
                    <div style={{ display:'flex', alignItems:'center', gap:12, padding:'12px 16px', borderRadius:10, background:l.bg, border:`1px solid ${l.border}` }}>
                      <span style={{ fontSize:20 }}>{l.icon}</span>
                      <div>
                        <p style={{ margin:0, fontSize:12, fontWeight:700, color:'#0f172a' }}>{l.name}</p>
                        <p style={{ margin:0, fontSize:10, color:'#64748b' }}>{l.tech}</p>
                      </div>
                    </div>
                    {i<LAYERS.length-1 && <div style={{ display:'flex', justifyContent:'center' }}><div style={{ width:1, height:10, background:'#cbd5e1' }}/></div>}
                  </React.Fragment>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* EVENT PIPELINE */}
      <section id="pipeline" style={{ padding:'80px 0', background:'#fff', borderTop:'1px solid #e2e8f0' }}>
        <div style={sec}>
          <div style={{ textAlign:'center', marginBottom:48 }}>
            <p style={{ fontSize:11, fontWeight:700, color:'#f59e0b', letterSpacing:'0.1em', margin:'0 0 12px' }}>EVENT PIPELINE</p>
            <h2 style={{ fontSize:32, fontWeight:800, color:'#0f172a', letterSpacing:'-0.03em', margin:'0 0 12px' }}>Async Processing Timeline</h2>
            <p style={{ fontSize:14, color:'#64748b', maxWidth:480, margin:'0 auto' }}>From mutation to persisted audit result — the complete lifecycle of a menu publish event.</p>
          </div>
          <div style={{ maxWidth:700, margin:'0 auto', position:'relative', paddingLeft:32 }}>
            <div style={{ position:'absolute', left:11, top:20, bottom:20, width:2, background:'linear-gradient(180deg,#3b82f6,#10b981)' }} />
            {TIMELINE.map((s,i)=>(
              <div key={i} style={{ position:'relative', paddingBottom:i<TIMELINE.length-1?32:0, display:'flex', gap:20, alignItems:'flex-start' }}>
                <div style={{ position:'absolute', left:-27, top:4, width:14, height:14, borderRadius:'50%', background:s.color, border:'3px solid #fff', boxShadow:`0 0 0 2px ${s.color}40`, zIndex:1 }} />
                <div style={{ flex:1 }}>
                  <span style={{ fontSize:10, fontWeight:700, color:s.color, fontFamily:mono, letterSpacing:'0.03em' }}>{s.t}</span>
                  <p style={{ margin:'4px 0 2px', fontSize:15, fontWeight:700, color:'#0f172a' }}>{s.label}</p>
                  <p style={{ margin:0, fontSize:12, color:'#94a3b8' }}>{s.sub}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* BOTTLENECK AUDIT */}
      <section id="audit" style={{ padding:'80px 0', background:'#fafbfc', borderTop:'1px solid #e2e8f0' }}>
        <div style={sec}>
          <div style={{ textAlign:'center', marginBottom:48 }}>
            <p style={{ fontSize:11, fontWeight:700, color:'#ef4444', letterSpacing:'0.1em', margin:'0 0 12px' }}>ARCHITECTURE AUDIT</p>
            <h2 style={{ fontSize:32, fontWeight:800, color:'#0f172a', letterSpacing:'-0.03em', margin:'0 0 12px' }}>Identified Bottlenecks & Fixes</h2>
            <p style={{ fontSize:14, color:'#64748b', maxWidth:520, margin:'0 auto' }}>Six architectural issues discovered during codebase analysis, with recommended production-grade solutions.</p>
          </div>
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:16 }}>
            {BOTTLENECKS.map(b=>(
              <div key={b.id} style={{ background:'#fff', border:'1px solid #e2e8f0', borderRadius:14, padding:'24px', boxShadow:shadow, borderLeft:`4px solid ${b.color}` }}>
                <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:12 }}>
                  <span style={{ fontSize:10, fontWeight:800, color:'#fff', background:b.color, borderRadius:4, padding:'3px 10px', letterSpacing:'0.05em' }}>{b.sev}</span>
                  <code style={{ fontSize:10, color:'#94a3b8', fontFamily:mono }}>{b.file}</code>
                </div>
                <h3 style={{ fontSize:14, fontWeight:700, color:'#0f172a', margin:'0 0 8px' }}>{b.title}</h3>
                <p style={{ fontSize:12, color:'#64748b', lineHeight:1.6, margin:'0 0 12px' }}>{b.desc}</p>
                <div style={{ background:'#ecfdf5', border:'1px solid #a7f3d0', borderRadius:8, padding:'8px 12px' }}>
                  <p style={{ margin:0, fontSize:11, color:'#065f46' }}><strong>Fix:</strong> {b.fix}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* FOOTER */}
      <footer style={{ background:grad1, padding:'48px 0', borderTop:'1px solid #334155' }}>
        <div style={{ ...sec, textAlign:'center' }}>
          <div style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:10, marginBottom:16 }}>
            <CrustLogo size={28} />
            <span style={{ fontSize:16, fontWeight:800, color:'#f8fafc' }}>Crust Architecture</span>
          </div>
          <p style={{ fontSize:11, color:'#64748b', fontFamily:mono, margin:0 }}>Spring Boot 3.4 · Kotlin 2.1 · PostgreSQL 15 · Apache Kafka · Gemini 2.0 Flash · Next.js 15</p>
          <p style={{ fontSize:10, color:'#475569', margin:'12px 0 0' }}>Generated from live codebase analysis · {new Date().getFullYear()}</p>
        </div>
      </footer>
    </div>
  );
}
