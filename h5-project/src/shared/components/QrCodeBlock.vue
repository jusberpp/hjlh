<template>
  <section class="qr-section" :aria-labelledby="titleId">
    <div class="qr-copy">
      <h2 :id="titleId">{{ title }}</h2>
      <p>{{ description }}</p>
    </div>
    <img class="qr-image" :src="image" :alt="title" />
  </section>
</template>

<script setup lang="ts">
const props = defineProps<{
  title: string;
  description: string;
  image: string;
}>();

const titleId = `qr-title-${props.title}`;
</script>

<style scoped lang="scss">
.qr-section {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-top: 32px;
  padding: 32px;
  border: 2px solid rgb(183 91 24 / 0.18);
  border-radius: 24px;
  background: linear-gradient(180deg, #fffefd 0%, #fffbf2 100%);
  box-shadow: 0 16px 36px rgb(77 42 17 / 0.08);
}

.qr-copy {
  flex: 1;
  min-width: 0;

  h2 {
    margin: 0;
    color: #b75b18;
    font-size: 38px;
    line-height: 1.25;
  }

  p {
    margin: 16px 0 0;
    color: #70503b;
    font-size: 26px;
    line-height: 1.55;
  }
}

.qr-image {
  display: block;
  flex: 0 0 224px;
  width: 224px;
  height: 224px;
  padding: 12px;
  border-radius: 16px;
  background: #fff;
  object-fit: contain;
}

@media (max-width: 700px) {
  .qr-section {
    gap: 24px;
    padding: 28px;
  }

  .qr-image {
    flex-basis: 224px;
    width: 224px;
    height: 224px;
  }
}
</style>
