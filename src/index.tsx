import { requireNativeComponent, ViewStyle } from 'react-native';

type MultipleQrcodeScannerProps = {
  color: string;
  style: ViewStyle;
};

export const MultipleQrcodeScannerViewManager = requireNativeComponent<MultipleQrcodeScannerProps>(
'MultipleQrcodeScannerView'
);

export default MultipleQrcodeScannerViewManager;
