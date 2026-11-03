package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btr.platform.common.BizException;
import com.btr.platform.common.PageResult;
import com.btr.platform.dto.Requests.NoticeReq;
import com.btr.platform.dto.Views.NoticeReadView;
import com.btr.platform.dto.Views.NoticeReads;
import com.btr.platform.entity.Notice;
import com.btr.platform.entity.NoticeRead;
import com.btr.platform.entity.User;
import com.btr.platform.mapper.NoticeMapper;
import com.btr.platform.mapper.NoticeReadMapper;
import com.btr.platform.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final NoticeReadMapper noticeReadMapper;
    private final UserMapper userMapper;

    public NoticeService(NoticeMapper noticeMapper, NoticeReadMapper noticeReadMapper, UserMapper userMapper) {
        this.noticeMapper = noticeMapper;
        this.noticeReadMapper = noticeReadMapper;
        this.userMapper = userMapper;
    }

    public PageResult<Notice> list(String keyword, long page, long size) {
        QueryWrapper<Notice> query = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            query.and(w -> w.like("title", keyword).or().like("content", keyword));
        }
        query.orderByDesc("pinned").orderByDesc("published_at");
        Page<Notice> result = noticeMapper.selectPage(new Page<>(page, size), query);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public List<Notice> recent(int limit) {
        return noticeMapper.selectList(new QueryWrapper<Notice>()
                .orderByDesc("pinned").orderByDesc("published_at").last("limit " + limit));
    }

    public Notice get(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BizException(404, "公告不存在");
        }
        return notice;
    }

    @Transactional
    public Notice create(NoticeReq req) {
        Notice notice = new Notice();
        notice.setTitle(req.title());
        notice.setContent(req.content());
        notice.setPinned(Boolean.TRUE.equals(req.pinned()));
        notice.setPublishedAt(LocalDateTime.now());
        noticeMapper.insert(notice);
        return notice;
    }

    @Transactional
    public Notice update(Long id, NoticeReq req) {
        Notice notice = get(id);
        notice.setTitle(req.title());
        notice.setContent(req.content());
        notice.setPinned(Boolean.TRUE.equals(req.pinned()));
        noticeMapper.updateById(notice);
        return notice;
    }

    @Transactional
    public void delete(Long id) {
        noticeMapper.deleteById(id);
        noticeReadMapper.delete(new QueryWrapper<NoticeRead>().eq("notice_id", id));
    }

    /** 记录某用户阅读公告（幂等，仅记录一次） */
    @Transactional
    public void recordRead(Long noticeId, Long userId) {
        if (noticeId == null || userId == null) return;
        Long exists = noticeReadMapper.selectCount(new QueryWrapper<NoticeRead>()
                .eq("notice_id", noticeId).eq("user_id", userId));
        if (exists != null && exists > 0) return;
        NoticeRead read = new NoticeRead();
        read.setNoticeId(noticeId);
        read.setUserId(userId);
        read.setReadAt(LocalDateTime.now());
        noticeReadMapper.insert(read);
    }

    /** 公告阅读统计与明细（管理端） */
    public NoticeReads reads(Long noticeId) {
        List<NoticeRead> list = noticeReadMapper.selectList(new QueryWrapper<NoticeRead>()
                .eq("notice_id", noticeId).orderByDesc("read_at"));
        Map<Long, User> users = list.stream().map(NoticeRead::getUserId).distinct()
                .collect(Collectors.toMap(Function.identity(), id -> {
                    User u = userMapper.selectById(id);
                    return u == null ? new User() : u;
                }, (a, b) -> a));
        NoticeReads out = new NoticeReads();
        out.setTotal((long) list.size());
        out.setList(list.stream().map(read -> {
            NoticeReadView view = new NoticeReadView();
            view.setUserId(read.getUserId());
            view.setReadAt(read.getReadAt());
            User user = users.get(read.getUserId());
            if (user != null) {
                view.setAccount(user.getAccount());
                view.setNickname(user.getNickname());
                view.setRole(user.getRole());
            }
            return view;
        }).toList());
        return out;
    }
}
