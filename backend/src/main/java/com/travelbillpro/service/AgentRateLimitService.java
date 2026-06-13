package com.travelbillpro.service;

import com.travelbillpro.config.AgentConfig;
import com.travelbillpro.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * In-memory sliding-window rate limiter for agent chat requests.
 * Tracks per-minute and per-day limits per user using ConcurrentHashMap (no Redis dependency).
 */
@Service
@Slf4j
public class AgentRateLimitService {

    private final AgentConfig agentConfig;

    // userId → queue of request timestamps (epoch millis)
    private final Map<Long, Queue<Long>> minuteWindows = new ConcurrentHashMap<>();
    private final Map<Long, Queue<Long>> dayWindows = new ConcurrentHashMap<>();

    public AgentRateLimitService(AgentConfig agentConfig) {
        this.agentConfig = agentConfig;
    }

    /**
     * Check if the user has exceeded their rate limit.
     * Throws BusinessException with HTTP 429 if exceeded.
     *
     * @param userId the user to check
     */
    public void checkRateLimit(Long userId) {
        long now = System.currentTimeMillis();

        // Check per-minute limit
        Queue<Long> minuteQueue = minuteWindows.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());
        pruneQueue(minuteQueue, now - 60_000L);
        if (minuteQueue.size() >= agentConfig.getRateLimitPerMinute()) {
            log.warn("Agent rate limit exceeded (per-minute) for user {}: {}/{}",
                    userId, minuteQueue.size(), agentConfig.getRateLimitPerMinute());
            throw new BusinessException(
                    "Rate limit exceeded. Maximum " + agentConfig.getRateLimitPerMinute()
                            + " requests per minute. Please try again shortly.",
                    "AGENT_RATE_LIMIT_EXCEEDED",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        // Check per-day limit
        Queue<Long> dayQueue = dayWindows.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());
        pruneQueue(dayQueue, now - 86_400_000L);
        if (dayQueue.size() >= agentConfig.getRateLimitPerDay()) {
            log.warn("Agent rate limit exceeded (per-day) for user {}: {}/{}",
                    userId, dayQueue.size(), agentConfig.getRateLimitPerDay());
            throw new BusinessException(
                    "Daily rate limit exceeded. Maximum " + agentConfig.getRateLimitPerDay()
                            + " requests per day. Please try again tomorrow.",
                    "AGENT_RATE_LIMIT_EXCEEDED",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        // Record this request
        minuteQueue.add(now);
        dayQueue.add(now);
    }

    /**
     * Get the remaining quota for a user.
     *
     * @param userId the user to check
     * @return map with remaining minute and day quotas
     */
    public Map<String, Integer> getRemainingQuota(Long userId) {
        long now = System.currentTimeMillis();

        Queue<Long> minuteQueue = minuteWindows.getOrDefault(userId, new ConcurrentLinkedQueue<>());
        pruneQueue(minuteQueue, now - 60_000L);

        Queue<Long> dayQueue = dayWindows.getOrDefault(userId, new ConcurrentLinkedQueue<>());
        pruneQueue(dayQueue, now - 86_400_000L);

        return Map.of(
                "remaining_per_minute", Math.max(0, agentConfig.getRateLimitPerMinute() - minuteQueue.size()),
                "remaining_per_day", Math.max(0, agentConfig.getRateLimitPerDay() - dayQueue.size())
        );
    }

    /**
     * Periodically clean up expired entries to prevent unbounded memory growth.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes
    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        int cleanedMinute = 0;
        int cleanedDay = 0;

        // Clean minute windows
        Iterator<Map.Entry<Long, Queue<Long>>> minuteIt = minuteWindows.entrySet().iterator();
        while (minuteIt.hasNext()) {
            Map.Entry<Long, Queue<Long>> entry = minuteIt.next();
            pruneQueue(entry.getValue(), now - 60_000L);
            if (entry.getValue().isEmpty()) {
                minuteIt.remove();
                cleanedMinute++;
            }
        }

        // Clean day windows
        Iterator<Map.Entry<Long, Queue<Long>>> dayIt = dayWindows.entrySet().iterator();
        while (dayIt.hasNext()) {
            Map.Entry<Long, Queue<Long>> entry = dayIt.next();
            pruneQueue(entry.getValue(), now - 86_400_000L);
            if (entry.getValue().isEmpty()) {
                dayIt.remove();
                cleanedDay++;
            }
        }

        if (cleanedMinute > 0 || cleanedDay > 0) {
            log.debug("Agent rate limiter cleanup: removed {} minute entries, {} day entries",
                    cleanedMinute, cleanedDay);
        }
    }

    /**
     * Remove all timestamps from the queue that are older than the cutoff.
     */
    private void pruneQueue(Queue<Long> queue, long cutoff) {
        while (!queue.isEmpty() && queue.peek() < cutoff) {
            queue.poll();
        }
    }
}
