import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import "./styles.css";

const app = createApp(App);
const pinia = createPinia();

// # MEMORY: admin app uses independent pinia instance to avoid cross-context leakage when merchant and admin run in parallel tabs.
app.use(pinia);
app.use(router);
app.mount("#app");
