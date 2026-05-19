import { HttpClient } from "./core/httpClient";
import { AuthApi } from "./modules/auth";
import { AccountApi } from "./modules/account";
import { PluginsApi } from "./modules/plugins";
import { BillingApi } from "./modules/billing";
import { AdminApi } from "./modules/admin";

export class ApiSdk {
  readonly auth: AuthApi;
  readonly account: AccountApi;
  readonly plugins: PluginsApi;
  readonly billing: BillingApi;
  readonly admin: AdminApi;

  constructor(baseUrl: string) {
    const http = new HttpClient(baseUrl);
    this.auth = new AuthApi(http);
    this.account = new AccountApi(http);
    this.plugins = new PluginsApi(http);
    this.billing = new BillingApi(http);
    this.admin = new AdminApi(http);
  }
}
