"use client";

import { ApolloClient, InMemoryCache, ApolloProvider, HttpLink, useQuery, useMutation, gql } from '@apollo/client';
import { useState, useMemo } from 'react';

function makeClient() {
  return new ApolloClient({
    link: new HttpLink({ uri: 'http://localhost:8080/graphql' }),
    cache: new InMemoryCache(),
    ssrMode: false,
  });
}

const GET_RISKS = gql`
  query GetRisks($versionId: ID!) {
    getMenuRisks(versionId: $versionId) {
      id
      category
      impactScore
      plainEnglishSummary
      suggestedAction
    }
  }
`;

const REVERT_VERSION = gql`
  mutation RevertVersion($versionId: ID!) {
    revertToCleanVersion(versionId: $versionId)
  }
`;

const GET_DEMAND_FORECAST = gql`
  query GetDemandForecast($dateFrom: String!, $dateTo: String!) {
    demandForecast(dateFrom: $dateFrom, dateTo: $dateTo) {
      id
      itemName
      forecastDate
      hourOfDay
      predictedQuantity
      predictedRevenue
      confidence
    }
  }
`;

type Risk = {
  id: string;
  category: 'REVENUE_LEAKAGE' | 'TAX_COMPLIANCE' | 'PRICING_STRATEGY';
  impactScore: number;
  plainEnglishSummary: string;
  suggestedAction: string;
};

const CATEGORY_META = {
  REVENUE_LEAKAGE: {
    label: 'Revenue Leakage',
    border: 'border-red-500/60',
    bg: 'bg-red-900/20',
    badge: 'bg-red-500/20 text-red-300 border border-red-500/40',
    bar: 'bg-red-500',
    dot: 'bg-red-500',
  },
  TAX_COMPLIANCE: {
    label: 'Tax Compliance',
    border: 'border-orange-500/60',
    bg: 'bg-orange-900/20',
    badge: 'bg-orange-500/20 text-orange-300 border border-orange-500/40',
    bar: 'bg-orange-500',
    dot: 'bg-orange-500',
  },
  PRICING_STRATEGY: {
    label: 'Pricing Strategy',
    border: 'border-yellow-500/60',
    bg: 'bg-yellow-900/20',
    badge: 'bg-yellow-500/20 text-yellow-300 border border-yellow-500/40',
    bar: 'bg-yellow-400',
    dot: 'bg-yellow-400',
  },
} as const;

function ActionCard({ risk }: { risk: Risk }) {
  const meta = CATEGORY_META[risk.category] ?? CATEGORY_META.PRICING_STRATEGY;
  const isHighImpact = risk.impactScore >= 7;

  return (
    <div className={`relative rounded-lg border ${meta.border} ${meta.bg} p-4 overflow-hidden`}>
      <div className={`absolute left-0 top-0 h-full w-1 ${meta.bar}`} />
      <div className="flex items-start justify-between gap-4 pl-2">
        <div className="flex-1 space-y-2">
          <div className="flex items-center gap-2 flex-wrap">
            <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${meta.badge}`}>
              {meta.label}
            </span>
            {isHighImpact && (
              <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-red-600/30 text-red-300 border border-red-600/40 flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-red-400 animate-pulse inline-block" />
                High Impact
              </span>
            )}
          </div>
          <p className="text-slate-200 text-sm leading-relaxed">{risk.plainEnglishSummary}</p>
          <div className="flex items-start gap-2 pt-1">
            <span className="text-slate-400 text-xs mt-0.5 shrink-0">Suggested action:</span>
            <p className="text-slate-300 text-xs leading-relaxed">{risk.suggestedAction}</p>
          </div>
        </div>
        <div className="flex flex-col items-center shrink-0">
          <span className="text-xs text-slate-400 mb-1">Impact</span>
          <span className={`text-2xl font-bold ${isHighImpact ? 'text-red-400' : 'text-yellow-400'}`}>
            {risk.impactScore}
          </span>
          <span className="text-xs text-slate-500">/10</span>
        </div>
      </div>
    </div>
  );
}

function AiAuditDashboard() {
  const [versionId, setVersionId] = useState("c2d29867-3d0b-d497-9191-18a9d8ee7830");
  const { loading, error, data, refetch } = useQuery(GET_RISKS, { variables: { versionId } });
  const [revert, { loading: revertLoading }] = useMutation(REVERT_VERSION);

  const risks: Risk[] = data?.getMenuRisks ?? [];
  const highImpact = risks.filter((r) => r.impactScore >= 7);
  const grouped = risks.reduce<Record<string, Risk[]>>((acc, r) => {
    (acc[r.category] = acc[r.category] ?? []).push(r);
    return acc;
  }, {});

  const handleFix = async () => {
    await revert({ variables: { versionId } });
    refetch();
    alert("Reverted safely!");
  };

  return (
    <div className="space-y-6">
      <header className="flex justify-between items-center border-b border-slate-700 pb-4">
        <div>
          <h1 className="text-3xl font-bold text-orange-400">Revenue Command Center</h1>
          <p className="text-slate-400 mt-1 text-sm">Real-time menu intelligence & auditing</p>
        </div>
        <span className="text-sm bg-slate-800 px-3 py-1 rounded-full border border-slate-600">
          Active Version: {versionId}
        </span>
      </header>

      {risks.length > 0 && (
        <div className="grid grid-cols-3 gap-4 text-center">
          {(["REVENUE_LEAKAGE", "TAX_COMPLIANCE", "PRICING_STRATEGY"] as const).map((cat) => (
            <div key={cat} className={`rounded-lg border ${CATEGORY_META[cat].border} ${CATEGORY_META[cat].bg} p-3`}>
              <p className="text-2xl font-bold text-white">{grouped[cat]?.length ?? 0}</p>
              <p className="text-xs text-slate-400 mt-1">{CATEGORY_META[cat].label}</p>
            </div>
          ))}
        </div>
      )}

      <div className="card">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold">AI Audit — Action Cards</h2>
          {risks.length > 0 && (
            <button onClick={handleFix} disabled={revertLoading} className="btn-orange">
              {revertLoading ? "Reverting..." : "Quick Fix (Rollback)"}
            </button>
          )}
        </div>

        {loading && <p className="text-slate-400">Scanning AI Auditor Logs...</p>}
        {error && <p className="text-red-400">Error connecting to Sidecar Backend.</p>}

        {!loading && !error && risks.length === 0 && (
          <div className="p-4 bg-emerald-900/20 border border-emerald-500/50 rounded-lg text-emerald-400">
            ✅ All structural matrices stable.
          </div>
        )}

        {risks.length > 0 && (
          <div className="space-y-3">
            {highImpact.length > 0 && (
              <p className="text-xs text-slate-400">
                {highImpact.length} high-impact risk{highImpact.length > 1 ? 's' : ''} detected — immediate action recommended.
              </p>
            )}
            {risks
              .sort((a, b) => b.impactScore - a.impactScore)
              .map((risk) => <ActionCard key={risk.id} risk={risk} />)}
          </div>
        )}
      </div>
    </div>
  );
}

type Forecast = {
  id: string;
  itemName: string;
  forecastDate: string;
  hourOfDay: number;
  predictedQuantity: number;
  predictedRevenue: number;
  confidence: number;
};

function DemandForecastDashboard() {
  // Fixed window for POC: next 7 days
  const today = new Date();
  const nextWeek = new Date(today);
  nextWeek.setDate(nextWeek.getDate() + 7);
  
  const dateFrom = today.toISOString().split('T')[0];
  const dateTo = nextWeek.toISOString().split('T')[0];

  const { loading, error, data } = useQuery(GET_DEMAND_FORECAST, { 
    variables: { dateFrom, dateTo } 
  });

  const forecasts: Forecast[] = data?.demandForecast ?? [];
  
  // Aggregate data for top line metrics
  const totalQuantity = forecasts.reduce((sum, f) => sum + f.predictedQuantity, 0);
  const totalRevenue = forecasts.reduce((sum, f) => sum + f.predictedRevenue, 0);

  // Group by item to show matrix
  const itemSummary = forecasts.reduce<Record<string, { qty: number, rev: number, confSum: number, count: number }>>((acc, f) => {
    if (!acc[f.itemName]) acc[f.itemName] = { qty: 0, rev: 0, confSum: 0, count: 0 };
    acc[f.itemName].qty += f.predictedQuantity;
    acc[f.itemName].rev += f.predictedRevenue;
    acc[f.itemName].confSum += f.confidence;
    acc[f.itemName].count += 1;
    return acc;
  }, {});

  const matrix = Object.entries(itemSummary)
    .map(([itemName, stats]) => ({
      itemName,
      qty: Math.round(stats.qty),
      rev: stats.rev,
      avgConf: stats.confSum / stats.count
    }))
    .sort((a, b) => b.rev - a.rev)
    .slice(0, 5); // top 5

  return (
    <div className="space-y-6 mt-12 pt-8 border-t border-slate-800">
      <header className="flex justify-between items-center pb-2">
        <div>
          <h2 className="text-2xl font-bold text-indigo-400">Demand Intelligence</h2>
          <p className="text-sm text-slate-400 mt-1">Machine Learning WMA-4w Forecast ({dateFrom} to {dateTo})</p>
        </div>
        <span className="text-xs bg-indigo-900/30 text-indigo-300 px-3 py-1 rounded-full border border-indigo-500/30 flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-indigo-500 animate-pulse"></span>
          Model Active
        </span>
      </header>

      {/* Block 1: Top Line */}
      <div className="grid grid-cols-2 gap-4">
        <div className="card bg-slate-800/50 border-slate-700/50 flex flex-col justify-center items-center p-6">
          <span className="text-sm text-slate-400 mb-1">Projected 7-Day Revenue</span>
          <span className="text-4xl font-bold text-emerald-400">${totalRevenue.toFixed(2)}</span>
        </div>
        <div className="card bg-slate-800/50 border-slate-700/50 flex flex-col justify-center items-center p-6">
          <span className="text-sm text-slate-400 mb-1">Total Units to Prep</span>
          <span className="text-4xl font-bold text-blue-400">{Math.round(totalQuantity)}</span>
        </div>
      </div>

      {/* Block 2 & 3: Bento Grid */}
      <div className="grid grid-cols-3 gap-6">
        
        {/* Forecast Matrix */}
        <div className="col-span-2 card">
          <h3 className="text-lg font-semibold mb-4 text-slate-200">Top Movers Forecast</h3>
          {loading && <p className="text-slate-400 text-sm animate-pulse">Running ML prediction models...</p>}
          {error && <p className="text-red-400 text-sm">Failed to connect to forecast engine.</p>}
          
          {!loading && !error && matrix.length === 0 && (
             <div className="p-8 text-center text-slate-500 border border-slate-700/50 rounded-lg border-dashed">
                Waiting for sufficient historical sales data to generate forecast.
             </div>
          )}

          {matrix.length > 0 && (
            <div className="space-y-3">
              <div className="grid grid-cols-12 text-xs font-semibold text-slate-500 pb-2 border-b border-slate-700/50">
                <div className="col-span-5">MENU ITEM</div>
                <div className="col-span-2 text-right">EST. QTY</div>
                <div className="col-span-3 text-right">PROJ. REVENUE</div>
                <div className="col-span-2 text-right">CONFIDENCE</div>
              </div>
              {matrix.map(m => (
                <div key={m.itemName} className="grid grid-cols-12 items-center text-sm py-2 border-b border-slate-700/20 last:border-0">
                  <div className="col-span-5 font-medium text-slate-200">{m.itemName}</div>
                  <div className="col-span-2 text-right text-blue-300">{m.qty}</div>
                  <div className="col-span-3 text-right text-emerald-400">${m.rev.toFixed(2)}</div>
                  <div className="col-span-2 flex justify-end items-center gap-2">
                    <span className="text-xs text-slate-400">{(m.avgConf * 100).toFixed(0)}%</span>
                    <div className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: m.avgConf > 0.7 ? '#10b981' : m.avgConf > 0.4 ? '#f59e0b' : '#ef4444' }}></div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Prep Recommendations */}
        <div className="col-span-1 card bg-indigo-900/10 border-indigo-500/20 flex flex-col">
          <h3 className="text-lg font-semibold mb-4 text-indigo-300">Smart Prep</h3>
          <div className="space-y-4 flex-1">
            {matrix.slice(0, 3).map((m, i) => (
              <div key={i} className="p-3 bg-slate-800/80 rounded-lg border border-slate-700/50">
                <p className="text-xs text-slate-400 mb-1 flex items-center gap-1">
                  <svg className="w-3 h-3 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
                  High Demand Alert
                </p>
                <p className="text-sm text-slate-200">
                  Prep <span className="font-bold text-white">{m.qty}x {m.itemName}</span> to meet projected demand.
                </p>
              </div>
            ))}
            {matrix.length === 0 && !loading && (
              <p className="text-sm text-slate-500 italic">No prep alerts active.</p>
            )}
          </div>
        </div>

      </div>
    </div>
  );
}

export default function App() {
  const client = useMemo(() => makeClient(), []);
  return (
    <ApolloProvider client={client}>
      <main className="container max-w-5xl mx-auto p-8 pb-20">
        <AiAuditDashboard />
        <DemandForecastDashboard />
      </main>
    </ApolloProvider>
  );
}
