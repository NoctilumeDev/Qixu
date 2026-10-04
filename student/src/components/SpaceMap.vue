<script setup lang="ts">
  import { ref } from 'vue';
  import type { Space } from '@qixu/client';
  import { modeLabel } from '../runtime';
  defineProps<{ spaces: Space[]; selected?: number }>();
  const zoom = ref(1);
  const emit = defineEmits<{ (event: 'select', space: Space): void }>();
  const position = (s: Space) => ({
    left: s.x / 10 + '%',
    top: s.y / 10 + '%',
    width: s.width / 10 + '%',
    height: s.height / 10 + '%',
  });
</script>
<template>
  <view class="map-controls"
    ><button role="button" :disabled="zoom <= 1" @click="zoom = Math.max(1, zoom - 0.5)">
      缩小</button
    ><text>{{ Math.round(zoom * 100) }}%</text
    ><button role="button" :disabled="zoom >= 3" @click="zoom = Math.min(3, zoom + 0.5)">
      放大</button
    ><button role="button" @click="zoom = 1">复位</button></view
  >
  <scroll-view class="map-viewport" scroll-x scroll-y
    ><view
      class="space-map"
      :style="{ width: zoom * 100 + '%', height: zoom * 320 + 'px' }"
      aria-label="演示数据坐标图，下面提供列表入口"
    >
      <view
        v-for="s in spaces.filter((s) => s.kind === 'AREA')"
        :key="s.id"
        class="map-area"
        :style="position(s)"
        ><text>{{ s.name }}</text></view
      >
      <button
        role="button"
        v-for="s in spaces.filter((s) => s.kind !== 'AREA')"
        :key="s.id"
        class="map-node"
        :class="[s.kind.toLowerCase(), s.useMode.toLowerCase(), { selected: s.id === selected }]"
        :style="position(s)"
        :aria-label="s.code + ' ' + modeLabel[s.useMode]"
        @click="emit('select', s)"
      >
        {{ s.kind === 'SEAT' ? s.code.slice(-2) : s.name }}
      </button>
    </view></scroll-view
  >
  <view class="map-key"
    ><text>绿：短约</text><text>灰蓝：长期批次</text><text>米色：场地</text></view
  >
  <text class="caption">位置为演示数据坐标；颜色表示使用规则，未查询时段可用性。</text>
</template>
