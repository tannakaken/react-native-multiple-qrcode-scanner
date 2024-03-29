//
//  QRCodeScannerView.m
//  lixil_qr_code_android
//
//  Created by 田中健策 on 2021/07/19.
//

#import "QRCodeScannerView.h"
#import "OverlayView.h"


@implementation QRCodeScannerView {
    AVCaptureSession *captureSession;
    AVCaptureVideoPreviewLayer *videoPreviewLayer;
    OverlayView *overlayView;
    NSInteger  reloadKey_;
    NSMutableDictionary<NSString *, NSDate *> *codesNowReading;
    // React Nativeからプロパティを設定した時点でoverlayViewがまだセットされていないことがある。
    // なので一度QRCodeScannerViewで持って、それをoverlayView生成時点でoverlayViewに設定する。
    NSInteger overlayAlpha_;
    NSInteger labelAlpha_;
    NSDictionary<NSString *, NSString *> *colorMap_;
    NSDictionary<NSString *, NSString *> *colorMapForAlreadyRead_;
    NSDictionary<NSString *, NSString *> *labelMap_;
    NSInteger labelFontSize_;
    BOOL labeledOnlyPatternMatched_;
    NSString *labelColor_;
    NSDictionary<NSString *, NSString *> *labelColorMap_;
    NSString *labelDirection_;
    NSMutableArray<NSValue *> *rects;
    NSMutableArray<NSString *> *codes;
}

- (id)initWithFrame:(CGRect)frame {
  self = [super initWithFrame:frame];
  if (self) {
    [self prepare];
  }
  return self;
}

- (id)initWithCoder:(NSCoder *)coder {
  self = [super initWithCoder:coder];
  if (self) {
    [self prepare];
  }
  return self;
}

- (void)prepare {
    reloadKey_ = 0;
    self.charset = nil;
    codesNowReading = [NSMutableDictionary new];
    overlayAlpha_ = 100;
    labelAlpha_ = -1;
    colorMap_ = [NSDictionary new];
    colorMapForAlreadyRead_ = nil;
    labelMap_ = [NSDictionary new];
    labelFontSize_ = 40;
    labeledOnlyPatternMatched_ = NO;
    labelColor_ = nil;
    labelColorMap_ = nil;
    labelDirection_ = @"right";
    rects = [NSMutableArray new];
    codes = [NSMutableArray new];
  
    UITapGestureRecognizer *tapGestureRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(tap:)];
    [self addGestureRecognizer:tapGestureRecognizer];
}

- (void)tap:(UITapGestureRecognizer *)tapGestureRecognizer {
  if (!self.onQRCodeTouch) {
    return;
  }
  CGPoint point = [tapGestureRecognizer locationOfTouch:0 inView:self];
  [rects enumerateObjectsUsingBlock:^(NSValue *value, NSUInteger idx, BOOL *stop) {
    CGRect rect = [value CGRectValue];
    NSString *code = codes[idx];
    [self onTapInRect:rect withCode:code at:point withHandler:self.onQRCodeTouch];
  }];
}

- (void)onTapInRect: (CGRect)rect withCode:(NSString *)code at:(CGPoint)point withHandler:(RCTBubblingEventBlock)handler {
  if (CGRectContainsPoint(rect, point)) {
    handler(@{@"code": code});
  }
}

- (void)layoutSubviews {
  [super layoutSubviews];
  [self startCamera];
}

- (void)startCamera {
  [self stopCamera];
  AVCaptureDeviceDiscoverySession *deviceDiscoverySession = [AVCaptureDeviceDiscoverySession
                                                             discoverySessionWithDeviceTypes:@[AVCaptureDeviceTypeBuiltInWideAngleCamera]
                                                             mediaType:AVMediaTypeVideo
                                                             position:AVCaptureDevicePositionBack];
  AVCaptureDevice *captureDevice = deviceDiscoverySession.devices.firstObject;
  if (!captureDevice) {
    NSLog(@"Failed to get the camera device");
    return;
  }
  NSError *error = nil;
  AVCaptureDeviceInput *input = [AVCaptureDeviceInput deviceInputWithDevice:captureDevice error:&error];
  if (error != nil) {
    NSLog(@"error: %@", error);
    return;
  }
  AVCaptureSession *captureSession = [AVCaptureSession new];
  if (captureSession) {
    [captureSession addInput:input];
  }
  AVCaptureMetadataOutput *captureMetadataOutput = [AVCaptureMetadataOutput new];
  [captureSession addOutput:captureMetadataOutput];
  [captureMetadataOutput setMetadataObjectsDelegate:self queue:dispatch_get_main_queue()];
  captureMetadataOutput.metadataObjectTypes = @[AVMetadataObjectTypeQRCode];
  videoPreviewLayer = [AVCaptureVideoPreviewLayer layerWithSession:captureSession];
  if (videoPreviewLayer) {
    videoPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    videoPreviewLayer.frame = self.layer.frame;
    [self.layer addSublayer:videoPreviewLayer];
  }
  overlayView = [[OverlayView alloc] initWithFrame:self.bounds];
  if (overlayView) {
      [overlayView setOpaque:NO];
      //React Nativeからプロパティを設定した時点でoverlayViewがまだセットされていないことがある。
      // なので一度QRCodeScannerViewで持って、それをoverlayView生成時点でoverlayViewに設定する。
      overlayView.overlayAlpha = overlayAlpha_;
      overlayView.labelAlpha = labelAlpha_;
      overlayView.colorMap = colorMap_;
      overlayView.colorMapForAlreadyRead = colorMapForAlreadyRead_;
      overlayView.labelMap = labelMap_;
      overlayView.labelFontSize = labelFontSize_;
      overlayView.labeledOnlyPatternMatched = labeledOnlyPatternMatched_;
      overlayView.labelColor = labelColor_;
      overlayView.labelColorMap = labelColorMap_;
      overlayView.labelDirection = labelDirection_;
      [self addSubview:overlayView];
      [self bringSubviewToFront:overlayView];
  }
  if (captureSession) {
    [captureSession startRunning];
  }
}

- (void)stopCamera {
  if (!captureSession) {
    return;
  }
  if (!captureSession.running) {
    return;
  }
  [captureSession stopRunning];
}

- (void)captureOutput:(AVCaptureOutput *)output didOutputMetadataObjects:(NSArray<__kindof AVMetadataObject *> *)metadataObjects fromConnection:(AVCaptureConnection *)connection {
  if (!overlayView) {
    return;
  }
  if (!videoPreviewLayer) {
    return;
  }
  rects = [NSMutableArray new];
  codes = [NSMutableArray new];
  NSDate *now = [NSDate new];
  NSDate *oneSecondAgo = [NSDate dateWithTimeIntervalSinceNow:-1];
  [metadataObjects enumerateObjectsUsingBlock:^(AVMetadataObject *object, NSUInteger idx, BOOL *stop) {
      if (![object isKindOfClass:AVMetadataMachineReadableCodeObject.class]) {
          return;
      }
      AVMetadataMachineReadableCodeObject *metadataObject = (AVMetadataMachineReadableCodeObject *)object;
      if (![metadataObject.type isEqualToString:AVMetadataObjectTypeQRCode]) {
          return;
      }
      AVMetadataObject *qrcodeObject = [videoPreviewLayer transformedMetadataObjectForMetadataObject:metadataObject];
      CGRect bounds = qrcodeObject.bounds;
      NSValue *value = [NSValue valueWithCGRect:bounds];
      [rects addObject:value];
      
      CIBarcodeDescriptor *descriptor = metadataObject.descriptor;
      if (![descriptor isKindOfClass:CIQRCodeDescriptor.class]) {
          [codes addObject:@""];
          return;
      }
      CIQRCodeDescriptor *qrDescriptor = (CIQRCodeDescriptor *)descriptor;
      NSData *rawData = [self decode:qrDescriptor];
      NSString *code;
      if (rawData) {
          code = [[NSString alloc] initWithData:rawData encoding:[self currentCharset]];
      } else {
          code = metadataObject.stringValue;
      }
      if (!code) {
          [codes addObject:@""];
          return;
      }
      if (overlayView.colorMapForAlreadyRead != nil) {
          NSDate *timestamp = codesNowReading[code];
          if (timestamp) {
              if ([timestamp compare:oneSecondAgo] == NSOrderedAscending) { // timestampが1秒よりも前
                  [overlayView.codesAlreadyRead addObject:code];
                  [codesNowReading removeObjectForKey:code];
              } else {
                  codesNowReading[code] = now;
              }
          } else {
              codesNowReading[code] = now;
          }
      }
      if (self.onQRCodeRead) {
          self.onQRCodeRead(@{@"code": code});
      }
      [codes addObject:code];
    }];
    overlayView.rects = rects;
    overlayView.codes = codes;
    [overlayView setNeedsDisplay];
}

/**
  * TODO まだ全てのQRコードで動かない。混合モードでは動かない。
 */
- (NSData *)decode:(CIQRCodeDescriptor *)descriptor {
    NSUInteger len = descriptor.errorCorrectedPayload.length;
    Byte *sourceData = (Byte*)malloc(len);
    memcpy(sourceData, descriptor.errorCorrectedPayload.bytes, len);
    Byte byte = 0x04; // Kanjiモード。shift jisの漢字
    Byte mode = sourceData[0] >> 4;
    if (mode != byte) { // 漢字モードのみ対応。その他はiosのデフォルトを使う。混合モードでは動かない。
        free(sourceData);
        return nil;
    }
    NSInteger location;
    NSInteger length;
    NSInteger version = descriptor.symbolVersion;
    if (1 <= version && version <= 9) {
        location = 1;
        NSUInteger a = (NSUInteger)(sourceData[0] & 0x0f) << 4;
        NSUInteger b = (NSUInteger)(sourceData[1] & 0xf0) >> 4;
        length = (NSInteger)(a | b);
    } else if (10 <= version && 40 <= version) {
        location = 2;
        NSUInteger a = (NSUInteger)(sourceData[0] & 0x0f) << 12;
        NSUInteger b = (NSUInteger)sourceData[1] << 4;
        NSUInteger c = (NSUInteger)(sourceData[2] & 0xf0) >> 4;
        length = (NSInteger)(a | b | c);
    } else {
        free(sourceData);
        return nil;
    }
    Byte *targetData = (Byte*)malloc(length);
    for (NSInteger i = 0; i < length; i++) {
        NSInteger j = location + i;
        targetData[i] = sourceData[j] << 4 | sourceData[j+1] >> 4;
    }
    NSData *data = [[NSData alloc] initWithBytes:targetData length:length];
    free(sourceData);
    free(targetData);
    return data;
}

// React Nativeからプロパティを設定した時点でoverlayViewがまだセットされていないことがある。
// なので一度QRCodeScannerViewで持って、それをoverlayView生成時点でoverlayViewに設定する。
- (void)setOverlayAlpha:(NSInteger)overlayAlpha {
  overlayAlpha_ = overlayAlpha;
  if (overlayView) {
    overlayView.overlayAlpha = overlayAlpha;
  }
}

- (NSInteger)overlayAlpha {
  return overlayAlpha_;
}

- (void)setLabelAlpha:(NSInteger)labelAlpha {
  labelAlpha_ = labelAlpha;
  if (overlayView) {
    overlayView.labelAlpha = labelAlpha;
  }
}

- (NSInteger)labelAlpha {
  return labelAlpha_;
}

- (void)setColorMap:(NSDictionary *)colorMap {
  colorMap_ = colorMap;
  if (overlayView) {
    overlayView.colorMap = colorMap;
    overlayView.codesAlreadyRead = [NSMutableSet new];
    codesNowReading = [NSMutableDictionary new];
  }
}

- (NSDictionary *)colorMap {
  return colorMap_;
}

- (void)setColorMapForAlreadyRead:(NSDictionary *)colorMapForAlreadyRead {
  colorMapForAlreadyRead_ = colorMapForAlreadyRead;
  if (overlayView) {
    overlayView.colorMapForAlreadyRead = colorMapForAlreadyRead;
    overlayView.codesAlreadyRead = [NSMutableSet new];
    codesNowReading = [NSMutableDictionary new];
  }
}

- (NSDictionary *)colorMapForAlreadyRead {
  return colorMapForAlreadyRead_;
}

- (void)setLabelMap:(NSDictionary *)labelMap {
  labelMap_ = labelMap;
  if (overlayView) {
    overlayView.labelMap = labelMap;
  }
}

- (NSDictionary *)labelMap {
  return labelMap_;
}

- (void)setLabelFontSize:(NSInteger)labelFontSize {
  labelFontSize_ = labelFontSize;
  if (overlayView) {
    overlayView.labelFontSize = labelFontSize;
  }
}

- (NSInteger)labelFontSize {
  return labelFontSize_;
}

- (void)setLabeledOnlyPatternMatched:(BOOL)labeledOnlyPatternMatched {
    labeledOnlyPatternMatched_ = labeledOnlyPatternMatched;
    if (overlayView) {
        overlayView.labeledOnlyPatternMatched = labeledOnlyPatternMatched;
    }
}

- (BOOL)labeledOnlyPatternMatched {
    return labeledOnlyPatternMatched_;
}

- (void)setLabelColor:(NSString *)labelColor {
    labelColor_ = labelColor;
    if (overlayView) {
        overlayView.labelColor = labelColor;
    }
}

- (NSString *)labelColor {
    return labelColor_;
}

- (void)setLabelColorMap:(NSDictionary *)labelColorMap {
    labelColorMap_ = labelColorMap;
    if (overlayView) {
        overlayView.labelColorMap = labelColorMap;
    }
}
- (NSDictionary *)labelColorMap {
    return labelMap_;
}

- (void)setLabelDirection:(NSString *)labelDirection {
    labelDirection_ = labelDirection;
    if (overlayView) {
        overlayView.labelDirection = labelDirection;
    }
}

- (NSString *)labelDirection {
    return labelDirection_;
}

- (NSStringEncoding)currentCharset {
    if ([self.charset isEqualToString:@"SJIS"]) {
        return NSShiftJISStringEncoding;
    }
    return NSUTF8StringEncoding;
}

// Hack。カメラをリスタートさせたい場合はこのプロパティを変化させる。
- (void)setReloadKey:(NSInteger)reloadKey {
  reloadKey_ = reloadKey;
  [self startCamera];
}
- (NSInteger)reloadKey {
  return reloadKey_;
}

@end
