let currentRoleFilter='Tất cả', numCols=5, currentUserRole='Custom';

const OFFICIAL_TIER_LIST_API='/api/tier-lists';
const OFFICIAL_TIER_LIST_TITLE='Tier List Meta Hien Tai';
let officialTierListPayload=null;
const communityTierListCache=new Map();

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
    const up=document.getElementById('user-prompt');
    const bc=document.getElementById('btn-create-community');
    if(ac) ac.style.display=isAdmin?'flex':'none';
    if(hp) hp.style.display=isAdmin?'block':'none';
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

async function initApp(){
    const grid=document.getElementById('hero-grid');
    if(!grid) return;
    await loadHeroesFromApi();
    grid.innerHTML='';
    if(heroes.length===0){
        grid.innerHTML='<div class="draft-warning" style="grid-column:1/-1">Chua co du lieu tuong trong database. Hay chay sql/seed_heroes.sql roi tai lai trang.</div>';
    }
    heroes.forEach(hero=>{
        const btn=document.createElement('button');
        btn.className='hero-btn'; btn.innerText=hero.name;
        btn.id=`hero-${hero.name}`; btn.setAttribute('data-role',hero.role);
        btn.setAttribute('data-roles',(Array.isArray(hero.classes)?hero.classes:[]).join(','));
        btn.dataset.heroId=hero.id||'';
        btn.dataset.heroName=hero.name;
        btn.title=[hero.name].concat(Array.isArray(hero.classes)?hero.classes:[]).concat(hero.laneRoles||[]).filter(Boolean).join(' / ');
        btn.draggable=true;
        const imgUrl=getHeroImgUrl(hero.name);
        const img=new Image();
        img.onload=()=>{ btn.style.backgroundImage=`url("${imgUrl}")`; btn.style.color='transparent'; btn.style.textShadow='none'; };
        img.src=imgUrl;
        btn.ondragstart=(ev)=>dragStartFromList(ev,hero.name);
        btn.ondragend=dragEnd;
        grid.appendChild(btn);
    });
    document.querySelectorAll('#tier-grid .tier-heroes').forEach(c=>{
        c.ondragover=allowDrop; c.ondragenter=dragEnter; c.ondragleave=dragLeave; c.ondrop=dropOnTierList;
    });
    document.body.ondragover=(ev)=>ev.preventDefault();
    document.body.ondrop=(ev)=>{
        if(!ev.target.closest('.tier-heroes')){
            const source=ev.dataTransfer.getData("source");
            const heroName=ev.dataTransfer.getData("heroName");
            if(source==='tier'){
                const sid=ev.dataTransfer.getData("sourceId");
                const el=document.getElementById(sid); if(el) el.remove();
                const btn=document.getElementById(`hero-${heroName}`);
                if(btn) btn.classList.remove('selected');
                filterHeroes();
            }
        }
    };
    renderCommunityCards();
    loadOfficialTierList();
    setTimeout(applyTierListRoleUI,200);
}

function dragStartFromList(ev,name){
    const hero=getHeroFromValue(name);
    ev.dataTransfer.setData("source","list");
    ev.dataTransfer.setData("heroName",normalizeHeroName(hero?.name||name));
    if(hero?.id) ev.dataTransfer.setData("heroId",String(hero.id));
    ev.dataTransfer.effectAllowed="move";
    setTimeout(()=>ev.target.classList.add('dragging'),0);
}
function dragStartFromTier(ev,name){
    const hero=getHeroFromValue(name);
    ev.dataTransfer.setData("source","tier");
    ev.dataTransfer.setData("heroName",normalizeHeroName(hero?.name||name));
    if(hero?.id) ev.dataTransfer.setData("heroId",String(hero.id));
    ev.dataTransfer.setData("sourceId",ev.target.id);
    ev.dataTransfer.effectAllowed="move";
    setTimeout(()=>ev.target.classList.add('dragging'),0);
}
function dragEnd(ev){ ev.target.classList.remove('dragging'); document.querySelectorAll('.drag-over').forEach(el=>el.classList.remove('drag-over')); }
function allowDrop(ev){ ev.preventDefault(); ev.dataTransfer.dropEffect="move"; }
function dragEnter(ev){ ev.preventDefault(); if(ev.currentTarget.classList.contains('tier-heroes')) ev.currentTarget.classList.add('drag-over'); }
function dragLeave(ev){ ev.currentTarget.classList.remove('drag-over'); }

function dropOnTierList(ev){
    ev.preventDefault();
    const tc=ev.currentTarget; tc.classList.remove('drag-over');
    const heroName=normalizeHeroName(ev.dataTransfer.getData("heroName")), source=ev.dataTransfer.getData("source");
    if(!heroName) return;
    const existingId=`tier-item-${heroName}`;
    if(document.getElementById(existingId)){
        if(source==='tier') tc.appendChild(document.getElementById(existingId));
        return;
    }
    const el=document.createElement('div');
    el.className='tier-hero'; el.id=existingId; el.dataset.heroName=heroName; el.draggable=true;
    const heroId=getHeroIdFromValue(heroName);
    if(heroId) el.dataset.heroId=String(heroId);
    el.ondragstart=(e)=>dragStartFromTier(e,heroName); el.ondragend=dragEnd;
    const heroBtn=document.getElementById(`hero-${heroName}`);
    if(heroBtn){ el.style.backgroundImage=heroBtn.style.backgroundImage; heroBtn.classList.add('selected'); }
    tc.appendChild(el); filterHeroes();
}

function setRoleFilter(role){
    currentRoleFilter=role;
    document.querySelectorAll('#role-filters .role-btn').forEach(b=>b.classList.toggle('active',b.textContent.trim()===role));
    filterHeroes();
}

function filterHeroes(){
    const q=(document.getElementById('searchInput')?.value||'').toLowerCase();
    document.querySelectorAll('#hero-grid .hero-btn').forEach(btn=>{
        const sel=btn.classList.contains('selected'), r=btn.getAttribute('data-role');
        const mr=currentRoleFilter==='Tất cả'||r===currentRoleFilter;
        const ms=btn.innerText.toLowerCase().includes(q);
        btn.style.display=(!sel&&mr&&ms)?'flex':'none';
    });
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
    cells.forEach(c=>{ c.querySelectorAll('.tier-hero').forEach(h=>{ const n=h.id.replace('tier-item-',''); const b=document.getElementById(`hero-${n}`); if(b) b.classList.remove('selected'); }); c.remove(); });
    filterHeroes();
}

function deleteColumn(btn){
    const hc=btn.parentElement, grid=document.getElementById('tier-grid');
    const headers=Array.from(grid.querySelectorAll('.header-cell'));
    const ci=headers.indexOf(hc); if(ci<1) return;
    const cells=[hc];
    grid.querySelectorAll('.tier-label').forEach(label=>{ let cell=label; for(let i=0;i<ci;i++){ if(cell) cell=cell.nextElementSibling; } if(cell&&cell.classList.contains('tier-content')) cells.push(cell); });
    cells.forEach(c=>{ c.querySelectorAll('.tier-hero').forEach(h=>{ const n=h.id.replace('tier-item-',''); const b=document.getElementById(`hero-${n}`); if(b) b.classList.remove('selected'); }); c.remove(); });
    numCols--; updateGridTemplate(); filterHeroes();
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
                heroMinis+=`<img class="hero-avatar-chip tier-hero-mini" src="${escapeTierHtml(getHeroImgUrl(h))}" alt="${escapeTierHtml(heroName)}" title="${escapeTierHtml(heroName)}" loading="lazy" onerror="this.onerror=null;this.src='/images/ui/default.png'">`;
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
        const btn=document.createElement('button');
        btn.className='hero-btn'; btn.innerText=hero.name;
        btn.id=`modal-hero-${hero.name}`; btn.setAttribute('data-role',hero.role);
        btn.dataset.heroId=hero.id||'';
        btn.dataset.heroName=hero.name;
        btn.draggable=true;
        btn.style.fontSize='0.65rem';
        const imgUrl=getHeroImgUrl(hero.name);
        const img=new Image();
        img.onload=()=>{ btn.style.backgroundImage=`url("${imgUrl}")`; btn.style.color='transparent'; };
        img.src=imgUrl;
        btn.ondragstart=(ev)=>{ ev.dataTransfer.setData("source","modal-list"); ev.dataTransfer.setData("heroName",hero.name); if(hero.id) ev.dataTransfer.setData("heroId",String(hero.id)); ev.dataTransfer.effectAllowed="move"; };
        btn.ondragend=dragEnd;
        grid.appendChild(btn);
    });
    document.querySelectorAll('#modal-tier-grid .modal-drop').forEach(c=>{
        c.ondragover=allowDrop; c.ondragenter=dragEnter; c.ondragleave=dragLeave;
        c.ondrop=(ev)=>{
            ev.preventDefault(); ev.currentTarget.classList.remove('drag-over');
            const name=normalizeHeroName(ev.dataTransfer.getData("heroName"));
            if(!name) return;
            const eid=`modal-tier-${name}`;
            if(document.getElementById(eid)){ ev.currentTarget.appendChild(document.getElementById(eid)); return; }
            const el=document.createElement('div'); el.className='tier-hero'; el.id=eid;
            const heroId=getHeroIdFromValue(name);
            if(heroId) el.dataset.heroId=String(heroId);
            el.dataset.heroName=name;
            el.style.width='30px'; el.style.height='30px';
            el.draggable=true;
            el.ondragstart=(e)=>{ e.dataTransfer.setData("source","modal-tier"); e.dataTransfer.setData("heroName",name); if(heroId) e.dataTransfer.setData("heroId",String(heroId)); e.dataTransfer.setData("sourceId",eid); e.dataTransfer.effectAllowed="move"; };
            const mbtn=document.getElementById(`modal-hero-${name}`);
            if(mbtn){ el.style.backgroundImage=mbtn.style.backgroundImage; mbtn.style.display='none'; }
            ev.currentTarget.appendChild(el);
        };
    });
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
    return heroId?{heroId:Number(heroId),name:heroName}:{name:heroName};
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

function createOfficialTierHero(heroValue){
    const heroId=getHeroIdFromValue(heroValue);
    const heroName=getHeroNameFromValue(heroValue);
    const el=document.createElement('div');
    el.className='tier-hero';
    el.id=`tier-item-${heroName}`;
    el.dataset.heroName=heroName;
    if(heroId) el.dataset.heroId=String(heroId);
    el.draggable=true;
    el.ondragstart=(e)=>dragStartFromTier(e,heroName);
    el.ondragend=dragEnd;

    const heroBtn=document.getElementById(`hero-${heroName}`);
    if(heroBtn&&heroBtn.style.backgroundImage){
        el.style.backgroundImage=heroBtn.style.backgroundImage;
    }else{
        el.style.backgroundImage=`url("${getHeroImgUrl(heroName)}")`;
    }
    return el;
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
}

function markOfficialHeroSelection(){
    clearOfficialHeroSelection();
    document.querySelectorAll('#tier-grid .tier-hero').forEach(heroEl=>{
        const heroName=getTierHeroName(heroEl);
        const heroBtn=document.querySelector(`#hero-grid .hero-btn[data-hero-id="${heroEl.dataset.heroId||''}"]`)||document.getElementById(`hero-${heroName}`);
        if(heroBtn) heroBtn.classList.add('selected');
    });
}

function renderOfficialTierList(contentData){
    const data=parseTierListContentData(contentData);
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
        return payload.error||payload.message||response.statusText||'Request failed';
    }catch(e){
        return response.statusText||'Request failed';
    }
}

async function loadOfficialTierList(){
    try{
        const response=await fetch(`${OFFICIAL_TIER_LIST_API}/official`,{headers:{Accept:'application/json'}});
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
    const q=(document.getElementById('modal-search')?.value||'').toLowerCase();
    document.querySelectorAll('#modal-hero-grid .hero-btn').forEach(btn=>{
        btn.style.display=btn.innerText.toLowerCase().includes(q)?'flex':'none';
    });
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

initApp();
