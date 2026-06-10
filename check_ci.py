import os, json, requests

env_path = os.path.expanduser("~/.hermes/.env")
with open(env_path) as f:
    for line in f:
        if line.startswith("GITHUB_TOKEN="):
            token = line.split("=", 1)[1].strip().strip('"').strip("'")
            break

url = "https://api.github.com/repos/kozvits/Znaki-PDD/actions/runs?per_page=1&branch=main"
headers = {"Accept": "application/vnd.github+json", "Authorization": "Bearer ***}

r = requests.get(url, headers=headers, timeout=15)
data = r.json()
runs = data.get("workflow_runs", [])
if runs:
    wf = runs[0]
    print(f"Status: {wf['status']}")
    print(f"Conclusion: {wf['conclusion']}")
    print(f"Commit: {wf['head_sha'][:8]}")
    print(f"URL: {wf['html_url']}")
else:
    print(f"Error: {data.get('message', 'unknown')}")
