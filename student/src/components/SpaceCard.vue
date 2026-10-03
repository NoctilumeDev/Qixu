<script setup lang="ts">
import {featureText,imageKey,type Space} from '@qixu/client';
import {asset,go} from '../runtime';
defineProps<{space:Space}>();
</script>
<template><button role="button" class="space-card" @click="go('space',space.id)"><image class="space-photo" :src="asset(imageKey(space.imageKey)+'.jpg')" mode="aspectFill"/><view class="space-card-body"><text class="card-title">{{space.code}}</text><text class="space-name">{{space.name}}</text><view class="tags"><text v-for="f in featureText(space).filter(f=>f.value==='有').slice(0,2)" :key="f.key">{{f.label}}</text></view></view></button></template>
