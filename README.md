# react-native-multiple-qrcode-scanner

multiple qrcode scanner for react native

## Installation

```sh
npm install react-native-multiple-qrcode-scanner
```

## Usage

```js
import * as React from 'react';

import { StyleSheet, View } from 'react-native';
import MultipleQRCodeScannerView, {
  requestCameraPermission,
} from 'react-native-multiple-qrcode-scanner';
import { useEffect } from 'react';

export default function App() {
  useEffect(() => {
    requestCameraPermission();
  }, []);
  return (
    <View style={styles.container}>
      <MultipleQRCodeScannerView
        onQRCodeTouch={(code) => {
          console.warn(code);
        }}
        colorMap={{
          '^30.*': '#00FF00',
        }}
        labeledOnlyPatternMatched={false}
        labelMap={{
          '^.*$': 'hello',
        }}
        labelColorMap={{
          '30.*$': '#000000',
        }}
        labelDirection="bottom"
        style={styles.box}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: '100%',
    height: '100%',
  },
});
```

## props
```js
type MultipleQRCodeScannerViewProps = {
  style?: ViewStyle; // CSS
  onQRCodeRead?: (code: string) => void; // QRコードを検知時に実行
  onQRCodeTouch?: (code: string) => void; // QRコードの枠をタッチした時に実行
  colorMap?: { [pattern: string]: string }; // 枠の色を決める正規表現をキーとしたマップ。マッチした正規表現に対応した色になる。何にもマッチしないと赤になる。
  colorMapForAlreadyRead?: { [pattern: string]: string }; // 一度表示した正規表現が画面外に出たあと戻ってきたときはこちらを使う。
  labelMap?: { [pattern: string]: string }; // QRコードにラベルをつけるための正規表現をキーとしたマップ
  labeledOnlyPatternMatched?: boolean; // defaultで　true。これがtrueなら、colorMapやcolorMapForAlreadyReadがマッチしたQRコードに同じパターンのラベルをつけるだけ。これがfalseなら、colorMapやcolorMapForAlreadyReadとは別に検索される
  labelColor?: string; // これを指定するとラベルの色は固定される。
  labelColorMap?: { [pattern: string]: string }; // ラベルの色を決める正規表現をキーとしたマップ。これもlabelColorも指定しないと枠と同じ色になる。
  labelDirection?: LabelDirection; // "right"か"bottom"かのどちらか。ラベルを右に出すか下に出すか
  labelFontSize?: number; // ラベルのフォントのポイント数
  alpha?: number; // 枠とラベルの透明度
};
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
