package com.cityguard.config.service;

import com.cityguard.common.exception.BusinessException;
import com.cityguard.config.entity.CaseStandard;
import com.cityguard.config.entity.CategoryBig;
import com.cityguard.config.entity.CategorySmall;
import com.cityguard.config.mapper.CaseStandardMapper;
import com.cityguard.config.mapper.CategoryBigMapper;
import com.cityguard.config.mapper.CategorySmallMapper;
import com.cityguard.config.mapper.StandardCatalogCleanupMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 按根目录规范文档解析 muban.xlsx（部件 / 事件），全量替换对应 {@code category_type} 下大类、小类与立案标准。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MubanStandardImportService {

    private static final String[] HEADER = {
            "大类代码", "大类名称", "小类代码", "小类名称", "处置单位", "主管部门", "立案条件", "结案条件", "处置时限"
    };

    private static final Pattern PAT_URGENT_WORK_DAY = Pattern.compile("(\\d+)\\s*紧急工作日");
    private static final Pattern PAT_URGENT_WORK_HOUR = Pattern.compile("(\\d+)\\s*紧急工作时");
    private static final Pattern PAT_WORK_DAY = Pattern.compile("(\\d+)\\s*工作日");
    private static final Pattern PAT_DAY = Pattern.compile("(\\d+)\\s*天");
    private static final Pattern PAT_HOUR = Pattern.compile("(\\d+)\\s*小时");

    private final StandardCatalogCleanupMapper cleanupMapper;
    private final CategoryBigMapper categoryBigMapper;
    private final CategorySmallMapper categorySmallMapper;
    private final CaseStandardMapper caseStandardMapper;

    public record ImportSummary(String sheetName, String categoryType, int bigCount, int smallCount, int standardCount) {}

    public record ImportResult(List<ImportSummary> summaries) {}

    @Transactional(rollbackFor = Exception.class)
    public ImportResult importWorkbook(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 Excel 文件");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessException("仅支持 .xlsx 格式");
        }
        List<ImportSummary> summaries = new ArrayList<>();
        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                String sheetName = sheet.getSheetName() == null ? "" : sheet.getSheetName().trim();
                String categoryType = mapSheetToCategoryType(sheetName);
                if (categoryType == null) {
                    log.info("跳过未识别的工作表: {}", sheetName);
                    continue;
                }
                purgeCategoryType(categoryType);
                ImportSummary s = importSheet(sheet, categoryType, sheetName);
                summaries.add(s);
            }
        }
        if (summaries.isEmpty()) {
            throw new BusinessException("未找到可导入的工作表（需包含「部件」或「事件」）");
        }
        return new ImportResult(summaries);
    }

    private void purgeCategoryType(String categoryType) {
        cleanupMapper.deleteResponsibilityByCategoryType(categoryType);
        cleanupMapper.deleteExtendByCategoryType(categoryType);
        cleanupMapper.deleteCaseStandardByCategoryType(categoryType);
        cleanupMapper.deleteCategorySmallByCategoryType(categoryType);
        cleanupMapper.deleteCategoryBigByCategoryType(categoryType);
    }

    private String mapSheetToCategoryType(String sheetName) {
        if ("部件".equals(sheetName)) {
            return "component";
        }
        if ("事件".equals(sheetName)) {
            return "event";
        }
        return null;
    }

    private ImportSummary importSheet(Sheet sheet, String categoryType, String sheetLabel) {
        DataFormatter fmt = new DataFormatter();
        int last = sheet.getLastRowNum();
        if (last < 1) {
            throw new BusinessException("工作表「" + sheetLabel + "」无表头");
        }
        Row headerRow = sheet.getRow(1);
        if (headerRow == null) {
            throw new BusinessException("工作表「" + sheetLabel + "」第2行应为表头");
        }
        for (int c = 0; c < HEADER.length; c++) {
            String cell = trim(fmt.formatCellValue(headerRow.getCell(c)));
            if (!HEADER[c].equals(cell)) {
                throw new BusinessException("工作表「" + sheetLabel + "」表头第" + (c + 1) + "列应为「" + HEADER[c] + "」，实际为「" + cell + "」");
            }
        }

        int bigCount = 0;
        int smallCount = 0;
        int standardCount = 0;

        String curBigCode = null;
        String curBigName = null;
        Long curBigId = null;
        int bigSort = 0;

        String curSmallCode = null;
        String curSmallName = null;
        String curHandle = null;
        String curSupervise = null;
        Long curSmallId = null;
        int smallSort = 0;
        int stdSeq = 0;

        for (int r = 2; r <= last; r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowEmpty(row)) {
                continue;
            }
            String col0 = trim(fmt.formatCellValue(row.getCell(0)));
            String col1 = trim(fmt.formatCellValue(row.getCell(1)));
            String col2 = trim(fmt.formatCellValue(row.getCell(2)));
            String col3 = trim(fmt.formatCellValue(row.getCell(3)));
            String col4 = trim(fmt.formatCellValue(row.getCell(4)));
            String col5 = trim(fmt.formatCellValue(row.getCell(5)));
            String col6 = trim(fmt.formatCellValue(row.getCell(6)));
            String col7 = trim(fmt.formatCellValue(row.getCell(7)));
            String col8 = trim(fmt.formatCellValue(row.getCell(8)));

            if (!col0.isEmpty() || !col1.isEmpty()) {
                if (col0.isEmpty() || col1.isEmpty()) {
                    throw new BusinessException(sheetLabel + " 第" + (r + 1) + "行：大类代码与名称须同时填写");
                }
                curBigCode = normalizeCode(col0, "大类代码", r + 1);
                curBigName = col1;
                CategoryBig big = new CategoryBig();
                big.setBigCode(curBigCode);
                big.setBigName(curBigName);
                big.setCategoryType(categoryType);
                big.setSortOrder(++bigSort);
                big.setStatus(1);
                big.setDeleted(0);
                categoryBigMapper.insert(big);
                curBigId = big.getId();
                bigCount++;
                curSmallId = null;
                curSmallCode = null;
                curSmallName = null;
                curHandle = null;
                curSupervise = null;
                smallSort = 0;
                stdSeq = 0;
            }

            if (curBigId == null) {
                throw new BusinessException(sheetLabel + " 第" + (r + 1) + "行：须先出现大类行");
            }

            if (!col2.isEmpty() || !col3.isEmpty()) {
                if (col2.isEmpty() || col3.isEmpty()) {
                    throw new BusinessException(sheetLabel + " 第" + (r + 1) + "行：小类代码与名称须同时填写");
                }
                if (col4.isEmpty() || col5.isEmpty()) {
                    throw new BusinessException(sheetLabel + " 第" + (r + 1) + "行：新小类须填写处置单位、主管部门");
                }
                curSmallCode = normalizeCode(col2, "小类代码", r + 1);
                curSmallName = col3;
                curHandle = col4;
                curSupervise = col5;

                CategorySmall sm = new CategorySmall();
                sm.setBigId(curBigId);
                sm.setBigCode(curBigCode);
                sm.setSmallCode(curSmallCode);
                sm.setSmallName(curSmallName);
                sm.setCategoryType(categoryType);
                sm.setFullCode(buildFullCode(curBigCode, curSmallCode));
                sm.setResponsibilitySubject(curHandle);
                sm.setSuperviseSubject(curSupervise);
                sm.setSortOrder(++smallSort);
                sm.setStatus(1);
                sm.setDeleted(0);
                sm.setIsExtended(0);
                categorySmallMapper.insert(sm);
                curSmallId = sm.getId();
                smallCount++;
                stdSeq = 0;
            }

            if (col6.isEmpty() && col7.isEmpty() && col8.isEmpty()) {
                continue;
            }
            if (curSmallId == null) {
                throw new BusinessException(sheetLabel + " 第" + (r + 1) + "行：立案/结案/时限前须有小类行");
            }
            if (col6.isEmpty() || col7.isEmpty() || col8.isEmpty()) {
                throw new BusinessException(sheetLabel + " 第" + (r + 1) + "行：立案条件、结案条件、处置时限均不能为空");
            }

            ParsedTime pt = parseHandleTime(col8);
            stdSeq++;
            CaseStandard cs = new CaseStandard();
            cs.setStandardCode(buildStandardCode(curSmallId, stdSeq));
            cs.setSmallId(curSmallId);
            cs.setBigCode(curBigCode);
            cs.setSmallCode(curSmallCode);
            cs.setCategoryType(categoryType);
            cs.setConditionDesc(col6);
            cs.setCloseCondition(col7);
            cs.setHandleTimeLimit(pt.raw());
            cs.setHandleTimeValue(pt.value());
            cs.setHandleTimeType(pt.type());
            cs.setSortOrder(stdSeq);
            cs.setStatus(1);
            cs.setDeleted(0);
            caseStandardMapper.insert(cs);
            standardCount++;
        }

        return new ImportSummary(sheetLabel, categoryType, bigCount, smallCount, standardCount);
    }

    private static boolean isRowEmpty(Row row) {
        for (int c = 0; c < 9; c++) {
            if (row.getCell(c) != null && row.getCell(c).getCellType() != CellType.BLANK) {
                String v = new DataFormatter().formatCellValue(row.getCell(c)).trim();
                if (!v.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String normalizeCode(String raw, String label, int excelRow1Based) {
        String s = raw.trim();
        if (s.length() > 20) {
            throw new BusinessException("第" + excelRow1Based + "行「" + label + "」过长（最多20字符）");
        }
        return s;
    }

    /**
     * 与库 {@code full_code VARCHAR(4)} 及补丁脚本习惯对齐：大类、小类各取末两位数字拼接；非纯数字则直接拼接并截断至4位。
     */
    static String buildFullCode(String bigCode, String smallCode) {
        String b = padCodePart(bigCode);
        String s = padCodePart(smallCode);
        String fc = b + s;
        if (fc.length() <= 4) {
            return fc;
        }
        return fc.substring(0, 4);
    }

    private static String padCodePart(String code) {
        if (code == null || code.isEmpty()) {
            return "00";
        }
        if (code.matches("\\d+")) {
            int n = Integer.parseInt(code);
            int capped = Math.min(Math.max(n, 0), 99);
            return String.format(Locale.ROOT, "%02d", capped);
        }
        if (code.length() >= 2) {
            return code.substring(0, 2);
        }
        return "0" + code;
    }

    private static String buildStandardCode(Long smallId, int seq) {
        return "S" + smallId + "_" + seq;
    }

    private static ParsedTime parseHandleTime(String raw) {
        String s = raw.trim();
        Matcher m;
        // 含「紧急」→ 连续计时（安全隐患，不能耽误）
        m = PAT_URGENT_WORK_DAY.matcher(s);
        if (m.find()) {
            return new ParsedTime(Integer.parseInt(m.group(1)), "natural_day", s);
        }
        m = PAT_URGENT_WORK_HOUR.matcher(s);
        if (m.find()) {
            return new ParsedTime(Integer.parseInt(m.group(1)), "urgent_hour", s);
        }
        // 不含「紧急」→ 仅计工作时（1天=8工作小时）
        m = PAT_WORK_DAY.matcher(s);
        if (m.find()) {
            return new ParsedTime(Integer.parseInt(m.group(1)), "work_day", s);
        }
        m = PAT_DAY.matcher(s);
        if (m.find()) {
            return new ParsedTime(Integer.parseInt(m.group(1)), "work_day", s);
        }
        m = PAT_HOUR.matcher(s);
        if (m.find()) {
            return new ParsedTime(Integer.parseInt(m.group(1)), "work_hour", s);
        }
        return new ParsedTime(1, "natural_day", s);
    }

    private record ParsedTime(int value, String type, String raw) {}

    public Map<String, Object> importToMap(MultipartFile file) throws IOException {
        ImportResult res = importWorkbook(file);
        List<Map<String, Object>> list = new ArrayList<>();
        for (ImportSummary s : res.summaries()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sheet", s.sheetName());
            m.put("categoryType", s.categoryType());
            m.put("bigCount", s.bigCount());
            m.put("smallCount", s.smallCount());
            m.put("standardCount", s.standardCount());
            list.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sheets", list);
        return out;
    }
}
