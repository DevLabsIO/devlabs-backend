# Redis Caching Issues & Fixes

## Issues Encountered

### 1. **LinkedHashMap Cast Error**

```
LinkedHashMap cannot be cast to DashboardResponse
```

**Cause**: Missing `@class` type metadata in Redis JSON  
**Why**: GenericJackson2JsonRedisSerializer needs `activateDefaultTyping()` to add type info

### 2. **LocalDate Serialization Error**

```
Java 8 date/time type `java.time.LocalDate` not supported by default
```

**Cause**: No-arg `GenericJackson2JsonRedisSerializer()` creates internal ObjectMapper without JavaTimeModule  
**Why**: DTOs contain LocalDate/LocalDateTime fields that need JavaTimeModule

### 3. **Lazy Initialization Error**

```
failed to lazily initialize a collection: could not initialize proxy - no Session
```

**Cause**: Trying to cache JPA entities with lazy collections  
**Fix**: Always return DTOs from @Cacheable methods, never JPA entities

### 4. **PolymorphicTypeValidator Denied Resolution**

```
Could not resolve type id 'com.devlabs...ReviewResponse' as subtype: PolymorphicTypeValidator denied resolution
```

**Cause**: Used `allowIfBaseType("com.devlabs.devlabsbackend")` instead of `allowIfSubType("com.devlabs.devlabsbackend.")`  
**Why**: Base type checks package prefix exactly, subtype checks if class name starts with prefix (needs trailing dot)

---

## The Solution

```kotlin
// CacheConfig.kt
val ptv = BasicPolymorphicTypeValidator.builder()
    .allowIfSubType("com.devlabs.devlabsbackend.")  // ← Trailing dot!
    .allowIfSubType("java.util.")
    .allowIfSubType("java.time.")
    .allowIfSubType("java.sql.")
    .allowIfSubType("kotlin.collections.")
    .build()

val objectMapper = ObjectMapper()
    .registerKotlinModule()              // For Kotlin data classes
    .registerModule(JavaTimeModule())    // For LocalDate/LocalDateTime
    .activateDefaultTyping(              // For @class type info
        ptv,
        ObjectMapper.DefaultTyping.EVERYTHING,  // Kotlin classes are final
        JsonTypeInfo.As.PROPERTY
    )

val serializer = GenericJackson2JsonRedisSerializer(objectMapper)
```

---

## What This Gives You

✅ **@class type metadata** → No more LinkedHashMap casting errors  
✅ **LocalDate/LocalDateTime support** → JavaTimeModule handles Java 8 dates  
✅ **Kotlin data class support** → KotlinModule + EVERYTHING typing  
✅ **Security** → PolymorphicTypeValidator limits allowed classes

---

## Redis Data Format

```json
{
  "@class": "com.devlabs.devlabsbackend.dashboard.domain.dto.AdminDashboardResponse",
  "totalUsers": ["java.lang.Integer", 150],
  "recentUsers": [
    "java.util.ArrayList",
    [
      {
        "@class": "com.devlabs.devlabsbackend.user.domain.dto.UserResponse",
        "createdAt": ["java.sql.Timestamp", 1729372800000]
      }
    ]
  ],
  "upcomingReviews": [
    "java.util.ArrayList",
    [
      {
        "@class": "com.devlabs.devlabsbackend.review.domain.dto.ReviewSummaryResponse",
        "startDate": ["java.time.LocalDate", "2025-10-22"]
      }
    ]
  ]
}
```

---

## Critical: Clear Cache After Config Changes

```bash
# Old cache data is incompatible with new serializer!
redis-cli -a admin -h 172.17.9.74 -p 7777 FLUSHDB
```

Or use the endpoint:

```bash
curl -X DELETE http://localhost:8090/api/dashboard/cache/clear \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

---

## Key Differences

| Method                                              | @class? | LocalDate? | Works?                 |
| --------------------------------------------------- | ------- | ---------- | ---------------------- |
| `GenericJackson2JsonRedisSerializer()` no-arg       | ❌ No   | ❌ No      | ❌ Fails               |
| Custom ObjectMapper without `activateDefaultTyping` | ❌ No   | ✅ Yes     | ❌ LinkedHashMap error |
| Custom ObjectMapper without JavaTimeModule          | ✅ Yes  | ❌ No      | ❌ LocalDate error     |
| **Custom ObjectMapper with BOTH**                   | ✅ Yes  | ✅ Yes     | ✅ **Works!**          |

---

## Best Practices

1. **Never cache JPA entities** → Use DTOs
2. **Always clear cache** after config changes
3. **Use `allowIfSubType` with trailing dot** for package matching
4. **Include `java.sql.` in validator** if using Timestamp
5. **Test with LocalDate fields** to ensure JavaTimeModule works
