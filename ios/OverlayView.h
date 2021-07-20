//
//  OverlayView.h
//  lixil_qr_code_android
//
//  Created by 田中健策 on 2021/07/19.
//

#ifndef OverlayView_h
#define OverlayView_h
#import <UIKit/UIKit.h>
@interface OverlayView : UIView
@property (nonnull, nonatomic, strong) NSArray<NSValue *> *rects;
@property (nonnull, nonatomic, strong) NSArray<NSString *> *codes;
@property (nonnull, nonatomic, strong) NSMutableSet<NSString *> *codesAlreadyRead;
@property NSInteger overlayAlpha;
@property (nonnull, nonatomic, strong) NSDictionary<NSString *, NSString *> *colorMap;
@property (nullable, nonatomic, strong) NSDictionary<NSString *, NSString *> *colorMapForAlreadyRead;
@property (nullable, nonatomic, strong) NSDictionary<NSString *, NSString *> *labelMap;
@property NSInteger labelFontSize;
@property BOOL labeledOnlyPatternMatched;
@property (nullable, nonatomic, strong) NSString *labelColor;
@property (nullable, nonatomic, strong) NSDictionary<NSString *, NSString *> *labelColorMap;
@property (nonnull, nonatomic, strong) NSString *labelDirection;
@end
#endif /* OverlayView_h */
