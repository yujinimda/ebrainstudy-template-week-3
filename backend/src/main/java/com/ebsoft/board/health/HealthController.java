package com.ebsoft.board.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * DB 연결 확인용 헬스체크 엔드포인트.
 * GET /api/health -> HealthMapper가 board 테이블을 세어 그 수를 JSON으로 돌려준다.
 * (이 컨트롤러가 JSON을 "그리는" 게 아니라, Map을 반환하면 Spring이 JSON으로 직렬화해준다.)
 */
@RestController
public class HealthController {

    private final HealthMapper healthMapper;

    // 생성자 주입: Spring이 HealthMapper 구현체(MyBatis가 만들어줌)를 넣어준다.
    public HealthController(HealthMapper healthMapper) {
        this.healthMapper = healthMapper;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        long boardCount = healthMapper.countBoards();
        return Map.of("status", "UP", "boardCount", boardCount);
    }
}
