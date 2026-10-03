/** Native mini-program buttons retain platform behavior; H5 custom widgets need keyboard semantics. */
export function installButtonKeyboard(){
  const decorate=()=>{
    document.querySelectorAll<HTMLElement>('uni-button[role="button"]').forEach(el=>{
    const disabled=el.hasAttribute('disabled')||el.classList.contains('uni-button-disabled');
    el.tabIndex=disabled?-1:0;
    const value=String(disabled);if(el.getAttribute('aria-disabled')!==value)el.setAttribute('aria-disabled',value);
    });
    document.querySelectorAll<HTMLInputElement|HTMLTextAreaElement>('uni-input input,uni-textarea textarea').forEach(el=>{
      if(el.hasAttribute('aria-label'))return;
      const field=el.closest('.field-label');
      const name=field?Array.from(field.childNodes).filter(n=>n.nodeType===Node.TEXT_NODE).map(n=>n.textContent?.trim()).filter(Boolean).join(' '):el.closest('uni-input,uni-textarea')?.querySelector('.input-placeholder,.textarea-placeholder')?.textContent?.trim();
      if(name)el.setAttribute('aria-label',name);
    });
  };
  const activate=(event:KeyboardEvent)=>{
    if(event.key!=='Enter'&&event.key!==' ')return;
    const target=(event.target as HTMLElement|null)?.closest<HTMLElement>('uni-button[role="button"]');
    if(!target)return;event.preventDefault();if(target.getAttribute('aria-disabled')!=='true')target.click();
  };
  const observer=new MutationObserver(decorate);observer.observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['disabled','class']});
  document.addEventListener('keydown',activate);decorate();
  return()=>{observer.disconnect();document.removeEventListener('keydown',activate);};
}
