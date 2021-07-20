import { Alert, PermissionsAndroid, Platform } from 'react-native';

export const requestCameraPermission = () => {
  if (Platform.OS === 'android') {
    PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.CAMERA, {
      title: 'カメラ利用を許可してください',
      message: 'QRコード読み取りのためにカメラへのアクセスが必要です',
      buttonPositive: 'OK',
    }).then((granted) => {
      if (granted !== 'granted') {
        console.log('camera permission:', granted);
        Alert.alert('カメラの取得ができませんでした。');
      }
    });
  }
};
