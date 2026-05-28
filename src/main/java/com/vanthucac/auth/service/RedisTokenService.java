package com.vanthucac.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class RedisTokenService {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    private static final Duration USER_SESSIONS_TTL = Duration.ofDays(30);

    private static final String KEY_REFRESH_TOKEN = "refresh:token:";
    private static final String KEY_REFRESH_USED = "refresh:used:";
    private static final String KEY_REFRESH_FAMILY = "refresh:family:";
    private static final String KEY_REFRESH_SESSION = "refresh:session:";
    private static final String KEY_USER_SESSIONS = "user:sessions:";

    private final StringRedisTemplate redis;

    public RedisTokenService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void saveRefreshToken(
            String tokenHash,
            Long userId,
            String sessionId,
            String familyId,
            String deviceInfo
    ) {
        var now = Instant.now();
        var expiresAt = now.plus(REFRESH_TOKEN_TTL);

        var tokenData = buildTokenData(userId, sessionId, familyId, now, expiresAt);
        redis.opsForHash().putAll(KEY_REFRESH_TOKEN + tokenHash, tokenData);
        redis.expire(KEY_REFRESH_TOKEN + tokenHash, REFRESH_TOKEN_TTL);

        var sessionData = Map.of(
                "userId", userId.toString(),
                "familyId", familyId,
                "deviceInfo", deviceInfo != null ? deviceInfo : "",
                "createdAt", now.toString(),
                "lastUsedAt", now.toString()
        );
        redis.opsForHash().putAll(KEY_REFRESH_SESSION + sessionId, sessionData);
        redis.expire(KEY_REFRESH_SESSION + sessionId, REFRESH_TOKEN_TTL);

        redis.opsForSet().add(KEY_REFRESH_FAMILY + familyId, tokenHash);
        redis.expire(KEY_REFRESH_FAMILY + familyId, REFRESH_TOKEN_TTL);

        redis.opsForSet().add(KEY_USER_SESSIONS + userId, sessionId);
        redis.expire(KEY_USER_SESSIONS + userId, USER_SESSIONS_TTL);
    }

    private static final String ROTATE_SCRIPT = """
            local oldKey = KEYS[1]
            local usedKey = KEYS[2]
            local newKey = KEYS[3]
            local familyKey = KEYS[4]
            local sessionKey = KEYS[5]
            local userSessionsKey = KEYS[6]
            
            local oldData = redis.call('HGETALL', oldKey)
            if #oldData == 0 then
                return 0
            end
            
            local ttl = %d
            local userSessionsTtl = %d
            
            redis.call('DEL', oldKey)
            
            redis.call('HSET', usedKey,
                'userId', ARGV[1],
                'sessionId', ARGV[2],
                'familyId', ARGV[3],
                'usedAt', ARGV[4]
            )
            redis.call('EXPIRE', usedKey, ttl)
            
            redis.call('HSET', newKey,
                'userId', ARGV[1],
                'sessionId', ARGV[2],
                'familyId', ARGV[3],
                'createdAt', ARGV[4],
                'expiresAt', ARGV[5]
            )
            redis.call('EXPIRE', newKey, ttl)
            
            redis.call('SREM', familyKey, ARGV[6])
            redis.call('SADD', familyKey, ARGV[7])
            redis.call('EXPIRE', familyKey, ttl)
            
            redis.call('HSET', sessionKey, 'lastUsedAt', ARGV[4])
            redis.call('EXPIRE', sessionKey, ttl)
            
            redis.call('EXPIRE', userSessionsKey, userSessionsTtl)
            
            return 1
            """.formatted(
            (int) REFRESH_TOKEN_TTL.toSeconds(),
            (int) USER_SESSIONS_TTL.toSeconds()
    );

    public boolean rotateRefreshToken(
            String oldTokenHash,
            String newTokenHash,
            Long userId,
            String sessionId,
            String familyId
    ) {
        var now = Instant.now();
        var expiresAt = now.plus(REFRESH_TOKEN_TTL);

        var script = RedisScript.of(ROTATE_SCRIPT, Long.class);

        var keys = List.of(
                KEY_REFRESH_TOKEN + oldTokenHash,
                KEY_REFRESH_USED + oldTokenHash,
                KEY_REFRESH_TOKEN + newTokenHash,
                KEY_REFRESH_FAMILY + familyId,
                KEY_REFRESH_SESSION + sessionId,
                KEY_USER_SESSIONS + userId
        );

        var args = new String[]{
                userId.toString(),
                sessionId,
                familyId,
                now.toString(),
                expiresAt.toString(),
                oldTokenHash,
                newTokenHash
        };

        var result = redis.execute(script, keys, (Object[]) args);
        return Long.valueOf(1L).equals(result);
    }

    public Optional<RefreshTokenData> findActiveToken(String tokenHash) {
        var data = redis.opsForHash().entries(KEY_REFRESH_TOKEN + tokenHash);
        if (data.isEmpty()) return Optional.empty();

        return Optional.of(new RefreshTokenData(
                data.get("userId").toString(),
                data.get("sessionId").toString(),
                data.get("familyId").toString()
        ));
    }

    public boolean isTokenUsed(String tokenHash) {
        return Objects.requireNonNullElse(redis.hasKey(KEY_REFRESH_USED + tokenHash), false);
    }

    public void revokeSession(String sessionId, String tokenHash, String familyId, Long userId) {
        redis.delete(KEY_REFRESH_TOKEN + tokenHash);
        redis.delete(KEY_REFRESH_SESSION + sessionId);
        redis.opsForSet().remove(KEY_REFRESH_FAMILY + familyId, tokenHash);
        redis.opsForSet().remove(KEY_USER_SESSIONS + userId, sessionId);
    }

    public void revokeFamilyAndSession(String familyId, String sessionId, Long userId) {
        var activeHashes = redis.opsForSet().members(KEY_REFRESH_FAMILY + familyId);
        if (activeHashes != null) {
            activeHashes.forEach(hash -> redis.delete(KEY_REFRESH_TOKEN + hash));
        }
        redis.delete(KEY_REFRESH_FAMILY + familyId);
        redis.delete(KEY_REFRESH_SESSION + sessionId);
        redis.opsForSet().remove(KEY_USER_SESSIONS + userId, sessionId);
    }

    public void revokeAllSessions(Long userId) {
        var sessionIds = redis.opsForSet().members(KEY_USER_SESSIONS + userId);
        if (sessionIds == null || sessionIds.isEmpty()) return;

        for (var sessionId : sessionIds) {
            var sessionData = redis.opsForHash().entries(KEY_REFRESH_SESSION + sessionId);
            if (sessionData.isEmpty()) continue;

            var familyId = sessionData.get("familyId").toString();
            var activeHashes = redis.opsForSet().members(KEY_REFRESH_FAMILY + familyId);

            if (activeHashes != null) {
                activeHashes.forEach(hash -> redis.delete(KEY_REFRESH_TOKEN + hash));
            }

            redis.delete(KEY_REFRESH_FAMILY + familyId);
            redis.delete(KEY_REFRESH_SESSION + sessionId);
        }

        redis.delete(KEY_USER_SESSIONS + userId);
    }

    private Map<String, String> buildTokenData(
            Long userId, String sessionId, String familyId,
            Instant createdAt, Instant expiresAt
    ) {
        return Map.of(
                "userId", userId.toString(),
                "sessionId", sessionId,
                "familyId", familyId,
                "createdAt", createdAt.toString(),
                "expiresAt", expiresAt.toString()
        );
    }

    public record RefreshTokenData(
            String userId,
            String sessionId,
            String familyId
    ) {
    }
}