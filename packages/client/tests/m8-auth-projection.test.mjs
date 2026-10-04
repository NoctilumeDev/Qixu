import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
import ts from 'typescript';
import * as core from '../dist/index.js';

// Execute the actual H5-preprocessed runtime and actual Client/Vue. The uni
// transport/storage are controlled dependencies; this is not a browser proof.
const root=path.resolve(import.meta.dirname,'../../..');
const studentRequire=createRequire(path.join(root,'student/package.json'));
const {reactive}=studentRequire('vue');
const actor={id:2,username:'student2',displayName:'fixture student',role:'STUDENT',studentVerified:true,authVersion:1};
const cookieSession={actor:{...actor,id:5,role:'ADMIN'},csrfToken:'fixture-cookie'};
const bearerSession={actor,csrfToken:'fixture-bearer',token:'fixture-token-not-a-credential'};

function fixture(validLogin=true) {
  const requests=[],memory=new Map();
  const uni={getStorageSync:k=>memory.get(k),setStorageSync:(k,v)=>memory.set(k,v),removeStorageSync:k=>memory.delete(k),getStorageInfoSync:()=>({keys:[...memory.keys()]}),
    request:r=>{requests.push({path:r.url,method:r.method,authorization:r.header.Authorization});
      const data=r.url.endsWith('/auth/login')?(validLogin?bearerSession:cookieSession):(r.header.Authorization?bearerSession:cookieSession);
      queueMicrotask(()=>r.success({statusCode:200,data:{data}}));
    }};
  const source=fs.readFileSync(path.join(root,'student/src/runtime.ts'),'utf8')
    .replace(/^[ \t]*\/\/ #ifndef H5[\s\S]*?^[ \t]*\/\/ #endif\r?$/gm,'')
    .replace(/^import .*;\r?$/gm,'');
  const compiled=ts.transpileModule(source,{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ESNext}}).outputText
    .replace(/^export /gm,'').replace(/^export \{\};?\r?$/gm,'');
  const dependencies={...core,reactive,uni};
  const names=Object.keys(dependencies).filter(k=>/^[$A-Za-z_][$A-Za-z0-9_]*$/.test(k)&&!['default','__esModule'].includes(k));
  const runtime=new Function(...names,compiled+'\nreturn {auth,client,initialize};')(...names.map(k=>dependencies[k]));
  return {...runtime,requests};
}

test('M8 successful Bearer login clears initialization identity error',async()=>{
  const f=fixture();await f.initialize();
  assert.equal(f.auth.session,null,'Cookie-only response cannot become a Bearer identity');
  assert.match(f.auth.error,/无法确认登录身份/,'Original bootstrap error is visibly retained');
  await f.client.login('student2','fixture-only');await f.client.bootstrap();
  assert.equal(f.auth.session.actor.id,2);assert.equal(f.client.needsLogin,false);
  assert.equal(f.auth.error,'','Validated current identity must not retain old initialization error');
  assert.ok(f.requests.at(-1).authorization?.startsWith('Bearer '));
  console.log('M8_AUTH_PROJECTION',{actor:f.auth.session.actor.id,error:f.auth.error,generation:f.auth.generation});
});

test('M8 invalid Bearer login cannot adopt a Cookie-only identity',async()=>{
  const f=fixture(false);await f.initialize();
  await assert.rejects(f.client.login('student2','fixture-only'),e=>e instanceof core.ApiError&&e.code==='INVALID_RESPONSE');
  assert.equal(f.client.session,null);assert.equal(f.auth.session,null);assert.equal(f.client.needsLogin,true);
  assert.equal(f.requests.filter(r=>r.method==='POST').length,1);
  console.log('M8_AUTH_REFUSAL',{identityAdopted:false,needsLogin:f.client.needsLogin});
});
