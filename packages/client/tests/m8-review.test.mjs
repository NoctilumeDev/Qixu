import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import ts from 'typescript';
import * as vue from 'vue';
import * as core from '../dist/index.js';

const root = path.resolve(import.meta.dirname, '../../..');
const read = name => fs.readFileSync(path.join(root, name), 'utf8');
const ok = data => ({ status: 200, body: { data } });
const session = id => ({ actor: { id, username:'demo'+id, displayName:'demo'+id, role:'STUDENT', studentVerified:true, authVersion:1 }, token:'token'+id, csrfToken:'csrf'+id });
const store = () => { const m = new Map(); return {get:k=>m.get(k)??null,set:(k,v)=>m.set(k,v),remove:k=>m.delete(k),keys:()=>[...m.keys()]}; };
const deferred = () => {let resolve;const promise=new Promise(r=>resolve=r);return {resolve,promise};};
const tick = () => new Promise(r=>setImmediate(r));
let serial=0;
const makeClient = (transport, mode='BEARER') => new core.Client(transport,store(),mode,()=>`m8-review-${++serial}`);

// Run the actual setup with real Vue refs/watchers and Client; navigation and
// Transport are controlled dependencies. This does not claim DOM/device proof.
function setup(file, ctx, exposed) {
  const source=read(file).split('<script setup lang="ts">')[1].split('</script>')[0].replace(/^import .*;\r?$/gm,'');
  const compiled=ts.transpileModule(source,{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ESNext}}).outputText.replace(/^export \{\};?\r?$/gm,'');
  const all={...vue,...core,...ctx};
  const names=Object.keys(all).filter(k=>/^[$A-Za-z_][$A-Za-z0-9_]*$/.test(k)&&k!=='default'&&k!=='__esModule');
  return new Function(...names,compiled+`\nreturn {${exposed}};`)(...names.map(k=>all[k]));
}
function screen(c, auth) {
  return setup('student/src/components/Screen.vue',{defineProps:()=>({view:'favorites',resourceId:0}),defineExpose:()=>{},onMounted:()=>{},auth,client:c,api:(p,s)=>c.get('/api/v1'+p,s),initialize:()=>Promise.resolve(),asset:x=>x,icon:x=>x,errorMessage:e=>e.message,go:()=>{},back:()=>{},kindLabel:{},modeLabel:{},uni:{getStorageSync:()=>'',setStorageSync:()=>{},pageScrollTo:()=>{},setNavigationBarTitle:()=>{}},getCurrentPages:()=>[{}]},'load,leave,favorites,error,loading');
}

test('M8 recover rechecks ownership before receipt adoption',async()=>{
  const d=deferred(),b=deferred();
  const c=makeClient(r=>r.path.endsWith('/auth/session')?b.promise:r.method==='GET'?d.promise:Promise.reject(Error('lost response')),'COOKIE');c.session=session(1);
  await assert.rejects(c.mutate('/api/v1/feedback',{description:'demo'},{sensitive:true}),core.UnknownSubmission);
  const key=c.visiblePending[0].key;
  const p=c.recover(key);const refused=assert.rejects(p,core.StaleResponse);const identity=c.bootstrap();await tick();
  d.resolve(ok({status:'COMMITTED',result:{receipt:{key,status:'COMMITTED'},owner:1}}));b.resolve(ok(session(2)));
  await Promise.all([refused,identity]);
  assert.equal(c.session.actor.id,2);assert.ok(c.pending.some(p=>p.actorId===1&&p.key===key),'old recovery coordinate retained');
});
test('M8 a legally settled receipt is not retroactively changed by later login',async()=>{
  const c=makeClient(r=>r.method==='GET'?Promise.resolve(ok({status:'COMMITTED',result:{receipt:{key:r.path.split('/').at(-1),status:'COMMITTED'},owner:1}})):r.path.endsWith('/login')?Promise.resolve(ok(session(2))):r.path.endsWith('/logout')?Promise.resolve(ok({})):Promise.reject(Error('lost')));c.session=session(1);
  await assert.rejects(c.mutate('/api/v1/favorites',{spaceId:2000}),core.UnknownSubmission);
  const result=await c.recover(c.visiblePending[0].key);await c.login('demo2','demo');assert.equal(result.owner,1);assert.equal(c.session.actor.id,2);
});
test('M8 Screen leave cannot repopulate favorites',async()=>{
  const d=deferred(),c=makeClient(()=>d.promise);c.session=session(1);const auth=vue.reactive({session:c.session,generation:c.generation,loading:false});
  const scope=vue.effectScope();try{const ui=scope.run(()=>screen(c,auth));const p=ui.load();await tick();ui.leave();d.resolve(ok([{id:101,owner:1}]));await p;assert.deepEqual(ui.favorites.value,[]);assert.equal(ui.error.value,'');assert.equal(ui.loading.value,false);}finally{scope.stop();}
});
test('M8 Screen generation change cannot repopulate favorites',async()=>{
  const d=deferred(),b=deferred(),c=makeClient(r=>r.path.endsWith('/auth/session')?b.promise:d.promise,'COOKIE');c.session=session(1);
  const auth=vue.reactive({session:c.session,generation:c.generation,loading:false});const scope=vue.effectScope();
  try{const ui=scope.run(()=>screen(c,auth));c.subscribe(()=>{auth.session=c.session;auth.generation=c.generation;});const p=ui.load(),identity=c.bootstrap();await tick();d.resolve(ok([{id:101,owner:1}]));b.resolve(ok(session(2)));await Promise.all([p,identity]);await vue.nextTick();assert.deepEqual(ui.favorites.value,[]);assert.equal(ui.error.value,'');assert.equal(ui.loading.value,false);}finally{scope.stop();}
});
test('M8 waitlist exit uses its own version',()=>{
  const button=read('student/src/components/PersonalFlows.vue').match(/<button[^>]*@click="([^"]+)"[^>]*>只退出候补<\/button>/);assert.ok(button);
  for(const revision of [1,2,3]){const data={current_version:revision,waitlist:{version:1,effectiveStatus:'ACTIVE'}},sent=[];new Function('data','id','write',button[1])(data,31,(path,body)=>sent.push({path,body}));assert.equal(sent[0].body.version,data.waitlist.version);assert.equal(sent[0].body.action,'EXIT_WAITLIST');assert.match(sent[0].path,/waitlist/);}
});
test('M8 boolean facts reject unknown rather than manufacturing absence',async()=>{
  const auth=vue.reactive({session:{actor:{id:4,role:'ADMIN'},adminFloors:[100]},generation:0});const scope=vue.effectScope();
  try{const ui=scope.run(()=>setup('admin/src/components/ManagementFlows.vue',{defineProps:()=>({section:'feedback',id:1}),useRoute:()=>({query:{}}),useRouter:()=>({replace:()=>{}}),onBeforeUnmount:()=>{},auth,client:{clearSensitive:()=>{}},api:async()=>({}),errorMessage:e=>e.message},'form,addFact,error'));
    for(const key of ['window','outlet','quiet','accessible']){ui.form.factKey=key;await vue.nextTick();ui.form.factValue='UNKNOWN';ui.form.facts=[];ui.addFact();assert.equal(ui.form.facts.length,0,'unknown must not become false');assert.ok(ui.error.value);ui.form.factValue='true';ui.addFact();assert.equal(ui.form.facts[0].value,true);ui.form.factValue='false';ui.addFact();assert.equal(ui.form.facts[0].value,false);}
  }finally{scope.stop();}
});
for(const surface of ['admin','student'])test(`M8 ${surface} recovery message belongs to its current actor`,async()=>{
  const d=deferred(),auth=vue.reactive({session:session(1),generation:0,pending:[]}),scope=vue.effectScope();
  let result=Promise.resolve({receipt:{key:'owned-key',status:'COMMITTED'}});
  const c={recover:()=>result,stop:()=>result};
  try{
    const ui=scope.run(()=>setup(`${surface}/src/components/RequestState.vue`,{auth,client:c,pendingAction:()=>result,errorMessage:e=>e.message,go:()=>{},onBeforeUnmount:()=>{}},'recover,stop,message,busy'));
    await ui.recover('owned-key');assert.match(ui.message.value,/已确认/);assert.equal(ui.busy.value,false);
    auth.generation++;auth.session=session(2);assert.equal(ui.message.value,'');
    result=d.promise;const pending=ui.stop('old-key');assert.equal(ui.busy.value,true);
    d.resolve({intentOutcome:'STOPPED_WITHOUT_EFFECT'});auth.generation++;auth.session=session(3);
    await pending;assert.equal(ui.message.value,'');assert.equal(ui.busy.value,false);
    await ui.recover('current-key');assert.match(ui.message.value,/安全停止/);
  }finally{scope.stop();}
});
test('M8 management result message is cleared before entering another module',async()=>{
  const props=vue.reactive({section:'repairs',id:1}),auth=vue.reactive({session:{actor:{id:4,role:'ADMIN'},adminFloors:[100]},generation:0}),scope=vue.effectScope();
  try{
    const ui=scope.run(()=>setup('admin/src/components/ManagementFlows.vue',{defineProps:()=>props,useRoute:()=>({query:{}}),useRouter:()=>({replace:()=>{}}),onBeforeUnmount:()=>{},auth,client:{clearSensitive:()=>{},mutate:async()=>({receipt:{key:'m8-legitimate-result'}})},api:async()=>({}),errorMessage:e=>e.message},'write,message'));
    await tick();await ui.write('/admin/repairs/1/actions',{});assert.match(ui.message.value,/已确认回执/);
    props.section='batches';assert.equal(ui.message.value,'');await tick();assert.equal(ui.message.value,'');
  }finally{scope.stop();}
});
