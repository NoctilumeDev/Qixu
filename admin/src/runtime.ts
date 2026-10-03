import {reactive} from 'vue';
import {Client,ApiError,StaleResponse,UnknownSubmission,type Session,type Pending,type Row} from '@qixu/client';
const storage={get:(key:string)=>localStorage.getItem('admin.'+key),set:(key:string,value:string)=>localStorage.setItem('admin.'+key,value),remove:(key:string)=>localStorage.removeItem('admin.'+key),keys:()=>Object.keys(localStorage).filter(k=>k.startsWith('admin.')).map(k=>k.slice(6))};
const sessionChannel=typeof BroadcastChannel==='undefined'?null:new BroadcastChannel('qixu-cookie-controls');
export const client=new Client(async r=>{const controller=new AbortController();const timeout=setTimeout(()=>controller.abort(),15000);try{const res=await fetch(r.path,{method:r.method,headers:r.headers,credentials:'same-origin',cache:'no-store',signal:controller.signal,body:r.body===undefined?undefined:JSON.stringify(r.body)});if(r.responseType==='FILE'&&res.ok){const tempFilePath=URL.createObjectURL(await res.blob());return {status:res.status,body:{data:{tempFilePath}},discard:()=>URL.revokeObjectURL(tempFilePath)};}return {status:res.status,body:await res.json()};}finally{clearTimeout(timeout);}},storage,'COOKIE',()=>crypto.randomUUID(),()=>sessionChannel?.postMessage('SESSION_CHANGED'));
if(sessionChannel)sessionChannel.onmessage=e=>{if(e.data==='SESSION_CHANGED')client.externalCookieChanged();};
const storageChanged=(e:StorageEvent)=>{if(e.key?.startsWith('admin.qixu.intent.v1.'))client.refreshPending();};
window.addEventListener('storage',storageChanged);
if(import.meta.hot)import.meta.hot.dispose(()=>{sessionChannel?.close();window.removeEventListener('storage',storageChanged);});
export const auth=reactive<{session:Session|null;pending:Pending[];generation:number;loading:boolean;error:string;needsReload:boolean}>({session:null,pending:[],generation:0,loading:true,error:'',needsReload:false});
client.subscribe(()=>{auth.session=client.session;auth.pending=client.visiblePending.slice();auth.generation=client.generation;auth.needsReload=client.needsReload;});
let ready:Promise<void>|undefined;
export function initialize(){if(!ready)ready=(async()=>{try{await client.bootstrap();}catch(e){auth.error=errorMessage(e);}finally{auth.loading=false;}})();return ready;}
export function errorMessage(e:unknown){if(e instanceof StaleResponse)return'';if(e instanceof UnknownSubmission)return'提交结果确认中，请先恢复原请求。';if(e instanceof ApiError){const hint:Record<number,string>={401:'请重新登录原账号以恢复请求。',403:'身份或当前管理范围不允许。',404:'内容不存在或你不可访问，请返回列表。',409:'输入保留，须核对当前版本/影响再提交。',422:'请核对字段与时限。',503:'服务暂不可用；已有提交先确认回执。'};return `${e.message} ${hint[e.status]||''}${e.requestId?'（编号 '+e.requestId.slice(0,8)+'）':''}`;}return'网络没有返回可确认结果，请重试；已有提交先查回执。';}
export const api=<T=Row>(path:string,slot?:string)=>client.get<T>('/api/v1'+path,slot);
export const roles:Record<string,string>={ADMIN:'空间管理员',TEACHER:'老师',STUDENT:'学生'};
export const kindLabel:Record<string,string>={AREA:'区域',SEAT:'座位',ROOM:'研讨室',HALL:'报告厅'};
export const modeLabel:Record<string,string>={BOOKABLE:'短期预约',PREPARATION:'长期批次',WALK_IN:'自由使用',VENUE:'场地申请'};
