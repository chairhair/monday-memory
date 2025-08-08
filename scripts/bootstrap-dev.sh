#!/usr/bin/env bash
set -e

echo "=== Dev Bootstrap ==="

# 1) Check GitHub SSH port 22
echo "[1/3] Checking GitHub SSH port 22..."
if ! nc -vz github.com 22 >/dev/null 2>&1; then
    echo "Port 22 blocked — configuring ~/.ssh/config to use port 443..."

    mkdir -p ~/.ssh
    chmod 700 ~/.ssh

    cat > ~/.ssh/config <<'EOF'
Host github.com
  HostName ssh.github.com
  Port 443
  User git
  IdentityFile ~/.ssh/id_ed25519
  IdentitiesOnly yes
EOF

    chmod 600 ~/.ssh/config
    echo "~/.ssh/config updated to use port 443 for GitHub"
else
    echo "Port 22 is open — no SSH config changes needed."
fi

# 2) Ensure SSH key exists and is added
if [ ! -f ~/.ssh/id_ed25519 ]; then
    echo "[2/3] No SSH key found — generating new one..."
    read -p "Enter your GitHub email: " GH_EMAIL
    ssh-keygen -t ed25519 -C "$GH_EMAIL" -f ~/.ssh/id_ed25519 -N ""
    eval "$(ssh-agent -s)"
    ssh-add ~/.ssh/id_ed25519
    echo "Public key generated at ~/.ssh/id_ed25519.pub"
    echo "Upload this key to GitHub → Settings → SSH and GPG Keys"
else
    echo "[2/3] SSH key exists — adding to agent..."
    eval "$(ssh-agent -s)"
    ssh-add ~/.ssh/id_ed25519
fi

# 3) Load .env and generate JWT_SECRET if missing
echo "[3/3] Loading .env variables..."
if ! grep -q '^JWT_SECRET=' .env 2>/dev/null; then
    echo "JWT_SECRET not found — generating..."
    echo "JWT_SECRET=$(openssl rand -base64 32)" >> .env
fi

set -a
source <(grep -v '^#' .env | tr -d '\r')
set +a

echo "All done ✅"
echo "JWT_SECRET=$JWT_SECRET"

