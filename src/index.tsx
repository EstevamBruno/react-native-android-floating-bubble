import { Platform } from 'react-native';
import NativeAndroidFloatingBubble from './NativeAndroidFloatingBubble';

/**
 * Sentinel value for {@link FloatingBubble.show}: when passed (or when `show`
 * is called with no argument), the native side keeps the current badge count
 * instead of overwriting it. MUST match KEEP_BADGE_COUNT on the native side.
 */
export const KEEP_BADGE_COUNT = -1;

export interface FloatingBubbleIcons {
  /** Drawable resource name in the host app for the bubble icon. */
  bubbleIcon?: string;
  /** Drawable resource name in the host app for the FGS notification icon. */
  notificationIcon?: string;
}

const isAndroid = Platform.OS === 'android';

export const FloatingBubble = {
  /**
   * Whether the floating bubble is supported on the current platform.
   * `true` on Android, `false` on iOS (no-op).
   */
  isSupported(): boolean {
    return isAndroid && NativeAndroidFloatingBubble.isSupported();
  },

  /**
   * Show the floating bubble.
   * @param badgeCount Badge value to display (>= 0). Omit (or pass
   * {@link KEEP_BADGE_COUNT}) to keep the current count.
   */
  show(badgeCount: number = KEEP_BADGE_COUNT): void {
    if (!isAndroid) return;
    NativeAndroidFloatingBubble.show(badgeCount);
  },

  /** Hide and stop the floating bubble. */
  hide(): void {
    if (!isAndroid) return;
    NativeAndroidFloatingBubble.hide();
  },

  /** Whether the overlay (SYSTEM_ALERT_WINDOW) permission is granted. */
  hasOverlayPermission(): boolean {
    if (!isAndroid) return false;
    return NativeAndroidFloatingBubble.hasOverlayPermission();
  },

  /** Open the system screen to grant the overlay permission. */
  requestOverlayPermission(): void {
    if (!isAndroid) return;
    NativeAndroidFloatingBubble.requestOverlayPermission();
  },

  /**
   * Configure the drawable resource names (from the host app) used for the
   * bubble icon and the FGS notification icon. Falls back to library defaults
   * when omitted or not found.
   */
  setIcons(icons: FloatingBubbleIcons): void {
    if (!isAndroid) return;
    NativeAndroidFloatingBubble.setIcons(
      icons.bubbleIcon ?? '',
      icons.notificationIcon ?? ''
    );
  },
};

export default FloatingBubble;
