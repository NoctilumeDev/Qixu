import test from 'node:test';
import assert from 'node:assert/strict';
import {Client,ApiError,StaleResponse,UnknownSubmission,businessTime} from '../dist/index.js';
const storage=()=>{const m=new Map();return {get:k=>m.get(k)||null,set:(k,v)=>m.set(k,v),remove:k=>m.delete(k),keys:()=>Array.from(m.keys())};};
const ok=data=>({status:200,body:{data}});
const no=(status,code)=>({status,body:{error:{code,message:code}}});
const session=id=>({actor:{id,username:'u'+id,displayName:'同学',role:'STUDENT',studentVerified:true,authVersion:1},csrfToken:'csrf'+id,token:'token'+id});
const receipt=(key,extra={})=>({receipt:{key,status:'COMMITTED'},...extra});
function deferred(){let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b});return {promise,resolve,reject};}
const tick=()=>new Promise(r=>setImmediate(r));
const client=(transport,s=storage(),mode='BEARER')=>new Client(transport,s,mode,()=>`submission_${++serial}`);let serial=0;

test('old A success cannot populate B, and old A 401 cannot log B out',async()=>{
  for(const response of [ok({secret:'A'}),no(401,'UNAUTHORIZED')]){
    const slow=deferred();const c=client(r=>r.path==='/slow'?slow.promise:Promise.resolve(r.path.endsWith('/login')?ok(session(r.body.username==='A'?1:2)):ok({loggedOut:true})));
    await c.login('A','p');const reading=c.get('/slow');await c.login('B','p');slow.resolve(response);
    await assert.rejects(reading,StaleResponse);assert.equal(c.session.actor.id,2);
  }
});
test('a later search wins even when earlier success returns last',async()=>{
  const first=deferred(),second=deferred();const c=client(r=>r.path==='/first'?first.promise:second.promise);c.session=session(1);
  const a=c.get('/first','search'),b=c.get('/second','search');second.resolve(ok('new'));assert.equal(await b,'new');first.resolve(ok('old'));await assert.rejects(a,StaleResponse);
});
test('dropped response, receipt404, reload and same-key replay have one business effect',async()=>{
  const store=storage(),server=new Map(),seen=[];
  const transport=async r=>{
    if(r.path.endsWith('/login'))return ok(session(1));
    if(r.method==='GET')return r.path.includes('/receipts/')?no(404,'NOT_FOUND'):ok([]);
    const key=r.headers['Idempotency-Key'];seen.push({key,body:r.body});
    if(!server.has(key)){server.set(key,receipt(key,{application:91}));throw Error('response dropped after commit');}
    return ok(server.get(key));
  };
  const c=client(transport,store);await c.login('A','p');await assert.rejects(c.mutate('/api/v1/applications',{version:1}),UnknownSubmission);
  const key=c.visiblePending[0].key;assert.equal(await c.recover(key),null);await c.get('/unrelated');assert.equal(c.visiblePending.length,1);
  const reopened=client(transport,store);await reopened.login('A','p');assert.equal(reopened.visiblePending[0].key,key);
  assert.equal((await reopened.recover(key,true)).application,91);assert.equal(server.size,1);assert.equal(reopened.visiblePending.length,0);assert.deepEqual(seen[0],seen[1]);
});
test('an unknown original remains unknown when a retry encounters expired identity',async()=>{
  let attempt=0;const c=client(async r=>r.path.endsWith('/login')?ok(session(1)):++attempt===1?Promise.reject(Error('unknown')):no(401,'UNAUTHORIZED'));
  await c.login('A','p');await assert.rejects(c.mutate('/api/v1/test',{}),UnknownSubmission);const key=c.pending[0].key;
  await assert.rejects(c.recover(key,true),ApiError);assert.equal(c.pending[0].key,key);assert.equal(c.session,null);
});
test('another actor cannot replay or see a prior actor intent',async()=>{
  const c=client(async()=>{throw Error('network');});c.session=session(1);await assert.rejects(c.mutate('/api/v1/test',{version:4}),UnknownSubmission);const key=c.pending[0].key;c.session=session(2);c.generation++;
  assert.equal(c.visiblePending.length,0);await assert.rejects(c.recover(key,true),e=>e.code==='ACTOR_CHANGED');
});
test('a new intention cannot replace an unknown body/key',async()=>{
  let calls=0;const c=client(async()=>{calls++;throw Error('network')});c.session=session(1);const original={version:3};await assert.rejects(c.mutate('/api/v1/test',original),UnknownSubmission);original.version=8;
  await assert.rejects(c.mutate('/api/v1/test',{version:8}),e=>e.code==='LOCAL_UNKNOWN_PENDING');assert.equal(calls,1);assert.equal(c.pending[0].body.version,3);
});
test('a rejected new attempt clears only after a server stop barrier; 500 keeps it',async()=>{
  for(const [response,remains]of [[no(409,'SPACE_CONFLICT'),false],[no(503,'DATABASE_UNAVAILABLE'),true],[{status:200,body:{data:{wrong:true}}},true]]){
    const c=client(async r=>r.path.endsWith('/stop')?ok({status:'COMMITTED',result:receipt(r.path.split('/').at(-2),{intentOutcome:'STOPPED_WITHOUT_EFFECT'})}):response);c.session=session(1);await assert.rejects(c.mutate('/api/v1/test',{}));assert.equal(c.pending.length,remains?1:0);
  }
});
test('cookie login and logout controls never run concurrently',async()=>{
  let running=0,max=0;const release=deferred(),started=[];
  const c=client(async r=>{running++;max=Math.max(max,running);started.push(r.path);if(started.length===1)await release.promise;running--;return ok(r.path.endsWith('/login')?session(1):{loggedOut:true});},storage(),'COOKIE');
  const a=c.login('A','p'),b=c.logout();await tick();assert.equal(started.length,1);release.resolve();await a;await b;assert.equal(max,1);assert.equal(c.session,null);
});
test('private body is cleared but a late network failure retains its recovery tombstone after logout',async()=>{
  const slow=deferred(),store=storage();const c=client(r=>r.path.endsWith('/logout')?Promise.resolve(ok({loggedOut:true})):slow.promise,store);c.session=session(1);
  const sending=c.mutate('/api/v1/feedback',{description:'private evidence'},{sensitive:true});assert.ok(!store.keys().map(k=>store.get(k)||'').join('').includes('private evidence'));await c.logout();slow.reject(Error('late timeout'));await assert.rejects(sending,StaleResponse);assert.equal(c.pending.length,1);assert.equal(c.pending[0].body,undefined);assert.equal(c.pending[0].replayable,false);
});
test('China date-time conversion preserves day and rejects normalized nonexistent dates',()=>{
  assert.equal(businessTime('2026-10-03T00:30'),'2026-10-03T00:30:00+08:00');assert.throws(()=>businessTime('2026-02-31T09:00'),ApiError);assert.throws(()=>businessTime('2026-10-03T24:00'),ApiError);
});

test('an older rejected search cannot replace a newer result or expire its session',async()=>{
  for(const rejected of [Error('offline old request'),new ApiError(401,'SESSION_REQUIRED','expired')]){
    const first=deferred(),second=deferred();const c=client(r=>r.path==='/first'?first.promise:second.promise);c.session=session(1);
    const a=c.get('/first','search'),b=c.get('/second','search');second.resolve(ok('new'));assert.equal(await b,'new');first.reject(rejected);
    await assert.rejects(a,StaleResponse);assert.equal(c.session.actor.id,1);
  }
});

test('invalid request keys are rejected before transport or local persistence',async()=>{
  let calls=0;const store=storage();const c=new Client(async()=>{calls++;throw Error('offline');},store,'BEARER',()=> 'bad!');c.session=session(1);
  await assert.rejects(c.mutate('/api/v1/test',{}),e=>e.code==='LOCAL_KEY_INVALID');assert.equal(calls,0);assert.equal(c.pending.length,0);
});

test('a malformed or mismatched historical receipt never clears the original unknown',async()=>{
  const c=client(async r=>r.method==='POST'?Promise.reject(Error('dropped')):ok({status:'COMMITTED',result:receipt('another_key')}));c.session=session(1);
  await assert.rejects(c.mutate('/api/v1/test',{}),UnknownSubmission);const key=c.pending[0].key;
  await assert.rejects(c.recover(key),UnknownSubmission);assert.equal(c.pending[0].key,key);
});

test('uncertain cookie control requires a new document before another login or business write',async()=>{
  let controls=0;const c=client(async()=>{controls++;throw Error('body lost after Set-Cookie');},storage(),'COOKIE');
  await assert.rejects(c.login('A','p'));assert.equal(c.needsReload,true);await assert.rejects(c.login('B','p'),e=>e.code==='COOKIE_CONTROL_UNKNOWN');
  await assert.rejects(c.bootstrap(),e=>e.code==='COOKIE_CONTROL_UNKNOWN');assert.equal(controls,1);
});


test('private unknown survives reload without its body and cannot be replayed after leave',async()=>{
  const store=storage();const c=client(async r=>r.path.endsWith('/login')?ok(session(1)):Promise.reject(Error('offline')),store);
  await c.login('A','p');await assert.rejects(c.mutate('/api/v1/feedback',{description:'secret-statement'},{sensitive:true}),UnknownSubmission);
  const key=c.pending[0].key;c.clearSensitive();assert.equal(c.pending[0].body,undefined);
  assert.ok(!store.keys().map(k=>store.get(k)||'').join('').includes('secret-statement'));
  const d=client(async r=>r.path.endsWith('/login')?ok(session(1)):no(404,'NOT_FOUND'),store);await d.login('A','p');
  assert.equal(d.visiblePending[0].key,key);assert.equal(d.visiblePending[0].replayable,false);
  await assert.rejects(d.recover(key,true),e=>e.code==='PRIVATE_BODY_CLEARED');assert.equal(await d.recover(key),null);
  await assert.rejects(d.mutate('/api/v1/feedback',{description:'new'}),e=>e.code==='LOCAL_UNKNOWN_PENDING');
});
test('stop recovery trusts only the matching committed original or barrier receipt',async()=>{
  for(const stopped of [true,false]){
    const c=client(async r=>r.path.endsWith('/stop')?ok({status:'COMMITTED',result:receipt(r.path.split('/').at(-2),stopped?{intentOutcome:'STOPPED_WITHOUT_EFFECT'}:{application:19})}):Promise.reject(Error('unknown')));c.session=session(1);
    await assert.rejects(c.mutate('/api/v1/feedback',{description:'private'},{sensitive:true}),UnknownSubmission);const key=c.pending[0].key;c.clearSensitive();
    const result=await c.stop(key);assert.equal(Boolean(result.intentOutcome),stopped);assert.equal(c.pending.length,0);
  }
});
test('stop failure or mismatched receipt cannot erase the intent; another actor cannot stop it',async()=>{
  let response=no(503,'UNAVAILABLE');const c=client(async r=>r.path.endsWith('/stop')?response:Promise.reject(Error('offline')));c.session=session(1);
  await assert.rejects(c.mutate('/api/v1/feedback',{}, {sensitive:true}),UnknownSubmission);const key=c.pending[0].key;c.clearSensitive();
  await assert.rejects(c.stop(key));assert.equal(c.pending.length,1);
  response=ok({status:'COMMITTED',result:receipt('different_key')});await assert.rejects(c.stop(key),UnknownSubmission);assert.equal(c.pending.length,1);
  c.session=session(2);c.generation++;await assert.rejects(c.stop(key),e=>e.code==='ACTOR_CHANGED');assert.equal(c.pending.length,1);
});

test('late transport failure after receipt recovery cannot resurrect an unknown submission',async()=>{
  for(const late of [Error('lost response'),no(409,'STATE_CONFLICT')]){
    const slow=deferred();const c=client(async r=>r.method==='GET'?ok({status:'COMMITTED',result:receipt(c.pending[0].key,{application:31})}):slow.promise);c.session=session(1);
    const sending=c.mutate('/api/v1/applications',{});const key=c.pending[0].key;
    assert.equal((await c.recover(key)).application,31);assert.equal(c.pending.length,0);
    if(late instanceof Error)slow.reject(late);else slow.resolve(late);
    await assert.rejects(sending,StaleResponse);assert.equal(c.pending.length,0);
  }
});

test('private file responses lose ownership on actor change or newer request and release their own asset',async()=>{
  for(const changedActor of [true,false]){
    const slow=deferred();let discarded=0;const c=client(r=>r.path==='/private-a'?slow.promise:Promise.resolve(ok({tempFilePath:'new-file'})));c.session=session(1);
    const old=c.get('/private-a','photo','FILE');
    if(changedActor){c.session=session(2);c.generation++;}else assert.equal((await c.get('/private-b','photo','FILE')).tempFilePath,'new-file');
    slow.resolve({...ok({tempFilePath:'old-private-file'}),discard:()=>discarded++});
    await assert.rejects(old,StaleResponse);assert.equal(discarded,1);
  }
});

test('invalid file response is released; successfully adopted asset stays with its page owner',async()=>{
  let discarded=0,response={...no(403,'FORBIDDEN'),discard:()=>discarded++};const c=client(async()=>response);c.session=session(1);
  await assert.rejects(c.get('/private','photo','FILE'),ApiError);assert.equal(discarded,1);
  response={...ok({tempFilePath:'page-owned'}),discard:()=>discarded++};assert.equal((await c.get('/private','photo','FILE')).tempFilePath,'page-owned');assert.equal(discarded,1);
});

test('external cookie control clears this document owner and body but keeps durable unknown metadata',async()=>{
  const slow=deferred(),store=storage();const c=client(r=>r.method==='GET'?slow.promise:Promise.reject(Error('lost')),store,'COOKIE');c.session=session(1);
  await assert.rejects(c.mutate('/api/v1/feedback',{description:'private A'},{sensitive:true}),UnknownSubmission);const key=c.pending[0].key;const reading=c.get('/private');
  c.externalCookieChanged();slow.resolve(ok({secret:'A'}));await assert.rejects(reading,StaleResponse);
  assert.equal(c.session,null);assert.equal(c.needsReload,true);assert.equal(c.pending[0].key,key);assert.equal(c.pending[0].body,undefined);assert.ok(!store.keys().map(k=>store.get(k)||'').join('').includes('private A'));
  await assert.rejects(c.login('B','p'),e=>e.code==='COOKIE_CONTROL_UNKNOWN');
  const bearer=client(async()=>ok({}),storage());bearer.session=session(1);bearer.externalCookieChanged();assert.equal(bearer.session.actor.id,1);
});

test('cookie read owner mismatch or stale write CSRF cannot adopt another actor or continue this document',async()=>{
  for(const [method,response]of [['GET',no(409,'SESSION_OWNER_CHANGED')],['POST',no(403,'CSRF_REQUIRED')]]){
    const c=client(async()=>response,storage(),'COOKIE');c.session=session(1);
    await assert.rejects(method==='GET'?c.get('/private'):c.mutate('/api/v1/feedback',{}),ApiError);
    assert.equal(c.session,null);assert.equal(c.needsReload,true);await assert.rejects(c.get('/next'),e=>e.code==='COOKIE_CONTROL_UNKNOWN');
  }
});

test('explicit cookie bootstrap identity or session rotation invalidates earlier reads before adopting the new facts',async()=>{
  for(const next of [session(2),{...session(1),csrfToken:'rotated-csrf'}]){
    const slow=deferred();const c=client(r=>r.path.endsWith('/session')?Promise.resolve(ok(next)):slow.promise,storage(),'COOKIE');c.session=session(1);
    const read=c.get('/private');const generation=c.generation;await c.bootstrap();assert.equal(c.generation,generation+1);assert.equal(c.session.csrfToken,next.csrfToken);
    slow.resolve(ok({old:true}));await assert.rejects(read,StaleResponse);
  }
});

test('cookie control attempts signal other documents without broadcasting credentials or performing remote logout',async()=>{
  let signals=0;const c=new Client(async r=>r.path.endsWith('/login')?ok(session(1)):ok({loggedOut:true}),storage(),'COOKIE',()=> 'submission_signal',()=>signals++);
  await c.login('A','p');assert.equal(signals,1);await c.logout();assert.equal(signals,2);c.externalCookieChanged();assert.equal(signals,2);
});

test('another document clearing its stale private cache cannot erase a newer durable intent',async()=>{
  const store=storage();const a=client(async()=>{throw Error('offline A');},store,'COOKIE');a.session=session(1);
  const b=client(async()=>{throw Error('offline B');},store,'COOKIE');b.session=session(2);
  await assert.rejects(b.mutate('/api/v1/feedback',{description:'B private'}, {sensitive:true}),UnknownSubmission);const key=b.pending[0].key;
  a.externalCookieChanged();
  const reload=client(async()=>ok(session(2)),store,'COOKIE');reload.session=session(2);
  assert.deepEqual(reload.visiblePending.map(p=>p.key),[key]);assert.equal(reload.visiblePending[0].body,undefined);
});

test('a rejection in the original document cannot erase another in-flight same-key retry',async()=>{
  const store=storage(),first=deferred(),retry=deferred(),barrier=deferred();
  const a=client(r=>r.path.endsWith('/stop')?barrier.promise:first.promise,store);a.session=session(1);
  const original=a.mutate('/api/v1/applications',{version:1});const originalOutcome=original.catch(e=>e);const key=a.pending[0].key;
  const b=client(()=>retry.promise,store);b.session=session(1);const repeat=b.recover(key,true);
  first.resolve(no(409,'VERSION_CONFLICT'));await tick();
  const reload=client(async()=>ok(session(1)),store);reload.session=session(1);
  assert.deepEqual(reload.visiblePending.map(p=>p.key),[key]);
  const committed=receipt(key,{application:72});retry.resolve(ok(committed));assert.equal((await repeat).application,72);
  barrier.resolve(ok({status:'COMMITTED',result:committed}));assert.equal((await originalOutcome).application,72);
  assert.equal(client(async()=>ok({}),store).pending.length,0);
});

test('separate keys survive a foreign resolution and late failure cannot resurrect the resolved key',async()=>{
  const store=storage(),late=deferred();const a=client(r=>r.method==='GET'?ok({status:'COMMITTED',result:receipt(a.pending[0].key)}):late.promise,store);a.session=session(1);
  const sending=a.mutate('/api/v1/applications',{});const done=sending.catch(e=>e);const key=a.pending[0].key;
  const b=client(async()=>{throw Error('B offline');},store);b.session=session(2);await assert.rejects(b.mutate('/api/v1/venues',{}),UnknownSubmission);const other=b.visiblePending[0].key;
  await a.recover(key);late.reject(Error('late failure'));assert.ok(await done instanceof StaleResponse);
  b.clearSensitive();const reload=client(async()=>ok({}),store);assert.deepEqual(reload.pending.map(p=>p.key),[other]);
});

test('storage quota failure prevents transport rather than losing the only recovery key',async()=>{
  const store=storage();let calls=0;const c=client(async()=>{calls++;return ok({});},store);c.session=session(1);
  const set=store.set;store.set=(k,v)=>{if(k.startsWith('qixu.intent.v1.'))throw Error('quota');return set(k,v);};
  await assert.rejects(c.mutate('/api/v1/venues',{}),e=>e.code==='LOCAL_STORAGE_UNAVAILABLE');assert.equal(calls,0);assert.equal(c.pending.length,0);
});

test('legacy unknown metadata migrates once and old arrays cannot revive a resolved intent',async()=>{
  const store=storage(),key='legacy_submission';store.set('qixu.pending',JSON.stringify([{key,actorId:1,path:'/api/v1/feedback',sensitive:true,createdAt:'2026-10-03T00:00:00Z'}]));
  const c=client(async()=>ok({status:'COMMITTED',result:receipt(key)}),store);c.session=session(1);assert.equal(c.visiblePending[0].replayable,false);await c.recover(key);
  store.set('qixu.pending',JSON.stringify([{key,actorId:1,path:'/api/v1/feedback',sensitive:true,createdAt:'2026-10-03T00:00:00Z'}]));assert.equal(client(async()=>ok({}),store).pending.length,0);
});


test('failure to prove the rejection barrier preserves the key and allows no new intention',async()=>{
  let calls=0;const c=client(async r=>{calls++;return r.path.endsWith('/stop')?no(503,'UNAVAILABLE'):no(422,'INVALID_BODY');});c.session=session(1);
  await assert.rejects(c.mutate('/api/v1/venues',{}),UnknownSubmission);assert.equal(calls,2);assert.equal(c.pending.length,1);
  await assert.rejects(c.mutate('/api/v1/venues',{new:true}),e=>e.code==='LOCAL_UNKNOWN_PENDING');assert.equal(calls,2);
});

test('malformed bootstrap cannot rotate a valid owner or leak a native TypeError',async()=>{
  const c=client(async()=>ok(null),storage(),'COOKIE');c.session=session(1);const generation=c.generation;
  await assert.rejects(c.bootstrap(),e=>e instanceof ApiError&&e.code==='INVALID_RESPONSE');assert.equal(c.generation,generation);
});
