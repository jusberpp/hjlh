import { computed, onBeforeUnmount, onMounted, ref, toValue, type MaybeRefOrGetter } from "vue";
import { getCountdown } from "../../utils/time";

export function useCountdown(targetTime: MaybeRefOrGetter<string>, intervalMs = 30000) {
  const now = ref(new Date());
  let timer: number | undefined;

  const countdown = computed(() => getCountdown(toValue(targetTime), now.value));

  onMounted(() => {
    timer = window.setInterval(() => {
      now.value = new Date();
    }, intervalMs);
  });

  onBeforeUnmount(() => {
    if (timer) window.clearInterval(timer);
  });

  return {
    now,
    countdown,
  };
}
