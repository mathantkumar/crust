import re

with open("src/main/kotlin/com/crust/menu/graphql/MenuGraphQueries.kt", "r") as f:
    q_content = f.read()

# Replace any lingering .map { ticketToMap(it) } or similar
q_content = re.sub(r'\.map\s*\{\s*\w+ToMap\(it\)\s*\}', '', q_content)
q_content = re.sub(r'return\s+\w+ToMap\((.*?)\)', r'return \1', q_content)

# Replace the helpers at the bottom
q_content = re.sub(r'// ─── Helpers ───.*', '', q_content, flags=re.DOTALL)

with open("src/main/kotlin/com/crust/menu/graphql/MenuGraphQueries.kt", "w") as f:
    f.write(q_content)

