package com.btr.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btr.platform.entity.Score;
import com.btr.platform.dto.Views.WorkScore;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
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
            <script>
            SELECT work_id AS workId, AVG(score) AS avgScore, COUNT(*) AS scoreCount
            FROM contest_score
            WHERE work_id IN
            <foreach collection="workIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY work_id
            </script>
            """)
    List<WorkScore> avgByWorkIds(@Param("workIds") Collection<Long> workIds);

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

    @Select("""
            SELECT COUNT(DISTINCT work_id) FROM contest_score
            WHERE contest_id = #{contestId} AND work_id IS NOT NULL
            """)
    long countScoredWorks(@Param("contestId") Long contestId);
}
