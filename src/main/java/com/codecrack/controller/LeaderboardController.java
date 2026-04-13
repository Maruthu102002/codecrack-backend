package com.codecrack.controller;

import com.codecrack.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
@Slf4j
public class LeaderboardController {

    private final RedisService redisService;

    // GET top 10 users
    @GetMapping
    public ResponseEntity<?> getLeaderboard() {
        Set<ZSetOperations.TypedTuple<Object>> topUsers = redisService.getTopUsers(10);

        List<Map<String, Object>> leaderboard = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<Object> entry : topUsers) {
            Map<String, Object> item = new HashMap<>();
            item.put("rank", rank++);
            item.put("userId", entry.getValue());
            item.put("score", entry.getScore());
            leaderboard.add(item);
        }

        return ResponseEntity.ok(Map.of(
                "leaderboard", leaderboard,
                "total", leaderboard.size()
        ));
    }

    // GET user rank
    @GetMapping("/rank/{userId}")
    public ResponseEntity<?> getUserRank(@PathVariable String userId) {
        Long rank = redisService.getUserRank(userId);
        if (rank == null) {
            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "rank", "Not ranked yet"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "rank", rank
        ));
    }
}