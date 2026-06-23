import { TurboModuleRegistry, type TurboModule } from 'react-native';

export interface Spec extends TurboModule {
  /**
   * Show the floating bubble. Pass a badge count >= 0 to set it, or
   * KEEP_BADGE_COUNT (-1) to keep the current count.
   */
  show(badgeCount: number): void;
  /** Hide and stop the floating bubble. */
  hide(): void;
  /** Whether the overlay (SYSTEM_ALERT_WINDOW) permission is granted. */
  hasOverlayPermission(): boolean;
  /** Open the system screen to grant the overlay permission. */
  requestOverlayPermission(): void;
  /** Whether the floating bubble is supported on this platform (Android only). */
  isSupported(): boolean;
  /**
   * Configure the drawable resource names (looked up in the host app's
   * resources) used for the bubble icon and the foreground-service
   * notification icon. Empty strings fall back to the library defaults.
   */
  setIcons(bubbleIcon: string, notificationIcon: string): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('AndroidFloatingBubble');
