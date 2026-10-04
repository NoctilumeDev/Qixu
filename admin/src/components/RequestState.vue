<script setup lang="ts">
  import { ref, watch, onBeforeUnmount } from 'vue';
  import { formatTime, StaleResponse } from '@qixu/client';
  import { auth, client, errorMessage } from '../runtime';
  const busy = ref(false),
    message = ref('');
  let operation = 0;
  watch(
    () => auth.generation,
    () => {
      operation++;
      busy.value = false;
      message.value = '';
    },
    { flush: 'sync' },
  );
  onBeforeUnmount(() => {
    operation++;
  });
  function owner() {
    const op = ++operation,
      generation = auth.generation,
      actor = auth.session?.actor.id;
    return () =>
      op === operation && generation === auth.generation && actor === auth.session?.actor.id;
  }
  function reload() {
    window.location.reload();
  }
  async function recover(key: string, replay = false) {
    if (busy.value) return;
    const current = owner();
    busy.value = true;
    message.value = '';
    try {
      const result = await client.recover(key, replay);
      if (!current()) return;
      message.value = result
        ? result.intentOutcome === 'STOPPED_WITHOUT_EFFECT'
          ? '原意图已安全停止，没有产生业务效果。'
          : '原提交已确认，请刷新当前详情。'
        : '暂未查询到回执，不代表提交失败；可稍后再查或同键重试。';
    } catch (e) {
      if (current() && !(e instanceof StaleResponse)) message.value = errorMessage(e);
    } finally {
      if (current()) busy.value = false;
    }
  }
  async function stop(key: string) {
    if (busy.value) return;
    const current = owner();
    busy.value = true;
    message.value = '';
    try {
      const result = await client.stop<Record<string, unknown>>(key);
      if (!current()) return;
      message.value =
        result.intentOutcome === 'STOPPED_WITHOUT_EFFECT'
          ? '原意图已安全停止，没有产生业务效果。'
          : '原提交已成立，不能用停止恢复撤销，请刷新当前详情。';
    } catch (e) {
      if (current() && !(e instanceof StaleResponse)) message.value = errorMessage(e);
    } finally {
      if (current()) busy.value = false;
    }
  }
</script>
<template>
  <div v-if="auth.needsReload" class="alert danger">
    会话已变更或控制结果尚未确认，当前页已停止读写。<button @click="reload">
      重新加载并核对身份
    </button>
  </div>
  <div v-for="p in auth.pending" :key="p.key" class="alert warning">
    <div>
      <strong>{{ p.state === 'SENDING' ? '正在提交' : '提交结果待确认' }}</strong
      ><small
        >原键 {{ p.key }} ·
        {{
          p.recoveryOnly ? '原请求信息不完整，仅可查询或安全停止' : formatTime(p.createdAt)
        }}</small
      >
    </div>
    <div class="actions">
      <button :disabled="busy || p.state === 'SENDING'" @click="recover(p.key)">查询回执</button
      ><button
        :disabled="busy || p.state === 'SENDING' || !p.replayable"
        @click="recover(p.key, true)"
      >
        同键重试</button
      ><button :disabled="busy || p.state === 'SENDING'" @click="stop(p.key)">
        确认或停止原意图
      </button>
    </div>
  </div>
  <div v-if="message" class="alert">{{ message }}</div>
</template>
