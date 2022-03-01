# Building

## Summary

I recommend the following steps to build the app from source:

1. Clone the project using Git:
```
git clone https://github.com/martinvanzijl/ping-android-app.git
```
1. Install Android Studio.
1. From Android Studio, open and build the project.

## Testing in an Emulator

I recommend testing changes using an emulator within Android studio.

### Known Issues

When testing the "mom-phone" branch in an emulator, with the preference to start the service automatically when the app starts enabled, the service sometimes fails to start. It sometimes fails with an error like the following:

```
2022-02-08 04:53:41.461 24226-24226/? W/Ping: Not allowed to start service Intent { cmp=com.example.myfirstapp/.TextService }: app is in background uid UidRecord{c4035d2 u0a147 TPSL idle procs:1 seq(0,0,0)}
```

It only seems to happen when I start the app from Android Studio in a "cold start": that is, when the emulator is still turned off. It also works fine so far on the real phone.
