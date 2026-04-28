# Keep serialization metadata for reflective edge cases.
-keepattributes *Annotation*

# Keep serialized API payload models used by kotlinx.serialization.
-keep class com.easpengren.ctextreader.data.api.** { *; }
-keep class com.easpengren.ctextreader.domain.model.ReaderHistoryEntry { *; }
