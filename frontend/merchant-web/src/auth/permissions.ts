import { useAuthStore } from "../stores/auth";

export function canInvokePlugin(): boolean {
  const store = useAuthStore();
  return ["merchant_owner", "merchant_operator", "merchant_editor"].includes(store.roleCode ?? "");
}

export function canRecharge(): boolean {
  const store = useAuthStore();
  return ["merchant_owner", "merchant_operator"].includes(store.roleCode ?? "");
}