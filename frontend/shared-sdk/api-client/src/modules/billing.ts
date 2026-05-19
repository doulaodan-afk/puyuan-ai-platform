import { HttpClient } from "../core/httpClient";
import type { RequestContext } from "../core/types";

export class BillingApi {
  constructor(private readonly http: HttpClient) {}

  dailyStatement(date: string, context: RequestContext) {
    return this.http.request("GET", `/api/v1/billing/statements/daily?date=${date}`, context);
  }

  monthlyStatement(month: string, context: RequestContext) {
    return this.http.request("GET", `/api/v1/billing/statements/monthly?month=${month}`, context);
  }
}
