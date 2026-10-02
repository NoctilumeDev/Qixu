import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {CHAIN,VERIFIER,verifyProof} from './verify.mjs';
const beacon=JSON.parse(readFileSync(new URL('./fixtures/quicknet-32721736.json',import.meta.url),'utf8'));
const input=()=>({chain:CHAIN,round:beacon.round,beacon:{...beacon}});

test('retained real public beacon is verified with the pinned BLS root',async()=>{
  const proof=await verifyProof(input());
  assert.equal(proof.verified,true); assert.equal(proof.randomness,beacon.randomness);assert.equal(proof.verifier,VERIFIER);
});
test('matching fake randomness hash does not make a fake signature valid',async()=>{
  const body=input(); body.beacon.signature='00'.repeat(48);
  body.beacon.randomness=createHash('sha256').update(Buffer.from(body.beacon.signature,'hex')).digest('hex');
  await assert.rejects(verifyProof(body));
});
test('wrong round, chain, public proof shape and hash fail closed',async()=>{
  for(const mutate of [b=>b.round++,b=>b.chain='00'.repeat(32),b=>b.beacon.randomness='00'.repeat(32),b=>b.beacon.previous_signature='ab',b=>b.beacon.signature='00']){
    const body=input();mutate(body);await assert.rejects(verifyProof(body));
  }
});
test('stdin helper is a finite offline process, rejecting malformed and oversized input',()=>{
  const helper=new URL('./verify.mjs',import.meta.url);
  for(const body of ['not json','x'.repeat(12001)]){
    const result=spawnSync(process.execPath,[fileURLToPath(helper)],{input:body,timeout:5000,encoding:'utf8'});
    assert.equal(result.status,1);assert.deepEqual(JSON.parse(result.stdout),{verified:false,code:'INVALID_PROOF'});
  }
  const valid=spawnSync(process.execPath,[fileURLToPath(helper)],{input:JSON.stringify(input()),timeout:5000,encoding:'utf8'});
  assert.equal(valid.status,0);assert.equal(JSON.parse(valid.stdout).verified,true);
});
