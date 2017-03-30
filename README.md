# react-native-test
## how-to build release version
1. `yarn`
2. `android/gradlew assembleRelease`
## how-to build jsbundle
1. `yarn`
2. `react-native bundle --platform ios --entry-file index.android.js --bundle-output <out>/index.android.bundle --assets-dest <out> --dev false`
