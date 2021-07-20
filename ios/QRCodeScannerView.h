//
//  QRCodeScannerView.h
//  lixil_qr_code_android
//
//  Created by 田中健策 on 2021/07/19.
//

#ifndef QRCodeScannerView_h
#define QRCodeScannerView_h
#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import <React/RCTUIManager.h>

@class OverlayView;

@interface QRCodeScannerView : UIView<AVCaptureMetadataOutputObjectsDelegate>
@property (nonnull, nonatomic, strong) RCTBubblingEventBlock onQRCodeRead;
@property (nonnull, nonatomic, strong) RCTBubblingEventBlock onQRCodeTouch;
- (void)setOverlayAlpha:(NSInteger)overlayAlpha;
- (NSInteger)overlayAlpha;
- (void)setColorMap:(NSDictionary * _Nonnull)colorMap;
- (NSDictionary * _Nonnull)colorMap;
- (void)setColorMapForAlreadyRead:(NSDictionary * _Nullable)colorMapForAlreadyRead;
- (NSDictionary * _Nullable)colorMapForAlreadyRead;
- (void)setLabelMap:(NSDictionary * _Nonnull)labelMap;
- (NSDictionary * _Nonnull)labelMap;
- (void)setLabelFontSize:(NSInteger)labelFontSize;
- (NSInteger)labelFontSize;
- (void)setLabeledOnlyPatternMatched:(BOOL)labeledOnlyPatternMatched;
- (BOOL)labeledOnlyPatternMatched;
- (void)setLabelColor:(NSString * _Nullable)labelColor;
- (NSString * _Nullable)labelColor;
- (void)setLabelColorMap:(NSDictionary * _Nullable)labelColorMap;
- (NSDictionary * _Nullable)labelColorMap;
- (void)setLabelDirection:(NSString * _Nonnull)labelDirection;
- (NSString * _Nonnull)labelDirection;
// Hack。カメラをリスタートさせたい場合はこのプロパティを変化させる。
- (void)setReloadKey:(NSInteger)reloadKey;
- (NSInteger)reloadKey;
@end
#endif /* QRCodeScannerView_h */