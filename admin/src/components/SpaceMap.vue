<script setup lang="ts">
  import { ref } from 'vue';
  import type { Space } from '@qixu/client';
  import { modeLabel } from '../runtime';
  defineProps<{ spaces: Space[]; selected: number }>();
  const emit = defineEmits<{ select: [space: Space] }>();
  const zoom = ref(1);
  const position = (s: Space) => ({
    left: s.x / 10 + '%',
    top: s.y / 10 + '%',
    width: s.width / 10 + '%',
    height: s.height / 10 + '%',
  });
</script>
<template>
  <div class="map-controls" aria-label="地图大小">
    <button :disabled="zoom <= 1" @click="zoom = Math.max(1, zoom - 0.5)">缩小</button
    ><span>{{ zoom * 100 }}%</span
    ><button :disabled="zoom >= 3" @click="zoom = Math.min(3, zoom + 0.5)">放大</button
    ><button @click="zoom = 1">复位</button>
  </div>
  <div class="map-scroll" tabindex="0" aria-label="可平移的演示楼层图">
    <div
      class="map-canvas"
      role="group"
      aria-label="演示楼层坐标图"
      :style="{ width: 560 * zoom + 'px' }"
    >
      <div
        v-for="s in spaces.filter((s) => s.kind === 'AREA')"
        :key="s.id"
        class="map-area"
        :style="position(s)"
      >
        <span>{{ s.name }}</span>
      </div>
      <button
        v-for="s in spaces.filter((s) => s.kind !== 'AREA')"
        :key="s.id"
        class="map-node"
        :class="[s.useMode.toLowerCase(), { selected: s.id === selected }]"
        :style="position(s)"
        :aria-label="s.code + ' ' + s.name + ' ' + modeLabel[s.useMode]"
        @click="emit('select', s)"
      >
        {{ s.kind === 'SEAT' ? s.code.slice(-2) : s.name }}
      </button>
    </div>
  </div>
  <div class="map-legend">
    <span>绿：短约规则</span><span>灰蓝：长期批次</span><span>米色：场地申请</span>
  </div>
  <p class="muted small">
    演示坐标 · 可放大和平移，或用列表选择。规则颜色不代表当前时段空闲或现场人数。
  </p>
</template>
