# SmarTrans

An AI-powered Android translation app that works with any OpenAI-compatible API. Built with Jetpack Compose and Kotlin.

## What it does

SmarTrans lets you translate text in real time using AI providers like Groq, Gemini, or any custom OpenAI-compatible endpoint. Type or paste text and get translations instantly as you type.

You can add multiple AI providers and API keys, set up custom language profiles with your own instructions, and the app handles rate limits and key rotation automatically. API keys are encrypted on your device using Android Keystore.

## Features

- Real-time translation with debounced input
- Support for any OpenAI-compatible API endpoint
- Multiple API keys per provider with automatic rotation
- Custom language profiles and system prompts
- RTL language support
- Encrypted local key storage
- No third-party HTTP libraries

## Building

Requires Android SDK 35 and JDK 21.

```bash
./gradlew assembleRelease
```

## CI/CD

GitHub Actions builds and releases signed APKs automatically. Pushes to `master` create stable releases; other branches get beta releases.

## License

MIT License. See [LICENSE](LICENSE).
