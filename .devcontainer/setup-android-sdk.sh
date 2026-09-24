#!/usr/bin/env bash
# Codespace(컨테이너)가 새로 만들어질 때마다 실행되어 Android SDK를
# 저장소 밖(홈 디렉터리)에 설치합니다. 이미 설치돼 있으면 다시 받지 않습니다.
set -euo pipefail

ANDROID_SDK_ROOT="$HOME/android-sdk"
CMDLINE_TOOLS_ZIP_URL="https://dl.google.com/android/repository/commandlinetools-linux-9862592_latest.zip"
JDK_CANDIDATE="21.0.12-ms"

if [ ! -d "$ANDROID_SDK_ROOT/cmdline-tools/latest" ]; then
  echo "[setup-android-sdk] Android SDK cmdline-tools 설치 중..."
  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
  tmp_zip="$(mktemp)"
  curl -fsSL -o "$tmp_zip" "$CMDLINE_TOOLS_ZIP_URL"
  unzip -q "$tmp_zip" -d "$ANDROID_SDK_ROOT/cmdline-tools"
  mv "$ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  rm -f "$tmp_zip"
else
  echo "[setup-android-sdk] Android SDK가 이미 설치되어 있어 건너뜁니다."
fi

# universal 이미지에는 sdkman으로 JDK 21이 이미 들어있는 경우가 많음. 없으면 설치.
SDKMAN_JAVA_DIR="/usr/local/sdkman/candidates/java"
if [ -d "$SDKMAN_JAVA_DIR" ] && ! ls -d "$SDKMAN_JAVA_DIR"/21.*-ms >/dev/null 2>&1; then
  if [ -s "/usr/local/sdkman/bin/sdkman-init.sh" ]; then
    set +u
    source "/usr/local/sdkman/bin/sdkman-init.sh"
    set -u
    sdk install java "$JDK_CANDIDATE" < /dev/null || true
  fi
fi
JAVA21_DIR="$(ls -d "$SDKMAN_JAVA_DIR"/21.*-ms 2>/dev/null | head -1 || true)"

export ANDROID_HOME="$ANDROID_SDK_ROOT"
export ANDROID_SDK_ROOT="$ANDROID_SDK_ROOT"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
[ -n "$JAVA21_DIR" ] && export PATH="$JAVA21_DIR/bin:$PATH"

echo "[setup-android-sdk] 라이선스 동의 및 필수 패키지 설치 중..."
yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses > /dev/null 2>&1 || true
sdkmanager --sdk_root="$ANDROID_HOME" "platform-tools" "platforms;android-36" "build-tools;36.0.0" > /dev/null

BASHRC="$HOME/.bashrc"
if ! grep -q "ANDROID_HOME" "$BASHRC" 2>/dev/null; then
  {
    echo ""
    echo "# Android SDK (installed outside repo by .devcontainer/setup-android-sdk.sh)"
    echo "export JAVA_HOME=\"$JAVA21_DIR\""
    echo "export ANDROID_HOME=\"$ANDROID_SDK_ROOT\""
    echo "export ANDROID_SDK_ROOT=\"$ANDROID_SDK_ROOT\""
    echo "export PATH=\"\$JAVA_HOME/bin:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH\""
  } >> "$BASHRC"
fi

echo "[setup-android-sdk] 완료."
