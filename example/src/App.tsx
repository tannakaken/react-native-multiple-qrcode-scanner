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
        labelAlpha={255}
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
