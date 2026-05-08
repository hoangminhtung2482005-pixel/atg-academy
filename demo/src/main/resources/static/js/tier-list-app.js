let currentRoleFilter='Tất cả', currentClassFilter='Tất cả', numCols=5, currentUserRole='Custom';
const tempHeroInstances=[];
let modalRoleFilter='Tất cả', modalClassFilter='Tất cả';
const modalTempHeroInstances=[];
let heroCatalogLoadPromise=null;

const OFFICIAL_TIER_LIST_API='/api/tier-lists';
const ADMIN_OFFICIAL_TIER_LIST_API='/api/admin/tier-lists/official/regenerate-from-hero-scores';
const OFFICIAL_TIER_LIST_TITLE='Tier List Meta';
const OFFICIAL_TIER_LIST_SUBTITLE_PREFIX='Cập nhật bởi Admin';
let officialTierListPayload=null;
const communityTierListCache=new Map();
const COMMUNITY_SAVED_API=`${OFFICIAL_TIER_LIST_API}/saved`;
const COMMUNITY_VIEW=Object.freeze({
    HIGHLIGHT:'highlight',
    ALL:'all',
    MINE:'mine'
});
const COMMUNITY_PAGE_ROUTES=Object.freeze({
    [COMMUNITY_VIEW.HIGHLIGHT]:'/tier-list',
    [COMMUNITY_VIEW.ALL]:'/tier-list/all',
    [COMMUNITY_VIEW.MINE]:'/tier-list/mine'
});
function normalizeCommunityViewValue(value){
    const normalized=String(value||'').trim().toLowerCase();
    if(normalized==='featured'||normalized==='recommended') return COMMUNITY_VIEW.HIGHLIGHT;
    if(Object.values(COMMUNITY_VIEW).includes(normalized)) return normalized;
    return COMMUNITY_VIEW.HIGHLIGHT;
}
const COMMUNITY_VIEW_CONFIG=Object.freeze({
    [COMMUNITY_VIEW.HIGHLIGHT]:{
        endpoint:`${OFFICIAL_TIER_LIST_API}/community`,
        emptyText:'Chưa có Community Tier List nổi bật nào.',
        guestMessage:'',
        errorText:'Không tải được danh sách Community Tier List nổi bật.'
    },
    [COMMUNITY_VIEW.ALL]:{
        endpoint:`${OFFICIAL_TIER_LIST_API}/community/all`,
        emptyText:'Chưa có Community Tier List nào.',
        guestMessage:'',
        errorText:'Không tải được toàn bộ Community Tier List.'
    },
    [COMMUNITY_VIEW.MINE]:{
        endpoint:`${OFFICIAL_TIER_LIST_API}/me`,
        emptyText:'Bạn chưa tạo Tier List nào.',
        guestMessage:'Vui lòng đăng nhập để xem Tier List của bạn.',
        errorText:'Không tải được Tier List của bạn.'
    }
});
const COMMUNITY_SECTION=Object.freeze({
    DEFAULT:{
        key:'default',
        countId:'community-count',
        gridId:'community-grid',
        statusId:'community-status',
        emptyId:'community-empty'
    },
    MINE_CREATED:{
        key:'mine-created',
        countId:'community-created-count',
        gridId:'community-created-grid',
        statusId:'community-created-status',
        emptyId:'community-created-empty'
    },
    MINE_SAVED:{
        key:'mine-saved',
        countId:'community-saved-count',
        gridId:'community-saved-grid',
        statusId:'community-saved-status',
        emptyId:'community-saved-empty'
    }
});
let currentCommunityView=typeof document!=='undefined'
    ? normalizeCommunityViewValue(document.body?.dataset?.communityView||COMMUNITY_VIEW.HIGHLIGHT)
    : COMMUNITY_VIEW.HIGHLIGHT;
let communityRenderRequestId=0;
const OFFICIAL_TIER_LIST_LEGACY_TITLES=['Tier List Meta Hien Tai','Tier List Meta Hiện Tại'];
let modalTierDragActive=false;
const GUEST_TIER_LIST_SESSION_KEY='atg_guest_tier_list_draft';
const DEFAULT_MODAL_COLUMNS=Object.freeze(['DSL','JGL','MID','ADL','SUP']);
const DEFAULT_MODAL_ROWS=Object.freeze([
    Object.freeze({label:'S',color:'#e74c3c'}),
    Object.freeze({label:'A',color:'#9b59b6'}),
    Object.freeze({label:'B',color:'#3498db'}),
    Object.freeze({label:'C',color:'#2ecc71'}),
    Object.freeze({label:'D',color:'#95a5a6'})
]);
const COMMUNITY_TIER_LIST_LIMIT=5;
const COMMUNITY_TIER_LIST_LIMIT_MESSAGE='B\u1ea1n ch\u1ec9 c\u00f3 th\u1ec3 l\u01b0u t\u1ed1i \u0111a 5 tier list.';
const COMMUNITY_TIER_LIST_LIMIT_REACHED_MESSAGE='\u0110\u00e3 \u0111\u1ea1t gi\u1edbi h\u1ea1n 5/5 tier list.';
let guestDraftSyncTimer=null;
let guestDraftStorageWarningShown=false;
let guestDraftSessionActive=false;
let modalDraftHydrated=false;
let guestDraftAutoRestoreAttempted=false;
let currentUserCommunityTierListCountPromise=null;
const currentUserCommunityTierListCountState={
    ownerEmail:'',
    tierListCount:0,
    loaded:false
};

function normalizeTierListText(value){
    return String(value??'').normalize('NFC').trim();
}

function isTierListUserAuthenticated(){
    if(typeof window.isAuthenticated==='function'){
        try{
            return window.isAuthenticated();
        }catch(error){
            console.warn('Tier list auth helper failed:',error);
        }
    }
    return !!getTierListCurrentUser();
}

function buildDefaultModalContentData(){
    return {
        columns:DEFAULT_MODAL_COLUMNS.map(label=>({label,icon:'',alt:label})),
        rows:DEFAULT_MODAL_ROWS.map(row=>({
            label:row.label,
            color:row.color,
            cells:DEFAULT_MODAL_COLUMNS.map(()=>[])
        }))
    };
}

function cloneModalContentData(contentData){
    return JSON.parse(JSON.stringify(contentData||buildDefaultModalContentData()));
}

function normalizeTierColorHex(value,fallback='#95a5a6'){
    if(/^#[0-9a-f]{6}$/i.test(value||'')) return value.toLowerCase();
    const rgb=String(value||'').match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/i);
    if(rgb){
        const [r,g,b]=rgb.slice(1,4).map(part=>Math.max(0,Math.min(255,Number(part)||0)));
        return `#${[r,g,b].map(part=>part.toString(16).padStart(2,'0')).join('')}`;
    }
    return fallback.toLowerCase();
}

function countGuestDraftHeroes(contentData){
    return (contentData?.rows||[]).reduce((total,row)=>total+(row?.cells||[]).reduce((rowTotal,cell)=>{
        return rowTotal+(Array.isArray(cell)?cell.length:0);
    },0),0);
}

function isDefaultModalDraftContentData(contentData){
    const normalized=contentData&&Array.isArray(contentData.columns)&&Array.isArray(contentData.rows)
        ? contentData
        : buildDefaultModalContentData();
    const columnsMatch=DEFAULT_MODAL_COLUMNS.every((label,index)=>{
        return normalizeTierListText(normalized.columns?.[index]?.label)===label;
    });
    const rowsMatch=DEFAULT_MODAL_ROWS.every((row,index)=>{
        const currentRow=normalized.rows?.[index];
        return normalizeTierListText(currentRow?.label)===row.label
            && normalizeTierColorHex(currentRow?.color,row.color)===row.color
            && Array.isArray(currentRow?.cells)
            && currentRow.cells.every(cell=>Array.isArray(cell)?cell.length===0:true);
    });
    return columnsMatch&&rowsMatch&&countGuestDraftHeroes(normalized)===0;
}

function isGuestDraftMeaningful(payload){
    const title=normalizeTierListText(payload?.title||'');
    const description=normalizeTierListText((payload?.note??payload?.description)||'');
    const contentData=payload?.contentData;
    return Boolean(title||description||countGuestDraftHeroes(contentData)>0||!isDefaultModalDraftContentData(contentData));
}

function notifyGuestDraftStorageFailure(){
    if(guestDraftStorageWarningShown) return;
    guestDraftStorageWarningShown=true;
    console.warn('Guest tier list draft could not be stored in sessionStorage.');
    if(typeof showTierToast==='function'){
        showTierToast('Không thể lưu draft tạm thời trong phiên trình duyệt này.','error');
    }
}

function loadGuestDraftFromSession(){
    try{
        const raw=window.sessionStorage?.getItem(GUEST_TIER_LIST_SESSION_KEY);
        if(!raw) return null;
        const payload=JSON.parse(raw);
        const contentData=payload?.contentData&&typeof payload.contentData==='object'
            ? payload.contentData
            : buildDefaultModalContentData();
        guestDraftSessionActive=true;
        return {
            title:String(payload?.title||''),
            description:String((payload?.note??payload?.description)||''),
            contentData:cloneModalContentData(contentData),
            updatedAt:payload?.updatedAt||null
        };
    }catch(error){
        console.warn('Cannot load guest tier list draft from sessionStorage:',error);
        try{
            window.sessionStorage?.removeItem(GUEST_TIER_LIST_SESSION_KEY);
        }catch(removeError){
            console.warn('Cannot clear invalid guest tier list draft:',removeError);
        }
        notifyGuestDraftStorageFailure();
        guestDraftSessionActive=false;
        return null;
    }
}

function clearGuestDraftSession(){
    try{
        window.sessionStorage?.removeItem(GUEST_TIER_LIST_SESSION_KEY);
    }catch(error){
        console.warn('Cannot clear guest tier list draft from sessionStorage:',error);
    }
    guestDraftSessionActive=false;
}

function shouldSyncGuestDraftToSession(){
    return !isTierListUserAuthenticated()||guestDraftSessionActive;
}

function buildGuestDraftPayload(){
    const note=document.getElementById('modal-note')?.value||'';
    return {
        title:document.getElementById('modal-title')?.value||'',
        note,
        description:note,
        contentData:serializeModalTierList(),
        updatedAt:new Date().toISOString()
    };
}

function saveGuestDraftToSession(){
    if(!shouldSyncGuestDraftToSession()) return false;
    const payload=buildGuestDraftPayload();
    if(!isGuestDraftMeaningful(payload)){
        clearGuestDraftSession();
        return false;
    }
    try{
        window.sessionStorage?.setItem(GUEST_TIER_LIST_SESSION_KEY,JSON.stringify(payload));
        guestDraftSessionActive=true;
        return true;
    }catch(error){
        console.warn('Cannot save guest tier list draft to sessionStorage:',error);
        notifyGuestDraftStorageFailure();
        return false;
    }
}

function queueGuestDraftSessionSync(){
    if(!shouldSyncGuestDraftToSession()) return;
    clearTimeout(guestDraftSyncTimer);
    guestDraftSyncTimer=setTimeout(()=>saveGuestDraftToSession(),120);
}

function syncGuestDraftNotice(){
    const note=document.getElementById('guest-draft-notice');
    if(note){
        note.hidden=isTierListUserAuthenticated();
    }
}

function restoreGuestDraftToModal(draft){
    const grid=document.getElementById('modal-tier-grid');
    if(!grid||!draft) return false;

    const contentData=draft.contentData&&Array.isArray(draft.contentData.columns)&&Array.isArray(draft.contentData.rows)
        ? draft.contentData
        : buildDefaultModalContentData();
    const columns=Array.isArray(contentData.columns)?contentData.columns:[];
    const rows=Array.isArray(contentData.rows)?contentData.rows:[];

    const titleInput=document.getElementById('modal-title');
    const noteInput=document.getElementById('modal-note');
    if(titleInput) titleInput.value=String(draft.title||'');
    if(noteInput) noteInput.value=String(draft.description||'');

    const headerCells=Array.from(grid.querySelectorAll('.header-cell:not(.empty)'));
    headerCells.forEach((cell,index)=>{
        const label=normalizeTierListText(columns[index]?.label)||DEFAULT_MODAL_COLUMNS[index]||`Cot ${index+1}`;
        const span=cell.querySelector('span');
        if(span) span.textContent=label;
    });

    const tierCells=Array.from(grid.querySelectorAll('.tier-label'));
    tierCells.forEach((labelCell,rowIndex)=>{
        const row=rows[rowIndex]||{};
        const span=labelCell.querySelector('span');
        if(span){
            span.textContent=normalizeTierListText(row.label)||DEFAULT_MODAL_ROWS[rowIndex]?.label||String(rowIndex+1);
        }
        labelCell.style.backgroundColor=normalizeTierColorHex(row.color,DEFAULT_MODAL_ROWS[rowIndex]?.color||'#95a5a6');

        let next=labelCell.nextElementSibling;
        for(let columnIndex=0;columnIndex<headerCells.length;columnIndex++){
            if(!next||!next.classList.contains('tier-content')) break;
            next.innerHTML='';
            const heroesInCell=Array.isArray(row.cells?.[columnIndex])?row.cells[columnIndex]:[];
            heroesInCell.forEach(hero=>next.appendChild(createTierHeroElement(hero,{pool:'modal'})));
            next=next.nextElementSibling;
        }
    });

    markHeroPoolSelection('modal');
    filterHeroPool('modal');
    guestDraftSessionActive=true;
    modalDraftHydrated=true;
    return true;
}

function hydrateModalDraftFromSession(options={}){
    if(modalDraftHydrated&&!options.force) return false;
    modalDraftHydrated=true;
    const draft=loadGuestDraftFromSession();
    syncGuestDraftNotice();
    if(!draft) return false;
    const restored=restoreGuestDraftToModal(draft);
    if(restored&&options.showToast&&typeof showTierToast==='function'){
        showTierToast('Đã khôi phục draft tạm thời trong phiên này.');
    }
    return restored;
}

function bindGuestDraftSessionSync(){
    const modal=document.getElementById('create-modal');
    if(!modal||modal.dataset.guestDraftBound==='true') return;
    modal.dataset.guestDraftBound='true';

    ['modal-title','modal-note'].forEach(id=>{
        document.getElementById(id)?.addEventListener('input',queueGuestDraftSessionSync);
    });

    const grid=document.getElementById('modal-tier-grid');
    if(grid){
        grid.addEventListener('input',queueGuestDraftSessionSync);
        const observer=new MutationObserver(()=>queueGuestDraftSessionSync());
        observer.observe(grid,{
            subtree:true,
            childList:true,
            characterData:true,
            attributes:true,
            attributeFilter:['style']
        });
    }
}

function getNavigationType(){
    const navigationEntry=performance.getEntriesByType?.('navigation')?.[0];
    if(navigationEntry?.type) return navigationEntry.type;
    if(typeof performance.navigation?.type==='number'){
        return performance.navigation.type===1?'reload':'navigate';
    }
    return '';
}

function autoRestoreGuestDraftAfterReload(){
    if(guestDraftAutoRestoreAttempted) return;
    guestDraftAutoRestoreAttempted=true;
    if(isTierListUserAuthenticated()) return;
    if(getNavigationType()!=='reload') return;
    if(!loadGuestDraftFromSession()) return;
    openCreateModal({restoreSessionDraft:true,showRestoreToast:true});
}

function getOfficialTierListTitleKey(value){
    return normalizeTierListText(value)
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g,'')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g,' ')
        .trim();
}

function normalizeOfficialTierListTitle(value){
    const normalized=normalizeTierListText(value);
    if(!normalized) return OFFICIAL_TIER_LIST_TITLE;
    const titleKey=getOfficialTierListTitleKey(normalized);
    if(OFFICIAL_TIER_LIST_LEGACY_TITLES.some(legacy=>getOfficialTierListTitleKey(legacy)===titleKey)){
        return OFFICIAL_TIER_LIST_TITLE;
    }
    return normalized;
}

function getOfficialTierListCreatorName(payload){
    const creatorName=normalizeTierListText(payload?.creatorName||payload?.creator_name||payload?.author?.name||'');
    return creatorName||'ATG Academy';
}

function normalizeOfficialTierListPayload(payload){
    if(!payload||typeof payload!=='object') return payload;
    return {
        ...payload,
        title:normalizeOfficialTierListTitle(payload.title),
        creatorName:getOfficialTierListCreatorName(payload)
    };
}

function updateOfficialTierListHeading(payload=officialTierListPayload){
    const titleEl=document.querySelector('.official-title');
    if(titleEl){
        titleEl.textContent=normalizeOfficialTierListTitle(payload?.title);
    }

    const subtitleEl=document.querySelector('.official-subtitle');
    if(subtitleEl){
        subtitleEl.textContent=`${OFFICIAL_TIER_LIST_SUBTITLE_PREFIX} ${getOfficialTierListCreatorName(payload)}`;
    }
}

function normalizeHeroName(name){
    const aliases={"Flowborn":"Flowborn (Marksman)","Flowborn ADL":"Flowborn (Marksman)","Flowborn MID":"Flowborn (Marksman)","Ngo Khong":"Wukong","Trieu Van":"Zanis","Dieu Thuyen":"Diaochan","Lu Bo":"Lu Bu","Roule":"Rouie","Governa":"Goverra","Tochi":"Tachi","Richter":"Riktor","KilGroth":"Kil'Groth"};
    const trimmed=String(name||'').trim();
    return aliases[trimmed]||trimmed;
}

function getUserRole(){
    return getTierListCurrentUser()?.role||'Custom';
}

async function ensureTierHeroCatalogLoaded(force=false){
    if(!force&&heroes.length>0) return;
    if(!force&&heroCatalogLoadPromise){
        await heroCatalogLoadPromise;
        return;
    }
    heroCatalogLoadPromise=(async()=>{
        await loadHeroesFromApi();
    })();
    try{
        await heroCatalogLoadPromise;
    }finally{
        heroCatalogLoadPromise=null;
    }
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
    document.querySelectorAll('#tier-grid .delete-btn').forEach(button=>{
        button.disabled=!isEditable;
        button.style.display=isEditable?'inline-flex':'none';
    });
    document.querySelectorAll('#tier-grid .tier-color-picker').forEach(input=>{
        input.disabled=!isEditable;
        input.style.display=isEditable?'block':'none';
    });
}

function applyTierListRoleUI(){
    currentUserRole=getUserRole();
    const isAdmin=currentUserRole==='Admin';
    const grid=document.getElementById('tier-grid');
    // Admin controls
    const ac=document.getElementById('admin-controls');
    const hp=document.getElementById('hero-pool-official');
    const up=document.getElementById('user-prompt');
    const bc=document.getElementById('btn-create-community');
    if(ac) ac.style.display=isAdmin?'flex':'none';
    if(hp) hp.style.display='none';
    if(up) up.style.display=(!isAdmin&&isTierListUserAuthenticated())?'flex':'none';
    if(bc) bc.style.display=document.getElementById('create-modal')?'inline-flex':'none';
    // Official meta board is now backend-generated from hero scores for every role,
    // so the official grid stays read-only even for admins.
    if(grid){
        grid.classList.add('tier-grid-readonly');
        setOfficialGridEditable(false);
    }
    syncGuestDraftNotice();
}

// Listen for header loaded + auth changes
document.addEventListener('headerLoaded',()=>{ setTimeout(applyTierListRoleUI,300); });
document.addEventListener('authChanged',()=>{
    resetCurrentUserCommunityTierListCountState(getTierListCurrentUser()?.email||'');
    if(isTierListUserAuthenticated()){
        loadCurrentUserCommunityTierListCount({force:true,silent:true});
    }
    applyTierListRoleUI();
    renderCommunityCards();
});
window.addEventListener('storage',()=>{
    resetCurrentUserCommunityTierListCountState(getTierListCurrentUser()?.email||'');
    if(isTierListUserAuthenticated()){
        loadCurrentUserCommunityTierListCount({force:true,silent:true});
    }
    applyTierListRoleUI();
    renderCommunityCards();
});

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

function setModalDragOutActive(active){
    modalTierDragActive=Boolean(active);
    const modal=document.getElementById('create-modal');
    const zone=document.getElementById('modal-drop-out-zone');
    modal?.classList.toggle('is-dragging-tier',modalTierDragActive);
    zone?.classList.toggle('is-active',modalTierDragActive);
    if(!modalTierDragActive) zone?.classList.remove('is-over');
}

function returnTierHeroToPoolById(sourceId,pool='modal'){
    if(!sourceId) return false;
    const element=document.getElementById(sourceId);
    if(!element) return false;
    const actualPool=pool|| (element.closest('#modal-tier-grid')?'modal':'official');
    element.remove();
    markHeroPoolSelection(actualPool);
    filterHeroPool(actualPool);
    return true;
}

function initModalDragOutZone(){
    const zone=document.getElementById('modal-drop-out-zone');
    if(!zone||zone.dataset.bound==='true') return;
    zone.dataset.bound='true';
    zone.addEventListener('dragover',event=>{
        if(!modalTierDragActive) return;
        event.preventDefault();
        event.dataTransfer.dropEffect='move';
        zone.classList.add('is-over');
    });
    zone.addEventListener('dragleave',event=>{
        if(!zone.contains(event.relatedTarget)) zone.classList.remove('is-over');
    });
    zone.addEventListener('drop',event=>{
        if(!modalTierDragActive) return;
        event.preventDefault();
        returnTierHeroToPoolById(event.dataTransfer.getData('sourceId'),'modal');
        setModalDragOutActive(false);
    });
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
    const officialGrid=document.getElementById('tier-grid');
    const communityGrid=document.getElementById('community-grid');
    const mineCreatedGrid=document.getElementById('community-created-grid');

    currentCommunityView=normalizeCommunityViewValue(document.body?.dataset?.communityView||currentCommunityView);
    syncOfficialCommunitySectionCopy();
    initCommunityNavigation();

    if(grid){
        await ensureTierHeroCatalogLoaded();
        grid.innerHTML='';
        if(heroes.length===0){
        grid.innerHTML='<div class="draft-warning" style="grid-column:1/-1">Chưa có dữ liệu tướng trong database. Hãy chạy sql/seed_heroes.sql rồi tải lại trang.</div>';
        }
        heroes.forEach(hero=>{
            grid.appendChild(createHeroPoolButton(hero));
        });
        renderClassFilterOptions();
        renderTempHeroPool();
    }

    if(officialGrid){
        document.querySelectorAll('#tier-grid .tier-heroes').forEach(c=>{
            c.ondragover=allowDrop; c.ondragenter=dragEnter; c.ondragleave=dragLeave; c.ondrop=dropOnTierList;
        });
    }

    if(officialGrid||document.getElementById('create-modal')){
        document.body.ondragover=(ev)=>ev.preventDefault();
        document.body.ondrop=(ev)=>{
        const source=ev.dataTransfer.getData("source");
        if(source==='modal-tier'&&!ev.target.closest('#modal-tier-grid .modal-drop')){
            ev.preventDefault();
            returnTierHeroToPoolById(ev.dataTransfer.getData("sourceId"),'modal');
            setModalDragOutActive(false);
            return;
        }
        if(!ev.target.closest('.tier-heroes')){
            if(source==='tier'){
                const sid=ev.dataTransfer.getData("sourceId");
                const el=document.getElementById(sid); if(el) el.remove();
                markOfficialHeroSelection();
                filterHeroes();
            }
        }
    };
    }

    if(communityGrid||mineCreatedGrid){
        await renderCommunityCards();
    }
    if(officialGrid){
        loadOfficialTierList();
    }
    setTimeout(applyTierListRoleUI,200);
    if(isTierListUserAuthenticated()){
        setTimeout(()=>loadCurrentUserCommunityTierListCount({silent:true}),220);
    }else{
        resetCurrentUserCommunityTierListCountState();
    }
    setTimeout(autoRestoreGuestDraftAfterReload,260);
}

function dragStartFromList(ev,value,source='list'){
    setModalDragOutActive(false);
    const heroRef=value&&typeof value==='object'?value:getHeroRefForDrag(value);
    setHeroDragData(ev,source,heroRef);
    setTimeout(()=>ev.target.classList.add('dragging'),0);
}
function dragStartFromTier(ev,value){
    setModalDragOutActive(false);
    const heroRef=getHeroRefForDrag(value&&typeof value==='object'?value:getTierHeroRef(ev.target));
    setHeroDragData(ev,'tier',heroRef,ev.target.id);
    setTimeout(()=>ev.target.classList.add('dragging'),0);
}
function dragEnd(ev){
    ev.target.classList.remove('dragging');
    document.querySelectorAll('.drag-over').forEach(el=>el.classList.remove('drag-over'));
    setModalDragOutActive(false);
}
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

window.showTierToast=showTierToast;

function getTierListCurrentUser(){
    if(typeof getAuthUser==='function') return getAuthUser();
    try{
        const raw=localStorage.getItem('aov_user');
        return raw?JSON.parse(raw):null;
    }catch(error){
        return null;
    }
}

function resetCurrentUserCommunityTierListCountState(ownerEmail=''){
    currentUserCommunityTierListCountState.ownerEmail=String(ownerEmail||'').trim().toLowerCase();
    currentUserCommunityTierListCountState.tierListCount=0;
    currentUserCommunityTierListCountState.loaded=false;
}

function ensureCurrentUserCommunityTierListCountOwner(){
    const ownerEmail=String(getTierListCurrentUser()?.email||'').trim().toLowerCase();
    if(currentUserCommunityTierListCountState.ownerEmail!==ownerEmail){
        resetCurrentUserCommunityTierListCountState(ownerEmail);
    }
    return ownerEmail;
}

function rememberCurrentUserCommunityTierListCount(count){
    currentUserCommunityTierListCountState.tierListCount=Math.max(0,Number(count)||0);
    currentUserCommunityTierListCountState.loaded=true;
}

async function loadCurrentUserCommunityTierListCount(options={}){
    if(!isTierListUserAuthenticated()){
        resetCurrentUserCommunityTierListCountState();
        return null;
    }
    ensureCurrentUserCommunityTierListCountOwner();
    if(currentUserCommunityTierListCountState.loaded&&!options.force){
        return currentUserCommunityTierListCountState.tierListCount;
    }
    if(currentUserCommunityTierListCountPromise){
        return currentUserCommunityTierListCountPromise;
    }
    currentUserCommunityTierListCountPromise=(async()=>{
        const response=await fetch('/api/users/me/content-summary',{
            headers:{Accept:'application/json'},
            cache:'no-store'
        });
        if(!response.ok){
            if(response.status===401){
                resetCurrentUserCommunityTierListCountState();
                return null;
            }
            throw new Error(await readApiError(response));
        }
        const payload=await response.json();
        const tierListCount=Math.max(0,Number(payload?.tierListCount||0));
        rememberCurrentUserCommunityTierListCount(tierListCount);
        return tierListCount;
    })().catch(error=>{
        if(options.silent!==true){
            console.warn('Cannot load current user community tier list count:',error);
        }
        return null;
    }).finally(()=>{
        currentUserCommunityTierListCountPromise=null;
    });
    return currentUserCommunityTierListCountPromise;
}

async function hasReachedCurrentUserCommunityTierListLimit(options={}){
    const tierListCount=await loadCurrentUserCommunityTierListCount(options);
    return Number.isFinite(tierListCount)&&tierListCount>=COMMUNITY_TIER_LIST_LIMIT;
}

function syncMineCommunityTierListQuota(count){
    const safeCount=Math.max(0,Number(count)||0);
    const countEl=document.getElementById(COMMUNITY_SECTION.MINE_CREATED.countId);
    if(countEl){
        countEl.textContent=`${safeCount}/${COMMUNITY_TIER_LIST_LIMIT}`;
    }
    const statusEl=document.getElementById(COMMUNITY_SECTION.MINE_CREATED.statusId);
    if(!statusEl||statusEl.classList.contains('is-error')) return;
    if(safeCount>=COMMUNITY_TIER_LIST_LIMIT){
        statusEl.dataset.quotaWarning='true';
        setCommunitySectionStatus(COMMUNITY_SECTION.MINE_CREATED,COMMUNITY_TIER_LIST_LIMIT_REACHED_MESSAGE,'warning');
        return;
    }
    if(statusEl.dataset.quotaWarning==='true'){
        delete statusEl.dataset.quotaWarning;
        setCommunitySectionStatus(COMMUNITY_SECTION.MINE_CREATED,'');
    }
}

function canCurrentUserDeleteTierList(tierList){
    if(!tierList||tierList.isOfficial) return false;
    if(tierList.canDelete===true) return true;
    const user=getTierListCurrentUser();
    if(!user) return false;
    if(user.role==='Admin') return true;
    const author=tierList.author||{};
    return (user.email&&author.email&&String(user.email).toLowerCase()===String(author.email).toLowerCase())
        || (user.id&&author.id&&String(user.id)===String(author.id));
}

function tierDeleteErrorMessage(status,fallback){
    if(status===401) return 'Vui lòng đăng nhập để xóa Tier List.';
    if(status===403) return 'Bạn không có quyền xóa Tier List này.';
    if(status===404) return 'Tier List không tồn tại hoặc đã bị xóa.';
    return fallback||'Không xóa được Tier List.';
}

function buildTierListDeleteRequest(id){
    const url=`${OFFICIAL_TIER_LIST_API}/${encodeURIComponent(String(id))}`;
    const headers=new Headers({Accept:'application/json'});
    const token=typeof getAuthToken==='function' ? getAuthToken() : null;
    if(token) headers.set('Authorization',`Bearer ${token}`);
    return {
        url,
        options:{
            method:'DELETE',
            headers
        }
    };
}

async function logTierListDeleteFailure(url,response){
    let body='';
    try{
        body=await response.clone().text();
    }catch(error){
        body='';
    }
    console.error('Delete tier list failed',{
        url,
        method:'DELETE',
        status:response.status,
        body
    });
}

function resolveTierDeleteErrorMessage(status,fallback){
    if(status===401) return 'Vui lòng đăng nhập để xóa Tier List.';
    if(status===403) return 'Bạn không có quyền xóa Tier List này.';
    if(status===404) return 'Tier List không tồn tại hoặc đã bị xóa.';
    if(status===405) return 'Lỗi kỹ thuật: endpoint xóa Tier List không chấp nhận DELETE.';
    return fallback||'Không xóa được Tier List.';
}

function openCommunityTierListDetail(id){
    if(!id) return;
    window.location.href=`/html/tier-list-detail.html?id=${encodeURIComponent(id)}`;
}

function getCommunityViewConfig(view=currentCommunityView){
    return COMMUNITY_VIEW_CONFIG[normalizeCommunityViewValue(view)]||COMMUNITY_VIEW_CONFIG[COMMUNITY_VIEW.HIGHLIGHT];
}

function getCommunityPageRoute(view){
    return COMMUNITY_PAGE_ROUTES[normalizeCommunityViewValue(view)]||COMMUNITY_PAGE_ROUTES[COMMUNITY_VIEW.HIGHLIGHT];
}

function syncCommunityNavigation(){
    const select=document.getElementById('community-nav-select');
    if(!select) return;
    const currentValue=document.getElementById('community-grid')?currentCommunityView:'';
    select.value=currentValue;
}

function initCommunityNavigation(){
    const select=document.getElementById('community-nav-select');
    if(!select||select.dataset.bound==='true'){
        syncCommunityNavigation();
        return;
    }
    select.dataset.bound='true';
    syncCommunityNavigation();
    select.addEventListener('change',event=>{
        const rawValue=String(event.target.value||'').trim();
        if(!rawValue) return;
        window.location.assign(getCommunityPageRoute(rawValue));
    });
}

function getCommunitySectionElements(section=COMMUNITY_SECTION.DEFAULT){
    const config=section||COMMUNITY_SECTION.DEFAULT;
    return {
        config,
        count:document.getElementById(config.countId),
        grid:document.getElementById(config.gridId),
        status:document.getElementById(config.statusId),
        empty:document.getElementById(config.emptyId)
    };
}

function resetCommunitySection(section){
    const elements=getCommunitySectionElements(section);
    if(elements.grid) elements.grid.innerHTML='';
    if(elements.count) elements.count.textContent='0';
    if(elements.empty) elements.empty.style.display='none';
    setCommunitySectionStatus(section,'');
    return elements;
}

function setCommunitySectionStatus(section,message,type='info'){
    const status=getCommunitySectionElements(section).status;
    if(!status) return;
    status.hidden=!message;
    status.textContent=message||'';
    status.classList.toggle('is-warning',type==='warning');
    status.classList.toggle('is-error',type==='error');
}

function setCommunitySectionEmptyState(section,message){
    const empty=getCommunitySectionElements(section).empty;
    if(!empty) return;
    const text=empty.querySelector('.community-empty-text');
    if(text) text.textContent=message||'Chưa có Community Tier List nào.';
}

function setCommunityStatus(message,type='info'){
    setCommunitySectionStatus(COMMUNITY_SECTION.DEFAULT,message,type);
}

function setCommunityEmptyState(message){
    setCommunitySectionEmptyState(COMMUNITY_SECTION.DEFAULT,message);
}

function syncOfficialCommunitySectionCopy(){
    if(!document.getElementById('tier-grid')) return;
    const subtitle=document.querySelector('#community-section .community-subtitle');
    if(subtitle){
        subtitle.textContent='Hi\u1ec3n th\u1ecb t\u1ed1i \u0111a 6 community tier list n\u1ed5i b\u1eadt hi\u1ec7n t\u1ea1i. M\u1ed7i card m\u1edf th\u00e0nh trang chi ti\u1ebft \u0111\u1ec3 xem rating, b\u00ecnh lu\u1eadn v\u00e0 t\u1ea3i \u1ea3nh.';
    }
}

async function setCommunityView(view){
    window.location.assign(getCommunityPageRoute(view));
}

async function loadCommunityTierLists(view=currentCommunityView){
    view=normalizeCommunityViewValue(view);
    const config=getCommunityViewConfig(view);
    if(view===COMMUNITY_VIEW.MINE&&!getTierListCurrentUser()){
        return {items:[],requiresAuth:true};
    }
    try{
        const response=await fetch(config.endpoint,{headers:{Accept:'application/json'},cache:'no-store'});
        if(!response.ok){
            if(view===COMMUNITY_VIEW.MINE&&response.status===401){
                return {items:[],requiresAuth:true};
            }
            throw new Error(await readApiError(response));
        }
        const payload=await response.json();
        const items=Array.isArray(payload)?payload.filter(tl=>tl&&!tl.isOfficial):[];
        return {items};
    }catch(error){
        console.error(`Cannot load community tier lists for view "${view}":`,error);
        return {items:[],error:config.errorText};
    }
}

async function loadSavedCommunityTierLists(){
    if(!getTierListCurrentUser()){
        return {items:[],requiresAuth:true};
    }
    try{
        const response=await fetch(COMMUNITY_SAVED_API,{headers:{Accept:'application/json'},cache:'no-store'});
        if(!response.ok){
            if(response.status===401){
                return {items:[],requiresAuth:true};
            }
            throw new Error(await readApiError(response));
        }
        const payload=await response.json();
        const items=Array.isArray(payload)?payload.filter(tl=>tl&&!tl.isOfficial):[];
        return {items};
    }catch(error){
        console.error('Cannot load saved tier lists:',error);
        return {items:[],error:'Không tải được Tier List đã lưu.'};
    }
}

function isMineCommunityPage(){
    return currentCommunityView===COMMUNITY_VIEW.MINE
        && !!document.getElementById(COMMUNITY_SECTION.MINE_CREATED.gridId)
        && !!document.getElementById(COMMUNITY_SECTION.MINE_SAVED.gridId);
}

function isTierListSavedByCurrentUser(tierList){
    return tierList?.isSavedByCurrentUser===true||tierList?.saved===true;
}

function buildCommunityTierSaveButtonHtml(tierList){
    if(!tierList||tierList.isOfficial) return '';
    const isSaved=isTierListSavedByCurrentUser(tierList);
    const label=isSaved?'Bỏ lưu':'Lưu';
    const extraClass=isSaved?' tier-saved-btn':'';
    return `<button type="button" class="tier-export-btn tier-secondary-btn tier-card-save-btn${extraClass}" onclick="toggleTierListSavedState(event,${tierList.id},this)">${label}</button>`;
}

function createCommunityTierCard(tl){
    communityTierListCache.set(String(tl.id),tl);
    const card=document.createElement('div');
    card.className='tier-card';
    card.tabIndex=0;
    card.setAttribute('role','link');
    card.setAttribute('aria-label',`Mo tier list ${tl.title||''}`);
    card.onclick=()=>openCommunityTierListDetail(tl.id);
    card.onkeydown=(event)=>{
        if(event.key==='Enter'||event.key===' '){
            event.preventDefault();
            openCommunityTierListDetail(tl.id);
        }
    };

    let thumbHtml='';
    const rows=tl.previewTiers||tl.contentData?.rows||tl.tiers||[];
    rows.forEach(tier=>{
        const tierKey=typeof getTierVisualKey==='function'?getTierVisualKey(tier.label):'';
        const tierClass=tierKey?` tier-${tierKey}`:'';
        const rowClass=tierKey?` tier-row-${tierKey}${tierClass}`:'';
        const labelClass=tierKey?` tier-label-${tierKey}${tierClass}`:'';
        const previewTierClass=tierKey?` tier-preview-${tierKey}`:'';
        const labelStyle=tierKey?'':` style="background:${tier.color||'#95a5a6'}"`;
        let heroMinis='';
        const heroesInRow=tier.heroes||((tier.cells||[]).flat());
        heroesInRow.slice(0,8).forEach(hero=>{
            const heroName=getHeroNameFromValue(hero);
            heroMinis+=`<img class="hero-avatar-chip tier-hero-mini" src="${escapeTierHtml(getHeroImgUrl(hero))}" alt="${escapeTierHtml(heroName)}" title="${escapeTierHtml(heroName)}" data-hero-name="${escapeTierHtml(heroName)}" loading="lazy" onerror="handleTierHeroImageError(this, this.dataset.heroName, '${TIER_HERO_FALLBACK_IMAGE}')">`;
        });
        thumbHtml+=`<div class="tier-row-preview tier-preview-row${rowClass}${previewTierClass}"><div class="tier-label-mini${labelClass}"${labelStyle}>${escapeTierHtml(tier.label)}</div><div class="tier-heroes-mini tier-preview-heroes${previewTierClass}">${heroMinis}</div></div>`;
    });

    let starsHtml=`<div class="star-rating-stars" data-id="${tl.id}">`;
    const averageRating=tl.averageUserRating??tl.communityRating??0;
    for(let i=1;i<=5;i++){
        const filled=i<=Math.round(averageRating)?'filled':'';
        starsHtml+=`<span class="star ${filled}" data-star="${i}" onclick="rateStar(event,${tl.id},${i})" onmouseenter="previewStars(this)" onmouseleave="clearPreview(this)">★</span>`;
    }
    starsHtml+='</div>';

    const adminRating=getAdminRatingValue(tl);
    const badgeHtml=adminRating
        ? `<div class="admin-endorsement"><span class="admin-badge-icon">AD</span> \u0110\u00e1nh gi\u00e1 c\u1ee7a Admin: ${formatRatingValue(adminRating)}/5</div>`
        : `<div class="admin-endorsement is-empty"><span class="admin-badge-icon">AD</span> Ch\u01b0a c\u00f3 \u0111\u00e1nh gi\u00e1 t\u1eeb Admin</div>`;
    const deleteHtml=canCurrentUserDeleteTierList(tl)
        ? `<button type="button" class="tier-export-btn tier-danger-btn tier-card-delete-btn" onclick="deleteCommunityTierListFromCard(event,${tl.id},this)">Xoa</button>`
        : '';
    const saveHtml=buildCommunityTierSaveButtonHtml(tl);
    const highlightBadge=tl.highlightBadge||tl.badgeLabel||'';
    const highlightBadgeHtml=highlightBadge
        ? `<div class="tier-card-highlight-badge">${escapeTierHtml(highlightBadge)}</div>`
        : '';

    card.innerHTML=`
        <div class="tier-card-thumbnail">${thumbHtml}</div>
        <div class="tier-card-body">
            ${highlightBadgeHtml}
            <div class="tier-card-title">${escapeTierHtml(tl.title)}</div>
            <div class="tier-card-author">
                <img src="${escapeTierHtml(tl.author?.avatar||'')}" alt="${escapeTierHtml(tl.author?.name||'ATG Member')}" referrerpolicy="no-referrer">
                <span class="tier-card-author-name">${escapeTierHtml(tl.author?.name||'ATG Member')}</span>
                <span class="tier-card-time">${escapeTierHtml(timeAgo(tl.createdAt))}</span>
            </div>
            <div class="star-rating">
                ${starsHtml}
                <span class="star-rating-avg">★ ${formatRatingValue(averageRating)||0}</span>
                <span class="star-rating-count">(${tl.userRatingCount??tl.totalRatings??0} ??nh gi?)</span>
            </div>
            ${badgeHtml}
            <div class="tier-card-actions">
                ${saveHtml}
                <button type="button" class="tier-export-btn tier-card-export-btn" onclick="exportCommunityTierListFromCard(event,${tl.id},this)">T?i ?nh</button>
                ${deleteHtml}
            </div>
        </div>`;
    return card;
}

function renderCommunitySectionCards(section,items,emptyText){
    const elements=resetCommunitySection(section);
    if(!elements.grid) return;
    if(!items.length){
        setCommunitySectionEmptyState(section,emptyText);
        if(elements.empty) elements.empty.style.display='block';
        return;
    }
    if(elements.count) elements.count.textContent=String(items.length);
    items.forEach(item=>elements.grid.appendChild(createCommunityTierCard(item)));
}

async function renderMineCommunityCards(requestId){
    const pageStatus=document.getElementById('mine-page-status');
    const createdConfig=getCommunityViewConfig(COMMUNITY_VIEW.MINE);
    resetCommunitySection(COMMUNITY_SECTION.MINE_CREATED);
    resetCommunitySection(COMMUNITY_SECTION.MINE_SAVED);
    if(pageStatus){
        pageStatus.hidden=true;
        pageStatus.textContent='';
        pageStatus.classList.remove('is-warning','is-error');
    }

    if(!getTierListCurrentUser()){
        if(pageStatus){
            pageStatus.hidden=false;
            pageStatus.textContent='Vui lòng đăng nhập để xem Tier List của bạn và danh sách đã lưu.';
            pageStatus.classList.add('is-warning');
        }
        return;
    }

    const [createdResult,savedResult]=await Promise.all([
        loadCommunityTierLists(COMMUNITY_VIEW.MINE),
        loadSavedCommunityTierLists()
    ]);
    if(requestId!==communityRenderRequestId) return;

    if(createdResult.requiresAuth||savedResult.requiresAuth){
        if(pageStatus){
            pageStatus.hidden=false;
            pageStatus.textContent='Vui lòng đăng nhập để xem Tier List của bạn và danh sách đã lưu.';
            pageStatus.classList.add('is-warning');
        }
        return;
    }

    if(createdResult.error){
        setCommunitySectionStatus(COMMUNITY_SECTION.MINE_CREATED,createdConfig.errorText,'error');
    }else{
        renderCommunitySectionCards(COMMUNITY_SECTION.MINE_CREATED,createdResult.items||[],'Bạn chưa tạo Tier List nào.');
        syncMineCommunityTierListQuota((createdResult.items||[]).length);
    }

    if(savedResult.error){
        setCommunitySectionStatus(COMMUNITY_SECTION.MINE_SAVED,savedResult.error,'error');
    }else{
        renderCommunitySectionCards(COMMUNITY_SECTION.MINE_SAVED,savedResult.items||[],'Bạn chưa lưu Tier List nào.');
    }
}

async function renderCommunityCards(){
    const requestId=++communityRenderRequestId;
    const grid=document.getElementById('community-grid');
    const countEl=document.getElementById('community-count');
    const emptyEl=document.getElementById('community-empty');
    if(!grid) return;
    currentCommunityView=normalizeCommunityViewValue(currentCommunityView);
    const config=getCommunityViewConfig();
    syncCommunityNavigation();
    communityTierListCache.clear();
    grid.innerHTML='';
    if(countEl) countEl.textContent='0';
    if(emptyEl) emptyEl.style.display='none';
    setCommunityStatus('');

    const {items:communityLists,error,requiresAuth}=await loadCommunityTierLists(currentCommunityView);
    if(requestId!==communityRenderRequestId) return;

    if(requiresAuth){
        setCommunityStatus(config.guestMessage,'warning');
        return;
    }
    if(error){
        setCommunityStatus(error,'error');
        return;
    }
    if(communityLists.length===0){
        setCommunityEmptyState(config.emptyText);
        if(emptyEl) emptyEl.style.display='block';
        return;
    }

    if(countEl) countEl.textContent=String(communityLists.length);
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
        const deleteHtml=canCurrentUserDeleteTierList(tl)
            ? `<button type="button" class="tier-export-btn tier-danger-btn tier-card-delete-btn" onclick="deleteCommunityTierListFromCard(event,${tl.id},this)">Xóa</button>`
            : '';
        const highlightBadge=tl.highlightBadge||tl.badgeLabel||'';
        const highlightBadgeHtml=highlightBadge
            ? `<div class="tier-card-highlight-badge">${escapeTierHtml(highlightBadge)}</div>`
            : '';
        card.innerHTML=`
            <div class="tier-card-thumbnail">${thumbHtml}</div>
            <div class="tier-card-body">
                ${highlightBadgeHtml}
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
                    ${deleteHtml}
                </div>
            </div>`;
        grid.appendChild(card);
    });
}

async function renderCommunityCards(){
    const requestId=++communityRenderRequestId;
    const defaultSection=getCommunitySectionElements(COMMUNITY_SECTION.DEFAULT);
    currentCommunityView=normalizeCommunityViewValue(currentCommunityView);
    syncCommunityNavigation();
    communityTierListCache.clear();

    if(isMineCommunityPage()){
        await renderMineCommunityCards(requestId);
        return;
    }

    if(!defaultSection.grid) return;
    const config=getCommunityViewConfig();
    resetCommunitySection(COMMUNITY_SECTION.DEFAULT);

    const {items:communityLists,error,requiresAuth}=await loadCommunityTierLists(currentCommunityView);
    if(requestId!==communityRenderRequestId) return;

    if(requiresAuth){
        setCommunityStatus(config.guestMessage,'warning');
        return;
    }
    if(error){
        setCommunityStatus(error,'error');
        return;
    }

    renderCommunitySectionCards(COMMUNITY_SECTION.DEFAULT,communityLists,config.emptyText);
}

async function toggleTierListSavedState(event,id,button){
    if(event){
        event.preventDefault();
        event.stopPropagation();
    }
    const tierList=communityTierListCache.get(String(id));
    if(!tierList||tierList.isOfficial) return;
    if(typeof requireLoginForPersistentAction==='function'&&!requireLoginForPersistentAction('luu Tier List')){
        return;
    }

    const isSaved=isTierListSavedByCurrentUser(tierList);
    const originalText=button?.textContent;
    if(button){
        button.disabled=true;
        button.textContent=isSaved?'Đang bỏ lưu...':'Đang lưu...';
    }

    try{
        const response=await fetch(`${OFFICIAL_TIER_LIST_API}/${id}/save`,{
            method:isSaved?'DELETE':'POST',
            headers:{Accept:'application/json'}
        });
        if(!response.ok) throw new Error(await readApiError(response));
        await response.json();
        await renderCommunityCards();
        showTierToast(isSaved?'Đã bỏ lưu Tier List.':'Đã lưu Tier List.');
    }catch(error){
        console.error('Cannot toggle saved tier list:',error);
        showTierToast(error.message||'Không cập nhật được trạng thái lưu Tier List.','error');
    }finally{
        if(button){
            button.disabled=false;
            button.textContent=originalText;
        }
    }
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

async function deleteCommunityTierListFromCard(event,id,button){
    if(event){
        event.preventDefault();
        event.stopPropagation();
    }
    const tierList=communityTierListCache.get(String(id));
    if(!canCurrentUserDeleteTierList(tierList)){
        showTierToast('Bạn không có quyền xóa Tier List này.','error');
        return;
    }
    const confirmed=window.confirm('Bạn có chắc muốn xóa Tier List này không? Hành động này không thể hoàn tác.');
    if(!confirmed) return;

    const originalText=button?.textContent;
    if(button){
        button.disabled=true;
        button.textContent='Đang xóa...';
    }
    try{
        const request=buildTierListDeleteRequest(id);
        const response=await fetch(request.url,request.options);
        if(!response.ok){
            await logTierListDeleteFailure(request.url,response);
            throw new Error(resolveTierDeleteErrorMessage(response.status,await readApiError(response)));
        }
        communityTierListCache.delete(String(id));
        await renderCommunityCards();
        showTierToast('Đã xóa Tier List.');
    }catch(error){
        console.error('Cannot delete community tier list:',error);
        showTierToast(error.message||'Không xóa được Tier List.','error');
    }finally{
        if(button){
            button.disabled=false;
            button.textContent=originalText;
        }
    }
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
    const u=getTierListCurrentUser();
    if(event){
        event.preventDefault();
        event.stopPropagation();
    }
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
async function openCreateModal(options={}){
    const u=true; // Keep the legacy guard truthy so guest users can open a local-only draft.
    if(!u){ alert('Vui lòng đăng nhập để tạo Tier List!'); return; }
    if(isTierListUserAuthenticated()&&await hasReachedCurrentUserCommunityTierListLimit({force:true,silent:true})){
        showTierToast(COMMUNITY_TIER_LIST_LIMIT_MESSAGE,'error');
        return;
    }
    const modal=document.getElementById('create-modal');
    modal?.classList.add('active');
    document.body.style.overflow='hidden';
    const grid=document.getElementById('modal-hero-grid');
    if(grid) grid.innerHTML='<div class="draft-warning" style="grid-column:1/-1">Đang tải danh sách tướng...</div>';
    await ensureTierHeroCatalogLoaded();
    initModalHeroes();
    bindGuestDraftSessionSync();
    syncGuestDraftNotice();
    hydrateModalDraftFromSession({
        force:options.restoreSessionDraft===true,
        showToast:options.showRestoreToast===true
    });
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
    if(heroes.length===0){
        grid.innerHTML='<div class="draft-warning" style="grid-column:1/-1">Chưa tải được danh sách tướng. Vui lòng thử lại.</div>';
        return;
    }
    heroes.forEach(hero=>{
        grid.appendChild(createHeroPoolButton(hero,{modal:true,pool:'modal'}));
    });
    renderClassFilterOptions('modal');
    renderTempHeroPool('modal');
    markHeroPoolSelection('modal');
    initModalDragOutZone();
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
                color:normalizeTierColorHex(labelCell.style.backgroundColor,'#95a5a6'),
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
        const source=pool==='modal'?'modal-tier':'tier';
        setHeroDragData(e,source,heroRef,el.id);
        setModalDragOutActive(source==='modal-tier');
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
    picker.title='Ch?n m?u tier';
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

function renderOfficialTierList(contentData,payload=officialTierListPayload){
    updateOfficialTierListHeading(payload);
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

async function readApiError(response){
    try{
        const payload=await response.json();
        return payload.error||payload.message||payload.detail||payload.title||response.statusText||'Request failed';
    }catch(e){
        return response.statusText||'Request failed';
    }
}

async function loadOfficialTierList(){
    try{
        const response=await fetch(`${OFFICIAL_TIER_LIST_API}/official`,{headers:{Accept:'application/json'},cache:'no-store'});
        if(!response.ok) throw new Error(await readApiError(response));

        const payload=await response.json();
        officialTierListPayload=normalizeOfficialTierListPayload(payload);
        renderOfficialTierList(officialTierListPayload.contentData,officialTierListPayload);
    }catch(error){
        console.error('Cannot load official tier list:',error);
    }
}

function exportOfficialTierList(button){
    const payload=normalizeOfficialTierListPayload({
        ...(officialTierListPayload||{}),
        id:officialTierListPayload?.id||'official',
        title:officialTierListPayload?.title||OFFICIAL_TIER_LIST_TITLE,
        author:officialTierListPayload?.author||{name:getOfficialTierListCreatorName(officialTierListPayload||{})},
        creatorName:getOfficialTierListCreatorName(officialTierListPayload||{}),
        createdAt:officialTierListPayload?.createdAt||officialTierListPayload?.updatedAt||new Date().toISOString(),
        contentData:serializeOfficialTierList()
    });
    exportTierListImage(payload,button);
}

function filterModalHeroes(){
    filterHeroPool('modal');
}

async function submitCommunityTierList(){
    const title=document.getElementById('modal-title')?.value||'Tier List m?i';
    const description=(document.getElementById('modal-note')?.value||'').trim();
    if(typeof requireLoginForPersistentAction==='function'&&!requireLoginForPersistentAction('dang Tier List')){
        return;
    }
    if(await hasReachedCurrentUserCommunityTierListLimit({force:true,silent:true})){
        showTierToast(COMMUNITY_TIER_LIST_LIMIT_MESSAGE,'error');
        return;
    }
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
        clearGuestDraftSession();
        await loadCurrentUserCommunityTierListCount({force:true,silent:true});
        renderCommunityCards();
        showTierToast(`Đã đăng "${title}" lên cộng đồng.`);
    }catch(error){
        console.error('Cannot submit community tier list:',error);
        if(error?.message===COMMUNITY_TIER_LIST_LIMIT_MESSAGE){
            await loadCurrentUserCommunityTierListCount({force:true,silent:true});
        }
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
        alert('Chỉ Admin mới được lưu Tier List chính.');
        return;
    }

    const button=findOfficialSaveButton();
    const originalText=button?.textContent;
    if(button){
        button.disabled=true;
        button.textContent='Đang cập nhật...';
    }

    try{
        const response=await fetch(ADMIN_OFFICIAL_TIER_LIST_API,{method:'POST'});

        if(!response.ok) throw new Error(await readApiError(response));

        const payload=normalizeOfficialTierListPayload(await response.json());
        officialTierListPayload=payload;
        renderOfficialTierList(payload.contentData,payload);
        alert('Đã cập nhật Tier List chính từ điểm hero. Người xem sẽ thấy dữ liệu mới ngay sau khi tải lại trang.');
    }catch(error){
        console.error('Cannot save official tier list:',error);
        alert(`Không cập nhật được Tier List chính: ${error.message}`);
    }finally{
        if(button){
            button.disabled=false;
            button.textContent=originalText;
        }
    }
};

window.setCommunityView=setCommunityView;
window.toggleTierListSavedState=toggleTierListSavedState;
window.saveGuestDraftToSession=saveGuestDraftToSession;
window.loadGuestDraftFromSession=loadGuestDraftFromSession;
window.clearGuestDraftSession=clearGuestDraftSession;

initApp();
