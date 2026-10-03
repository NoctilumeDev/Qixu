<script setup lang="ts">
import {ref} from 'vue';
import {formatTime} from '@qixu/client';
import {auth,client,errorMessage} from '../runtime';
const busy=ref(false),message=ref('');
function reload(){window.location.reload();}
async function recover(key:string,replay=false){if(busy.value)return;busy.value=true;message.value='';try{const result=await client.recover(key,replay);message.value=result?(result.intentOutcome==='STOPPED_WITHOUT_EFFECT'?'原意图已安全停止，没有产生业务效果。':'原提交已确认，请刷新当前详情。'):'暂未查询到回执，不代表提交失败；可稍后再查或同键重试。';}catch(e){message.value=errorMessage(e);}finally{busy.value=false;}}
async function stop(key:string){if(busy.value)return;busy.value=true;message.value='';try{const result=await client.stop<Record<string,unknown>>(key);message.value=result.intentOutcome==='STOPPED_WITHOUT_EFFECT'?'原意图已安全停止，没有产生业务效果。':'原提交已成立，不能用停止恢复撤销，请刷新当前详情。';}catch(e){message.value=errorMessage(e);}finally{busy.value=false;}}
</script>
<template><div v-if="auth.needsReload" class="alert danger">会话已变更或控制结果尚未确认，当前页已停止读写。<button @click="reload">重新加载并核对身份</button></div><div v-for="p in auth.pending" :key="p.key" class="alert warning"><div><strong>{{p.state==='SENDING'?'正在提交':'提交结果待确认'}}</strong><small>原键 {{p.key}} · {{formatTime(p.createdAt)}}</small></div><div class="actions"><button :disabled="busy||p.state==='SENDING'" @click="recover(p.key)">查询回执</button><button :disabled="busy||p.state==='SENDING'||!p.replayable" @click="recover(p.key,true)">同键重试</button><button :disabled="busy||p.state==='SENDING'" @click="stop(p.key)">确认或停止原意图</button></div></div><div v-if="message" class="alert">{{message}}</div></template>
