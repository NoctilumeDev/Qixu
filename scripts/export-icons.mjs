import {readFile,writeFile,mkdir,copyFile} from 'node:fs/promises';
import {resolve} from 'node:path';
const root=resolve(import.meta.dirname,'..');
const names=['house','armchair','map-trifold','book-open','clipboard-text','chat-circle','user','bell','calendar-blank','clock','check-circle','warning-circle','arrow-right','caret-left','map-pin','plug','sun','wind','wheelchair','heart','funnel','magnifying-glass','arrows-left-right','recycle','sign-out','list','buildings','wrench','shield-check','arrow-clockwise'];
const target=resolve(root,'student/src/static/icons');await mkdir(target,{recursive:true});
for(const name of names){const raw=await readFile(resolve(root,'node_modules/@phosphor-icons/core/assets/regular',name+'.svg'),'utf8');for(const [variant,color]of Object.entries({ink:'#81766d',red:'#a8473d',green:'#597b63'}))await writeFile(resolve(target,`${name}-${variant}.svg`),raw.replaceAll('currentColor',color));}
await copyFile(resolve(root,'node_modules/@phosphor-icons/core/LICENSE'),resolve(target,'LICENSE.txt'));
console.log(`Exported ${names.length} licensed Phosphor icons in 3 colors.`);
