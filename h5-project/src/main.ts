import { createApp } from "vue";
import { createPinia } from "pinia";
import piniaPersistedstate from "pinia-plugin-persistedstate";
import App from "./App.vue";
import router from "./router";
import "./styles/tailwind.css";
import "./styles/base.scss";

const pinia = createPinia();
pinia.use(piniaPersistedstate);

const app = createApp(App);
app.use(pinia);
app.use(router);

app.mount("#app");
