<template>
  <main class="landing-page">
    <img
      class="hero-image"
      :src="topImage"
      alt="高考假期黄金窗口学科能力跃迁课"
    />
    <p class="sel-grade">请选择年级</p>
    <section class="course-section" aria-labelledby="course-title">
      <h1 id="course-title" class="sr-only">{{ activePage.label }}课程安排</h1>

      <div class="grade-tabs" aria-label="年级">
        <button
          v-for="page in gradePages"
          :key="page.id"
          class="grade-tab"
          :class="{ 'is-active': page.id === activeGradeId }"
          type="button"
          :aria-pressed="page.id === activeGradeId"
          @click="activeGradeId = page.id"
        >
          {{ page.label }}
        </button>
      </div>

      <a
        class="reservation-link"
        :href="activePage.reserveUrl"
        :aria-label="`${activePage.label}点击预约课程`"
      >
        <div class="course-list">
          <article
            v-for="course in activePage.courses"
            :key="`${activePage.id}-${course.subject}`"
            class="course-card"
          >
            <div class="subject-badge">{{ course.subject }}</div>
            <div class="course-time">
              <span>{{ course.date }}</span>
              <span v-for="time in course.times" :key="time">{{ time }}</span>
            </div>
            <div class="course-info">
              <strong>主讲:{{ course.teacher }}</strong>
              <p v-for="topic in course.topics" :key="topic">{{ topic }}</p>
            </div>
          </article>
        </div>

        <span class="reserve-title">点击预约课程</span>
      </a>
    </section>

    <section class="group-panel" aria-labelledby="group-title">
      <img
        class="group-bg"
        :src="activePage.bottomImage"
        alt=""
        aria-hidden="true"
      />
      <h2 id="group-title" class="sr-only">{{ activePage.qrTitle }}</h2>
      <div class="group-benefits">
        <span v-for="benefit in activePage.benefits" :key="benefit">{{
          benefit
        }}</span>
      </div>
      <button class="group-cta" type="button" @click="openQrCode">
        申请进群
      </button>
    </section>

    <Teleport to="body">
      <div
        v-if="isQrCodeVisible"
        class="qr-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="qr-modal-title"
        @click.self="closeQrCode"
        @keydown.esc="closeQrCode"
      >
        <div class="qr-dialog" tabindex="-1">
          <button
            class="qr-close"
            type="button"
            aria-label="关闭二维码弹窗"
            @click="closeQrCode"
          >
            ×
          </button>
          <h2 id="qr-modal-title">{{ activePage.qrTitle }}</h2>
          <img
            class="qr-image"
            :src="activePage.qrImage"
            alt="申请进群二维码"
          />
        </div>
      </div>
    </Teleport>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from "vue";
import { gradePages, topImage, type GradeId } from "./data";

const activeGradeId = ref<GradeId>("senior-two");
const activePage = computed(() => {
  return (
    gradePages.find((page) => page.id === activeGradeId.value) ?? gradePages[0]
  );
});

const isQrCodeVisible = ref(false);

const openQrCode = async () => {
  isQrCodeVisible.value = true;
  await nextTick();
  document.querySelector<HTMLElement>(".qr-dialog")?.focus();
};

const closeQrCode = () => {
  isQrCodeVisible.value = false;
};
</script>

<style scoped lang="scss">
.landing-page {
  width: 100%;
  min-height: 100vh;
  background: #eaf4ff;
  overflow-x: hidden;
}

.sel-grade {
  color: #1764f1;
  margin: 10px 24px 0;
  padding-left: 20px;
  font-weight: bold;
}

.hero-image {
  display: block;
  width: 100%;
  height: auto;
}

.course-section {
  padding: 20px 40px 0;
  background: #eaf4ff;
}

.sr-only {
  position: absolute;
  width: 2px;
  height: 2px;
  padding: 0;
  margin: -2px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.grade-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 108px;
  align-items: center;
  margin-bottom: 36px;
}

.grade-tab {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 64px;
  padding: 0;
  border: 0;
  border-radius: 10px;
  background: #82aef0;
  color: #fff;
  font-size: 32px;
  font-weight: 800;
  line-height: 1;
  box-shadow: none;
}

.grade-tab.is-active {
  background: #2e69e8;
  box-shadow: 6px 8px 0 #7ea9f3;
}

.course-list {
  display: grid;
  gap: 22px;
}

.reservation-link {
  display: block;
  color: inherit;
  text-decoration: none;
  cursor: pointer;
}

.reservation-link:focus-visible {
  border-radius: 12px;
  outline: 6px solid #ffd84f;
  outline-offset: 8px;
}

.course-card {
  display: flex;
  align-items: center;
  min-height: 110px;
  padding: 18px 14px;
  border: 2px solid #91b7ff;
  border-radius: 10px;
  background: #fff;
}

.subject-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 108px;
  height: 64px;
  border-radius: 8px;
  background: #2e69e8;
  color: #fff;
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: 8px;
  text-indent: 8px;
}

.course-time {
  display: grid;
  justify-items: center;
  gap: 4px;
  color: #1e2230;
  margin: 0 28px;
  font-size: 21px;
  line-height: 1.1;

  span {
    white-space: nowrap;
  }
}

.course-info {
  min-width: 0;
  color: #1b1f2b;
  font-size: 17px;

  strong {
    display: block;
    margin-bottom: 6px;
    padding-left: 12px;
    color: #1764f1;
    font-size: 24px;
    line-height: 1.25;
    white-space: nowrap;
  }

  p {
    position: relative;
    margin: 0;
    padding-left: 14px;
    font-weight: 600;
    white-space: nowrap;
  }

  p::before {
    position: absolute;
    top: 0.58em;
    left: 0;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #1764f1;
    content: "";
  }
}

.reserve-title {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 340px;
  height: 64px;
  margin: 40px auto 0;
  border-radius: 1998px;
  background: #2e69e8;
  box-shadow: 0 12px 28px rgb(46 105 232 / 0.28);
  color: #fff;
  font-size: 30px;
  font-weight: 900;
  line-height: 1;
  animation: reserve-breathe 1.8s ease-in-out infinite;
  transform-origin: center;
  overflow: hidden;
  isolation: isolate;
}

.reserve-title::before {
  position: absolute;
  top: -40%;
  bottom: -40%;
  left: -70%;
  z-index: 0;
  width: 46%;
  background: linear-gradient(
    105deg,
    rgb(255 255 255 / 0) 0%,
    rgb(255 255 255 / 0.16) 35%,
    rgb(255 255 255 / 0.78) 50%,
    rgb(255 255 255 / 0.16) 65%,
    rgb(255 255 255 / 0) 100%
  );
  content: "";
  pointer-events: none;
  transform: skewX(-22deg);
  animation: reserve-shine 1.8s ease-in-out infinite;
}

@keyframes reserve-breathe {
  0%,
  100% {
    box-shadow: 0 12px 28px rgb(46 105 232 / 0.28);
    transform: scale(1);
  }

  50% {
    box-shadow: 0 16px 36px rgb(46 105 232 / 0.42);
    transform: scale(1.05);
  }
}

@keyframes reserve-shine {
  0% {
    left: -70%;
  }

  46%,
  100% {
    left: 124%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .reserve-title,
  .reserve-title::before,
  .group-cta,
  .group-cta::before {
    animation: none;
  }
}

.group-panel {
  position: relative;
  width: calc(100% - 48px);
  margin: 36px auto 56px;
}

.group-bg {
  display: block;
  width: 100%;
  height: auto;
}

.group-benefits {
  position: absolute;
  top: 36%;
  left: 6%;
  right: 6%;
  display: grid;
  grid-template-columns: 1fr 1fr;
  row-gap: 34px;
  color: #2c68e9;
  font-size: 28px;
  font-weight: 500;
  line-height: 1;
  text-align: center;
  white-space: nowrap;
}

.group-cta {
  position: absolute;
  left: 50%;
  right: auto;
  bottom: 12%;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 340px;
  height: 64px;
  padding: 0;
  border: 0;
  border-radius: 1998px;
  transform: translateX(-50%);
  background: #ffde49;
  box-shadow: 0 12px 28px rgb(255 154 47 / 0.24);
  color: #554c2c;
  font-size: 28px;
  font-weight: 500;
  line-height: 1;
  animation: group-cta-breathe 1.8s ease-in-out infinite;
  transform-origin: center;
  cursor: pointer;
  isolation: isolate;
  overflow: hidden;
}

.group-cta::before {
  position: absolute;
  top: -40%;
  bottom: -40%;
  left: -70%;
  z-index: 0;
  width: 46%;
  background: linear-gradient(
    105deg,
    rgb(255 255 255 / 0) 0%,
    rgb(255 255 255 / 0.18) 35%,
    rgb(255 255 255 / 0.82) 50%,
    rgb(255 255 255 / 0.18) 65%,
    rgb(255 255 255 / 0) 100%
  );
  content: "";
  pointer-events: none;
  transform: skewX(-22deg);
  animation: group-cta-shine 1.8s ease-in-out infinite;
}

.group-cta:focus-visible {
  outline: 6px solid #ffd84f;
  outline-offset: 6px;
}

@keyframes group-cta-breathe {
  0%,
  100% {
    box-shadow: 0 12px 28px rgb(255 154 47 / 0.24);
    transform: translateX(-50%) scale(1);
  }

  50% {
    box-shadow: 0 16px 36px rgb(255 154 47 / 0.4);
    transform: translateX(-50%) scale(1.05);
  }
}

@keyframes group-cta-shine {
  0% {
    left: -70%;
  }

  46%,
  100% {
    left: 124%;
  }
}

.qr-modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: rgb(6 19 45 / 0.56);
}

.qr-dialog {
  position: relative;
  width: min(592px, 100%);
  padding: 44px 36px 40px;
  border-radius: 28px;
  background: #fff;
  box-shadow: 0 32px 80px rgb(0 41 115 / 0.24);
  outline: none;
  text-align: center;

  h2 {
    margin: 0 0 28px;
    color: #7ba2f3;
    font-size: 40px;
    font-weight: 900;
    line-height: 1.2;
  }
}

.qr-close {
  position: absolute;
  top: 14px;
  right: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #6f7c93;
  font-size: 50px;
  line-height: 1;
}

.qr-close:focus-visible {
  outline: 4px solid #1764f1;
  outline-offset: 4px;
}

.qr-image {
  display: block;
  width: 100%;
  max-height: 70vh;
  border-radius: 16px;
  object-fit: contain;
}

@media (max-width: 700px) {
  .course-section {
    padding-right: 36px;
    padding-left: 36px;
  }

  .grade-tabs {
    gap: 68px;
  }

  .course-card {
    grid-template-columns: 100px 116px minmax(0, 1fr);
    padding-right: 12px;
    padding-left: 12px;
  }

  .subject-badge {
    width: 100px;
    height: 80px;
    font-size: 30px;
  }

  .course-info {
    font-size: 17px;
  }

  .group-benefits {
    row-gap: 28px;
    font-size: 28px;
  }

  .group-cta {
    height: 64px;
  }
}
</style>
