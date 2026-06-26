export type LiveState = "upcoming" | "live" | "ended";

interface TimeRange {
  startTime: string;
  endTime: string;
}

export function getLiveState(
  course: TimeRange,
  now = new Date(),
): LiveState {
  const start = new Date(course.startTime).getTime();
  const end = new Date(course.endTime).getTime();
  const current = now.getTime();

  if (current < start) return "upcoming";
  if (current <= end) return "live";
  return "ended";
}

export function getCountdown(targetTime: string, now = new Date()) {
  const diff = Math.max(0, new Date(targetTime).getTime() - now.getTime());
  const totalMinutes = Math.floor(diff / 60000);
  const days = Math.floor(totalMinutes / 1440);
  const hours = Math.floor((totalMinutes % 1440) / 60);
  const minutes = totalMinutes % 60;

  return {
    days: pad(days),
    hours: pad(hours),
    minutes: pad(minutes),
  };
}

export function formatDate(value: string) {
  const date = new Date(value);
  return `${date.getFullYear()}年${pad(date.getMonth() + 1)}月${pad(date.getDate())}日`;
}

export function formatMonthDay(value: string) {
  const date = new Date(value);
  return `${date.getMonth() + 1}月${date.getDate()}日`;
}

export function formatTimeRange(course: TimeRange) {
  return `${formatTime(course.startTime)} - ${formatTime(course.endTime)}`;
}

function formatTime(value: string) {
  const date = new Date(value);
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function pad(value: number) {
  return String(value).padStart(2, "0");
}
