package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiModels;
import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.entity.BillingLedger;
import com.puyuanmaoshan.platform.entity.BillingStatementDaily;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.service.BillingLedgerService;
import com.puyuanmaoshan.platform.service.BillingStatementDailyService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/billing/statements")
public class BillingController {
    private final BillingStatementDailyService billingStatementDailyService;
    private final BillingLedgerService billingLedgerService;

    public BillingController(BillingStatementDailyService billingStatementDailyService,
                             BillingLedgerService billingLedgerService) {
        this.billingStatementDailyService = billingStatementDailyService;
        this.billingLedgerService = billingLedgerService;
    }

    @GetMapping("/daily")
    public ApiResponse<ApiModels.DailyStatementResponse> daily(
            @RequestParam String date,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        LocalDate statDate = parseDate(date);

        BillingStatementDaily daily = billingStatementDailyService.lambdaQuery()
                .eq(BillingStatementDaily::getTenantId, parsedTenantId)
                .eq(BillingStatementDaily::getStatDate, statDate)
                .one();

        DailyAggregate aggregate;
        if (daily != null) {
            aggregate = new DailyAggregate(
                    daily.getTokenIn() == null ? 0L : daily.getTokenIn(),
                    daily.getTokenOut() == null ? 0L : daily.getTokenOut(),
                    daily.getCallCount() == null ? 0 : daily.getCallCount(),
                    daily.getAmountRecharge() == null ? BigDecimal.ZERO : daily.getAmountRecharge(),
                    daily.getAmountRefund() == null ? BigDecimal.ZERO : daily.getAmountRefund()
            );
        } else {
            aggregate = aggregateFromLedger(parsedTenantId, statDate.atStartOfDay(), statDate.plusDays(1).atStartOfDay());
        }

        ApiModels.DailyStatementResponse data = new ApiModels.DailyStatementResponse(
                statDate.toString(),
                aggregate.tokenIn(),
                aggregate.tokenOut(),
                aggregate.callCount(),
                aggregate.amountRecharge(),
                aggregate.amountRefund()
        );
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-statement-daily"));
    }

    @GetMapping("/monthly")
    public ApiResponse<ApiModels.MonthlyStatementResponse> monthly(
            @RequestParam String month,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        long parsedTenantId = RequestContextUtil.parseTenantId(tenantId);
        YearMonth yearMonth = parseMonth(month);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<BillingStatementDaily> statementList = billingStatementDailyService.lambdaQuery()
                .eq(BillingStatementDaily::getTenantId, parsedTenantId)
                .between(BillingStatementDaily::getStatDate, startDate, endDate)
                .list();

        DailyAggregate aggregate;
        if (!statementList.isEmpty()) {
            aggregate = new DailyAggregate(
                    statementList.stream().mapToLong(item -> item.getTokenIn() == null ? 0L : item.getTokenIn()).sum(),
                    statementList.stream().mapToLong(item -> item.getTokenOut() == null ? 0L : item.getTokenOut()).sum(),
                    statementList.stream().mapToInt(item -> item.getCallCount() == null ? 0 : item.getCallCount()).sum(),
                    statementList.stream().map(item -> item.getAmountRecharge() == null ? BigDecimal.ZERO : item.getAmountRecharge())
                            .reduce(BigDecimal.ZERO, BigDecimal::add),
                    statementList.stream().map(item -> item.getAmountRefund() == null ? BigDecimal.ZERO : item.getAmountRefund())
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
            );
        } else {
            aggregate = aggregateFromLedger(parsedTenantId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        }

        ApiModels.MonthlyStatementResponse data = new ApiModels.MonthlyStatementResponse(
                yearMonth.toString(),
                aggregate.tokenIn(),
                aggregate.tokenOut(),
                aggregate.callCount(),
                aggregate.amountRecharge(),
                aggregate.amountRefund()
        );
        return ApiResponse.ok(data, RequestContextUtil.resolveRequestId(requestId, "req-statement-monthly"));
    }

    private DailyAggregate aggregateFromLedger(long tenantId, LocalDateTime start, LocalDateTime endExclusive) {
        List<BillingLedger> ledgers = billingLedgerService.lambdaQuery()
                .eq(BillingLedger::getTenantId, tenantId)
                .ge(BillingLedger::getOccurredAt, start)
                .lt(BillingLedger::getOccurredAt, endExclusive)
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

        return new DailyAggregate(tokenIn, tokenOut, callCount, amountRecharge, amountRefund);
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "date must be yyyy-MM-dd");
        }
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "month must be yyyy-MM");
        }
    }

    private record DailyAggregate(long tokenIn, long tokenOut, int callCount, BigDecimal amountRecharge,
                                  BigDecimal amountRefund) {
    }
}
