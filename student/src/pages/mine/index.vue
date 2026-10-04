<script setup lang="ts">
  import { ref, nextTick } from 'vue';
  import { onLoad, onShow, onHide, onUnload } from '@dcloudio/uni-app';
  import Screen from '../../components/Screen.vue';
  const id = ref(0),
    screen = ref<{ refresh: () => void; leave: () => void } | null>(null);
  onLoad((options) => {
    id.value = Number(options?.id) || 0;
  });
  onShow(() => nextTick(() => screen.value?.refresh()));
  onHide(() => screen.value?.leave());
  onUnload(() => screen.value?.leave());
</script>
<template><Screen ref="screen" view="mine" :resource-id="id" /></template>
