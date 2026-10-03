export interface Actor { id:number; username:string; displayName:string; role:'STUDENT'|'TEACHER'|'ADMIN'; studentVerified:boolean; authVersion:number }
export interface Session { actor:Actor; csrfToken:string; token?:string; adminFloors?:number[] }
export interface HttpRequest { method:string; path:string; headers:Record<string,string>; body?:unknown; responseType?:'FILE' }
export interface HttpResponse { status:number; body:unknown; discard?:()=>void }
export type Transport = (request:HttpRequest)=>Promise<HttpResponse>;
export interface Storage { get(key:string):string|null; set(key:string,value:string):void; remove(key:string):void; keys():string[] }
export interface Pending { key:string; actorId:number; path:string; body:unknown; createdAt:string; sensitive:boolean; replayable:boolean; recoveryOnly?:boolean; state:'SENDING'|'UNKNOWN' }
export class ApiError extends Error {
  status:number; code:string; requestId:string;
  constructor(status:number,code:string,message:string,requestId='') {super(message);this.name='ApiError';this.status=status;this.code=code;this.requestId=requestId;}
}
export class StaleResponse extends Error {constructor(){super('响应已过时');this.name='StaleResponse';}}
export class UnknownSubmission extends Error {key:string;constructor(key:string){super('提交结果确认中，请使用原请求确认结果。');this.name='UnknownSubmission';this.key=key;}}
const safeClone = <T>(value:T):T => JSON.parse(JSON.stringify(value));
const isRecord = (value:unknown):value is Record<string,unknown> => value!==null&&typeof value==='object'&&!Array.isArray(value);
function unwrap<T>(response:HttpResponse):T {
  const body=response.body;
  if(response.status>=200&&response.status<300&&isRecord(body)&&Object.prototype.hasOwnProperty.call(body,'data'))return body.data as T;
  const error=isRecord(body)&&isRecord(body.error)?body.error:{};
  throw new ApiError(response.status,String(error.code||'INVALID_RESPONSE'),String(error.message||'服务返回内容无法确认，请重试。'),isRecord(body)?String(body.requestId||''):'');
}

/** Transport-independent ownership and durable intent; never grants server authority. */
export class Client {
  session:Session|null=null; generation=0; pending:Pending[]=[];
  private transport:Transport; private storage:Storage; private mode:'BEARER'|'COOKIE'; private key:()=>string;private cookieControls?:()=>void;
  private token=''; private control:Promise<unknown>=Promise.resolve(); private reads=new Map<string,number>(); private listeners=new Set<()=>void>(); private expired=false; private cookieUnknown=false; private storageInitialized=false;
  constructor(transport:Transport,storage:Storage,mode:'BEARER'|'COOKIE',key:()=>string,cookieControls?:()=>void){
    this.transport=transport;this.storage=storage;this.mode=mode;this.key=key;this.cookieControls=cookieControls;
    try{this.token=mode==='BEARER'?(storage.get('qixu.token')||''):'';}catch{/* No readable credential means no adopted identity. */}
    try{this.refreshPending();}catch{/* The document can render; mutations must retry the same storage checks. */}
  }
  private storageError(){return new ApiError(503,'LOCAL_STORAGE_UNAVAILABLE','本地恢复凭据暂不可读写，请检查存储后再确认原请求；不要重新创建提交。');}
  private metadataError(){return new ApiError(503,'LOCAL_RECOVERY_METADATA_INVALID','本地恢复记录不完整且无法确认原坐标；已停止新提交，请保留记录并检查恢复存储。');}
  private initializeStorage(){
    const storage=this.storage;
    // One immutable record per intent. State is intentionally UNKNOWN after reload.
    if(storage.get('qixu.pending-format')!=='per-intent-v1'){
      const bytes=storage.get('qixu.pending')??'[]';let rows:unknown;
      try{rows=JSON.parse(bytes);}catch{throw this.metadataError();}
      if(!Array.isArray(rows))throw this.metadataError();
      const records=new Map<string,string>();
      // Validate the whole legacy collection before changing marker or bytes.
      for(const raw of rows){const p=this.decode(raw);if(!p)throw this.metadataError();const name=this.intentKey(p),value=this.intentBytes(p);if(records.has(name)&&records.get(name)!==value)throw this.metadataError();records.set(name,value);}
      for(const [name,value] of records){const existing=storage.get(name);if(existing!==null&&existing!==value)throw this.metadataError();}
      for(const [name,value] of records)if(storage.get(name)===null)storage.set(name,value);
      storage.set('qixu.pending-format','per-intent-v1');storage.remove('qixu.pending');
    }
    this.storageInitialized=true;
  }
  private intentKey(p:Pick<Pending,'actorId'|'key'>){return 'qixu.intent.v1.'+p.actorId+'.'+p.key;}
  private coordinate(name:string):Pick<Pending,'actorId'|'key'>|null{
    const match=/^qixu\.intent\.v1\.([1-9][0-9]*)\.([A-Za-z0-9_-]{8,80})$/.exec(name);
    if(!match||!Number.isSafeInteger(Number(match[1])))return null;
    return {actorId:Number(match[1]),key:match[2]};
  }
  private recoveryRecord(coordinate:Pick<Pending,'actorId'|'key'>):Pending{return {...coordinate,path:'',body:undefined,createdAt:'',sensitive:true,replayable:false,recoveryOnly:true,state:'UNKNOWN'};}
  private decode(raw:unknown):Pending|null{
    if(!isRecord(raw)||typeof raw.key!=='string'||!/^[A-Za-z0-9_-]{8,80}$/.test(raw.key)||!Number.isSafeInteger(raw.actorId)||Number(raw.actorId)<=0||typeof raw.path!=='string'||!/^\/api\/v1\/[A-Za-z0-9/_-]+$/.test(raw.path)||typeof raw.createdAt!=='string'||!Number.isFinite(Date.parse(raw.createdAt))||typeof raw.sensitive!=='boolean'||(!raw.sensitive&&!Object.prototype.hasOwnProperty.call(raw,'body')))return null;
    return {key:raw.key,actorId:Number(raw.actorId),path:raw.path,createdAt:raw.createdAt,sensitive:raw.sensitive,body:raw.sensitive?undefined:raw.body,replayable:!raw.sensitive,state:'UNKNOWN'};
  }
  private intentBytes(p:Pending){return JSON.stringify(p.sensitive?{key:p.key,actorId:p.actorId,path:p.path,createdAt:p.createdAt,sensitive:true,replayable:false,state:'UNKNOWN'}:{...p,state:'UNKNOWN'});}
  refreshPending(){
    try{
      if(!this.storageInitialized)this.initializeStorage();
      const previous=new Map(this.pending.map(p=>[this.intentKey(p),p])),rows:Pending[]=[];
      for(const name of this.storage.keys().filter(k=>k.startsWith('qixu.intent.v1.'))){
        const coordinate=this.coordinate(name);if(!coordinate)throw this.metadataError();
        const bytes=this.storage.get(name);let raw:Pending|null=null;
        try{raw=this.decode(JSON.parse(bytes??'null'));}catch{/* Retain the address; never replay corrupt bytes. */}
        rows.push(raw&&this.intentKey(raw)===name?(previous.get(name)||raw):this.recoveryRecord(coordinate));
      }
      this.pending=rows;
    }catch(e){if(e instanceof ApiError&&e.code==='LOCAL_RECOVERY_METADATA_INVALID')throw e;throw this.storageError();}
    this.changed();
  }
  subscribe(callback:()=>void){this.listeners.add(callback);return()=>this.listeners.delete(callback);}
  private changed(){for(const callback of this.listeners)callback();}
  clearSensitive(){for(const p of this.pending)if(p.sensitive){p.body=undefined;p.replayable=false;p.state='UNKNOWN';}this.changed();}

  private headers(key?:string){const h:Record<string,string>={'Content-Type':'application/json'};if(this.mode==='BEARER'&&this.token)h.Authorization='Bearer '+this.token;if(this.mode==='COOKIE'&&this.session)h['X-CSRF-Token']=this.session.csrfToken;if(key)h['Idempotency-Key']=key;return h;}
  private owned(generation:number,actor?:number){return this.generation===generation&&(actor===undefined||this.session?.actor.id===actor);}
  private unauthorize(generation:number){if(this.generation!==generation)return;this.expired=true;this.transition();}
  private transition(){this.generation++;this.session=null;this.token='';this.clearSensitive();this.reads.clear();try{this.storage.remove('qixu.token');}catch{/* Local storage must not stop identity clearing or remote revocation. */}this.changed();}
  private exclusive<T>(action:()=>Promise<T>):Promise<T>{const next=this.control.then(action,action);this.control=next.catch(()=>undefined);return next;}
  private adopt(session:Session){if(!isRecord(session)||!isRecord(session.actor)||!Number.isSafeInteger(session.actor.id)||!['STUDENT','TEACHER','ADMIN'].includes(String(session.actor.role))||typeof session.csrfToken!=='string'||(this.mode==='BEARER'&&!session.token&&!this.token))throw new ApiError(200,'INVALID_RESPONSE','无法确认登录身份。');const token=session.token||this.token;try{if(this.mode==='BEARER'&&token)this.storage.set('qixu.token',token);}catch{this.transition();throw this.storageError();}this.session=session;this.expired=false;this.token=token;this.changed();}
  get needsReload(){return this.cookieUnknown;}
  externalCookieChanged(){if(this.mode==='COOKIE'&&!this.cookieUnknown){this.cookieUnknown=true;this.transition();}}
  private cookieOwnerError(e:unknown,generation:number){if(this.mode==='COOKIE'&&this.generation===generation&&e instanceof ApiError&&['SESSION_OWNER_CHANGED','CSRF_REQUIRED'].includes(e.code))this.externalCookieChanged();}
  private guardCookie(){if(this.cookieUnknown)throw new ApiError(409,'COOKIE_CONTROL_UNKNOWN','登录状态尚未确认，请重新加载页面后核对身份。');}
  private async cookieControl<T>(action:()=>Promise<T>):Promise<T>{this.guardCookie();try{return await action();}catch(e){if(this.mode==='COOKIE'&&(!(e instanceof ApiError&&e.status>=400&&e.status<500&&e.code!=='INVALID_RESPONSE')||(e instanceof ApiError&&['SESSION_OWNER_CHANGED','CSRF_REQUIRED'].includes(e.code))))this.externalCookieChanged();throw e;}finally{if(this.mode==='COOKIE')this.cookieControls?.();}}
  bootstrap():Promise<Session|null>{return this.exclusive(async()=>{
    this.guardCookie();
    const generation=this.generation;
    try {const result=unwrap<Session>(await this.transport({method:'GET',path:'/api/v1/auth/session',headers:this.headers()}));if(!this.owned(generation))throw new StaleResponse();if(this.mode==='COOKIE'&&this.session&&isRecord(result)&&isRecord(result.actor)&&(result.actor.id!==this.session.actor.id||result.csrfToken!==this.session.csrfToken))this.transition();this.adopt(result);return result;}
    catch(e){if(e instanceof ApiError&&e.status===401){this.unauthorize(generation);return null;}throw e;}
  });}
  login(username:string,password:string):Promise<Session>{return this.exclusive(()=>this.cookieControl(async()=>{
    // Serialize actual cookie controls; dropping a JS callback cannot undo Set-Cookie.
    if(this.session){try{unwrap(await this.transport({method:'POST',path:'/api/v1/auth/logout',headers:this.headers()}));}catch(e){if(!(e instanceof ApiError&&e.status===401))throw e;}}
    this.transition();
    const result=unwrap<Session>(await this.transport({method:'POST',path:'/api/v1/auth/login',headers:{'Content-Type':'application/json'},body:{username,password,mode:this.mode}}));
    this.adopt(result);return result;
  }));}
  logout():Promise<void>{return this.exclusive(()=>this.cookieControl(async()=>{
    const headers=this.headers();this.transition();
    try{unwrap(await this.transport({method:'POST',path:'/api/v1/auth/logout',headers}));}catch(e){if(!(e instanceof ApiError&&e.status===401))throw e;}
  }));}
  get visiblePending(){return this.pending.filter(p=>p.actorId===this.session?.actor.id);}
  get needsLogin(){return !this.session||this.expired;}
  async get<T=Record<string,unknown>>(path:string,slot=path,responseType?:'FILE'):Promise<T>{
    return this.observe<T>('GET',path,undefined,slot,responseType);
  }
  /** Read-only previews use POST for structured input; no business receipt is invented. */
  async observe<T=Record<string,unknown>>(method:'GET'|'POST',path:string,body?:unknown,slot=path,responseType?:'FILE'):Promise<T>{
    this.guardCookie();
    const generation=this.generation,actor=this.session?.actor.id,sequence=(this.reads.get(slot)||0)+1;this.reads.set(slot,sequence);
    let response:HttpResponse|undefined;
    try{response=await this.transport({method,path,headers:this.headers(),body,...(responseType?{responseType}:{})});if(!this.owned(generation,actor)||this.reads.get(slot)!==sequence)throw new StaleResponse();return unwrap<T>(response);}
    catch(e){response?.discard?.();if(!this.owned(generation,actor)||this.reads.get(slot)!==sequence)throw new StaleResponse();this.cookieOwnerError(e,generation);if(e instanceof ApiError&&e.status===401)this.unauthorize(generation);throw e;}
  }
  async mutate<T=Record<string,unknown>>(path:string,body:unknown,options:{sensitive?:boolean}={}):Promise<T>{
    this.guardCookie();
    if(!this.session)throw new ApiError(401,'LOGIN_REQUIRED','请先登录。');
    this.refreshPending();
    if(this.visiblePending.length)throw new ApiError(409,'LOCAL_UNKNOWN_PENDING','有提交尚未确认，请先恢复原请求。');
    if(this.pending.length>=16)throw new ApiError(409,'LOCAL_PENDING_LIMIT','待确认记录已满，请先处理原账号的请求。');
    const key=this.key();if(!/^[A-Za-z0-9_-]{8,80}$/.test(key)||this.pending.some(p=>p.actorId===this.session?.actor.id&&p.key===key))throw new ApiError(422,'LOCAL_KEY_INVALID','无法创建唯一有效请求标识，请重新加载。');
    if(!/^\/api\/v1\/[A-Za-z0-9/_-]+$/.test(path))throw new ApiError(422,'LOCAL_PATH_INVALID','请求目标不正确。');
    const pending:Pending={key,actorId:this.session.actor.id,path,body:safeClone(body),createdAt:new Date().toISOString(),sensitive:options.sensitive||false,replayable:true,state:'SENDING'};
    try{this.storage.set(this.intentKey(pending),this.intentBytes(pending));}catch{
      // A throwing adapter may already have persisted the record. Never send,
      // but expose its original address immediately instead of minting a new key.
      try{this.refreshPending();}catch{if(!this.pending.some(p=>this.sameIntent(p,pending)))this.pending.push(this.recoveryRecord(pending));this.changed();}
      throw new ApiError(503,'LOCAL_STORAGE_UNAVAILABLE','保存恢复凭据时发生异常，本次尚未发送；请先查询或安全停止原请求，不要创建新提交。');
    }
    this.pending.push(pending);this.changed();return this.send<T>(pending);
  }
  private sameIntent(a:Pending,b:Pick<Pending,'actorId'|'key'>){return a.actorId===b.actorId&&a.key===b.key;}
  private remove(pending:Pending){
    // Only reached after a matching server receipt. Local absence is cleanup
    // evidence, never proof that the business mutation did not happen.
    try{this.storage.remove(this.intentKey(pending));}catch{
      let remains:boolean;try{remains=this.storage.get(this.intentKey(pending))!==null;}catch{throw this.storageError();}
      if(remains)throw this.storageError();
    }
    this.pending=this.pending.filter(p=>!this.sameIntent(p,pending));this.changed();
  }
  private async send<T>(pending:Pending):Promise<T>{
    const generation=this.generation;
    const previouslyUnknown=pending.state==='UNKNOWN';
    if(this.session?.actor.id!==pending.actorId)throw new ApiError(403,'ACTOR_CHANGED','请以提交时的同一账号恢复。');
    if(!pending.replayable)throw new ApiError(409,'PRIVATE_BODY_CLEARED','敏感正文已清除，请查询回执或安全停止原意图。');
    try{
      const response=await this.transport({method:'POST',path:pending.path,headers:this.headers(pending.key),body:safeClone(pending.body)});
      const result=unwrap<T>(response);
      if(!isRecord(result)||!isRecord(result.receipt)||result.receipt.key!==pending.key||result.receipt.status!=='COMMITTED')throw new ApiError(200,'MISSING_RECEIPT','响应缺少原请求回执。');
      this.remove(pending);if(!this.owned(generation,pending.actorId))throw new StaleResponse();return result;
    }catch(e){
      if(e instanceof StaleResponse)throw e;
      if(e instanceof ApiError&&e.code==='LOCAL_STORAGE_UNAVAILABLE'){pending.state='UNKNOWN';this.changed();if(!this.owned(generation,pending.actorId))throw new StaleResponse();throw e;}
      let stored:string|null;try{stored=this.storage.get(this.intentKey(pending));}catch{pending.state='UNKNOWN';this.changed();if(!this.owned(generation,pending.actorId))throw new StaleResponse();throw this.storageError();}
      if(!stored){this.pending=this.pending.filter(p=>!this.sameIntent(p,pending));this.changed();throw new StaleResponse();}
      if(!this.pending.some(p=>this.sameIntent(p,pending)))throw new StaleResponse();
      // A rejection describes this attempt, not every retry sharing its key.
      if(e instanceof ApiError&&[400,401,403,404,409,413,422,429].includes(e.status)&&e.code!=='INVALID_RESPONSE'){
        pending.state='UNKNOWN';this.changed();
        if(!this.owned(generation,pending.actorId))throw new StaleResponse();
        this.cookieOwnerError(e,generation);
        if(e.status===401){this.unauthorize(generation);throw e;}
        if(this.cookieUnknown)throw e;
        if(previouslyUnknown)throw new UnknownSubmission(pending.key);
        // Atomically return a committed original, or prevent every late original.
        // This never cancels a committed booking/right. Failure preserves metadata.
        try{
          const result=await this.stop<Record<string,unknown>>(pending.key);
          if(result.intentOutcome==='STOPPED_WITHOUT_EFFECT')throw e;
          return result as T;
        }catch(barrierError){
          if(barrierError===e)throw e;
          if(!this.owned(generation,pending.actorId))throw new StaleResponse();
          throw new UnknownSubmission(pending.key);
        }
      }
      // Another response/receipt may already have proved COMMITTED, or a sensitive
      // intent may have been deliberately cleared by leaving its owner session.
      if(this.pending.some(p=>this.sameIntent(p,pending))){pending.state='UNKNOWN';this.changed();}
      if(!this.owned(generation,pending.actorId))throw new StaleResponse();throw new UnknownSubmission(pending.key);
    }
  }
  private receiptResult<T>(pending:Pending,receipt:unknown):T {
    if(!isRecord(receipt)||receipt.status!=='COMMITTED'||!isRecord(receipt.result)||!isRecord(receipt.result.receipt)||receipt.result.receipt.key!==pending.key||receipt.result.receipt.status!=='COMMITTED')throw new UnknownSubmission(pending.key);
    this.remove(pending);return receipt.result as T;
  }
  async stop<T=Record<string,unknown>>(key:string):Promise<T>{
    this.guardCookie();
    this.refreshPending();
    const pending=this.pending.find(p=>p.key===key&&p.actorId===this.session?.actor.id);
    if(!pending||this.session?.actor.id!==pending.actorId)throw new ApiError(403,'ACTOR_CHANGED','只能以提交时的同一账号确认原意图。');
    const generation=this.generation;
    try{
      const response=await this.transport({method:'POST',path:'/api/v1/receipts/'+encodeURIComponent(key)+'/stop',headers:this.headers()});
      if(!this.owned(generation,pending.actorId))throw new StaleResponse();
      return this.receiptResult<T>(pending,unwrap(response));
    }catch(e){if(!this.owned(generation,pending.actorId))throw new StaleResponse();this.cookieOwnerError(e,generation);if(e instanceof ApiError&&e.status===401)this.unauthorize(generation);throw e;}
  }
  async recover<T=Record<string,unknown>>(key:string,replay=false):Promise<T|null>{
    this.refreshPending();
    const pending=this.pending.find(p=>p.key===key&&p.actorId===this.session?.actor.id);
    if(!pending||this.session?.actor.id!==pending.actorId)throw new ApiError(403,'ACTOR_CHANGED','只能用提交时的同一账号恢复。');
    if(replay)return this.send<T>(pending);
    try{
      const receipt=await this.get<{status:string;result:T}>('/api/v1/receipts/'+encodeURIComponent(key));
      return this.receiptResult<T>(pending,receipt);
    }catch(e){if(e instanceof ApiError&&e.status===404)return null;throw e;}
  }
}

export interface Space {id:number;floorId:number;parentId:number|null;code:string;name:string;kind:string;useMode:string;capacity:number;version:number;x:number;y:number;width:number;height:number;imageKey:string|null;profile:{source?:string;description?:string;notice?:string;lighting?:string;dimensions?:string;airflow?:string;features?:Record<string,boolean>;conditions?:Record<string,string>};availability:string}
export interface Floor {id:number;name:string;building:string;levelNumber:number;version:number;sourceKind:string}
export interface Page<T>{items:T[];total:number;page:number;size:number}
export type Row=Record<string,any>;
export const statusText:Record<string,string>={OPEN:'申请开放',NOT_OPEN:'尚未开放',APPLICATION_OPEN:'填写志愿',FREEZE_DUE:'等待冻结',FROZEN:'等待固定来源',RESULT_PUBLISHED:'结果已公布',FAILED_NO_RESULT:'本轮未产生结果',CLOSED:'周期已结束',PENDING:'待到场',CHECKED_IN:'已确认到场',ENDED:'已离开',EXPIRED:'已到期',CANCELED:'已取消',SUBMITTED:'待审核',APPROVED:'已批准',REJECTED:'未通过',DRAFT:'草稿',PUBLISHED:'已发布',REGISTRATION_OPEN:'预约开放',REGISTRATION_CLOSED:'预约结束',IN_PROGRESS:'活动进行中',COMPLETED:'已结束',CONFIRMED:'已确认',WAITLISTED:'候补',EVENT_CANCELED:'活动已取消',ACTIVE:'有效',ACCEPTED:'已接受',DECLINED:'已拒绝',REVOKED:'已收回',REPLACED:'已原子换位',REPORTED:'待核实',ACKNOWLEDGED:'已受理',VERIFIED:'已核实',IN_REPAIR:'维修中',RESOLVED:'已解决',NOT_REPRODUCED:'未复现',REOPENED:'重新核实',REPAIR_LINKED:'已关联维修',REPAIR_DONE:'维修完成，等待复验',OPENED:'事项已建立',VERIFICATION_FAILED:'复验未通过',WORK_DONE:'工作已完成，待复验',ASSIGNED:'已安排',VERIFIED_CLOSED:'复验关闭',NOTICE:'陈述窗口',DISMISSED:'通知已撤销',APPEALED:'待独立复核',UPHELD:'裁决维持',RESTORED:'原权已恢复',CORRECTED_UNAVAILABLE:'裁决已纠正，原位无法恢复',WAITING:'等待候补',EXITED:'已退出',UNKNOWN:'未知',WORKING:'正常',BROKEN:'损坏',REPAIRING:'维修中',OFFERED:'待确认',UNALLOCATED:'未分配',NO_ACCEPTABLE_SEAT:'无可接受席位'};
export const label=(status:unknown,domain?:'REPAIR')=>domain==='REPAIR'&&status==='OPEN'?'待安排维修':statusText[String(status)]||String(status||'未采集');
export const formatTime=(value:unknown)=>{if(!value)return'未设置';const date=new Date(String(value));if(Number.isNaN(date.getTime()))return'时间无效';return new Intl.DateTimeFormat('zh-CN',{timeZone:'Asia/Shanghai',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hour12:false}).format(date);};
export const localDateTime=(value:unknown)=>{const d=new Date(String(value));if(Number.isNaN(d.getTime()))return'';const parts=new Intl.DateTimeFormat('sv-SE',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hour12:false}).format(d);return parts.replace(' ','T');};
export function businessTime(value:string){if(!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value))throw new ApiError(422,'LOCAL_TIME_INVALID','请选择完整日期和时间。');const full=value+':00+08:00';const parsed=new Date(full);if(!Number.isFinite(parsed.getTime())||localDateTime(parsed.toISOString())!==value)throw new ApiError(422,'LOCAL_TIME_INVALID','日期或时间不正确。');return full;}
export const featureText=(s:Space)=>['window','outlet','quiet','accessible'].map((key,i)=>({key,label:['靠窗','插座','安静','无障碍'][i],value:s.profile.features?.[key]===true?'有':s.profile.features?.[key]===false?'无':'未知'}));
export const query=(params:Record<string,unknown>)=>Object.entries(params).filter(([,v])=>v!==''&&v!==undefined&&v!==null).map(([k,v])=>encodeURIComponent(k)+'='+encodeURIComponent(String(v))).join('&');
export const imageKey=(key:string|null)=>['window-seat','quiet-room','hall'].includes(key||'')?key:'window-seat';

export const factLabel:Record<string,string>={outletCondition:'插座状况',lightCondition:'照明状况',deskCondition:'桌面状况',environmentCondition:'环境状况',window:'靠窗',outlet:'有插座',quiet:'安静条件',accessible:'无障碍通行'};
