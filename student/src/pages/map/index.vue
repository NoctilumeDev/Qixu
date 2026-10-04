<script setup lang="ts">
  import { ref, nextTick } from 'vue';
  import { onLoad, onShow, onHide, onUnload, onPageScroll } from '@dcloudio/uni-app';
  import Screen from '../../components/Screen.vue';
  const id = ref(0),
    screen = ref<{
      refresh: () => void;
      leave: () => void;
      trackScroll: (value: number) => void;
    } | null>(null);
  onLoad((options) => {
    id.value = Number(options?.id) || 0;
  });
  onPageScroll((e) => screen.value?.trackScroll(e.scrollTop));
  onShow(() => nextTick(() => screen.value?.refresh()));
  onHide(() => screen.value?.leave());
  onUnload(() => screen.value?.leave());
</script>
<template><Screen ref="screen" view="map" :resource-id="id" /></template>
