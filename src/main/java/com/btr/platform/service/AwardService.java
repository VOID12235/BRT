package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btr.platform.common.BizException;
import com.btr.platform.dto.Views.AwardItem;
import com.btr.platform.dto.Views.MyAwards;
import com.btr.platform.entity.Award;
import com.btr.platform.entity.Contest;
import com.btr.platform.entity.User;
import com.btr.platform.entity.Winner;
import com.btr.platform.entity.Work;
import com.btr.platform.mapper.AwardMapper;
import com.btr.platform.mapper.ContestMapper;
import com.btr.platform.mapper.WinnerMapper;
import com.btr.platform.mapper.WorkMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 选手获奖查询与获奖证书 PDF 生成。
 * 数据源为公示时固化的 contest_winner 快照，保证与实际榜单一致。
 */
@Slf4j
@Service
public class AwardService {

    private final WinnerMapper winnerMapper;
    private final WorkMapper workMapper;
    private final ContestMapper contestMapper;
    private final AwardMapper awardMapper;
    private final ConfigService configService;

    public AwardService(WinnerMapper winnerMapper, WorkMapper workMapper,
                        ContestMapper contestMapper, AwardMapper awardMapper,
                        ConfigService configService) {
        this.winnerMapper = winnerMapper;
        this.workMapper = workMapper;
        this.contestMapper = contestMapper;
        this.awardMapper = awardMapper;
        this.configService = configService;
    }

    /** 当前用户的获奖明细（来自公示固化的 winner 快照） */
    public MyAwards listMyAwards(Long userId) {
        List<Winner> winners = winnerMapper.selectList(new QueryWrapper<Winner>()
                .eq("published", true).orderByDesc("created_at"));
        List<AwardItem> items = toItems(winners, userId);
        MyAwards result = new MyAwards();
        result.setTotal(items.size());
        result.setItems(items);
        return result;
    }

    /** 校验某赛事的获奖归属（防止越权下载他人证书） */
    public AwardItem myAward(Long userId, Long contestId) {
        List<Winner> winners = winnerMapper.selectList(new QueryWrapper<Winner>()
                .eq("contest_id", contestId).eq("published", true));
        List<AwardItem> items = toItems(winners, userId);
        if (items.isEmpty()) {
            throw new BizException(404, "未找到你的获奖记录");
        }
        return items.get(0);
    }

    private List<AwardItem> toItems(List<Winner> winners, Long userId) {
        if (winners.isEmpty()) {
            return List.of();
        }
        List<Long> workIds = winners.stream().map(Winner::getWorkId).distinct().toList();
        Map<Long, Work> works = workMapper.selectBatchIds(workIds).stream()
                .collect(Collectors.toMap(Work::getId, Function.identity(), (a, b) -> a));
        List<Long> contestIds = winners.stream().map(Winner::getContestId).distinct().toList();
        Map<Long, Contest> contests = contestMapper.selectBatchIds(contestIds).stream()
                .collect(Collectors.toMap(Contest::getId, Function.identity(), (a, b) -> a));
        List<Long> awardIds = winners.stream().map(Winner::getAwardId).distinct().toList();
        Map<Long, Award> awards = awardMapper.selectBatchIds(awardIds).stream()
                .collect(Collectors.toMap(Award::getId, Function.identity(), (a, b) -> a));

        List<AwardItem> items = new ArrayList<>();
        for (Winner winner : winners) {
            Work work = works.get(winner.getWorkId());
            if (work == null || !userId.equals(work.getUserId())) {
                continue;
            }
            AwardItem item = new AwardItem();
            item.setContestId(winner.getContestId());
            Contest contest = contests.get(winner.getContestId());
            item.setContestTitle(contest == null ? "未知赛事" : contest.getTitle());
            item.setAwardId(winner.getAwardId());
            Award award = awards.get(winner.getAwardId());
            item.setAwardName(award == null ? "获奖" : award.getName());
            item.setPrizeAmount(award == null ? null : award.getPrizeAmount());
            item.setRankNo(winner.getRankNo());
            item.setWorkId(work.getId());
            item.setWorkTitle(work.getTitle());
            item.setPublishedAt(winner.getCreatedAt());
            items.add(item);
        }
        return items;
    }

    /* ------------------------- 获奖证书 PDF ------------------------- */

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    /** 生成获奖证书 PDF（A4 横向）。证书文案来自后台「通知模板-获奖证书」，每行一段，支持 # 标题 / ## 强调。 */
    public byte[] buildCertificatePdf(AwardItem item, User user) {
        try {
            BaseFont baseFont = chineseFont();
            Font titleFont = new Font(baseFont, 26, Font.BOLD);
            Font emphasisFont = new Font(baseFont, 20, Font.BOLD);
            Font bodyFont = new Font(baseFont, 15, Font.NORMAL);

            String template = configService.getTemplate(ConfigService.KEY_TPL_CERT,
                    ConfigService.DEFAULT_TPL_CERT);
            java.util.Map<String, String> vars = new java.util.HashMap<>();
            vars.put("nickname", user == null ? "选手" : safe(user.getNickname()));
            vars.put("contestTitle", safe(item.getContestTitle()));
            vars.put("workTitle", safe(item.getWorkTitle()));
            vars.put("awardName", safe(item.getAwardName()));
            vars.put("rankNo", item.getRankNo() == null ? "" : String.valueOf(item.getRankNo()));
            vars.put("prizeAmount", item.getPrizeAmount() == null ? ""
                    : item.getPrizeAmount().stripTrailingZeros().toPlainString());
            vars.put("date", item.getPublishedAt() == null ? "—"
                    : item.getPublishedAt().format(DATE_FMT));
            String rendered = ConfigService.render(template, vars);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 44, 44, 44, 44);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // 金色边框
            Rectangle border = new Rectangle(24, 24,
                    PageSize.A4.rotate().getWidth() - 24, PageSize.A4.rotate().getHeight() - 24);
            border.setBorder(Rectangle.BOX);
            border.setBorderWidth(2.5f);
            border.setBorderColor(new Color(0xE8, 0xA3, 0x3D));
            doc.add(border);

            boolean first = true;
            for (String rawLine : rendered.split("\\R")) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }
                Font font = bodyFont;
                if (line.startsWith("##")) {
                    line = line.substring(2).trim();
                    font = emphasisFont;
                } else if (line.startsWith("#")) {
                    line = line.substring(1).trim();
                    font = titleFont;
                }
                if (line.isEmpty()) {
                    continue;
                }
                Paragraph paragraph = new Paragraph(line, font);
                paragraph.setAlignment(Element.ALIGN_CENTER);
                paragraph.setSpacingBefore(first ? 22 : 12);
                paragraph.setSpacingAfter(6);
                doc.add(paragraph);
                first = false;
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("生成获奖证书 PDF 失败：{}", e.getMessage(), e);
            if (e instanceof BizException) {
                throw (BizException) e;
            }
            throw new BizException(500, "生成获奖证书失败：" + e.getMessage());
        }
    }

    private String safe(String value) {
        return value == null ? "—" : value;
    }

    /** 解析支持中文的字体：优先系统黑体，其次宋体/雅黑，最后 Linux 常见 CJK 字体 */
    private BaseFont chineseFont() throws Exception {
        String[] candidates = {
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/simsun.ttc,0",
                "C:/Windows/Fonts/msyh.ttc,0",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc,0",
                "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf",
        };
        for (String candidate : candidates) {
            String path = candidate.split(",")[0];
            if (new File(path).exists()) {
                return BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            }
        }
        throw new BizException(500, "服务器缺少中文字体，无法生成获奖证书 PDF");
    }
}
