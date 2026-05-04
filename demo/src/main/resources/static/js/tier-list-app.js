let currentRoleFilter='Tất cả', currentClassFilter='Tất cả', numCols=5, currentUserRole='Custom';
const tempHeroInstances=[];
let modalRoleFilter='Tất cả', modalClassFilter='Tất cả';
const modalTempHeroInstances=[];

const OFFICIAL_TIER_LIST_API='/api/tier-lists';
const OFFICIAL_TIER_LIST_TITLE='Tier List Meta Hien Tai';
let officialTierListPayload=null;
const communityTierListCache=new Map();
const IMPORT_TIER_ROWS=['S','A','B','C','D'];
const IMPORT_ROLE_COLUMNS=typeof TIER_ROLE_ORDER!=='undefined'?TIER_ROLE_ORDER:['DSL','JGL','MID','ADL','SUP'];
const IMPORT_TIER_COLORS={S:'#e74c3c',A:'#9b59b6',B:'#3498db',C:'#2ecc71',D:'#95a5a6'};
const IMPORT_COLUMN_META={
    DSL:{label:'DSL',icon:'/images/ui/top.png',alt:'DSL'},
    JGL:{label:'JGL',icon:'/images/ui/jungle.png',alt:'JGL'},
    MID:{label:'MID',icon:'/images/ui/mid.png',alt:'MID'},
    SUP:{label:'SUP',icon:'/images/ui/support.png',alt:'SUP'},
    ADL:{label:'ADL',icon:'/images/ui/adc.png',alt:'ADL'}
};
const IMPORT_ROLE_ALIASES=[
    {label:'DSL',code:'DSL'},
    {label:'Top',code:'DSL'},
    {label:'Solo',code:'DSL'},
    {label:'JGL',code:'JGL'},
    {label:'JG',code:'JGL'},
    {label:'Jungle',code:'JGL'},
    {label:'MID',code:'MID'},
    {label:'Mid lane',code:'MID'},
    {label:'SUP',code:'SUP'},
    {label:'SP',code:'SUP'},
    {label:'ADL',code:'ADL'},
    {label:'ADC',code:'ADL'},
    {label:'Đấu sĩ',code:'DSL'},
    {label:'Dau si',code:'DSL'},
    {label:'Đấu Sĩ',code:'DSL'},
    {label:'Sát thủ',code:'JGL'},
    {label:'Sat thu',code:'JGL'},
    {label:'Sát Thủ',code:'JGL'},
    {label:'Pháp sư',code:'MID'},
    {label:'Phap su',code:'MID'},
    {label:'Pháp Sư',code:'MID'},
    {label:'Trợ thủ',code:'SUP'},
    {label:'Tro thu',code:'SUP'},
    {label:'Support',code:'SUP'},
    {label:'Đỡ đòn',code:'SUP'},
    {label:'Do don',code:'SUP'},
    {label:'Tank',code:'SUP'},
    {label:'Xạ thủ',code:'ADL'},
    {label:'Xa thu',code:'ADL'},
    {label:'Marksman',code:'ADL'},
    {label:'AD',code:'ADL'}
];
let lastTierImportPreview=null;

function normalizeHeroName(name){
    const aliases={"Flowborn":"Flowborn (Marksman)","Flowborn ADL":"Flowborn (Marksman)","Flowborn MID":"Flowborn (Marksman)","Ngo Khong":"Wukong","Trieu Van":"Zanis","Dieu Thuyen":"Diaochan","Lu Bo":"Lu Bu","Roule":"Rouie","Governa":"Goverra","Tochi":"Tachi","Richter":"Riktor","KilGroth":"Kil'Groth"};
    const trimmed=String(name||'').trim();
    return aliases[trimmed]||trimmed;
}

function getUserRole(){
    const u=localStorage.getItem('aov_user');
    if(!u) return 'Custom';
    try{ return JSON.parse(u).role||'Custom'; }catch(e){ return 'Custom'; }
}

function setOfficialGridEditable(isEditable){
    document.querySelectorAll('#tier-grid .tier-heroes').forEach(c=>{
        c.ondragover=isEditable?allowDrop:null;
        c.ondragenter=isEditable?dragEnter:null;
        c.ondragleave=isEditable?dragLeave:null;
        c.ondrop=isEditable?dropOnTierList:null;
    });
    document.querySelectorAll('#tier-grid .tier-hero').forEach(h=>{
        h.draggable=isEditable;
    });
    document.querySelectorAll('#tier-grid [contenteditable]').forEach(e=>{
        e.contentEditable=String(isEditable);
    });
}

function applyTierListRoleUI(){
    currentUserRole=getUserRole();
    const isAdmin=currentUserRole==='Admin';
    const isLoggedIn=currentUserRole!=='Custom';
    const grid=document.getElementById('tier-grid');
    // Admin controls
    const ac=document.getElementById('admin-controls');
    const hp=document.getElementById('hero-pool-official');
    const importPanel=document.getElementById('tier-import-panel');
    const up=document.getElementById('user-prompt');
    const bc=document.getElementById('btn-create-community');
    if(ac) ac.style.display=isAdmin?'flex':'none';
    if(hp) hp.style.display=isAdmin?'block':'none';
    if(importPanel) importPanel.style.display=isAdmin?'block':'none';
    if(up) up.style.display=(!isAdmin&&isLoggedIn)?'flex':'none';
    if(bc) bc.style.display=isLoggedIn?'inline-flex':'none';
    // Read-only for non-admin
    if(grid){
        if(isAdmin){
            grid.classList.remove('tier-grid-readonly');
            setOfficialGridEditable(true);
        }else{
            grid.classList.add('tier-grid-readonly');
            setOfficialGridEditable(false);
        }
    }
}

// Listen for header loaded + auth changes
document.addEventListener('headerLoaded',()=>{ setTimeout(applyTierListRoleUI,300); });
document.addEventListener('authChanged',applyTierListRoleUI);
window.addEventListener('storage',applyTierListRoleUI);

function getTierFilterText(value){
    if(typeof normalizeHeroFilterText==='function') return normalizeHeroFilterText(value);
    return String(value||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/đ/g,'d').trim();
}

function getHeroClassKeys(hero){
    const classNames=typeof getHeroClassNames==='function'?getHeroClassNames(hero):(Array.isArray(hero?.classes)?hero.classes:(hero?.heroClass?[hero.heroClass]:[]));
    return classNames.map(getTierFilterText).filter(Boolean);
}

function heroMatchesClassFilter(hero,selectedClass=currentClassFilter){
    if(!selectedClass||selectedClass==='Tất cả'||selectedClass==='ALL') return true;
    const selectedKey=getTierFilterText(selectedClass);
    return getHeroClassKeys(hero).some(key=>key===selectedKey);
}

function getTierItemKey(item){
    if(!item) return '';
    if(typeof item==='string'||typeof item==='number') return `hero-${getHeroIdFromValue(item)||getTierFilterText(getHeroNameFromValue(item))}`;
    if(item.instanceId) return String(item.instanceId);
    const heroId=item.heroId??item.id??item.sourceHeroId;
    if(heroId!==undefined&&heroId!==null&&heroId!=='') return `hero-${heroId}`;
    return `hero-${getTierFilterText(item.slug||item.name||item.heroName||Date.now())}`;
}

function sanitizeTierDomId(value){
    return String(value||'item').replace(/[^a-zA-Z0-9_-]+/g,'-').replace(/^-+|-+$/g,'')||'item';
}

function cssEscapeValue(value){
    if(window.CSS&&typeof window.CSS.escape==='function') return window.CSS.escape(String(value||''));
    return String(value||'').replace(/["\\]/g,'\\$&');
}

function getTierItemDomId(item){
    return `tier-item-${sanitizeTierDomId(getTierItemKey(item))}`;
}

function getTierItemDomIdForPool(item,pool='official'){
    const prefix=pool==='modal'?'modal-tier':'tier-item';
    return `${prefix}-${sanitizeTierDomId(getTierItemKey(item))}`;
}

function getHeroPoolConfig(pool='official'){
    const isModal=pool==='modal';
    return {
        pool,
        isModal,
        heroGridSelector:isModal?'#modal-hero-grid':'#hero-grid',
        tempGridSelector:isModal?'#modal-hero-temp-grid':'#hero-temp-grid',
        tempSectionSelector:isModal?'#modal-hero-temp-section':'#hero-temp-section',
        emptySelector:isModal?'#modal-hero-pool-empty':'#hero-pool-empty',
        classFilterSelector:isModal?'#modal-class-filter':'#classFilter',
        roleFilterSelector:isModal?'#modal-role-filters':'#role-filters',
        tierGridSelector:isModal?'#modal-tier-grid':'#tier-grid',
        tempItems:isModal?modalTempHeroInstances:tempHeroInstances,
        get roleFilter(){ return isModal?modalRoleFilter:currentRoleFilter; },
        set roleFilter(value){ if(isModal) modalRoleFilter=value; else currentRoleFilter=value; },
        get classFilter(){ return isModal?modalClassFilter:currentClassFilter; },
        set classFilter(value){ if(isModal) modalClassFilter=value; else currentClassFilter=value; }
    };
}

function parseHeroPayload(raw){
    if(!raw) return null;
    try{ return JSON.parse(raw); }catch(error){ return null; }
}

function getHeroRefForDrag(value){
    const ref=typeof getHeroRefForStorage==='function'?getHeroRefForStorage(value):null;
    if(ref&&Object.keys(ref).length) return ref;
    const hero=getHeroFromValue(value);
    if(hero) return {heroId:hero.id,name:hero.name,slug:hero.slug||'',avatarUrl:hero.avatarUrl||''};
    return {name:getHeroNameFromValue(value)};
}

function setHeroDragData(ev,source,heroRef,sourceId){
    ev.dataTransfer.setData('source',source);
    ev.dataTransfer.setData('heroPayload',JSON.stringify(heroRef));
    ev.dataTransfer.setData('heroName',heroRef.name||'');
    if(heroRef.heroId) ev.dataTransfer.setData('heroId',String(heroRef.heroId));
    if(heroRef.instanceId) ev.dataTransfer.setData('instanceId',String(heroRef.instanceId));
    if(sourceId) ev.dataTransfer.setData('sourceId',sourceId);
    ev.dataTransfer.effectAllowed='move';
}

function getDragHeroRef(ev){
    const payload=parseHeroPayload(ev.dataTransfer.getData('heroPayload'));
    if(payload) return payload;
    const heroName=normalizeHeroName(ev.dataTransfer.getData('heroName'));
    const heroId=ev.dataTransfer.getData('heroId');
    const ref=heroId?getHeroRefForDrag({heroId:Number(heroId),name:heroName}):getHeroRefForDrag(heroName);
    const instanceId=ev.dataTransfer.getData('instanceId');
    if(instanceId){
        ref.instanceId=instanceId;
        ref.tempInstance=true;
        ref.sourceHeroId=ref.sourceHeroId||ref.heroId||null;
    }
    return ref;
}

function createTempHeroInstance(value){
    const base=getHeroRefForDrag(value);
    const sourceHeroId=value?.sourceHeroId??base.sourceHeroId??base.heroId??value?.heroId??value?.id??null;
    const sourceKey=sourceHeroId||base.slug||base.name||'hero';
    return {
        ...base,
        heroId:base.heroId||sourceHeroId,
        sourceHeroId,
        instanceId:`temp-${sanitizeTierDomId(sourceKey)}-${Date.now()}-${Math.random().toString(36).slice(2,7)}`,
        tempInstance:true
    };
}

function createHeroPoolButton(heroRef,{temp=false,modal=false,pool='official'}={}){
    if(modal) pool='modal';
    const hero=getHeroFromValue(heroRef)||heroRef;
    const ref=temp?heroRef:getHeroRefForDrag(hero);
    const btn=document.createElement('button');
    const heroName=ref.name||getHeroNameFromValue(hero);
    btn.type='button';
    btn.className=`hero-btn${temp?' is-temp':''}`;
    btn.id=temp?`temp-hero-${sanitizeTierDomId(ref.instanceId)}`:`${modal?'modal-hero':'hero'}-${heroName}`;
    btn.dataset.heroPayload=JSON.stringify(ref);
    btn.dataset.heroId=ref.heroId||'';
    btn.dataset.heroName=heroName;
    btn.dataset.instanceId=ref.instanceId||'';
    btn.dataset.role=typeof getHeroPrimaryRoleCode==='function'?getHeroPrimaryRoleCode(hero):(hero.role||'');
    const classNames=typeof getHeroClassNames==='function'?getHeroClassNames(hero):(Array.isArray(hero.classes)?hero.classes:[]);
    btn.dataset.classKeys=classNames.map(getTierFilterText).join('|');
    btn.dataset.searchText=[heroName,hero.slug,ref.slug].concat(classNames).filter(Boolean).map(getTierFilterText).join(' ');
    btn.title=[heroName].concat(classNames).concat(hero.laneRoles||[]).filter(Boolean).join(' / ');
    btn.draggable=true;

    const img=document.createElement('img');
    img.src=getHeroImgUrl(ref);
    img.alt=heroName;
    img.loading='lazy';
    img.dataset.heroName=heroName;
    img.onerror=function(){ handleTierHeroImageError(this,this.dataset.heroName,TIER_HERO_FALLBACK_IMAGE); };
    btn.appendChild(img);

    const name=document.createElement('span');
    name.className='hero-btn-name';
    name.textContent=heroName;
    btn.appendChild(name);

    if(temp){
        const badge=document.createElement('span');
        badge.className='hero-temp-badge';
        badge.textContent='Tạm';
        btn.appendChild(badge);
        const remove=document.createElement('span');
        remove.setAttribute('role','button');
        remove.setAttribute('tabindex','0');
        remove.className='hero-temp-remove';
        remove.textContent='×';
        remove.title='Xóa bản tạm khỏi pool';
        remove.addEventListener('click',event=>{
            event.stopPropagation();
            removeTempHeroInstance(ref.instanceId,pool);
        });
        remove.addEventListener('keydown',event=>{
            if(event.key==='Enter'||event.key===' '){
                event.preventDefault();
                event.stopPropagation();
                removeTempHeroInstance(ref.instanceId,pool);
            }
        });
        btn.appendChild(remove);
    }

    btn.ondragstart=ev=>dragStartFromList(ev,ref,temp?`${pool}-temp-list`:(pool==='modal'?'modal-list':'list'));
    btn.ondragend=dragEnd;
    btn.addEventListener('contextmenu',event=>{
        event.preventDefault();
        showHeroContextMenu(event,{type:'pool',heroRef:ref,element:btn,temp,pool});
    });
    return btn;
}

function renderClassFilterOptions(pool='official'){
    const config=getHeroPoolConfig(pool);
    const select=document.querySelector(config.classFilterSelector);
    if(!select) return;
    const current=select.value||config.classFilter;
    const classMap=new Map();
    heroes.forEach(hero=>{
        (typeof getHeroClassNames==='function'?getHeroClassNames(hero):(hero.classes||[])).forEach(name=>{
            const key=getTierFilterText(name);
            if(key&&!classMap.has(key)) classMap.set(key,name);
        });
    });
    select.innerHTML='<option value="Tất cả">Tất cả class</option>'+
        Array.from(classMap.values()).sort((a,b)=>a.localeCompare(b,'vi')).map(name=>`<option value="${escapeTierHtml(name)}">${escapeTierHtml(name)}</option>`).join('');
    if(Array.from(select.options).some(option=>option.value===current)) select.value=current;
    config.classFilter=select.value||'Tất cả';
}

function renderTempHeroPool(pool='official'){
    const config=getHeroPoolConfig(pool);
    const section=document.querySelector(config.tempSectionSelector);
    const grid=document.querySelector(config.tempGridSelector);
    if(!section||!grid) return;
    section.hidden=config.tempItems.length===0;
    grid.innerHTML='';
    config.tempItems.forEach(instance=>{
        grid.appendChild(createHeroPoolButton(instance,{temp:true,pool}));
    });
    markHeroPoolSelection(pool);
    filterHeroPool(pool);
}

function addTempHeroToPool(value,pool='official'){
    const config=getHeroPoolConfig(pool);
    const instance=createTempHeroInstance(value);
    config.tempItems.push(instance);
    renderTempHeroPool(pool);
    showTierToast(`${instance.name||'Tướng'} đã có bản tạm trong pool.`);
}

function removeTempHeroInstance(instanceId,pool='official'){
    const config=getHeroPoolConfig(pool);
    const index=config.tempItems.findIndex(item=>item.instanceId===instanceId);
    if(index>=0) config.tempItems.splice(index,1);
    document.querySelectorAll(`${config.tierGridSelector} .tier-hero[data-instance-id="${cssEscapeValue(instanceId||'')}"]`).forEach(el=>el.remove());
    renderTempHeroPool(pool);
    markHeroPoolSelection(pool);
    filterHeroPool(pool);
}

function removeTierHeroElement(element){
    if(!element) return;
    const pool=element.closest('#modal-tier-grid')?'modal':'official';
    element.remove();
    markHeroPoolSelection(pool);
    filterHeroPool(pool);
}

function closeHeroContextMenu(){
    const menu=document.getElementById('hero-context-menu');
    if(menu){
        menu.hidden=true;
        menu.innerHTML='';
    }
}

function showHeroContextMenu(event,{type,heroRef,element,temp=false,pool='official'}){
    const menu=document.getElementById('hero-context-menu');
    if(!menu) return;
    closeHeroContextMenu();
    const actions=[
        {label:'Tạo bản tạm',handler:()=>addTempHeroToPool(heroRef,pool)}
    ];
    if(type==='tier'){
        actions.push({label:'Xóa khỏi tier list',danger:true,handler:()=>removeTierHeroElement(element)});
    }else if(temp){
        actions.push({label:'Xóa bản tạm khỏi pool',danger:true,handler:()=>removeTempHeroInstance(heroRef.instanceId,pool)});
    }
    actions.push({label:'Đóng',handler:()=>{}});
    menu.innerHTML=actions.map((action,index)=>`<button type="button" class="${action.danger?'danger':''}" data-action-index="${index}">${escapeTierHtml(action.label)}</button>`).join('');
    menu.querySelectorAll('[data-action-index]').forEach(button=>{
        button.addEventListener('click',()=>{
            const action=actions[Number(button.dataset.actionIndex)];
            closeHeroContextMenu();
            action?.handler?.();
        });
    });
    menu.hidden=false;
    const x=Math.min(event.clientX,window.innerWidth-210);
    const y=Math.min(event.clientY,window.innerHeight-160);
    menu.style.left=`${Math.max(8,x)}px`;
    menu.style.top=`${Math.max(8,y)}px`;
}

document.addEventListener('click',event=>{
    if(!event.target.closest('#hero-context-menu')) closeHeroContextMenu();
});
document.addEventListener('keydown',event=>{
    if(event.key==='Escape') closeHeroContextMenu();
});

async function initApp(){
    const grid=document.getElementById('hero-grid');
    if(!grid) return;
    await loadHeroesFromApi();
    grid.innerHTML='';
    if(heroes.length===0){
        grid.innerHTML='<div class="draft-warning" style="grid-column:1/-1">Chua co du lieu tuong trong database. Hay chay sql/seed_heroes.sql roi tai lai trang.</div>';
    }
    heroes.forEach(hero=>{
        grid.appendChild(createHeroPoolButton(hero));
    });
    renderClassFilterOptions();
    renderTempHeroPool();
    document.querySelectorAll('#tier-grid .tier-heroes').forEach(c=>{
        c.ondragover=allowDrop; c.ondragenter=dragEnter; c.ondragleave=dragLeave; c.ondrop=dropOnTierList;
    });
    document.body.ondragover=(ev)=>ev.preventDefault();
    document.body.ondrop=(ev)=>{
        if(!ev.target.closest('.tier-heroes')){
            const source=ev.dataTransfer.getData("source");
            if(source==='tier'){
                const sid=ev.dataTransfer.getData("sourceId");
                const el=document.getElementById(sid); if(el) el.remove();
                markOfficialHeroSelection();
                filterHeroes();
            }
        }
    };
    renderCommunityCards();
    loadOfficialTierList();
    initTierListImportUi();
    setTimeout(applyTierListRoleUI,200);
}

function dragStartFromList(ev,value,source='list'){
    const heroRef=value&&typeof value==='object'?value:getHeroRefForDrag(value);
    setHeroDragData(ev,source,heroRef);
    setTimeout(()=>ev.target.classList.add('dragging'),0);
}
function dragStartFromTier(ev,value){
    const heroRef=getHeroRefForDrag(value&&typeof value==='object'?value:getTierHeroRef(ev.target));
    setHeroDragData(ev,'tier',heroRef,ev.target.id);
    setTimeout(()=>ev.target.classList.add('dragging'),0);
}
function dragEnd(ev){ ev.target.classList.remove('dragging'); document.querySelectorAll('.drag-over').forEach(el=>el.classList.remove('drag-over')); }
function allowDrop(ev){ ev.preventDefault(); ev.dataTransfer.dropEffect="move"; }
function dragEnter(ev){ ev.preventDefault(); if(ev.currentTarget.classList.contains('tier-heroes')) ev.currentTarget.classList.add('drag-over'); }
function dragLeave(ev){ ev.currentTarget.classList.remove('drag-over'); }

function dropOnTierList(ev){
    ev.preventDefault();
    const tc=ev.currentTarget; tc.classList.remove('drag-over');
    const source=ev.dataTransfer.getData("source");
    const heroRef=getDragHeroRef(ev);
    const heroName=normalizeHeroName(heroRef?.name||'');
    if(!heroName) return;
    const sourceId=ev.dataTransfer.getData("sourceId");
    if(source==='tier'&&sourceId){
        const existing=document.getElementById(sourceId);
        if(existing) tc.appendChild(existing);
        markOfficialHeroSelection();
        filterHeroes();
        return;
    }
    const existingId=getTierItemDomId(heroRef);
    if(!heroRef.tempInstance&&document.getElementById(existingId)){
        return;
    }
    tc.appendChild(createOfficialTierHero(heroRef));
    markOfficialHeroSelection();
    filterHeroes();
}

function setRoleFilter(role){
    currentRoleFilter=role;
    document.querySelectorAll('#role-filters .role-btn').forEach(b=>{
        const value=b.getAttribute('data-role-filter')||b.textContent.trim();
        b.classList.toggle('active',value===role);
    });
    filterHeroes();
}

function setClassFilter(heroClass){
    currentClassFilter=heroClass||'Tất cả';
    filterHeroPool('official');
}

function setModalRoleFilter(role){
    modalRoleFilter=role;
    document.querySelectorAll('#modal-role-filters .role-btn').forEach(b=>{
        const value=b.getAttribute('data-role-filter')||b.textContent.trim();
        b.classList.toggle('active',value===role);
    });
    filterHeroPool('modal');
}

function setModalClassFilter(heroClass){
    modalClassFilter=heroClass||'Tất cả';
    filterHeroPool('modal');
}

function filterHeroPool(pool='official'){
    const config=getHeroPoolConfig(pool);
    const q=getTierFilterText(document.querySelector(pool==='modal'?'#modal-search':'#searchInput')?.value||'');
    const selectedClass=document.querySelector(config.classFilterSelector)?.value||config.classFilter;
    const selectedRole=config.roleFilter;
    config.classFilter=selectedClass;
    let visibleCount=0;
    document.querySelectorAll(`${config.heroGridSelector} .hero-btn`).forEach(btn=>{
        const sel=btn.classList.contains('selected'), r=btn.getAttribute('data-role');
        const mr=selectedRole==='Tất cả'||r===selectedRole;
        const classKeys=(btn.dataset.classKeys||'').split('|').filter(Boolean);
        const mc=selectedClass==='Tất cả'||selectedClass==='ALL'||classKeys.includes(getTierFilterText(selectedClass));
        const ms=!q||(btn.dataset.searchText||getTierFilterText(btn.innerText)).includes(q);
        const visible=!sel&&mr&&mc&&ms;
        btn.style.display=visible?'flex':'none';
        if(visible) visibleCount++;
    });
    document.querySelectorAll(`${config.tempGridSelector} .hero-btn`).forEach(btn=>{
        const sel=btn.classList.contains('selected'), r=btn.getAttribute('data-role');
        const mr=selectedRole==='Tất cả'||r===selectedRole;
        const classKeys=(btn.dataset.classKeys||'').split('|').filter(Boolean);
        const mc=selectedClass==='Tất cả'||selectedClass==='ALL'||classKeys.includes(getTierFilterText(selectedClass));
        const ms=!q||(btn.dataset.searchText||getTierFilterText(btn.innerText)).includes(q);
        const visible=!sel&&mr&&mc&&ms;
        btn.style.display=visible?'flex':'none';
        if(visible) visibleCount++;
    });
    const empty=document.querySelector(config.emptySelector);
    if(empty) empty.hidden=visibleCount>0;
}

function filterHeroes(){
    filterHeroPool('official');
}

function updateTierColor(p){ p.parentElement.style.backgroundColor=p.value; }
function updateGridTemplate(){
    const grid=document.getElementById('tier-grid');
    if(!grid) return;
    grid.style.gridTemplateColumns=`80px repeat(${numCols},minmax(160px,1fr))`;
    grid.style.minWidth=(80+(numCols*160))+'px';
}

function deleteRow(btn){
    const label=btn.parentElement; let next=label.nextElementSibling; const cells=[label];
    for(let i=0;i<numCols;i++){ if(next&&next.classList.contains('tier-content')){ cells.push(next); next=next.nextElementSibling; } }
    cells.forEach(c=>c.remove());
    markOfficialHeroSelection();
    filterHeroes();
}

function deleteColumn(btn){
    const hc=btn.parentElement, grid=document.getElementById('tier-grid');
    const headers=Array.from(grid.querySelectorAll('.header-cell'));
    const ci=headers.indexOf(hc); if(ci<1) return;
    const cells=[hc];
    grid.querySelectorAll('.tier-label').forEach(label=>{ let cell=label; for(let i=0;i<ci;i++){ if(cell) cell=cell.nextElementSibling; } if(cell&&cell.classList.contains('tier-content')) cells.push(cell); });
    cells.forEach(c=>c.remove());
    numCols--; updateGridTemplate(); markOfficialHeroSelection(); filterHeroes();
}

function addRow(){
    const grid=document.getElementById('tier-grid');
    const label=document.createElement('div'); label.className='tier-cell tier-label situational-tier'; label.style.backgroundColor='#f1c40f';
    label.innerHTML=`<input type="color" class="tier-color-picker" value="#f1c40f" title="Chọn màu tier mới" oninput="updateTierColor(this)"><span contenteditable="true">Mới</span><button class="delete-btn" onclick="deleteRow(this)">×</button>`;
    grid.appendChild(label);
    for(let i=0;i<numCols;i++){ const c=document.createElement('div'); c.className='tier-cell tier-content tier-heroes'; c.ondragover=allowDrop; c.ondragenter=dragEnter; c.ondragleave=dragLeave; c.ondrop=dropOnTierList; grid.appendChild(c); }
}

function addColumn(){
    const grid=document.getElementById('tier-grid');
    const nh=document.createElement('div'); nh.className='tier-cell header-cell';
    nh.innerHTML='<span contenteditable="true">Mới</span><button class="delete-btn" onclick="deleteColumn(this)">×</button>';
    grid.insertBefore(nh,grid.querySelector('.tier-label'));
    const labels=Array.from(grid.querySelectorAll('.tier-label'));
    labels.forEach((label,i)=>{ const c=document.createElement('div'); c.className='tier-cell tier-content tier-heroes'; c.ondragover=allowDrop; c.ondragenter=dragEnter; c.ondragleave=dragLeave; c.ondrop=dropOnTierList; if(i<labels.length-1) grid.insertBefore(c,labels[i+1]); else grid.appendChild(c); });
    numCols++; updateGridTemplate();
}

// === Community Cards ===
function timeAgo(dateStr){
    const diff=Date.now()-new Date(dateStr).getTime();
    const mins=Math.floor(diff/60000);
    if(mins<60) return mins+' phút trước';
    const hrs=Math.floor(mins/60);
    if(hrs<24) return hrs+' giờ trước';
    return Math.floor(hrs/24)+' ngày trước';
}

function escapeTierHtml(value){
    return String(value||'')
        .replace(/&/g,'&amp;')
        .replace(/</g,'&lt;')
        .replace(/>/g,'&gt;')
        .replace(/"/g,'&quot;')
        .replace(/'/g,'&#039;');
}

function formatRatingValue(value){
    const num=Number(value);
    if(!Number.isFinite(num)||num<=0) return '';
    return Number.isInteger(num)?String(num):num.toFixed(1).replace(/\.0$/,'');
}

function getAdminRatingValue(tierList){
    const detail=tierList.adminRatingDetail;
    return detail?.ratingValue ?? tierList.adminRating ?? null;
}

function showTierToast(message,type='success'){
    let toast=document.getElementById('tier-toast');
    if(!toast){
        toast=document.createElement('div');
        toast.id='tier-toast';
        toast.className='guide-toast tier-toast';
        document.body.appendChild(toast);
    }
    toast.textContent=message;
    toast.classList.toggle('error',type==='error');
    toast.classList.add('is-visible');
    clearTimeout(showTierToast._timer);
    showTierToast._timer=setTimeout(()=>toast.classList.remove('is-visible'),2600);
}

function openCommunityTierListDetail(id){
    if(!id) return;
    window.location.href=`/html/tier-list-detail.html?id=${encodeURIComponent(id)}`;
}

async function loadCommunityTierLists(){
    try{
        const response=await fetch(`${OFFICIAL_TIER_LIST_API}/community`,{headers:{Accept:'application/json'},cache:'no-store'});
        if(!response.ok) throw new Error(await readApiError(response));
        const payload=await response.json();
        return Array.isArray(payload)?payload:[];
    }catch(error){
        console.error('Cannot load community tier lists:',error);
        return [];
    }
}

async function renderCommunityCards(){
    const grid=document.getElementById('community-grid');
    const countEl=document.getElementById('community-count');
    const emptyEl=document.getElementById('community-empty');
    if(!grid) return;
    const communityLists=await loadCommunityTierLists();
    communityTierListCache.clear();
    grid.innerHTML='';
    if(communityLists.length===0){ emptyEl.style.display='block'; countEl.textContent='0'; return; }
    emptyEl.style.display='none';
    countEl.textContent=communityLists.length;
    communityLists.forEach(tl=>{
        communityTierListCache.set(String(tl.id),tl);
        const card=document.createElement('div'); card.className='tier-card';
        card.tabIndex=0;
        card.setAttribute('role','link');
        card.setAttribute('aria-label',`Mở tier list ${tl.title||''}`);
        card.onclick=()=>openCommunityTierListDetail(tl.id);
        card.onkeydown=(event)=>{
            if(event.key==='Enter'||event.key===' '){
                event.preventDefault();
                openCommunityTierListDetail(tl.id);
            }
        };
        // Thumbnail
        let thumbHtml='';
        const rows=tl.previewTiers||tl.contentData?.rows||tl.tiers||[];
        rows.forEach(t=>{
            const tierKey=typeof getTierVisualKey==='function'?getTierVisualKey(t.label):'';
            const tierClass=tierKey?` tier-${tierKey}`:'';
            const rowClass=tierKey?` tier-row-${tierKey}${tierClass}`:'';
            const labelClass=tierKey?` tier-label-${tierKey}${tierClass}`:'';
            const previewTierClass=tierKey?` tier-preview-${tierKey}`:'';
            const labelStyle=tierKey?'':` style="background:${t.color||'#95a5a6'}"`;
            let heroMinis='';
            const heroesInRow=t.heroes||((t.cells||[]).flat());
            heroesInRow.slice(0,8).forEach(h=>{
                const heroName=getHeroNameFromValue(h);
                heroMinis+=`<img class="hero-avatar-chip tier-hero-mini" src="${escapeTierHtml(getHeroImgUrl(h))}" alt="${escapeTierHtml(heroName)}" title="${escapeTierHtml(heroName)}" data-hero-name="${escapeTierHtml(heroName)}" loading="lazy" onerror="handleTierHeroImageError(this, this.dataset.heroName, '${TIER_HERO_FALLBACK_IMAGE}')">`;
            });
            thumbHtml+=`<div class="tier-row-preview tier-preview-row${rowClass}${previewTierClass}"><div class="tier-label-mini${labelClass}"${labelStyle}>${escapeTierHtml(t.label)}</div><div class="tier-heroes-mini tier-preview-heroes${previewTierClass}">${heroMinis}</div></div>`;
        });
        // Stars
        let starsHtml='<div class="star-rating-stars" data-id="'+tl.id+'">';
        const averageRating=tl.averageUserRating??tl.communityRating??0;
        for(let i=1;i<=5;i++){
            const filled=i<=Math.round(averageRating)?'filled':'';
            starsHtml+=`<span class="star ${filled}" data-star="${i}" onclick="rateStar(event,${tl.id},${i})" onmouseenter="previewStars(this)" onmouseleave="clearPreview(this)">★</span>`;
        }
        starsHtml+='</div>';
        // Admin badge
        const adminRating=getAdminRatingValue(tl);
        const badgeHtml=adminRating
            ? `<div class="admin-endorsement"><span class="admin-badge-icon">AD</span> Đánh giá của Admin: ${formatRatingValue(adminRating)}/5</div>`
            : `<div class="admin-endorsement is-empty"><span class="admin-badge-icon">AD</span> Chưa có đánh giá từ Admin</div>`;
        card.innerHTML=`
            <div class="tier-card-thumbnail">${thumbHtml}</div>
            <div class="tier-card-body">
                <div class="tier-card-title">${escapeTierHtml(tl.title)}</div>
                <div class="tier-card-author">
                    <img src="${escapeTierHtml(tl.author?.avatar||'')}" alt="${escapeTierHtml(tl.author?.name||'ATG Member')}" referrerpolicy="no-referrer">
                    <span class="tier-card-author-name">${escapeTierHtml(tl.author?.name||'ATG Member')}</span>
                    <span class="tier-card-time">${escapeTierHtml(timeAgo(tl.createdAt))}</span>
                </div>
                <div class="star-rating">
                    ${starsHtml}
                    <span class="star-rating-avg">★ ${formatRatingValue(averageRating)||0}</span>
                    <span class="star-rating-count">(${tl.userRatingCount??tl.totalRatings??0} đánh giá)</span>
                </div>
                ${badgeHtml}
                <div class="tier-card-actions">
                    <button type="button" class="tier-export-btn tier-card-export-btn" onclick="exportCommunityTierListFromCard(event,${tl.id},this)">Tải ảnh</button>
                </div>
            </div>`;
        grid.appendChild(card);
    });
}

function exportCommunityTierListFromCard(event,id,button){
    if(event){
        event.preventDefault();
        event.stopPropagation();
    }
    const tierList=communityTierListCache.get(String(id));
    if(!tierList) return;
    exportTierListImage(tierList,button);
}

function previewStars(el){
    const stars=el.parentElement.querySelectorAll('.star');
    const val=parseInt(el.dataset.star);
    stars.forEach(s=>{ s.classList.toggle('hover-preview',parseInt(s.dataset.star)<=val); });
}
function clearPreview(el){
    el.parentElement.querySelectorAll('.star').forEach(s=>s.classList.remove('hover-preview'));
}
async function rateStar(event,id,stars){
    if(event){
        event.preventDefault();
        event.stopPropagation();
    }
    const u=localStorage.getItem('aov_user');
    if(!u){ showTierToast('Vui lòng đăng nhập để đánh giá.','error'); return; }
    const container=document.querySelector(`.star-rating-stars[data-id="${id}"]`);
    if(container){
        container.querySelectorAll('.star').forEach(s=>{
            s.classList.toggle('filled',parseInt(s.dataset.star)<=stars);
        });
        container.classList.add('is-saving');
    }
    try{
        const response=await fetch(`${OFFICIAL_TIER_LIST_API}/${id}/ratings`,{
            method:'POST',
            headers:{'Content-Type':'application/json'},
            body:JSON.stringify({ratingValue:stars})
        });
        if(!response.ok) throw new Error(await readApiError(response));
        const payload=await response.json();
        const ratingShell=container?.closest('.star-rating');
        const avgEl=ratingShell?.querySelector('.star-rating-avg');
        const countEl=ratingShell?.querySelector('.star-rating-count');
        if(avgEl) avgEl.textContent=`★ ${formatRatingValue(payload.averageUserRating??payload.average)||0}`;
        if(countEl) countEl.textContent=`(${payload.userRatingCount??payload.count??0} đánh giá)`;
        showTierToast('Đã lưu đánh giá.');
    }catch(error){
        console.error('Cannot rate tier list:',error);
        showTierToast(`Không lưu được đánh giá: ${error.message}`,'error');
        renderCommunityCards();
    }finally{
        container?.classList.remove('is-saving');
    }
}

// === Modal ===
function openCreateModal(){
    const u=localStorage.getItem('aov_user');
    if(!u){ alert('Vui lòng đăng nhập để tạo Tier List!'); return; }
    document.getElementById('create-modal').classList.add('active');
    document.body.style.overflow='hidden';
    initModalHeroes();
}
function closeCreateModal(){
    document.getElementById('create-modal').classList.remove('active');
    document.body.style.overflow='';
}
document.getElementById('create-modal')?.addEventListener('click',function(e){
    if(e.target===this) closeCreateModal();
});

function initModalHeroes(){
    const grid=document.getElementById('modal-hero-grid');
    if(!grid) return;
    grid.innerHTML='';
    heroes.forEach(hero=>{
        grid.appendChild(createHeroPoolButton(hero,{modal:true,pool:'modal'}));
    });
    renderClassFilterOptions('modal');
    renderTempHeroPool('modal');
    markHeroPoolSelection('modal');
    document.querySelectorAll('#modal-tier-grid .modal-drop').forEach(c=>{
        c.ondragover=allowDrop; c.ondragenter=dragEnter; c.ondragleave=dragLeave;
        c.ondrop=(ev)=>{
            ev.preventDefault(); ev.currentTarget.classList.remove('drag-over');
            const heroRef=getDragHeroRef(ev);
            const name=normalizeHeroName(heroRef?.name||ev.dataTransfer.getData("heroName"));
            if(!name) return;
            const source=ev.dataTransfer.getData("source");
            const sourceId=ev.dataTransfer.getData("sourceId");
            if(source==='modal-tier'&&sourceId){
                const existing=document.getElementById(sourceId);
                if(existing) ev.currentTarget.appendChild(existing);
                markHeroPoolSelection('modal');
                filterHeroPool('modal');
                return;
            }
            const eid=getTierItemDomIdForPool(heroRef,'modal');
            if(!heroRef.tempInstance&&document.getElementById(eid)){
                ev.currentTarget.appendChild(document.getElementById(eid));
                markHeroPoolSelection('modal');
                filterHeroPool('modal');
                return;
            }
            ev.currentTarget.appendChild(createTierHeroElement(heroRef,{pool:'modal'}));
            markHeroPoolSelection('modal');
            filterHeroPool('modal');
        };
    });
    filterHeroPool('modal');
}

function parseTierListContentData(contentData){
    if(!contentData) return null;
    if(typeof contentData==='string'){
        try{ return JSON.parse(contentData); }catch(e){ return null; }
    }
    return contentData;
}

function getOfficialHeaderCells(){
    const grid=document.getElementById('tier-grid');
    if(!grid) return [];
    return Array.from(grid.children).filter(cell=>cell.classList.contains('header-cell')&&!cell.classList.contains('empty'));
}

function getCellLabel(cell){
    return (cell?.querySelector('span')?.textContent||'').trim();
}

function normalizeImagePath(src){
    if(!src) return '';
    try{ return new URL(src, window.location.origin).pathname; }catch(e){ return src; }
}

function normalizeHexColor(value,fallback){
    return /^#[0-9a-f]{6}$/i.test(value||'')?value:fallback;
}

function getTierHeroName(heroEl){
    return normalizeHeroName(heroEl.dataset.heroName||heroEl.id.replace(/^tier-item-/,''));
}

function getTierHeroRef(heroEl){
    const heroId=heroEl.dataset.heroId||getHeroIdFromValue(heroEl.dataset.heroName);
    const heroName=getTierHeroName(heroEl);
    const sourceHeroId=heroEl.dataset.sourceHeroId||heroId||'';
    const base={
        name:heroName,
        slug:heroEl.dataset.slug||'',
        avatarUrl:heroEl.dataset.avatarUrl||''
    };
    if(heroId) base.heroId=Number(heroId);
    if(heroEl.dataset.tempInstance==='true'){
        base.tempInstance=true;
        base.instanceId=heroEl.dataset.instanceId||getTierItemKey(base);
        if(sourceHeroId) base.sourceHeroId=Number(sourceHeroId);
    }
    return typeof getHeroRefForStorage==='function'?getHeroRefForStorage(base):base;
}

function serializeOfficialTierList(){
    const grid=document.getElementById('tier-grid');
    const columns=getOfficialHeaderCells().map(cell=>{
        const img=cell.querySelector('img');
        return {
            label:getCellLabel(cell),
            icon:normalizeImagePath(img?.getAttribute('src')||img?.src||''),
            alt:img?.getAttribute('alt')||getCellLabel(cell)
        };
    });

    const columnCount=columns.length;
    const rows=Array.from(grid.children)
        .filter(cell=>cell.classList.contains('tier-label'))
        .map(labelCell=>{
            const cells=[];
            let next=labelCell.nextElementSibling;
            for(let i=0;i<columnCount;i++){
                const heroesInCell=[];
                if(next&&next.classList.contains('tier-content')){
                    next.querySelectorAll('.tier-hero').forEach(heroEl=>{
                        const heroRef=getTierHeroRef(heroEl);
                        if(heroRef.name||heroRef.heroId) heroesInCell.push(heroRef);
                    });
                    next=next.nextElementSibling;
                }
                cells.push(heroesInCell);
            }
            return {
                label:getCellLabel(labelCell),
                color:normalizeHexColor(labelCell.querySelector('.tier-color-picker')?.value,'#95a5a6'),
                cells
            };
        });

    return {columns,rows};
}

function serializeModalTierList(){
    const grid=document.getElementById('modal-tier-grid');
    if(!grid) return {columns:[],rows:[]};

    const headers=Array.from(grid.children).filter(cell=>cell.classList.contains('header-cell')&&!cell.classList.contains('empty'));
    const columns=headers.map(cell=>({label:getCellLabel(cell),icon:'',alt:getCellLabel(cell)}));
    const rows=Array.from(grid.children)
        .filter(cell=>cell.classList.contains('tier-label'))
        .map(labelCell=>{
            const cells=[];
            let next=labelCell.nextElementSibling;
            for(let i=0;i<columns.length;i++){
                const heroesInCell=[];
                if(next&&next.classList.contains('tier-content')){
                    next.querySelectorAll('.tier-hero').forEach(heroEl=>{
                        const heroRef=getTierHeroRef(heroEl);
                        if(heroRef.name||heroRef.heroId) heroesInCell.push(heroRef);
                    });
                    next=next.nextElementSibling;
                }
                cells.push(heroesInCell);
            }
            return {
                label:getCellLabel(labelCell),
                color:normalizeHexColor(labelCell.style.backgroundColor,'#95a5a6'),
                cells
            };
        });

    return {columns,rows};
}

function createTierHeroElement(heroValue,{pool='official'}={}){
    const heroRef=getHeroRefForDrag(heroValue);
    const heroId=heroRef.heroId||getHeroIdFromValue(heroValue);
    const heroName=getHeroNameFromValue(heroRef);
    const el=document.createElement('div');
    el.className=`tier-hero${heroRef.tempInstance?' is-temp':''}`;
    el.id=getTierItemDomIdForPool(heroRef,pool);
    el.dataset.heroName=heroName;
    if(heroId) el.dataset.heroId=String(heroId);
    if(heroRef.slug) el.dataset.slug=heroRef.slug;
    if(heroRef.avatarUrl) el.dataset.avatarUrl=heroRef.avatarUrl;
    if(heroRef.tempInstance){
        el.dataset.tempInstance='true';
        el.dataset.instanceId=heroRef.instanceId;
        el.dataset.sourceHeroId=String(heroRef.sourceHeroId||heroId||'');
    }
    el.title=heroRef.tempInstance?`${heroName} (bản tạm)`:heroName;
    el.draggable=true;
    el.ondragstart=(e)=>{
        setHeroDragData(e,pool==='modal'?'modal-tier':'tier',heroRef,el.id);
        setTimeout(()=>e.target.classList.add('dragging'),0);
    };
    el.ondragend=dragEnd;
    el.addEventListener('contextmenu',event=>{
        if(pool!=='modal'&&currentUserRole!=='Admin') return;
        event.preventDefault();
        showHeroContextMenu(event,{type:'tier',heroRef:getTierHeroRef(el),element:el,pool});
    });

    el.style.backgroundImage=`url("${getHeroImgUrl(heroRef)}")`;
    return el;
}

function createOfficialTierHero(heroValue){
    return createTierHeroElement(heroValue,{pool:'official'});
}

function createDeleteButton(onclickName){
    const btn=document.createElement('button');
    btn.className='delete-btn';
    btn.type='button';
    btn.textContent='x';
    btn.setAttribute('onclick',onclickName);
    return btn;
}

function createHeaderCell(column){
    const cell=document.createElement('div');
    cell.className='tier-cell header-cell';
    if(column.icon){
        const img=document.createElement('img');
        img.src=column.icon;
        img.alt=column.alt||column.label||'';
        img.className='role-icon-header';
        img.style.cssText='width:24px;height:24px;margin-right:8px;vertical-align:middle;';
        cell.appendChild(img);
    }

    const span=document.createElement('span');
    span.contentEditable='true';
    span.style.verticalAlign='middle';
    span.textContent=column.label||'';
    cell.appendChild(span);
    cell.appendChild(createDeleteButton('deleteColumn(this)'));
    return cell;
}

function createTierLabelCell(row){
    const color=normalizeHexColor(row.color,'#95a5a6');
    const cell=document.createElement('div');
    cell.className='tier-cell tier-label';
    cell.style.backgroundColor=color;

    const picker=document.createElement('input');
    picker.type='color';
    picker.className='tier-color-picker';
    picker.value=color;
    picker.title='Chon mau tier';
    picker.setAttribute('oninput','updateTierColor(this)');
    cell.appendChild(picker);

    const span=document.createElement('span');
    span.contentEditable='true';
    span.textContent=row.label||'';
    cell.appendChild(span);
    cell.appendChild(createDeleteButton('deleteRow(this)'));
    return cell;
}

function createTierContentCell(heroNames){
    const cell=document.createElement('div');
    cell.className='tier-cell tier-content tier-heroes';
    cell.ondragover=allowDrop;
    cell.ondragenter=dragEnter;
    cell.ondragleave=dragLeave;
    cell.ondrop=dropOnTierList;
    (heroNames||[]).forEach(heroName=>cell.appendChild(createOfficialTierHero(heroName)));
    return cell;
}

function clearOfficialHeroSelection(){
    document.querySelectorAll('#hero-grid .hero-btn.selected').forEach(btn=>btn.classList.remove('selected'));
    document.querySelectorAll('#hero-temp-grid .hero-btn.selected').forEach(btn=>btn.classList.remove('selected'));
}

function clearHeroPoolSelection(pool='official'){
    const config=getHeroPoolConfig(pool);
    document.querySelectorAll(`${config.heroGridSelector} .hero-btn.selected`).forEach(btn=>btn.classList.remove('selected'));
    document.querySelectorAll(`${config.tempGridSelector} .hero-btn.selected`).forEach(btn=>btn.classList.remove('selected'));
}

function markHeroPoolSelection(pool='official'){
    const config=getHeroPoolConfig(pool);
    clearHeroPoolSelection(pool);
    document.querySelectorAll(`${config.tierGridSelector} .tier-hero`).forEach(heroEl=>{
        if(heroEl.dataset.tempInstance==='true'&&heroEl.dataset.instanceId){
            const tempBtn=document.querySelector(`${config.tempGridSelector} .hero-btn[data-instance-id="${cssEscapeValue(heroEl.dataset.instanceId)}"]`);
            if(tempBtn) tempBtn.classList.add('selected');
            return;
        }
        const heroName=getTierHeroName(heroEl);
        const heroBtn=document.querySelector(`${config.heroGridSelector} .hero-btn[data-hero-id="${heroEl.dataset.heroId||''}"]`)||document.getElementById(`${config.isModal?'modal-hero':'hero'}-${heroName}`);
        if(heroBtn) heroBtn.classList.add('selected');
    });
}

function markOfficialHeroSelection(){
    markHeroPoolSelection('official');
}

function renderOfficialTierList(contentData){
    const parsed=parseTierListContentData(contentData);
    const data=typeof normalizeTierRoleColumnOrder==='function'?normalizeTierRoleColumnOrder(parsed):parsed;
    if(!data||!Array.isArray(data.columns)||!Array.isArray(data.rows)||data.columns.length===0) return;

    const grid=document.getElementById('tier-grid');
    if(!grid) return;

    grid.innerHTML='';
    const empty=document.createElement('div');
    empty.className='tier-cell header-cell empty';
    grid.appendChild(empty);

    data.columns.forEach(column=>grid.appendChild(createHeaderCell(column)));
    numCols=data.columns.length;

    data.rows.forEach(row=>{
        grid.appendChild(createTierLabelCell(row));
        for(let i=0;i<numCols;i++){
            grid.appendChild(createTierContentCell(row.cells?.[i]||[]));
        }
    });

    updateGridTemplate();
    markOfficialHeroSelection();
    filterHeroes();
    applyTierListRoleUI();
}

function initTierListImportUi(){
    const input=document.getElementById('tier-import-input');
    if(!input||input.dataset.bound==='true') return;
    input.dataset.bound='true';
    input.addEventListener('input',()=>{
        lastTierImportPreview=null;
        setTierImportApplyEnabled(false);
        const feedback=document.getElementById('tier-import-feedback');
        const summary=document.getElementById('tier-import-summary');
        const preview=document.getElementById('tier-import-preview');
        if(feedback) feedback.innerHTML='<div class="tier-import-alert warning">Dữ liệu đã thay đổi. Bấm "Xem trước" để parse lại trước khi áp dụng.</div>';
        if(summary) summary.innerHTML='';
        if(preview) preview.innerHTML='';
    });
}

function normalizeImportText(value){
    return String(value||'')
        .replace(/[’‘`´]/g,"'")
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g,'')
        .replace(/[đĐ]/g,'d')
        .toLowerCase()
        .replace(/[^a-z0-9'()]+/g,' ')
        .replace(/\s+/g,' ')
        .trim();
}

function makeLooseImportKey(value){
    return normalizeImportText(value).replace(/[^a-z0-9]+/g,'');
}

function toAliasCandidate(value){
    return normalizeImportText(value)
        .split(/\s+/)
        .filter(Boolean)
        .map(token=>token.charAt(0).toUpperCase()+token.slice(1))
        .join(' ');
}

function getTierImportRoleAliases(){
    return IMPORT_ROLE_ALIASES
        .map(alias=>({
            ...alias,
            normalized:normalizeImportText(alias.label),
            tokenCount:String(alias.label).trim().split(/\s+/).filter(Boolean).length
        }))
        .sort((a,b)=>b.tokenCount-a.tokenCount||b.normalized.length-a.normalized.length);
}

function normalizeTierImportRole(roleName){
    const normalized=normalizeImportText(roleName);
    const alias=getTierImportRoleAliases().find(item=>item.normalized===normalized);
    return alias?.code||null;
}

function findRoleAliasAtLineEnd(tokens){
    const aliases=getTierImportRoleAliases();
    for(const alias of aliases){
        if(tokens.length<alias.tokenCount) continue;
        const candidate=tokens.slice(tokens.length-alias.tokenCount).join(' ');
        if(normalizeImportText(candidate)===alias.normalized) return alias;
    }
    return null;
}

function isTierImportHeaderLine(line){
    const normalized=normalizeImportText(String(line||'').replace(/[\t,]+/g,' '));
    return [
        'tuong role tier',
        'tuong vai tro tier',
        'tuong vi tri tier',
        'hero role tier',
        'name role tier',
        'champion role tier'
    ].includes(normalized);
}

function splitTierImportLine(line){
    const trimmed=String(line||'').trim();
    if(!trimmed) return null;

    if(trimmed.includes('\t')){
        const parts=trimmed.split(/\t+/).map(part=>part.trim()).filter(Boolean);
        if(parts.length<3) return null;
        return {heroName:parts[0],roleName:parts[1],tier:parts[2]};
    }

    if(trimmed.includes(',')){
        const parts=trimmed.split(',').map(part=>part.trim()).filter(Boolean);
        if(parts.length<3) return null;
        return {heroName:parts[0],roleName:parts[1],tier:parts[2]};
    }

    const tokens=trimmed.split(/\s+/).filter(Boolean);
    if(tokens.length<3) return null;
    const tier=tokens[tokens.length-1];
    const beforeTier=tokens.slice(0,-1);
    const roleAlias=findRoleAliasAtLineEnd(beforeTier);
    if(roleAlias){
        return {
            heroName:beforeTier.slice(0,beforeTier.length-roleAlias.tokenCount).join(' '),
            roleName:beforeTier.slice(beforeTier.length-roleAlias.tokenCount).join(' '),
            tier
        };
    }

    return {
        heroName:tokens.slice(0,-2).join(' '),
        roleName:tokens[tokens.length-2],
        tier
    };
}

function buildTierImportHeroLookup(){
    const lookup={exact:new Map(),normalized:new Map(),loose:new Map()};
    const add=(map,key,hero)=>{ if(key&&!map.has(key)) map.set(key,hero); };
    heroes.forEach(hero=>{
        const name=String(hero.name||'').trim();
        if(!name) return;
        add(lookup.exact,name.toLowerCase(),hero);
        add(lookup.normalized,normalizeImportText(name),hero);
        add(lookup.loose,makeLooseImportKey(name),hero);
    });
    return lookup;
}

function findHeroForTierImport(rawName,lookup){
    const candidates=Array.from(new Set([
        String(rawName||'').trim(),
        normalizeHeroName(rawName),
        normalizeHeroName(toAliasCandidate(rawName))
    ].filter(Boolean)));

    for(const candidate of candidates){
        const exact=lookup.exact.get(candidate.toLowerCase());
        if(exact) return exact;
    }
    for(const candidate of candidates){
        const normalized=lookup.normalized.get(normalizeImportText(candidate));
        if(normalized) return normalized;
    }
    for(const candidate of candidates){
        const loose=lookup.loose.get(makeLooseImportKey(candidate));
        if(loose) return loose;
    }
    return null;
}

function getImportColumnMeta(roleCode){
    const existing=getOfficialHeaderCells().find(cell=>{
        const label=getCellLabel(cell).toUpperCase();
        const imgAlt=(cell.querySelector('img')?.getAttribute('alt')||'').toUpperCase();
        return label===roleCode||imgAlt===roleCode;
    });
    if(existing){
        const img=existing.querySelector('img');
        return {
            label:getCellLabel(existing)||roleCode,
            icon:normalizeImagePath(img?.getAttribute('src')||img?.src||''),
            alt:img?.getAttribute('alt')||roleCode
        };
    }
    return {...IMPORT_COLUMN_META[roleCode]};
}

function getImportTierColor(tier){
    const labelCell=Array.from(document.querySelectorAll('#tier-grid .tier-label'))
        .find(cell=>getCellLabel(cell).toUpperCase()===tier);
    return normalizeHexColor(labelCell?.querySelector('.tier-color-picker')?.value,IMPORT_TIER_COLORS[tier]);
}

function buildTierImportContentData(validItems){
    const cellsByTierRole=new Map();
    validItems.forEach(item=>{
        const key=`${item.tier}:${item.roleCode}`;
        if(!cellsByTierRole.has(key)) cellsByTierRole.set(key,[]);
        cellsByTierRole.get(key).push(getHeroRefForStorage(item.hero));
    });

    return {
        columns:IMPORT_ROLE_COLUMNS.map(getImportColumnMeta),
        rows:IMPORT_TIER_ROWS.map(tier=>({
            label:tier,
            color:getImportTierColor(tier),
            cells:IMPORT_ROLE_COLUMNS.map(roleCode=>cellsByTierRole.get(`${tier}:${roleCode}`)||[])
        }))
    };
}

function parseTierImportText(rawText){
    const lookup=buildTierImportHeroLookup();
    const errors=[];
    const unresolvedHeroes=[];
    const roleMismatches=[];
    const validItems=[];
    const seenHeroIds=new Set();
    let linesRead=0;

    String(rawText||'').split(/\r?\n/).forEach((line,index)=>{
        const lineNumber=index+1;
        const trimmed=line.trim();
        if(!trimmed||isTierImportHeaderLine(trimmed)) return;
        linesRead++;

        const parts=splitTierImportLine(trimmed);
        if(!parts||!parts.heroName||!parts.roleName||!parts.tier){
            errors.push({lineNumber,message:`Dòng ${lineNumber}: không đọc được đủ 3 cột Tướng, Role, Tier.`});
            return;
        }

        const tier=String(parts.tier||'').trim().toUpperCase();
        const roleCode=normalizeTierImportRole(parts.roleName);
        if(!IMPORT_TIER_ROWS.includes(tier)){
            errors.push({lineNumber,message:`Dòng ${lineNumber}: tier "${parts.tier}" không hợp lệ. Chỉ nhận S, A, B, C, D.`});
            return;
        }
        if(!roleCode){
            errors.push({lineNumber,message:`Dòng ${lineNumber}: role "${parts.roleName}" không hợp lệ hoặc chưa được map.`});
            return;
        }

        const hero=findHeroForTierImport(parts.heroName,lookup);
        if(!hero){
            unresolvedHeroes.push({lineNumber,heroName:parts.heroName,roleName:parts.roleName,roleCode,tier});
            return;
        }

        const primaryRoleCode=typeof getHeroPrimaryRoleCode==='function'?getHeroPrimaryRoleCode(hero):(hero.primaryRoleCode||hero.role||'');
        if(primaryRoleCode&&primaryRoleCode!==roleCode){
            roleMismatches.push({
                lineNumber,
                heroName:hero.name,
                importRoleCode:roleCode,
                primaryRoleCode
            });
        }

        const heroKey=hero.id!==undefined&&hero.id!==null?`id:${hero.id}`:`name:${normalizeImportText(hero.name)}`;
        if(seenHeroIds.has(heroKey)){
            errors.push({lineNumber,message:`Dòng ${lineNumber}: tướng "${parts.heroName}" bị trùng, đã bỏ qua dòng này.`});
            return;
        }
        seenHeroIds.add(heroKey);
        validItems.push({lineNumber,hero,heroName:hero.name,roleName:parts.roleName,roleCode,tier});
    });

    if(linesRead===0){
        errors.push({lineNumber:0,message:'Chưa có dòng dữ liệu hợp lệ để import.'});
    }

    const contentData=buildTierImportContentData(validItems);
    return {
        contentData,
        validItems,
        errors,
        unresolvedHeroes,
        roleMismatches,
        summary:{
            linesRead,
            validItems:validItems.length,
            errors:errors.length,
            unresolvedHeroes:unresolvedHeroes.length,
            roleMismatches:roleMismatches.length
        }
    };
}

function setTierImportApplyEnabled(enabled){
    const button=document.getElementById('tier-import-apply-btn');
    if(button) button.disabled=!enabled;
}

function renderTierImportSummary(result){
    const target=document.getElementById('tier-import-summary');
    if(!target) return;
    target.innerHTML=`
        <div class="tier-import-stats">
            <div class="tier-import-stat"><span>Dòng đã đọc</span><strong>${result.summary.linesRead}</strong></div>
            <div class="tier-import-stat"><span>Hero hợp lệ</span><strong>${result.summary.validItems}</strong></div>
            <div class="tier-import-stat"><span>Lỗi dữ liệu</span><strong>${result.summary.errors}</strong></div>
            <div class="tier-import-stat"><span>Không tìm thấy</span><strong>${result.summary.unresolvedHeroes}</strong></div>
            <div class="tier-import-stat"><span>Khác Primary Role</span><strong>${result.summary.roleMismatches||0}</strong></div>
        </div>`;
}

function renderTierImportIssues(result){
    const blocks=[];
    if(result.errors.length){
        blocks.push(`
            <div class="tier-import-list-block">
                <strong>Lỗi dữ liệu (${result.errors.length})</strong>
                <ul>${result.errors.map(error=>`<li>${escapeTierHtml(error.message)}</li>`).join('')}</ul>
            </div>`);
    }
    if(result.unresolvedHeroes.length){
        blocks.push(`
            <div class="tier-import-list-block">
                <strong>Không tìm thấy tướng (${result.unresolvedHeroes.length})</strong>
                <ul>${result.unresolvedHeroes.map(item=>`<li>Dòng ${item.lineNumber}: ${escapeTierHtml(item.heroName)} (${escapeTierHtml(item.roleName)} -> ${item.roleCode}, tier ${item.tier})</li>`).join('')}</ul>
            </div>`);
    }
    if(result.roleMismatches&&result.roleMismatches.length){
        blocks.push(`
            <div class="tier-import-list-block">
                <strong>Role import khác Primary Role (${result.roleMismatches.length})</strong>
                <ul>${result.roleMismatches.map(item=>`<li>Dòng ${item.lineNumber}: ${escapeTierHtml(item.heroName)} import ${item.importRoleCode}, Primary Role hiện tại ${item.primaryRoleCode}</li>`).join('')}</ul>
            </div>`);
    }
    return blocks.join('');
}

function renderTierImportPreviewTable(result){
    const rows=result.contentData.rows||[];
    return `
        <div class="tier-import-table-wrap">
            <table class="tier-import-table">
                <thead>
                    <tr>
                        <th>Tier</th>
                        ${IMPORT_ROLE_COLUMNS.map(role=>`<th>${role}</th>`).join('')}
                    </tr>
                </thead>
                <tbody>
                    ${rows.map(row=>`
                        <tr>
                            <td class="tier-import-tier" style="background:${escapeTierHtml(row.color)}">${escapeTierHtml(row.label)}</td>
                            ${IMPORT_ROLE_COLUMNS.map((role,index)=>{
                                const heroesInCell=row.cells?.[index]||[];
                                return `<td class="tier-import-cell">
                                    <span class="tier-import-count">${heroesInCell.length} tướng</span>
                                    <div class="tier-import-heroes">
                                        ${heroesInCell.map(heroRef=>{
                                            const heroName=getHeroNameFromValue(heroRef);
                                            return `<span class="tier-import-chip"><img src="${escapeTierHtml(getHeroImgUrl(heroRef))}" alt="${escapeTierHtml(heroName)}" data-hero-name="${escapeTierHtml(heroName)}" onerror="handleTierHeroImageError(this, this.dataset.heroName, '${TIER_HERO_FALLBACK_IMAGE}')">${escapeTierHtml(heroName)}</span>`;
                                        }).join('')}
                                    </div>
                                </td>`;
                            }).join('')}
                        </tr>`).join('')}
                </tbody>
            </table>
        </div>`;
}

function renderTierImportResult(result){
    const feedback=document.getElementById('tier-import-feedback');
    const preview=document.getElementById('tier-import-preview');
    if(feedback){
        const hasWarnings=result.errors.length>0||result.unresolvedHeroes.length>0||(result.roleMismatches&&result.roleMismatches.length>0);
        const type=result.summary.validItems===0?'error':(hasWarnings?'warning':'success');
        const message=result.summary.validItems===0
            ? 'Không có hero hợp lệ để áp dụng. Kiểm tra lỗi bên dưới.'
            : (hasWarnings?'Preview đã tạo cho phần hợp lệ. Có lỗi hoặc tướng chưa tìm thấy cần kiểm tra.':'Preview hợp lệ, có thể áp dụng vào Tier List chính thức.');
        feedback.innerHTML=`<div class="tier-import-alert ${type}">${escapeTierHtml(message)}</div>`;
    }
    renderTierImportSummary(result);
    if(preview){
        preview.innerHTML=renderTierImportPreviewTable(result)+renderTierImportIssues(result);
    }
    setTierImportApplyEnabled(result.summary.validItems>0);
}

async function previewTierListImport(){
    if(getUserRole()!=='Admin'){
        alert('Chỉ Admin mới được import Tier List chính.');
        return;
    }

    if(heroes.length===0) await loadHeroesFromApi();
    const input=document.getElementById('tier-import-input');
    const result=parseTierImportText(input?.value||'');
    lastTierImportPreview=result.summary.validItems>0?result:null;
    renderTierImportResult(result);
}

async function applyTierListImport(){
    if(getUserRole()!=='Admin'){
        alert('Chỉ Admin mới được áp dụng import Tier List chính.');
        return;
    }
    if(!lastTierImportPreview?.contentData||lastTierImportPreview.summary.validItems===0){
        alert('Hãy bấm "Xem trước" và đảm bảo có ít nhất 1 hero hợp lệ trước khi áp dụng.');
        return;
    }

    const summary=lastTierImportPreview.summary;
    const warning=(summary.errors||summary.unresolvedHeroes||summary.roleMismatches)
        ? `\nCòn ${summary.errors} lỗi dữ liệu, ${summary.unresolvedHeroes} tướng không tìm thấy và ${summary.roleMismatches||0} dòng role import khác Primary Role. Những dòng lỗi sẽ không được lưu; dòng khác Primary Role vẫn dùng role import cho tier list.`
        : '';
    const confirmed=confirm(`Áp dụng ${summary.validItems} hero hợp lệ vào Tier List chính thức?${warning}\nDữ liệu official hiện tại sẽ được thay bằng bản preview.`);
    if(!confirmed) return;

    const button=document.getElementById('tier-import-apply-btn');
    const originalText=button?.textContent;
    if(button){
        button.disabled=true;
        button.textContent='Đang áp dụng...';
    }

    try{
        const response=await fetch(OFFICIAL_TIER_LIST_API,{
            method:'POST',
            headers:{'Content-Type':'application/json'},
            body:JSON.stringify({
                title:OFFICIAL_TIER_LIST_TITLE,
                isOfficial:true,
                contentData:lastTierImportPreview.contentData
            })
        });
        if(!response.ok) throw new Error(await readApiError(response));
        officialTierListPayload=await response.json();
        renderOfficialTierList(officialTierListPayload.contentData||lastTierImportPreview.contentData);
        await loadOfficialTierList();
        showTierToast(`Đã áp dụng ${summary.validItems} hero vào Tier List chính.`);
    }catch(error){
        console.error('Cannot apply tier list import:',error);
        alert(`Không áp dụng được Tier List import: ${error.message}`);
        setTierImportApplyEnabled(true);
    }finally{
        if(button){
            button.textContent=originalText;
            button.disabled=false;
        }
    }
}

async function readApiError(response){
    try{
        const payload=await response.json();
        return payload.error||payload.message||response.statusText||'Request failed';
    }catch(e){
        return response.statusText||'Request failed';
    }
}

async function loadOfficialTierList(){
    try{
        const response=await fetch(`${OFFICIAL_TIER_LIST_API}/official`,{headers:{Accept:'application/json'},cache:'no-store'});
        if(!response.ok) throw new Error(await readApiError(response));

        const payload=await response.json();
        if(payload.exists===false) return;
        officialTierListPayload=payload;
        renderOfficialTierList(payload.contentData);
    }catch(error){
        console.error('Cannot load official tier list:',error);
    }
}

function exportOfficialTierList(button){
    const payload={
        ...(officialTierListPayload||{}),
        id:officialTierListPayload?.id||'official',
        title:officialTierListPayload?.title||OFFICIAL_TIER_LIST_TITLE,
        author:officialTierListPayload?.author||{name:'ATG Academy'},
        createdAt:officialTierListPayload?.createdAt||officialTierListPayload?.updatedAt||new Date().toISOString(),
        contentData:serializeOfficialTierList()
    };
    exportTierListImage(payload,button);
}

function filterModalHeroes(){
    filterHeroPool('modal');
}

function saveOfficialTierList(){
    alert('✅ Đã lưu Meta Chính thành công! (Sẽ gọi API POST /api/tier-lists khi kết nối backend)');
}

async function submitCommunityTierList(){
    const title=document.getElementById('modal-title')?.value||'Tier List Moi';
    const description=(document.getElementById('modal-note')?.value||'').trim();
    try{
        const response=await fetch(OFFICIAL_TIER_LIST_API,{
            method:'POST',
            headers:{'Content-Type':'application/json'},
            body:JSON.stringify({
                title,
                description,
                isOfficial:false,
                contentData:serializeModalTierList()
            })
        });
        if(!response.ok) throw new Error(await readApiError(response));
        closeCreateModal();
        const titleInput=document.getElementById('modal-title');
        const noteInput=document.getElementById('modal-note');
        if(titleInput) titleInput.value='';
        if(noteInput) noteInput.value='';
        renderCommunityCards();
        showTierToast(`Đã đăng "${title}" lên cộng đồng.`);
    }catch(error){
        console.error('Cannot submit community tier list:',error);
        showTierToast(`Không đăng được Tier List: ${error.message}`,'error');
    }
}

function findOfficialSaveButton(){
    return document.getElementById('save-official-btn')
        || Array.from(document.querySelectorAll('.btn-save-official'))
            .find(button=>button.getAttribute('onclick')==='saveOfficialTierList()');
}

window.saveOfficialTierList=async function saveOfficialTierList(){
    if(getUserRole()!=='Admin'){
        alert('Chi Admin moi duoc luu Tier List chinh.');
        return;
    }

    const button=findOfficialSaveButton();
    const originalText=button?.textContent;
    if(button){
        button.disabled=true;
        button.textContent='Dang luu...';
    }

    try{
        const response=await fetch(OFFICIAL_TIER_LIST_API,{
            method:'POST',
            headers:{'Content-Type':'application/json'},
            body:JSON.stringify({
                title:OFFICIAL_TIER_LIST_TITLE,
                isOfficial:true,
                contentData:serializeOfficialTierList()
            })
        });

        if(!response.ok) throw new Error(await readApiError(response));

        const payload=await response.json();
        renderOfficialTierList(payload.contentData);
        alert('Da luu Tier List chinh. Nguoi ngoai vao trang se thay ngay.');
    }catch(error){
        console.error('Cannot save official tier list:',error);
        alert(`Khong luu duoc Tier List chinh: ${error.message}`);
    }finally{
        if(button){
            button.disabled=false;
            button.textContent=originalText;
        }
    }
};

window.previewTierListImport=previewTierListImport;
window.applyTierListImport=applyTierListImport;

initApp();
