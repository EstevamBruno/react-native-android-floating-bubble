import { useCallback, useEffect, useState } from 'react';
import { AppState, Pressable, StyleSheet, Text, View } from 'react-native';
import { FloatingBubble } from 'react-native-android-floating-bubble';

export default function App() {
  const [hasPermission, setHasPermission] = useState(false);
  const [badge, setBadge] = useState(0);

  const refreshPermission = useCallback(() => {
    setHasPermission(FloatingBubble.hasOverlayPermission());
  }, []);

  useEffect(() => {
    refreshPermission();
    // Re-check when returning from the system permission screen.
    const sub = AppState.addEventListener('change', (state) => {
      if (state === 'active') refreshPermission();
    });
    return () => sub.remove();
  }, [refreshPermission]);

  const showWithBadge = (count: number) => {
    setBadge(count);
    FloatingBubble.show(count);
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}> React-Native Android Floating Bubble</Text>
      <Text style={styles.status}>
        supported: {String(FloatingBubble.isSupported())}
        {'  '}overlay: {String(hasPermission)}
      </Text>

      {!hasPermission && (
        <Button
          label="Request overlay permission"
          onPress={() => FloatingBubble.requestOverlayPermission()}
        />
      )}

      <Button label="Show (badge 3)" onPress={() => showWithBadge(3)} />
      <Button label="Show (badge 12)" onPress={() => showWithBadge(12)} />
      <Button
        label="Show (badge 150 → 99+)"
        onPress={() => showWithBadge(150)}
      />
      <Button
        label="Show (keep current count)"
        onPress={() => FloatingBubble.show()}
      />
      <Button label="Hide" onPress={() => FloatingBubble.hide()} />

      <Text style={styles.hint}>last badge set: {badge}</Text>
      <Text style={styles.hint}>
        Send the app to background to see the bubble.
      </Text>
    </View>
  );
}

function Button({ label, onPress }: { label: string; onPress: () => void }) {
  return (
    <Pressable
      style={({ pressed }) => [styles.button, pressed && styles.buttonPressed]}
      onPress={onPress}
    >
      <Text style={styles.buttonText}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
    gap: 10,
  },
  title: {
    fontSize: 22,
    fontWeight: '700',
    marginBottom: 4,
    textAlign: 'center',
  },
  status: { fontSize: 13, color: '#555', marginBottom: 12 },
  button: {
    backgroundColor: '#1E88E5',
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 8,
    minWidth: 240,
    alignItems: 'center',
  },
  buttonPressed: { opacity: 0.7 },
  buttonText: { color: '#fff', fontWeight: '600' },
  hint: { fontSize: 12, color: '#888', marginTop: 8, textAlign: 'center' },
});
