package com.puyuanmaoshan.platform.schedule;

import com.puyuanmaoshan.platform.entity.AuditLog;
import com.puyuanmaoshan.platform.entity.BillingLedger;
import com.puyuanmaoshan.platform.entity.BillingStatementDaily;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.service.AuditLogService;
import com.puyuanmaoshan.platform.service.BillingLedgerService;
import com.puyuanmaoshan.platform.service.BillingStatementDailyService;
import com.puyuanmaoshan.platform.service.TenantService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BillingStatementScheduler {
    private final TenantService tenantService;
    private final BillingLedgerService billingLedgerService;
    private final BillingStatementDailyService billingStatementDailyService;
    private final AuditLogService auditLogService;

    public BillingStatementScheduler(TenantService tenantService,
                                     BillingLedgerService billingLedgerService,
                                     BillingStatementDailyService billingStatementDailyService,
                                     AuditLogService auditLogService) {
        this.tenantService = tenantService;
        this.billingLedgerService = billingLedgerService;
        this.billingStatementDailyService = billingStatementDailyService;
        this.auditLogService = auditLogService;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Shanghai")
    @Transactional(rollbackFor = Exception.class)
    public void generateDailyStatements() {
        LocalDate statDate = LocalDate.now().minusDays(1);
        LocalDateTime start = statDate.atStartOfDay();
        LocalDateTime end = statDate.plusDays(1).atStartOfDay();

        List<Long> tenantIds = tenantService.lambdaQuery()
                .orderByAsc(Tenant::getId)
                .list()
                .stream()
                .map(Tenant::getId)
                .toList();

        for (Long tenantId : tenantIds) {
            BillingStatementDaily existing = billingStatementDailyService.lambdaQuery()
                    .eq(BillingStatementDaily::getTenantId, tenantId)
                    .eq(BillingStatementDaily::getStatDate, statDate)
                    .one();
            if (existing != null) {
                continue;
            }

            List<BillingLedger> ledgers = billingLedgerService.lambdaQuery()
                    .eq(BillingLedger::getTenantId, tenantId)
                    .ge(BillingLedger::getOccurredAt, start)
                    .lt(BillingLedger::getOccurredAt, end)
                    .list();

            long tokenIn = ledgers.stream()
                    .filter(item -> "in".equalsIgnoreCase(item.getDirection()))
                    .mapToLong(item -> item.getTokenAmount() == null ? 0L : item.getTokenAmount())
                    .sum();

            long tokenOut = ledgers.stream()
                    .filter(item -> "out".equalsIgnoreCase(item.getDirection()))
                    .mapToLong(item -> item.getTokenAmount() == null ? 0L : item.getTokenAmount())
                    .sum();

            int callCount = (int) ledgers.stream()
                    .filter(item -> "debit".equalsIgnoreCase(item.getEntryType()))
                    .count();

            BigDecimal amountRecharge = ledgers.stream()
                    .filter(item -> "recharge".equalsIgnoreCase(item.getEntryType()))
                    .map(item -> item.getCashAmount() == null ? BigDecimal.ZERO : item.getCashAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal amountRefund = ledgers.stream()
                    .filter(item -> "refund".equalsIgnoreCase(item.getEntryType()))
                    .map(item -> item.getCashAmount() == null ? BigDecimal.ZERO : item.getCashAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            billingStatementDailyService.save(BillingStatementDaily.builder()
                    .tenantId(tenantId)
                    .statDate(statDate)
                    .tokenIn(tokenIn)
                    .tokenOut(tokenOut)
                    .callCount(callCount)
                    .amountRecharge(amountRecharge)
                    .amountRefund(amountRefund)
                    .generatedAt(LocalDateTime.now())
                    .build());
        }

        auditLogService.save(AuditLog.builder()
                .action("billing_statement_daily_generate")
                .targetType("statement_daily")
                .targetId(statDate.toString())
                .detailJson("{\"tenant_count\":" + tenantIds.size() + "}")
                .build());
    }

    @Scheduled(cron = "0 15 0 1 * *", zone = "Asia/Shanghai")
    @Transactional(rollbackFor = Exception.class)
    public void summarizeMonthlyStatements() {
        YearMonth month = YearMonth.now().minusMonths(1);
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        List<BillingStatementDaily> dailyList = billingStatementDailyService.lambdaQuery()
                .between(BillingStatementDaily::getStatDate, startDate, endDate)
                .list();

        Map<Long, List<BillingStatementDaily>> byTenant = dailyList.stream()
                .collect(Collectors.groupingBy(BillingStatementDaily::getTenantId));

        byTenant.forEach((tenantId, statements) -> {
            long tokenIn = statements.stream().mapToLong(item -> item.getTokenIn() == null ? 0L : item.getTokenIn()).sum();
            long tokenOut = statements.stream().mapToLong(item -> item.getTokenOut() == null ? 0L : item.getTokenOut()).sum();
            int callCount = statements.stream().mapToInt(item -> item.getCallCount() == null ? 0 : item.getCallCount()).sum();
            BigDecimal amountRecharge = statements.stream()
                    .map(item -> item.getAmountRecharge() == null ? BigDecimal.ZERO : item.getAmountRecharge())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            auditLogService.save(AuditLog.builder()
                    .tenantId(tenantId)
                    .action("billing_statement_monthly_aggregate")
                    .targetType("statement_monthly")
                    .targetId(month.toString())
                    .detailJson("{\"token_in\":" + tokenIn
                            + ",\"token_out\":" + tokenOut
                            + ",\"call_count\":" + callCount
                            + ",\"amount_recharge\":" + amountRecharge + "}")
                    .build());
        });
    }
}