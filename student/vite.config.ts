import {defineConfig} from 'vite';
import uni from '@dcloudio/vite-plugin-uni';
import {createRequire} from 'node:module';
import {fileURLToPath} from 'node:url';
const require=createRequire(import.meta.url);
// uni's customized Vue runtime is 3.4.21. A hoisted 3.5 shared helper changes
// slot property writability, so bind this client to its own exact helper/compiler.
// Resolve shared source outside node_modules so development updates cannot retain
// an obsolete ownership/transport implementation through workspace-link caching.
export default defineConfig({optimizeDeps:{exclude:['@qixu/client']},plugins:[uni({vueOptions:{compiler:require('@vue/compiler-sfc')}})],resolve:{alias:{'@qixu/client':fileURLToPath(new URL('../packages/client/src/index.ts',import.meta.url)),'@vue/shared':require.resolve('@vue/shared/dist/shared.esm-bundler.js')}},server:{host:'127.0.0.1',port:6968,strictPort:true,proxy:{'/api':{target:'http://127.0.0.1:6967',changeOrigin:false}}}});
