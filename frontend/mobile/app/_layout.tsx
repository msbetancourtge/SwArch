// Polyfill TextEncoder/TextDecoder for @stomp/stompjs in React Native.
// Hermes/JSC do not expose these globals; stompjs v7 requires them for
// binary frame encoding (forceBinaryWSFrames: true).
import { TextEncoder, TextDecoder } from 'text-encoding';
if (typeof globalThis.TextEncoder === 'undefined') {
    (globalThis as any).TextEncoder = TextEncoder;
}
if (typeof globalThis.TextDecoder === 'undefined') {
    (globalThis as any).TextDecoder = TextDecoder;
}

import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { SplashScreen, Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useColorScheme } from 'react-native';
import { useFonts } from 'expo-font'
import 'react-native-reanimated';
import { useEffect } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

SplashScreen.preventAutoHideAsync();

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    }
  }
})


export default function RootLayout() {
  const colorScheme = useColorScheme();

  const [loaded] = useFonts({
      KanitRegular: require('../assets/fonts/Kanit-Regular.ttf'),
      KanitBold: require('../assets/fonts/Kanit-Bold.ttf'),
      KanitThin: require('../assets/fonts/Kanit-Thin.ttf'),
  })

  useEffect(() => {
    if (loaded) {
      SplashScreen.hideAsync();
    }
  }, [loaded]);

  if (!loaded) return null;

  return (

    <QueryClientProvider client={ queryClient }>
      <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
        <Stack
          screenOptions={
            {headerShown: false,}
          }
        >
          {/* <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
          <Stack.Screen name="modal" options={{ presentation: 'modal', title: 'Modal' }} /> */}
        </Stack>
        <StatusBar style="auto" />
      </ThemeProvider>
    </QueryClientProvider>
    
  );
}
