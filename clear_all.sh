rm -rf ~/.gradle/caches/
rm -rf ~/.gradle/daemon/
rm -rf ~/.gradle/native/

# キャッシュ完全削除
rm -rf ~/.gradle/caches/
rm -rf .gradle/
# 再ビルド
./gradlew clean runIde

