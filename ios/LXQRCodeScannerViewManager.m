//
//  LXQRCodeScannerViewManager.m
//  lixil_qr_code_android
//
//  Created by 田中健策 on 2021/04/04.
//

#import <React/RCTViewManager.h>
#import "QRCodeScannerView.h"

@interface LXQRCodeScannerViewManager : RCTViewManager
@end

@implementation LXQRCodeScannerViewManager

RCT_EXPORT_MODULE(LXQRCodeScannerView)
RCT_EXPORT_VIEW_PROPERTY(onQRCodeRead, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onQRCodeTouch, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(alpha, NSInteger)
RCT_EXPORT_VIEW_PROPERTY(labelAlpha, NSInteger)
RCT_EXPORT_VIEW_PROPERTY(reloadKey, NSInteger)
RCT_EXPORT_VIEW_PROPERTY(charset, NSString)
RCT_CUSTOM_VIEW_PROPERTY(colorMap, NSDictionary, QRCodeScannerView) {
  [view setColorMap:json];
}
RCT_CUSTOM_VIEW_PROPERTY(colorMapForAlreadyRead, NSDictionary, QRCodeScannerView) {
  [view setColorMapForAlreadyRead:json];
}
RCT_CUSTOM_VIEW_PROPERTY(labelMap, NSDictionary, QRCodeScannerView) {
  [view setLabelMap:json];
}
RCT_EXPORT_VIEW_PROPERTY(labelFontSize, NSInteger)
RCT_EXPORT_VIEW_PROPERTY(labeledOnlyPatternMatched, BOOL)
RCT_EXPORT_VIEW_PROPERTY(labelColor, NSString)
RCT_CUSTOM_VIEW_PROPERTY(labelColorMap, NSDictionary, QRCodeScannerView) {
    [view setLabelColorMap:json];
}
RCT_EXPORT_VIEW_PROPERTY(labelDirection, NSString);
- (UIView *)view
{
  return [QRCodeScannerView new];
}

@end
