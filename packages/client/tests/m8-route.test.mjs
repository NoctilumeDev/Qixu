import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
import ts from 'typescript';
import * as core from '../dist/index.js';

// Actual ManagementFlows setup and Client, using the admin's Vue/Router instance.
// The renderer/history and Transport are offline dependencies, not a browser/DB.
// Harness entry: node packages/client/tests/m8-route.test.mjs --harness-only
const root=path.resolve(import.meta.dirname,'../../..');
const adminRequire=createRequire(path.join(root,'admin/package.json'));
const vue=adminRequire('vue'),routerApi=adminRequire('vue-router');
const read=name=>fs.readFileSync(path.join(root,name),'utf8');
const ok=data=>({status:200,body:{data}});
const tick=async()=>{await Promise.resolve();await vue.nextTick();await new Promise(r=>setImmediate(r));};
const feedback={id:1,space_id:2000,status:'VERIFIED',version:3,attachments:[],repairs:[{id:1,status:'OPEN'}]};
const repair={id:1,space_id:2000,status:'OPEN',version:1};
const venues=[{id:7,space_id:2000,floor_id:100,status:'SUBMITTED',version:1}];
const events=[{id:9,venue_request_id:7,status:'PUBLISHED',version:1}];
const floors=[{id:100,name:'fixture floor',building:'fixture building',level:1,mapWidth:560,mapHeight:360,version:1,openingRules:{}}];
const spaces={items:[{id:2000,floorId:100,parentId:null,code:'fixture-seat',name:'fixture seat',kind:'SEAT',useMode:'BOOKABLE',capacity:1,version:1,x:0,y:0,width:20,height:20,imageKey:null,profile:{source:'fixture',description:'offline fixture'},availability:'NOT_QUERIED'}],total:1};
function responseFor(request) {
  const p=request.path;
  if(p==='/api/v1/feedback/1')return feedback;
  if(p==='/api/v1/admin/repairs/1')return repair;
  if(p==='/api/v1/admin/feedback')return {items:[feedback],total:1};
  if(p==='/api/v1/admin/venue-requests'||p==='/api/v1/venue-requests')return venues;
  if(p==='/api/v1/organizer/events')return events;
  if(p==='/api/v1/floors')return floors;
  if(p.startsWith('/api/v1/spaces?floor=100&page='))return spaces;
  throw new Error('Unexpected fixture request '+p);
}
function versionOf(name) {
  let at=path.dirname(adminRequire.resolve(name));
  for(;;) {
    const manifest=path.join(at,'package.json');
    if(fs.existsSync(manifest)){const value=JSON.parse(fs.readFileSync(manifest,'utf8'));if(value.name===name)return value.version;}
    const next=path.dirname(at);assert.notEqual(next,at,'Cannot locate dependency '+name);at=next;
  }
}
const hostNode=(kind,text='')=>({kind,text,parent:null,children:[]});
const host={
  createElement:kind=>hostNode(kind),createText:text=>hostNode('text',text),createComment:text=>hostNode('comment',text),
  setText:(n,text)=>n.text=text,setElementText:(n,text)=>n.text=text,patchProp:()=>{},
  insert:(n,parent,anchor=null)=>{if(n.parent){const siblings=n.parent.children;siblings.splice(siblings.indexOf(n),1);}n.parent=parent;const at=anchor?parent.children.indexOf(anchor):-1;if(at<0)parent.children.push(n);else parent.children.splice(at,0,n);},
  remove:n=>{if(n.parent){const siblings=n.parent.children;siblings.splice(siblings.indexOf(n),1);n.parent=null;}},
  parentNode:n=>n.parent,nextSibling:n=>n.parent?.children[n.parent.children.indexOf(n)+1]??null,
};
function setup(props,auth,client,api) {
  const body=read('admin/src/components/ManagementFlows.vue').split('<script setup lang="ts">')[1].split('</script>')[0].replace(/^import .*;\r?$/gm,'');
  const compiled=ts.transpileModule(body,{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ESNext}}).outputText.replace(/^export \{\};?\r?$/gm,'');
  const dependencies={...vue,...core,defineProps:()=>props,useRoute:routerApi.useRoute,useRouter:routerApi.useRouter,auth,client,api,errorMessage:e=>e instanceof core.StaleResponse?'':e.message};
  const names=Object.keys(dependencies).filter(k=>/^[$A-Za-z_][$A-Za-z0-9_]*$/.test(k)&&!['default','__esModule'].includes(k));
  return new Function(...names,compiled+'\nreturn {load,items,data,aux,spaces,loading,error,message};')(...names.map(k=>dependencies[k]));
}
function fixture() {
  const requests=[],reads=new Set(),scopes=[];let ui,closing=false,mounted=false;
  const memory=new Map(),storage={get:k=>memory.get(k)??null,set:(k,v)=>memory.set(k,v),remove:k=>memory.delete(k),keys:()=>[...memory.keys()]};
  const client=new core.Client(r=>{
    const request={number:requests.length+1,path:r.path,done:false,resolve:null};requests.push(request);
    if(closing){request.done=true;return Promise.resolve(ok(responseFor(request)));}
    return new Promise(resolve=>request.resolve=resolve);
  },storage,'COOKIE',()=>crypto.randomUUID());
  client.session={actor:{id:4,role:'ADMIN',username:'admin1',displayName:'fixture administrator',studentVerified:false,authVersion:1},csrfToken:'fixture-csrf',adminFloors:[100]};
  const auth=vue.reactive({session:client.session,generation:client.generation});
  const unsubscribe=client.subscribe(()=>{auth.session=client.session;auth.generation=client.generation;});
  const api=(p,slot)=>{const promise=client.get('/api/v1'+p,slot);reads.add(promise);promise.then(()=>reads.delete(promise),()=>reads.delete(promise));return promise;};
  const Management=vue.defineComponent({name:'M8ActualManagementSetup',props:{section:String,id:Number},setup(props){scopes.push(vue.getCurrentScope());ui=setup(props,auth,client,api);return ()=>vue.h('main',JSON.stringify({section:props.section,id:props.id,dataId:ui.data.value.id??null}));}});
  // Workspace's actual route expressions and child prop order; real Vue updates
  // route projections and child props. Tests never assign props one at a time.
  const Workspace=vue.defineComponent({setup(){const route=routerApi.useRoute(),section=vue.computed(()=>String(route.params.section||'map')),id=vue.computed(()=>Number(route.params.id||0));return ()=>vue.h(Management,{section:section.value,id:id.value});}});
  const router=routerApi.createRouter({history:routerApi.createMemoryHistory(),routes:[{path:'/:section?/:id?',component:Workspace}]});
  const app=vue.createRenderer(host).createApp({render:()=>vue.h(routerApi.RouterView)});app.use(router);
  const pending=p=>requests.filter(r=>!r.done&&(p===undefined||r.path===p));
  async function respond(request) {assert.ok(request,'Expected fixture request');assert.equal(request.done,false,'Request already settled');request.done=true;request.resolve(ok(responseFor(request)));await tick();}
  async function settleAll() {
    for(let round=0;round<40;round++){const waiting=pending();for(const request of waiting)await respond(request);await tick();if(!pending().length&&!reads.size){await tick();if(!pending().length&&!reads.size)return;}}
    assert.fail('Fixture did not settle all read/Transport promises');
  }
  return {
    requests,client,pending,respond,settleAll,get ui(){return ui;},
    async start(url){await router.push(url);app.mount(hostNode('root'));mounted=true;await tick();assert.ok(ui,'Management setup mounted');},
    async navigate(url){await router.push(url);await tick();},
    async dispose(){
      closing=true;unsubscribe();for(const scope of scopes)scope?.stop();if(mounted)app.unmount();
      await settleAll();assert.ok(scopes.every(scope=>scope&&!scope.active),'All component scopes stopped');
      assert.equal(pending().length,0,'No deferred Transport remains');assert.equal(reads.size,0,'No Client read promise remains');
      console.log('M8_ROUTE_CLEANUP',JSON.stringify({stoppedScopes:scopes.length,pendingTransport:pending().length,pendingClientReads:reads.size}));
    },
  };
}
async function feedbackToRepair(initialRoute) {
  const f=fixture();try {
    await f.start(initialRoute);await f.settleAll();assert.equal(f.ui.data.value.id,1,'Normal feedback fixture is loaded');
    const initialRequests=f.requests.length;await f.navigate('/repairs/1');
    await f.respond(f.pending('/api/v1/admin/repairs/1')[0]);
    assert.equal(f.pending('/api/v1/admin/feedback').length,1,'Current repair auxiliary read waits');
    // Current repair is waiting on its auxiliary lane when a legal old feedback
    // response arrives. A stale continuation must not append another read.
    for(const old of f.pending('/api/v1/feedback/1'))await f.respond(old);
    await f.settleAll();
    const observed={dataId:f.ui.data.value.id??null,auxiliaryRequests:f.requests.slice(initialRequests).filter(r=>r.path==='/api/v1/admin/feedback').length,loading:f.ui.loading.value,error:f.ui.error.value,actor:f.client.session.actor.id,generation:f.client.generation};
    console.log('M8_ROUTE_OBSERVATION',JSON.stringify({initialRoute,...observed}));
    assert.deepEqual(observed,{dataId:1,auxiliaryRequests:1,loading:false,error:'',actor:4,generation:0},'Normal navigation displays repair; no stale feedback auxiliary query');
  } finally {await f.dispose();}
}
async function eventsToVenueChoices() {
  const f=fixture();try {
    await f.start('/events');assert.ok(f.pending('/api/v1/organizer/events').length,'Old event main read is intentionally in flight');
    await f.navigate('/venues');await f.respond(f.pending('/api/v1/admin/venue-requests')[0]);
    assert.equal(f.pending('/api/v1/floors').length,1,'Current venue chooser floor lane waits');
    for(const old of f.pending('/api/v1/organizer/events'))await f.respond(old);
    // Settle any wrongly appended old event auxiliary before the chooser's
    // original floor response, exposing a superseding old loadSpaceChoices call.
    for(const oldAux of f.pending('/api/v1/venue-requests'))await f.respond(oldAux);
    await f.settleAll();
    const observed={oldVenueAuxiliaries:f.requests.filter(r=>r.path==='/api/v1/venue-requests').length,floorReads:f.requests.filter(r=>r.path==='/api/v1/floors').length,itemIds:f.ui.items.value.map(r=>r.id),spaceIds:f.ui.spaces.value.map(r=>r.id),loading:f.ui.loading.value,error:f.ui.error.value,actor:f.client.session.actor.id,generation:f.client.generation};
    console.log('M8_ROUTE_CONTINUATION_OBSERVATION',JSON.stringify(observed));
    assert.deepEqual(observed,{oldVenueAuxiliaries:0,floorReads:1,itemIds:[7],spaceIds:[2000],loading:false,error:'',actor:4,generation:0},'Old events cannot append auxiliary queries or take the current venue chooser lane');
  } finally {await f.dispose();}
}

if(process.argv.includes('--harness-only')) {
  const f=fixture();try{await f.start('/feedback/1?page=1');await f.settleAll();assert.equal(f.ui.data.value.id,1);console.log('M8_ROUTE_HARNESS_ONLY_READY',JSON.stringify({vue:vue.version,router:versionOf('vue-router'),typescript:ts.version,client:'existing dist',renderer:'memory',qualification:'harness-only; no route regression/browser/DB claim'}));}finally{await f.dispose();}
} else {
  test('M8 route with page loads repair after feedback navigation',()=>feedbackToRepair('/feedback/1?page=1'));
  test('M8 route without page loads repair after feedback navigation',()=>feedbackToRepair('/feedback/1'));
  test('M8 stale event read cannot append venue auxiliary or displace venue choices',eventsToVenueChoices);
}
