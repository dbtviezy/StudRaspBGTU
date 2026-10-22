# Release: R8 minify + shrink. Keep only what the platform needs by name.
# JSON is parsed by hand (JSONObject) — no keep for data models.

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Compose / coroutines / Nearby ship their own consumer rules.
