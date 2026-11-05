package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btr.platform.dto.Views.RankGroup;
import com.btr.platform.dto.Views.RankResult;
import com.btr.platform.dto.Views.RankRow;
import com.btr.platform.entity.Contest;
import com.btr.platform.entity.Signup;
import com.btr.platform.entity.User;
import com.btr.platform.entity.Work;
import com.btr.platform.mapper.ContestMapper;
import com.btr.platform.mapper.SignupMapper;
import com.btr.platform.mapper.UserMapper;
import com.btr.platform.mapper.WorkMapper;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExportService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SignupMapper signupMapper;
    private final WorkMapper workMapper;
    private final ContestMapper contestMapper;
    private final UserMapper userMapper;
    private final RankService rankService;

    public ExportService(SignupMapper signupMapper, WorkMapper workMapper, ContestMapper contestMapper,
                         UserMapper userMapper, RankService rankService) {
        this.signupMapper = signupMapper;
        this.workMapper = workMapper;
        this.contestMapper = contestMapper;
        this.userMapper = userMapper;
        this.rankService = rankService;
    }

    public byte[] exportSignups(Long contestId) {
        QueryWrapper<Signup> query = new QueryWrapper<>();
        if (contestId != null) {
            query.eq("contest_id", contestId);
        }
        query.orderByAsc("created_at");
        List<Signup> signups = signupMapper.selectList(query);
        List<Long> contestIds = signups.stream().map(Signup::getContestId).distinct().toList();
        Map<Long, Contest> contests = contestIds.isEmpty() ? Map.of()
                : contestMapper.selectBatchIds(contestIds).stream()
                        .collect(Collectors.toMap(Contest::getId, Function.identity()));
        Map<Long, User> users = signups.isEmpty() ? Map.of() : userMapper.selectBatchIds(signups.stream()
                        .map(Signup::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Work> works = signups.isEmpty() ? Map.of() : workMapper.selectList(new QueryWrapper<Work>()
                        .in("signup_id", signups.stream().map(Signup::getId).toList())).stream()
                .collect(Collectors.toMap(Work::getSignupId, Function.identity(), (a, b) -> a));

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("报名名单");
            String[] headers = {"序号", "赛事", "姓名", "学校", "联系方式", "账号", "报名状态", "报名时间", "作品名称"};
            header(workbook, sheet, headers);
            int rowIndex = 1;
            for (Signup signup : signups) {
                Row row = sheet.createRow(rowIndex);
                Contest contest = contests.get(signup.getContestId());
                User user = users.get(signup.getUserId());
                Work work = works.get(signup.getId());
                row.createCell(0).setCellValue(rowIndex);
                row.createCell(1).setCellValue(contest == null ? "" : contest.getTitle());
                row.createCell(2).setCellValue(signup.getRealName());
                row.createCell(3).setCellValue(nullSafe(signup.getSchool()));
                row.createCell(4).setCellValue(nullSafe(signup.getContact()));
                row.createCell(5).setCellValue(user == null ? "" : user.getAccount());
                row.createCell(6).setCellValue(statusLabel(signup.getStatus()));
                row.createCell(7).setCellValue(signup.getCreatedAt() == null ? "" : signup.getCreatedAt().format(TIME));
                row.createCell(8).setCellValue(work == null ? "" : work.getTitle());
                rowIndex++;
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new com.btr.platform.common.BizException(500, "导出失败：" + e.getMessage());
        }
    }

    public byte[] exportWinners(Long contestId) {
        RankResult result = rankService.result(contestId, true);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("获奖名单");
            String[] headers = {"奖项", "名次", "作品", "作者", "学校", "平均分", "评审人数"};
            header(workbook, sheet, headers);
            int rowIndex = 1;
            for (RankGroup group : result.getGroups()) {
                for (RankRow row : group.getItems()) {
                    Row sheetRow = sheet.createRow(rowIndex);
                    sheetRow.createCell(0).setCellValue(group.getAwardName());
                    sheetRow.createCell(1).setCellValue(row.getRank());
                    sheetRow.createCell(2).setCellValue(row.getWorkTitle());
                    sheetRow.createCell(3).setCellValue(nullSafe(row.getAuthor()));
                    sheetRow.createCell(4).setCellValue(nullSafe(row.getSchool()));
                    sheetRow.createCell(5).setCellValue(row.getAvgScore() == null ? 0 : row.getAvgScore().doubleValue());
                    sheetRow.createCell(6).setCellValue(row.getScoreCount());
                    rowIndex++;
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new com.btr.platform.common.BizException(500, "导出失败：" + e.getMessage());
        }
    }

    private void header(XSSFWorkbook workbook, Sheet sheet, String[] headers) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            var cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "APPROVED" -> "已报名";
            case "PENDING" -> "待审核";
            case "CANCELED" -> "已取消";
            default -> status;
        };
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
