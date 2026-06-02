package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.puyuanmaoshan.platform.entity.AccountWallet;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.mapper.AccountWalletMapper;
import com.puyuanmaoshan.platform.service.AccountWalletService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AccountWalletServiceImpl extends ServiceImpl<AccountWalletMapper, AccountWallet> implements AccountWalletService {

    @Override
    public long deductToken(Long tenantId, int tokenAmount, String pluginCode) {
        LambdaQueryWrapper<AccountWallet> query = new LambdaQueryWrapper<AccountWallet>()
                .eq(AccountWallet::getTenantId, tenantId)
                .eq(AccountWallet::getStatus, 1);

        AccountWallet wallet = getOne(query);
        if (wallet == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "wallet not found");
        }

        long currentBalance = wallet.getTokenBalance() == null ? 0L : wallet.getTokenBalance();
        if (currentBalance < tokenAmount) {
            throw new AppException(ErrorCode.BUSINESS_ERROR, "insufficient token balance");
        }

        wallet.setTokenBalance(currentBalance - tokenAmount);
        wallet.setUpdatedAt(LocalDateTime.now());

        if (!updateById(wallet)) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "failed to deduct token");
        }
        return wallet.getTokenBalance();
    }
}
