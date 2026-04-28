"use client";

import { ApolloClient, InMemoryCache, ApolloProvider, useQuery, useMutation, gql } from '@apollo/client';
import { useState } from 'react';

const client = new ApolloClient({
  uri: 'http://localhost:8080/graphql',
  cache: new InMemoryCache(),
});

const GET_RISKS = gql`
  query GetRisks($versionId: ID!) {
    getMenuRisks(versionId: $versionId) {
      id
      description
      severityScore
    }
  }
`;

const REVERT_VERSION = gql`
  mutation RevertVersion($versionId: ID!) {
    revertToCleanVersion(versionId: $versionId)
  }
`;

function Dashboard() {
  const [versionId, setVersionId] = useState("c2d29867-3d0b-d497-9191-18a9d8ee7830");
  const { loading, error, data, refetch } = useQuery(GET_RISKS, { variables: { versionId }});
  const [revert, { loading: revertLoading }] = useMutation(REVERT_VERSION);

  const handleFix = async () => {
    await revert({ variables: { versionId } });
    refetch();
    alert("Reverted safely!");
  };

  return (
    <div className="space-y-6">
      <header className="flex justify-between items-center border-b border-slate-700 pb-4">
        <h1 className="text-3xl font-bold text-orange-400">Revenue Command Center</h1>
        <span className="text-sm bg-slate-800 px-3 py-1 rounded-full border border-slate-600">Active Version: {versionId}</span>
      </header>
      
      <div className="card">
        <h2 className="text-xl font-semibold mb-4">Integrity Auditors</h2>
        {loading && <p>Scanning AI Auditor Logs...</p>}
        {error && <p className="text-red-400">Error connecting to Sidecar Backend.</p>}
        
        {data && data.getMenuRisks && data.getMenuRisks.length > 0 ? (
          <div className="space-y-4 shadow-lg border border-red-500/50 bg-red-900/20 p-4 rounded-lg relative overflow-hidden">
            <div className="absolute top-0 left-0 w-1 h-full bg-red-500"></div>
            <div className="flex justify-between items-start">
              <div>
                <h3 className="text-red-400 font-bold text-lg flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-red-500 animate-pulse"></span> Red Alert
                </h3>
                <p className="text-slate-300 mt-1">Anomalies detected in published structure.</p>
              </div>
              <button onClick={handleFix} disabled={revertLoading} className="btn-orange">
                {revertLoading ? "Reverting..." : "Quick Fix (Rollback)"}
              </button>
            </div>
            
            <ul className="mt-4 space-y-2">
              {data.getMenuRisks.map((risk: any) => (
                <li key={risk.id} className="bg-slate-800/80 p-3 rounded border border-slate-700 text-slate-200">
                  <strong className="text-orange-400">Severity {risk.severityScore}:</strong> {risk.description}
                </li>
              ))}
            </ul>
          </div>
        ) : data && (
          <div className="p-4 bg-emerald-900/20 border border-emerald-500/50 rounded-lg text-emerald-400">
            ✅ All structural matrices stable.
          </div>
        )}
      </div>
    </div>
  );
}

export default function App() {
  return (
    <ApolloProvider client={client}>
      <Dashboard />
    </ApolloProvider>
  );
}
