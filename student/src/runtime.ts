import {reactive} from 'vue';
import {Client,ApiError,StaleResponse,UnknownSubmission,type Session,type Pending,type Row} from '@qixu/client';
const storage={get:(key:string)=>String(uni.getStorageSync(key)||'')||null,set:(key:string,value:string)=>uni.setStorageSync(key,value),remove:(key:string)=>uni.removeStorageSync(key),keys:()=>uni.getStorageInfoSync().keys};
// H5 uses a same-origin dev proxy. Native mini programs require an explicit HTTPS origin.
export function baseUrl(){
  let url='';
  // #ifndef H5
  url=String(uni.getStorageSync('qixu.apiOrigin')||import.meta.env.VITE_API_ORIGIN||'');
  if(!/^https:\/\/[^/]+$/.test(url))throw new ApiError(503,'API_ORIGIN_REQUIRED','请在设置中填写已部署的 HTTPS 服务地址。');
  // #endif
  return url;
}
const key=()=>`qx_${Date.now().toString(36)}_${Array.from({length:24},()=>Math.floor(Math.random()*36).toString(36)).join('')}`;
export const client=new Client(r=>new Promise((resolve,reject)=>{
  let url:string;try{url=baseUrl()+r.path;}catch(e){reject(e);return;}
  if(r.responseType==='FILE'){
    uni.downloadFile({url,header:r.headers,timeout:15000,success:res=>resolve({status:res.statusCode,body:res.statusCode===200?{data:{tempFilePath:res.tempFilePath}}:{error:{code:'ATTACHMENT_NOT_ACCESSIBLE',message:'照片不可访问，请核对身份和权限。'}},discard:()=>releaseDownloaded(res.tempFilePath)}),fail:err=>reject(new Error(err.errMsg||'照片未下载'))});return;
  }
  if(r.method==='POST'&&/^\/api\/v1\/feedback\/\d+\/attachments$/.test(r.path)){
    const headers={...r.headers};delete headers['Content-Type'];
    const filePath=String((r.body as Row)?.filePath||'');if(!filePath){reject(new ApiError(422,'FILE_REQUIRED','请选择一张照片。'));return;}
    uni.uploadFile({url,header:headers,filePath,name:'file',timeout:15000,success:res=>{let body:unknown;try{body=JSON.parse(res.data);}catch{body=null;}resolve({status:res.statusCode,body});},fail:err=>reject(new Error(err.errMsg||'照片上传结果未确认'))});return;
  }
  uni.request({url,method:r.method as 'GET'|'POST',data:r.body as Record<string,unknown>,header:r.headers,timeout:15000,success:res=>resolve({status:res.statusCode,body:res.data}),fail:err=>reject(new Error(err.errMsg||'网络未响应'))});
}),storage,'BEARER',key);
export const auth=reactive<{session:Session|null;pending:Pending[];generation:number;loading:boolean;error:string}>({session:null,pending:[],generation:0,loading:true,error:''});
client.subscribe(()=>{auth.session=client.session;auth.pending=client.visiblePending.slice();auth.generation=client.generation;});
let initialized:Promise<void>|undefined;
export function initialize(){if(!initialized)initialized=(async()=>{try{await client.bootstrap();}catch(e){auth.error=errorMessage(e);}finally{auth.loading=false;}})();return initialized;}
export function errorMessage(error:unknown){if(error instanceof StaleResponse)return'';if(error instanceof UnknownSubmission)return'提交结果待确认，请在上方恢复原请求。';if(error instanceof ApiError){const hints:Record<number,string>={401:'重新登录后可恢复同一账号的请求。',403:'当前身份或管理范围无此权限。',404:'内容不存在或你不可访问，可返回上级入口。',409:'请保留输入，刷新当前事实后再决定。',422:'请核对字段与时间规则。',503:'服务暂时不可用，可稍后确认。'};return `${error.message} ${hints[error.status]||''}${error.requestId?'（编号 '+error.requestId.slice(0,8)+'）':''}`;}return'网络暂时未响应，请重试；已有提交请先确认结果。';}
export function go(view:string,id?:number,replace=false){const allowed=['home','map','space','favorites','batches','batch','events','event','mine','messages','profile','feedback','report','governance','about'];if(!allowed.includes(view))view='home';const url='/pages/'+view+'/index'+(id===undefined?'':'?id='+id);if(replace)uni.redirectTo({url});else if(getCurrentPages().length>=9)uni.redirectTo({url});else uni.navigateTo({url});}
export function back(parent='home'){const pages=getCurrentPages();if(pages.length>1)uni.navigateBack();else go(parent,undefined,true);}
export const api=<T=Row>(path:string,slot?:string)=>client.get<T>('/api/v1'+path,slot);
export const asset=(name:string)=>'/static/assets/'+name;
export const icon=(name:string,color='ink')=>'/static/icons/'+name+'-'+color+'.svg';
export const kindLabel:Record<string,string>={AREA:'区域',SEAT:'座位',ROOM:'研讨室',HALL:'报告厅'};
export const modeLabel:Record<string,string>={WALK_IN:'自由使用',BOOKABLE:'短期预约',PREPARATION:'长期批次',VENUE:'场地申请'};
export async function pendingAction(key:string,replay=false){return client.recover(key,replay);}

export function releaseDownloaded(path:string){
  // #ifdef H5
  if(path.startsWith('blob:'))URL.revokeObjectURL(path);
  // #endif
  // #ifndef H5
  if(path)uni.getFileSystemManager().unlink({filePath:path,fail:()=>undefined});
  // #endif
}
