//
//  OverlayView.m
//  lixil_qr_code_android
//
//  Created by 田中健策 on 2021/07/19.
//

#import "OverlayView.h"

@implementation OverlayView

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
    self.contentMode = UIViewContentModeRedraw;
    self.rects = [NSArray new];
    self.codes = [NSArray new];
    self.codesAlreadyRead = [NSMutableSet new];
    self.overlayAlpha = 100;
    self.labelAlpha = -1;
    self.colorMap = [NSDictionary new];
    self.colorMapForAlreadyRead = nil;
    self.labelMap = [NSDictionary new];
    self.labelFontSize = 40;
    self.labeledOnlyPatternMatched = YES;
    self.labelColorMap = nil;
    self.labelColor = nil;
    self.labelDirection = @"right";
}

- (void)drawRect:(CGRect)rect {
    NSDate *now = [NSDate new];
    [self.rects enumerateObjectsUsingBlock:^(NSValue *value, NSUInteger idx, BOOL *stop) {
        CGRect currentRect = [value CGRectValue];
        NSString *code = self.codes[idx];
        [self drawRect:currentRect withCode:code at:now];
    }];
}

- (void)drawRect:(CGRect)rect withCode:(NSString *)code at:(NSDate *)now {
    if ([self.codesAlreadyRead containsObject:code]) {
        if (self.colorMapForAlreadyRead) {
            [self drawRect:rect withCode:code at:now withColorMap:self.colorMapForAlreadyRead];
            return;
        }
    }
    [self drawRect:rect withCode:code at:now withColorMap:self.colorMap];
}

- (void) drawRect:(CGRect)rect withCode:(NSString *)code at:(NSDate *)now withColorMap:(NSDictionary *)colorMap {
    __block BOOL found = false;
    NSInteger actualLabelAlpha = self.labelAlpha > 0 ? self.labelAlpha : self.overlayAlpha;
    UIBezierPath *rectangle = [UIBezierPath bezierPathWithRect:rect];
    [colorMap enumerateKeysAndObjectsUsingBlock:^(NSString * pattern, NSString *colorCode, BOOL *stop) {
        NSError *error = nil;
        NSRegularExpression *regexp = [NSRegularExpression regularExpressionWithPattern:pattern options:0 error:&error];
        if (error == nil) {
            NSArray *matches = [regexp matchesInString:code options:0 range:NSMakeRange(0, code.length)];
            if (matches.count > 0) {
                UIColor *color = [self colorFromHexString:colorCode withAlpha:self.overlayAlpha];
                [color setFill];
                [rectangle fill];
                NSString *label = self.labelMap[pattern];
                if (!label && !self.labeledOnlyPatternMatched) {
                    label = [self getValueOfCode:code fromPatternDictionary:self.labelMap];
                }
                if (label) {
                    if (self.labelColor != nil) {
                        color = [self colorFromHexString:self.labelColor withAlpha:actualLabelAlpha];
                    } else if (self.labelColorMap) {
                        NSString *colorCode = [self getValueOfCode:code fromPatternDictionary:self.labelColorMap];
                        if (colorCode) {
                            color = [self colorFromHexString:colorCode withAlpha:actualLabelAlpha];
                        }
                    }
                    [self drawLabel:label ByRect:rect withColor:color];
                }
                found = true;
                *stop = true;
            }
        }
    }];
    if (!found) {
        UIColor *color = [UIColor colorWithRed:1.0 green:0 blue:0 alpha:(CGFloat)self.overlayAlpha/(255.0)];
        [color setFill];
        [rectangle fill];
        if (!self.labeledOnlyPatternMatched) {
            NSString *label = [self getValueOfCode:code fromPatternDictionary:self.labelMap];
            if (!label) {
                return;
            }
            NSString *colorCode = [self getValueOfCode:code fromPatternDictionary:self.labelColorMap];
            if (colorCode) {
                color = [self colorFromHexString:colorCode withAlpha:actualLabelAlpha];
            }
            [self drawLabel:label ByRect:rect withColor:color];
        }
    }
}

- (void)drawLabel: (NSString *)label ByRect:(CGRect)rect withColor:(UIColor *)color {
    UIFont *font = [UIFont boldSystemFontOfSize: (CGFloat)self.labelFontSize];
    
    if ([self.labelDirection isEqualToString:@"right"]) {
        CGRect rect2 = CGRectMake(rect.origin.x + rect.size.width, rect.origin.y, self.frame.size.width, self.frame.size.width);
        [label drawInRect:rect2 withAttributes:@{NSFontAttributeName: font, NSForegroundColorAttributeName: color}];
    } else if ([self.labelDirection isEqualToString:@"bottom"]) {
        CGRect rect2 = CGRectMake(rect.origin.x, rect.origin.y + rect.size.height, self.frame.size.width, self.frame.size.width);
        [label drawInRect:rect2 withAttributes:@{NSFontAttributeName: font, NSForegroundColorAttributeName: color}];
    }
}

- (NSString *)getValueOfCode:(NSString *)code fromPatternDictionary:(NSDictionary<NSString *, NSString *> *)patternDictionary {
    __block NSString *found = nil;
    
    [patternDictionary enumerateKeysAndObjectsUsingBlock:^(NSString *pattern, NSString *value, BOOL* stop) {
        NSError *error = nil;
        NSRegularExpression *regexp = [NSRegularExpression regularExpressionWithPattern:pattern options:0 error:&error];
        if (error == nil) {
            NSArray *matches = [regexp matchesInString:code options:0 range:NSMakeRange(0, code.length)];
            if (matches.count > 0) {
                found = value;
                *stop = YES;
            }
        }
    }];
    return found;
}


- (UIColor *)colorFromHexString:(NSString *)hexString withAlpha:(NSInteger)alpha {
    unsigned rgbValue = 0;
    NSScanner *scanner = [NSScanner scannerWithString:hexString];
    [scanner setScanLocation:1]; // bypass '#' character
    [scanner scanHexInt:&rgbValue];
    return [UIColor colorWithRed:((rgbValue & 0xFF0000) >> 16)/255.0 green:((rgbValue & 0xFF00) >> 8)/255.0 blue:(rgbValue & 0xFF)/255.0 alpha: (CGFloat)alpha / 255.0];
}

@end
