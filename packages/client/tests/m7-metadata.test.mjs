import test from 'node:test';
import assert from 'node:assert/strict';
import {Client,ApiError} from '../dist/index.js';
const key='m7_original_metadata_key',name='qixu.intent.v1.1.'+key;
const storage=()=>{const map=new Map();return {get:k=>map.get(k)??null,set:(k,v)=>map.set(k,v),remove:k=>map.delete(k),keys:()=>[...map.keys()]};};
const row=(actorId=1)=>({key,actorId,path:'/api/v1/favorites',body:{spaceId:2000,selected:true},createdAt:'2026-10-03T00:00:00Z',sensitive:false});
const ok=data=>({status:200,body:{data}});
const result={receipt:{key,status:'COMMITTED'}};
const session={actor:{id:1,username:'u1',displayName:'同学',role:'STUDENT',studentVerified:true,authVersion:1},csrfToken:'csrf',token:'token'};
const make=(store,transport)=>{const c=new Client(transport,store,'BEARER',()=>'m7_forbidden_fresh_key');c.session=session;return c;};
const error=code=>e=>e instanceof ApiError&&e.code===code;

test('M7 corrupt addressed bytes preserve owner key and permit only receipt query or safe stop',async()=>{
  for(const action of ['recover','stop']){
    const s=storage();s.set(name,'{"actorId":1');s.set('qixu.intent.v1.2.'+key,JSON.stringify(row(2)));
    const calls=[];const c=make(s,async r=>{calls.push(r);return ok({status:'COMMITTED',result});});
    assert.equal(c.visiblePending.length,1);assert.equal(c.visiblePending[0].key,key);
    assert.equal(c.visiblePending[0].recoveryOnly,true);assert.equal(c.visiblePending[0].replayable,false);
    await assert.rejects(c.mutate('/api/v1/favorites',{}),error('LOCAL_UNKNOWN_PENDING'));assert.equal(calls.length,0);
    await c[action](key);assert.equal(calls.length,1);assert.ok(calls[0].path.includes('/receipts/'+key));
    assert.equal(s.get(name),null);assert.ok(s.get('qixu.intent.v1.2.'+key));
  }
});
test('M7 mismatched or invalid payload cannot be replayed from a valid intent name',async()=>{
  for(const bad of [{...row(2)}, {...row(),path:'/foreign'}, {...row(),createdAt:'invalid'},null]){
    const s=storage(),bytes=JSON.stringify(bad);s.set(name,bytes);let calls=0;
    const c=make(s,async()=>{calls++;return ok({});});
    assert.equal(c.visiblePending.length,1);assert.equal(c.visiblePending[0].actorId,1);
    assert.equal(c.visiblePending[0].body,undefined);assert.equal(c.visiblePending[0].path,'');assert.equal(c.visiblePending[0].createdAt,'');
    await assert.rejects(c.recover(key,true),error('PRIVATE_BODY_CLEARED'));assert.equal(calls,0);assert.equal(s.get(name),bytes);
  }
});
test('M7 malformed intent coordinate blocks new writes without discarding raw metadata',async()=>{
  for(const badName of ['qixu.intent.v1.x.'+key,'qixu.intent.v1.0.'+key,'qixu.intent.v1.9007199254740992.'+key,'qixu.intent.v1.1.bad']){
    const s=storage();s.set('qixu.pending-format','per-intent-v1');s.set(badName,'raw');let calls=0;
    const c=make(s,async()=>{calls++;return ok({});});
    await assert.rejects(c.mutate('/api/v1/favorites',{}),error('LOCAL_RECOVERY_METADATA_INVALID'));assert.equal(calls,0);assert.equal(s.get(badName),'raw');
  }
});
test('M7 malformed legacy collection is not marked migrated or erased',async()=>{
  for(const bytes of ['{', '{}',JSON.stringify([row(),{}]),JSON.stringify([row(),{...row(),body:{spaceId:2001,selected:true}}])]){
    const s=storage();s.set('qixu.pending',bytes);let calls=0;const c=make(s,async()=>{calls++;return ok({});});
    await assert.rejects(c.mutate('/api/v1/favorites',{}),error('LOCAL_RECOVERY_METADATA_INVALID'));assert.equal(calls,0);
    assert.equal(s.get('qixu.pending'),bytes);assert.equal(s.get('qixu.pending-format'),null);assert.equal(s.get(name),null);
  }
});
test('M7 interrupted legacy migration retains original collection and can finish without duplicate effects',async()=>{
  const s=storage(),other={...row(),key:'m7_second_metadata_key'};s.set('qixu.pending',JSON.stringify([row(),other]));
  const set=s.set;let failed=false;s.set=(k,v)=>{set(k,v);if(k===name&&!failed){failed=true;throw Error('write completed then failed');}};
  const calls=[];const c=make(s,async r=>{calls.push(r);return ok({status:'COMMITTED',result:{receipt:{key:r.path.split('/').at(-1),status:'COMMITTED'}}});});
  assert.ok(s.get('qixu.pending'));assert.equal(s.get('qixu.pending-format'),null);
  c.refreshPending();assert.equal(c.visiblePending.length,2);assert.equal(s.get('qixu.pending'),null);
  await c.recover(key);assert.equal(c.visiblePending.length,1);assert.equal(c.visiblePending[0].key,other.key);assert.equal(calls.length,1);
});
test('M7 write then throw exposes original durable key without sending a new request',async()=>{
  const s=storage(),set=s.set,calls=[];let failed=false;
  const c=make(s,async r=>{calls.push(r);return ok({status:'COMMITTED',result:{receipt:{key:'m7_forbidden_fresh_key',status:'COMMITTED'}}});});
  s.set=(k,v)=>{set(k,v);if(k.startsWith('qixu.intent.v1.')&&!failed){failed=true;throw Error('write completed then failed');}};
  await assert.rejects(c.mutate('/api/v1/favorites',{spaceId:2000,selected:true}),error('LOCAL_STORAGE_UNAVAILABLE'));
  assert.equal(calls.length,0);assert.equal(c.visiblePending.length,1);assert.equal(c.visiblePending[0].key,'m7_forbidden_fresh_key');assert.equal(c.visiblePending[0].state,'UNKNOWN');
  await assert.rejects(c.mutate('/api/v1/favorites',{}),error('LOCAL_UNKNOWN_PENDING'));
  await c.recover('m7_forbidden_fresh_key');assert.equal(calls.length,1);assert.equal(calls[0].method,'GET');assert.equal(c.visiblePending.length,0);
});
test('M7 confirmed receipt plus verified deletion completes cleanup when remove throws after effect',async()=>{
  for(const action of ['recover','stop']){
    const s=storage();s.set(name,JSON.stringify(row()));const c=make(s,async()=>ok({status:'COMMITTED',result}));
    const remove=s.remove;s.remove=k=>{remove(k);if(k===name)throw Error('delete completed then failed');};
    assert.deepEqual(await c[action](key),result);assert.equal(c.visiblePending.length,0);assert.equal(s.get(name),null);
  }
});
test('M7 corruption after valid memory scan clears replay body and retains recovery coordinate',async()=>{
  const s=storage();s.set(name,JSON.stringify(row()));let calls=0;const c=make(s,async()=>{calls++;return ok({});});
  assert.ok(c.visiblePending[0].body);s.set(name,'{');c.refreshPending();
  assert.equal(c.visiblePending.length,1);assert.equal(c.visiblePending[0].body,undefined);assert.equal(c.visiblePending[0].recoveryOnly,true);
  await assert.rejects(c.recover(key,true),error('PRIVATE_BODY_CLEARED'));assert.equal(calls,0);assert.equal(s.get(name),'{');
});
