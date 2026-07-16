package com.ebsoft.board.health;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;  // TODO(human): 필요하면 주석 해제

/**
 * MyBatis 매퍼 인터페이스.
 * - @Mapper 가 붙은 인터페이스를 MyBatis가 스캔해서, SQL을 실행하는 "구현체"를 런타임에 자동 생성한다.
 * - 그래서 우리는 인터페이스 + SQL만 쓰면 되고, 구현 클래스는 안 짜도 된다.
 */
@Mapper
public interface HealthMapper {

    // TODO(human): 이 메서드가 실행할 SELECT 쿼리를 애노테이션으로 붙이세요.
    //
    //   목표   : board 테이블의 전체 행 수(현재 시드 기준 24)를 세어 long 으로 돌려받기
    //   방법   : 메서드 바로 위에 @Select("여기에 SQL") 을 붙인다
    //   힌트   : 전체 개수 세기 = COUNT(*), 대상 테이블 = board
    //   확인법 : 채운 뒤 앱을 켜고 GET http://localhost:8080/api/health 호출 →
    //            { "status": "UP", "boardCount": 24 } 가 나오면 DB 연결 + 쿼리 성공
    //
    // (지금은 애노테이션이 없어서, 이 메서드를 호출하면 MyBatis가 "매핑된 SQL 없음" 에러를 낸다.)
    @Select ("SELECT COUNT(*) FROM board")

    long countBoards();
}
