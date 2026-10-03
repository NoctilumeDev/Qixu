import {defineConfig} from 'vite';
import vue from '@vitejs/plugin-vue';
import {fileURLToPath} from 'node:url';
export default defineConfig({optimizeDeps:{exclude:['@qixu/client']},resolve:{alias:{'@qixu/client':fileURLToPath(new URL('../packages/client/src/index.ts',import.meta.url))}},plugins:[vue()],server:{host:'127.0.0.1',port:6969,strictPort:true,proxy:{'/api':{target:'http://127.0.0.1:6967',changeOrigin:false}}},build:{sourcemap:false}});
