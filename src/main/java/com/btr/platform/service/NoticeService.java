package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btr.platform.common.BizException;
import com.btr.platform.common.PageResult;
import com.btr.platform.dto.Requests.NoticeReq;
import com.btr.platform.entity.Notice;
import com.btr.platform.mapper.NoticeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;

    public NoticeService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
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
    }
}
