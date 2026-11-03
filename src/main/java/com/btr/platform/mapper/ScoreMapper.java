package com.btr.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btr.platform.entity.Score;
import com.btr.platform.dto.Views.WorkScore;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ScoreMapper extends BaseMapper<Score> {

    @Select("""
            SELECT work_id AS workId, AVG(score) AS avgScore, COUNT(*) AS scoreCount
            FROM contest_score
            WHERE contest_id = #{contestId}
            GROUP BY work_id
            """)
    List<WorkScore> avgByContest(@Param("contestId") Long contestId);

    @Select("""
            SELECT signup_id AS signupId, AVG(score) AS avgScore, COUNT(*) AS scoreCount
            FROM contest_score
            WHERE contest_id = #{contestId} AND signup_id IS NOT NULL
            GROUP BY signup_id
            """)
    List<WorkScore> avgByContestSignup(@Param("contestId") Long contestId);

    @Select("""
            SELECT COUNT(DISTINCT judge_id) FROM contest_score WHERE contest_id = #{contestId}
            """)
    long judgeCount(@Param("contestId") Long contestId);
}
