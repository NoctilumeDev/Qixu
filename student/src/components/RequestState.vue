<script setup lang="ts">
import {ref,watch,onBeforeUnmount} from 'vue';
import {formatTime,StaleResponse} from '@qixu/client';
import {auth,client,errorMessage,go,pendingAction} from '../runtime';
const busy=ref(false),message=ref('');let operation=0;
watch(()=>auth.generation,()=>{operation++;busy.value=false;message.value='';},{flush:'sync'});
onBeforeUnmount(()=>{operation++;});
function owner(){const op=++operation,generation=auth.generation,actor=auth.session?.actor.id;return ()=>op===operation&&generation===auth.generation&&actor===auth.session?.actor.id;}
async function recover(key:string,replay=false){if(busy.value)return;const current=owner();busy.value=true;message.value='';try{const result=await pendingAction(key,replay);if(!current())return;message.value=result?(result.intentOutcome==='STOPPED_WITHOUT_EFFECT'?'原意图已安全停止，没有产生业务效果。':'原请求已确认，请刷新当前详情。'):'暂未查询到回执；这不代表提交失败，可稍后再查或使用原键重试。';}catch(e){if(current()&&!(e instanceof StaleResponse))message.value=errorMessage(e);}finally{if(current())busy.value=false;}}
async function stop(key:string){if(busy.value)return;const current=owner();busy.value=true;message.value='';try{const result=await client.stop<Record<string,unknown>>(key);if(!current())return;message.value=result.intentOutcome==='STOPPED_WITHOUT_EFFECT'?'原意图已安全停止，没有产生业务效果。':'原提交已成立，不能用停止恢复撤销，请刷新当前详情。';}catch(e){if(current()&&!(e instanceof StaleResponse))message.value=errorMessage(e);}finally{if(current())busy.value=false;}}
</script>
<template>
  <view v-if="auth.error" class="error-note"><text>{{auth.error}}</text><button role="button" class="text-button" @click="go('profile')">查看连接设置</button></view>
  <view v-for="p in auth.pending" :key="p.key" class="pending-card">
    <text class="card-title">{{p.state==='SENDING'?'正在提交，请稍候':'提交结果待确认'}}</text>
    <text class="muted">{{p.recoveryOnly?'原请求信息不完整，仅可查询或安全停止':formatTime(p.createdAt)}} · 原请求 {{p.key.slice(-8)}}</text>
    <view class="actions"><button role="button" :disabled="busy||p.state==='SENDING'" @click="recover(p.key)">查询回执</button><button role="button" :disabled="busy||p.state==='SENDING'||!p.replayable" @click="recover(p.key,true)">同键重试</button><button role="button" :disabled="busy||p.state==='SENDING'" @click="stop(p.key)">确认或停止原意图</button></view>
  </view>
  <view v-if="message" class="info-note">{{message}}</view>
</template>
