"""Optimize previously generated originals without synthesizing or changing their content."""
from pathlib import Path
from PIL import Image
import hashlib, json, argparse
parser=argparse.ArgumentParser()
parser.add_argument('source',type=Path)
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
targets=[root/'student/src/static/assets',root/'admin/public/assets']
for p in targets:p.mkdir(parents=True,exist_ok=True)
sizes={'campus-hero':(1440,600),'window-seat':(1200,800),'quiet-room':(1200,800),'hall':(1200,800),'ink-sidebar':(400,700),'qixu-brand':(900,400)}
records=[]
for name,size in sizes.items():
    source=args.source/(name+'.png')
    original=source.read_bytes()
    with Image.open(source) as im:
        origin_size=im.size
        im.thumbnail(size,Image.Resampling.LANCZOS)
        ext='.png' if im.mode=='RGBA' else '.jpg'
        for target in targets:
            out=target/(name+ext)
            if ext=='.png':im.save(out,optimize=True)
            else:im.save(out,quality=87,optimize=True,progressive=True)
        data=(targets[0]/(name+ext)).read_bytes()
        records.append({'name':name+ext,'sourceKind':'GENERATED_DEMO','originalPixels':origin_size,'pixels':im.size,'originalSha256':hashlib.sha256(original).hexdigest(),'sha256':hashlib.sha256(data).hexdigest(),'bytes':len(data)})
(root/'docs/assets.json').write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'assets':len(records),'totalBytesPerClient':sum(r['bytes'] for r in records)}))
