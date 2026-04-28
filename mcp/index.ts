import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import pg from "pg";

const pool = new pg.Pool({
    connectionString: process.env.DATABASE_URL || "postgresql://postgres:password@postgres:5432/postgres" 
});

const server = new McpServer({
    name: "Revenue MCP Context Server",
    version: "1.0.0"
});

server.tool(
    "query_menu_integrity",
    "Query the raw menu_audit_result and menu_version tables for an explicit MenuVersion ID.",
    {
        versionId: z.string().describe("The UUID string of the MenuVersion to inspect.")
    },
    async ({ versionId }) => {
        try {
            const versionRes = await pool.query('SELECT * FROM menu_version WHERE id = $1', [versionId]);
            const auditsRes = await pool.query('SELECT * FROM menu_audit_result WHERE menu_version_id = $1', [versionId]);

            const response = {
                versionData: versionRes.rows[0] || null,
                auditIdentifications: auditsRes.rows || []
            };

            return {
                content: [{ type: "text", text: JSON.stringify(response, null, 2) }]
            };
        } catch (e: any) {
            return {
                isError: true,
                content: [{ type: "text", text: `PostgreSQL Query Error: ${e.message}` }]
            };
        }
    }
);

async function main() {
    const transport = new StdioServerTransport();
    await server.connect(transport);
    console.log("Revenue Command Center MCP Server Online.");
}

main().catch(console.error);
