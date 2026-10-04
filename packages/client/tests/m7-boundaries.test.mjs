import test from 'node:test';
import assert from 'node:assert/strict';
import { Client, ApiError, StaleResponse, UnknownSubmission } from '../dist/index.js';
const storage = () => {
  const map = new Map();
  return {
    get: (k) => map.get(k) || null,
    set: (k, v) => map.set(k, v),
    remove: (k) => map.delete(k),
    keys: () => [...map.keys()],
  };
};
const ok = (data) => ({ status: 200, body: { data } });
const session = (id) => ({
  actor: {
    id,
    username: 'u' + id,
    displayName: '同学',
    role: 'STUDENT',
    studentVerified: true,
    authVersion: 1,
  },
  csrfToken: 'csrf' + id,
  token: 'token' + id,
});
const receipt = (key) => ({ receipt: { key, status: 'COMMITTED' } });
const key = 'm7_shared_actor_key';
function seed(store, actor) {
  store.set(
    'qixu.intent.v1.' + actor + '.' + key,
    JSON.stringify({
      key,
      actorId: actor,
      path: '/api/v1/favorites',
      body: { spaceId: 2000, selected: true },
      createdAt: '2026-10-03T00:00:00Z',
      sensitive: false,
    }),
  );
}
function deferred() {
  let resolve;
  const promise = new Promise((r) => (resolve = r));
  return { promise, resolve };
}
function client(transport, store) {
  return new Client(transport, store, 'BEARER', () => key);
}

test('M7 current actor can recover and stop its same-text key without clearing another actor', async () => {
  for (const action of ['recover', 'stop']) {
    const store = storage();
    seed(store, 1);
    seed(store, 2);
    let calls = 0;
    const c = client(async () => {
      calls++;
      return ok({ status: 'COMMITTED', result: receipt(key) });
    }, store);
    c.session = session(2);
    await c[action](key);
    assert.equal(calls, 1);
    assert.equal(c.session.actor.id, 2);
    assert.deepEqual(
      c.pending.map((p) => p.actorId),
      [1],
    );
    assert.ok(store.get('qixu.intent.v1.1.' + key));
    assert.equal(store.get('qixu.intent.v1.2.' + key), null);
  }
});
test('M7 late committed A response cannot erase B same-text unknown intent', async () => {
  const store = storage(),
    late = deferred();
  const c = client(() => late.promise, store);
  c.session = session(1);
  const outcome = c.mutate('/api/v1/favorites', { spaceId: 2000, selected: true }).catch((e) => e);
  seed(store, 2);
  c.refreshPending();
  c.session = session(2);
  c.generation++;
  late.resolve(ok(receipt(key)));
  assert.ok((await outcome) instanceof StaleResponse);
  const reopened = client(async () => ok({}), store);
  reopened.session = session(2);
  assert.deepEqual(
    reopened.visiblePending.map((p) => p.key),
    [key],
  );
  assert.equal(store.get('qixu.intent.v1.1.' + key), null);
});
test('M7 constructor storage denial is explicit and blocks writes before transport', async () => {
  for (const fault of ['get', 'set', 'keys']) {
    const store = storage();
    store[fault] = () => {
      throw Error('storage denied');
    };
    let calls = 0;
    const c = client(async () => {
      calls++;
      return ok({});
    }, store);
    c.session = session(1);
    await assert.rejects(
      c.mutate('/api/v1/favorites', {}),
      (e) => e instanceof ApiError && e.code === 'LOCAL_STORAGE_UNAVAILABLE',
    );
    assert.equal(calls, 0);
  }
});
test('M7 failed intent scan preserves known metadata and cannot authorize a fresh intention', async () => {
  const store = storage();
  seed(store, 1);
  let calls = 0;
  const c = client(async () => {
    calls++;
    return ok({});
  }, store);
  c.session = session(2);
  store.keys = () => {
    throw Error('storage denied');
  };
  await assert.rejects(
    c.mutate('/api/v1/favorites', {}),
    (e) => e instanceof ApiError && e.code === 'LOCAL_STORAGE_UNAVAILABLE',
  );
  assert.equal(calls, 0);
  assert.equal(c.pending[0].actorId, 1);
});
test('M7 local token removal failure still clears private memory and attempts remote logout', async () => {
  const store = storage();
  let logouts = 0;
  const c = client(async (r) => {
    if (r.path.endsWith('/logout')) {
      logouts++;
      return ok({ loggedOut: true });
    }
    throw Error('response lost');
  }, store);
  c.session = session(1);
  await assert.rejects(
    c.mutate('/api/v1/feedback', { description: 'private statement' }, { sensitive: true }),
    UnknownSubmission,
  );
  const remove = store.remove;
  store.remove = (k) => {
    if (k === 'qixu.token') throw Error('storage denied');
    return remove(k);
  };
  await c.logout();
  assert.equal(c.session, null);
  assert.equal(logouts, 1);
  assert.equal(c.pending[0].body, undefined);
  assert.equal(c.pending[0].replayable, false);
  assert.ok(
    !store
      .keys()
      .map((k) => store.get(k) || '')
      .join('')
      .includes('private statement'),
  );
});
test('M7 failed recovery deletion retains original key and reports local storage failure', async () => {
  const store = storage();
  seed(store, 1);
  const c = client(async () => ok({ status: 'COMMITTED', result: receipt(key) }), store);
  c.session = session(1);
  store.remove = () => {
    throw Error('storage denied');
  };
  await assert.rejects(
    c.recover(key),
    (e) => e instanceof ApiError && e.code === 'LOCAL_STORAGE_UNAVAILABLE',
  );
  assert.equal(c.visiblePending[0].key, key);
  assert.ok(store.get('qixu.intent.v1.1.' + key));
});
