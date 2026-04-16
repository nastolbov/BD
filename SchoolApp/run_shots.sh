#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

ROLES=("ivanov_dir" "petrov_t" "sidorov_s" "kuzmina_med")
NAMES=("director" "teacher" "student" "medic")

# Start Xvfb
rm -f /tmp/.X99-lock
Xvfb :99 -screen 0 1280x800x24 >/tmp/xvfb.log 2>&1 &
XVFB_PID=$!
export DISPLAY=:99
sleep 1

mkdir -p screenshots

for i in "${!ROLES[@]}"; do
    login="${ROLES[$i]}"
    name="${NAMES[$i]}"
    echo "=== Capturing role: $name ($login) ==="
    java -cp "build:lib/sqlite-jdbc.jar" \
         -Dapp.autologin="$login" \
         -Dawt.useSystemAAFontSettings=on \
         -Dswing.aatext=true \
         SchoolApp &
    APP_PID=$!
    sleep 3
    import -display :99 -window root "screenshots/${name}_${login}.png"
    kill $APP_PID 2>/dev/null || true
    wait $APP_PID 2>/dev/null || true
    sleep 1
done

# Also capture login screen
echo "=== Capturing login screen ==="
java -cp "build:lib/sqlite-jdbc.jar" SchoolApp &
APP_PID=$!
sleep 3
import -display :99 -window root "screenshots/00_login.png"
kill $APP_PID 2>/dev/null || true
wait $APP_PID 2>/dev/null || true

kill $XVFB_PID 2>/dev/null || true
ls -la screenshots/
