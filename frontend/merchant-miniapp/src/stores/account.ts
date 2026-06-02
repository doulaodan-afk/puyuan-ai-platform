import { defineStore } from 'pinia';
import { accountService } from '@/services/account';

export interface BalanceData {
  tokenBalance: number;
  storageUsedGb: number;
  storageFreeQuotaGb: number;
  storageExtraGb: number;
  expireDate?: string;
}

export interface LedgerItem {
  bizNo: string;
  entryType: string;
  direction: string;
  tokenAmount: number;
  cashAmount: number;
  balanceAfter: number;
  pluginId: string;
  occurredAt: string;
}

export interface LedgerPage {
  list: LedgerItem[];
  page: number;
  pageSize: number;
  total: number;
}

export const useAccountStore = defineStore('account', {
  state: () => ({
    balance: null as BalanceData | null,
    ledger: null as LedgerPage | null,
    loading: false
  }),

  getters: {
    balanceText(): string {
      return this.balance?.tokenBalance?.toLocaleString() || '0';
    },
    storageUsedText(): string {
      return `${this.balance?.storageUsedGb || 0} GB`;
    },
    storageQuotaText(): string {
      const free = this.balance?.storageFreeQuotaGb || 0;
      const extra = this.balance?.storageExtraGb || 0;
      return `${free + extra} GB`;
    }
  },

  actions: {
    async fetchBalance() {
      this.loading = true;
      try {
        this.balance = await accountService.getBalance();
      } finally {
        this.loading = false;
      }
    },

    async fetchLedger(params?: { page?: number; pageSize?: number; entryType?: string }) {
      this.loading = true;
      try {
        this.ledger = await accountService.getLedger(params);
      } finally {
        this.loading = false;
      }
    },

    clearData() {
      this.balance = null;
      this.ledger = null;
    }
  }
});
