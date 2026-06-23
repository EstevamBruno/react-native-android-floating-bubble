#import "AndroidFloatingBubble.h"

// iOS has no equivalent to Android's system overlay window (the OS does not
// allow floating windows on top of other apps), so the whole module is a no-op
// on iOS. The same JS API is exposed; `isSupported` returns false.
@implementation AndroidFloatingBubble

- (void)show:(double)badgeCount {
  // no-op on iOS
}

- (void)hide {
  // no-op on iOS
}

- (NSNumber *)hasOverlayPermission {
  return @(NO);
}

- (void)requestOverlayPermission {
  // no-op on iOS
}

- (NSNumber *)isSupported {
  return @(NO);
}

- (void)setIcons:(NSString *)bubbleIcon notificationIcon:(NSString *)notificationIcon {
  // no-op on iOS
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeAndroidFloatingBubbleSpecJSI>(params);
}

+ (NSString *)moduleName
{
  return @"AndroidFloatingBubble";
}

@end
