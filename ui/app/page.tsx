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

function Dashboard() {
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
        <h1 className="text-3xl font-bold text-orange-400">Revenue Command Center</h1>
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

export default function App() {
  const client = useMemo(() => makeClient(), []);
  return (
    <ApolloProvider client={client}>
      <main className="container mx-auto p-8">
        <Dashboard />
      </main>
    </ApolloProvider>
  );
}
