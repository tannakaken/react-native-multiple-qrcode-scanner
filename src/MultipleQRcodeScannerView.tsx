import React, { FC, useCallback, useEffect, useRef, useState } from 'react';

import {
  AppState,
  AppStateStatus,
  NativeSyntheticEvent,
  Platform,
  requireNativeComponent,
  ViewStyle,
} from 'react-native';

type LabelDirection = 'right' | 'bottom';

const AndroidQRCodeScannerView = requireNativeComponent<{
  style?: ViewStyle;
  onQRCodeRead: (event: NativeSyntheticEvent<any>) => void;
  onQRCodeTouch: (event: NativeSyntheticEvent<any>) => void;
  colorMap?: { [pattern: string]: string };
  colorMapForAlreadyRead?: { [pattern: string]: string };
  labelMap?: { [pattern: string]: string };
  labeledOnlyPatternMatched: boolean;
  labelColor?: string;
  labelColorMap?: { [pattern: string]: string };
  labelDirection: LabelDirection;
  alpha?: number;
  labelAlpha?: number;
  labelFontSize?: number; // ポイント数
}>('QRCodeScannerView');

const IOSQRCodeScannerView = requireNativeComponent<{
  style?: ViewStyle;
  onQRCodeRead: (event: NativeSyntheticEvent<any>) => void;
  onQRCodeTouch: (event: NativeSyntheticEvent<any>) => void;
  colorMap?: { [pattern: string]: string };
  colorMapForAlreadyRead?: { [pattern: string]: string };
  labelMap?: { [pattern: string]: string };
  overlayAlpha?: number;
  labelAlpha?: number;
  labelFontSize?: number;
  labeledOnlyPatternMatched: boolean;
  labelColor?: string;
  labelColorMap?: { [pattern: string]: string };
  labelDirection: LabelDirection;
  // IOSはアプリがバックグラウンドからフォアグアウンドになった時にカメラが止まる
  // このreloadKeyを変化させるとカメラが再起動する。
  // Androidにはこの仕組みは必要ない。
  reloadKey: number;
}>('LXQRCodeScannerView');

type MultipleQRCodeScannerViewProps = {
  style?: ViewStyle;
  onQRCodeRead?: (code: string) => void;
  onQRCodeTouch?: (code: string) => void;
  colorMap?: { [pattern: string]: string };
  colorMapForAlreadyRead?: { [pattern: string]: string };
  labelMap?: { [pattern: string]: string };
  labeledOnlyPatternMatched?: boolean;
  labelColor?: string;
  labelColorMap?: { [pattern: string]: string };
  labelDirection?: LabelDirection;
  labelFontSize?: number;
  alpha?: number;
  labelAlpha?: number;
};

/**
 * @param props
 * @constructor
 */
const MultipleQRCodeScannerView: FC<MultipleQRCodeScannerViewProps> = (
  props
) => {
  const appState = useRef(AppState.currentState);
  const [reloadKey, setReloadKey] = useState(0);
  const _handleAppStateChange = useCallback(
    (nextAppState: AppStateStatus) => {
      if (
        appState.current.match(/inactive|background/) &&
        nextAppState === 'active'
      ) {
        setReloadKey(reloadKey + 1);
        console.log('App has come to the foreground!');
      }

      appState.current = nextAppState;
      console.log('AppState', appState.current);
    },
    [reloadKey]
  );

  useEffect(() => {
    AppState.addEventListener('change', _handleAppStateChange);
    return () => {
      AppState.removeEventListener('change', _handleAppStateChange);
    };
  }, [_handleAppStateChange]);

  const onQRCodeRead = useCallback(
    (event) => {
      if (!props.onQRCodeRead) {
        return;
      }
      props.onQRCodeRead(event.nativeEvent.code);
    },
    [props]
  );
  const onQRCodeTouch = useCallback(
    (event) => {
      if (!props.onQRCodeTouch) {
        return;
      }
      props.onQRCodeTouch(event.nativeEvent.code);
    },
    [props]
  );
  const labeledOnlyPatternMatched =
    props.labeledOnlyPatternMatched !== undefined
      ? props.labeledOnlyPatternMatched
      : true;
  if (Platform.OS === 'ios') {
    return (
      <IOSQRCodeScannerView
        reloadKey={reloadKey}
        style={props.style}
        onQRCodeRead={onQRCodeRead}
        onQRCodeTouch={onQRCodeTouch}
        colorMap={props.colorMap}
        colorMapForAlreadyRead={props.colorMapForAlreadyRead}
        labelMap={props.labelMap}
        labeledOnlyPatternMatched={labeledOnlyPatternMatched}
        labelColor={props.labelColor}
        labelColorMap={props.labelColorMap}
        labelDirection={props.labelDirection || 'right'}
        overlayAlpha={props.alpha}
        labelAlpha={props.labelAlpha}
        labelFontSize={props.labelFontSize}
      />
    );
  }
  return (
    <AndroidQRCodeScannerView
      style={props.style}
      onQRCodeRead={onQRCodeRead}
      onQRCodeTouch={onQRCodeTouch}
      colorMap={props.colorMap}
      colorMapForAlreadyRead={props.colorMapForAlreadyRead}
      labelMap={props.labelMap}
      labeledOnlyPatternMatched={labeledOnlyPatternMatched}
      labelColor={props.labelColor}
      labelColorMap={props.labelColorMap}
      labelDirection={props.labelDirection || 'right'}
      alpha={props.alpha}
      labelAlpha={props.labelAlpha}
      labelFontSize={props.labelFontSize}
    />
  );
};

export default MultipleQRCodeScannerView;
