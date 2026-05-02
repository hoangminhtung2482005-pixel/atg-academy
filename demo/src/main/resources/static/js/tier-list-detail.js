const TIER_DETAIL_API='/api/tier-lists';
const DETAIL_HERO_FALLBACK_IMAGE='/images/ui/logo.png';

let tierDetailId=null;
let tierDetailData=null;
let tierDetailSummary=null;
let tierDetailComments=[];

function escapeDetailHtml(value){
    return String(value||'')
        .replace(/&/g,'&amp;')
        .replace(/</g,'&lt;')
        .replace(/>/g,'&gt;')
        .replace(/"/g,'&quot;')
        .replace(/'/g,'&#039;');
}

function formatDetailRating(value){
    const num=Number(value);
    if(!Number.isFinite(num)||num<=0) return '0';
    return Number.isInteger(num)?String(num):num.toFixed(1).replace(/\.0$/,'');
}

function formatDetailDate(value){
    if(!value) return '';
    const date=new Date(value);
    if(Number.isNaN(date.getTime())) return '';
    return new Intl.DateTimeFormat('vi-VN',{
        day:'2-digit',
        month:'2-digit',
        year:'numeric',
        hour:'2-digit',
        minute:'2-digit'
    }).format(date);
}

async function readDetailApiError(response){
    try{
        const payload=await response.json();
        return payload.error||payload.message||response.statusText||'Request failed';
    }catch(error){
        return response.statusText||'Request failed';
    }
}

function showDetailToast(message,type='success'){
    let toast=document.getElementById('tier-detail-toast');
    if(!toast){
        toast=document.createElement('div');
        toast.id='tier-detail-toast';
        toast.className='guide-toast tier-toast';
        document.body.appendChild(toast);
    }
    toast.textContent=message;
    toast.classList.toggle('error',type==='error');
    toast.classList.add('is-visible');
    clearTimeout(showDetailToast._timer);
    showDetailToast._timer=setTimeout(()=>toast.classList.remove('is-visible'),2800);
}

function setDetailState(message,isError=false){
    const state=document.getElementById('tier-detail-state');
    const shell=document.getElementById('tier-detail-shell');
    if(state){
        state.textContent=message;
        state.classList.toggle('error',isError);
        state.hidden=!message;
    }
    if(shell) shell.hidden=!!message;
}

function getDetailUser(){
    if(typeof getAuthUser==='function') return getAuthUser();
    try{
        const raw=localStorage.getItem('aov_user');
        return raw?JSON.parse(raw):null;
    }catch(error){
        return null;
    }
}

function isDetailAdmin(){
    const user=getDetailUser();
    return user?.role==='Admin';
}

async function loadTierDetail(){
    const response=await fetch(`${TIER_DETAIL_API}/${tierDetailId}`,{headers:{Accept:'application/json'},cache:'no-store'});
    if(!response.ok) throw new Error(await readDetailApiError(response));
    tierDetailData=await response.json();
}

async function loadTierSummary(){
    const response=await fetch(`${TIER_DETAIL_API}/${tierDetailId}/ratings/summary`,{headers:{Accept:'application/json'},cache:'no-store'});
    if(!response.ok) throw new Error(await readDetailApiError(response));
    tierDetailSummary=await response.json();
}

async function loadTierComments(){
    const response=await fetch(`${TIER_DETAIL_API}/${tierDetailId}/comments`,{headers:{Accept:'application/json'},cache:'no-store'});
    if(!response.ok) throw new Error(await readDetailApiError(response));
    const payload=await response.json();
    tierDetailComments=Array.isArray(payload)?payload:[];
}

function renderTierHeader(){
    document.title=`${tierDetailData.title||'Tier List'} - AoV Tactics & Guides`;
    document.getElementById('tier-detail-title').textContent=tierDetailData.title||'Tier List';
    document.getElementById('tier-detail-author').textContent=tierDetailData.author?.name||'ATG Member';
    document.getElementById('tier-detail-created').textContent=`Tạo: ${formatDetailDate(tierDetailData.createdAt)}`;
    document.getElementById('tier-detail-updated').textContent=`Cập nhật: ${formatDetailDate(tierDetailData.updatedAt)}`;

    const avatar=document.getElementById('tier-detail-author-avatar');
    avatar.src=tierDetailData.author?.avatar||'/images/ui/logo.png';
    avatar.alt=tierDetailData.author?.name||'ATG Member';

    const note=document.getElementById('tier-detail-note');
    const noteText=tierDetailData.description||tierDetailData.note||'';
    note.textContent=noteText;
    note.hidden=!noteText;
}

function renderRatingSummary(){
    const summary=tierDetailSummary||tierDetailData||{};
    const average=summary.averageUserRating??summary.average??tierDetailData?.averageUserRating??0;
    const count=summary.userRatingCount??summary.count??tierDetailData?.userRatingCount??0;
    const userRating=summary.userRating??tierDetailData?.currentUserRating??0;

    document.getElementById('tier-detail-average').textContent=`${formatDetailRating(average)}/5`;
    document.getElementById('tier-detail-rating-count').textContent=`${count} đánh giá`;

    const stars=document.getElementById('tier-detail-stars');
    stars.innerHTML='';
    for(let i=1;i<=5;i++){
        const button=document.createElement('button');
        button.type='button';
        button.className=i<=Number(userRating)?'active':'';
        button.textContent='★';
        button.setAttribute('aria-label',`${i} sao`);
        button.onclick=()=>saveTierUserRating(i);
        stars.appendChild(button);
    }

    const message=document.getElementById('tier-detail-rating-message');
    message.textContent=getDetailUser()?`Điểm của bạn: ${userRating?`${userRating}/5`:'chưa đánh giá'}`:'Đăng nhập để đánh giá.';
}

function renderAdminRating(){
    const detail=(tierDetailSummary?.adminRatingDetail||tierDetailData?.adminRatingDetail)||null;
    const ratingValue=detail?.ratingValue??tierDetailSummary?.adminRating??tierDetailData?.adminRating??null;
    const valueEl=document.getElementById('tier-detail-admin-value');
    const noteEl=document.getElementById('tier-detail-admin-note');
    const form=document.getElementById('tier-detail-admin-form');

    if(ratingValue){
        valueEl.textContent=`Đánh giá của Admin: ${formatDetailRating(ratingValue)}/5`;
    }else{
        valueEl.textContent='Chưa có đánh giá từ Admin';
    }

    noteEl.textContent=detail?.note||'';
    noteEl.hidden=!detail?.note;

    form.hidden=!isDetailAdmin();
    if(isDetailAdmin()){
        document.getElementById('tier-admin-rating-input').value=ratingValue||'';
        document.getElementById('tier-admin-note-input').value=detail?.note||'';
    }
}

function getDetailHeroImage(hero){
    if(hero&&typeof hero==='object'&&hero.avatarUrl) return hero.avatarUrl;
    const heroName=getHeroNameFromValue(hero);
    return heroName?getHeroImgUrl(heroName):DETAIL_HERO_FALLBACK_IMAGE;
}

function renderHeroCell(hero){
    const name=getHeroNameFromValue(hero);
    return `
        <div class="hero-avatar-chip detail-hero-chip" title="${escapeDetailHtml(name)}" aria-label="${escapeDetailHtml(name)}">
            <img class="tier-detail-hero-avatar" src="${escapeDetailHtml(getDetailHeroImage(hero))}" alt="${escapeDetailHtml(name)}" title="${escapeDetailHtml(name)}" loading="lazy" onerror="this.onerror=null;this.src='${DETAIL_HERO_FALLBACK_IMAGE}'">
        </div>
    `;
}

function normalizeTierRows(contentData){
    if(!contentData) return {columns:[],rows:[]};
    if(Array.isArray(contentData.rows)){
        return {
            columns:Array.isArray(contentData.columns)?contentData.columns:[],
            rows:contentData.rows
        };
    }
    if(Array.isArray(contentData.tiers)){
        return {
            columns:[{label:'Tướng'}],
            rows:contentData.tiers.map(tier=>({
                label:tier.label,
                color:tier.color,
                cells:[tier.heroes||[]]
            }))
        };
    }
    return {columns:[],rows:[]};
}

function renderTierBoard(){
    const board=document.getElementById('tier-detail-board');
    const normalized=normalizeTierRows(tierDetailData.contentData);
    let columns=normalized.columns;
    const rows=normalized.rows;
    if(!columns.length&&rows.length){
        const maxCells=Math.max(...rows.map(row=>Array.isArray(row.cells)?row.cells.length:0),1);
        columns=Array.from({length:maxCells},(_,index)=>({label:`Cột ${index+1}`}));
    }

    if(!rows.length){
        board.innerHTML='<div class="tier-detail-empty">Tier List chưa có dữ liệu.</div>';
        return;
    }

    board.style.gridTemplateColumns=`90px repeat(${Math.max(columns.length,1)},minmax(170px,1fr))`;
    let html='<div class="detail-tier-cell detail-tier-header empty"></div>';
    columns.forEach(column=>{
        const icon=column.icon?`<img src="${escapeDetailHtml(column.icon)}" alt="${escapeDetailHtml(column.alt||column.label||'')}" class="detail-role-icon">`:'';
        html+=`<div class="detail-tier-cell detail-tier-header">${icon}<span>${escapeDetailHtml(column.label||'')}</span></div>`;
    });

    rows.forEach(row=>{
        const tierKey=typeof getTierVisualKey==='function'?getTierVisualKey(row.label):'';
        const tierClass=tierKey?` tier-${tierKey}`:'';
        const rowClass=tierKey?` tier-detail-row tier-row-${tierKey}${tierClass}`:'';
        const labelClass=tierKey?` tier-detail-label tier-label-${tierKey}${tierClass}`:'';
        const labelStyle=tierKey?'':` style="background:${escapeDetailHtml(row.color||'#95a5a6')}"`;
        html+=`<div class="detail-tier-cell detail-tier-label${rowClass}${labelClass}"${labelStyle}>${escapeDetailHtml(row.label||'')}</div>`;
        for(let i=0;i<columns.length;i++){
            const heroes=Array.isArray(row.cells?.[i])?row.cells[i]:[];
            html+=`<div class="detail-tier-cell detail-tier-heroes tier-detail-content${rowClass}">${heroes.length?heroes.map(renderHeroCell).join(''):'<span class="detail-tier-placeholder">-</span>'}</div>`;
        }
    });
    board.innerHTML=html;
}

function renderCommentComposer(){
    const composer=document.getElementById('tier-detail-comment-composer');
    if(!getDetailUser()){
        composer.innerHTML='<p class="comment-login-message">Đăng nhập để nhận xét.</p>';
        return;
    }
    composer.innerHTML=`
        <textarea id="tier-comment-input" rows="4" maxlength="2000" placeholder="Viết nhận xét của bạn..."></textarea>
        <div class="comment-composer-actions">
            <span id="tier-comment-feedback"></span>
            <button type="button" id="tier-comment-submit">Gửi bình luận</button>
        </div>
    `;
    document.getElementById('tier-comment-submit').onclick=submitTierComment;
}

function renderComments(){
    document.getElementById('tier-detail-comment-count').textContent=String(tierDetailComments.length);
    const list=document.getElementById('tier-detail-comments-list');
    if(!tierDetailComments.length){
        list.innerHTML='<div class="comment-empty">Chưa có bình luận.</div>';
        return;
    }
    list.innerHTML=tierDetailComments.map(comment=>{
        const user=comment.user||{};
        return `
            <article class="comment-item">
                <img src="${escapeDetailHtml(user.avatar||'/images/ui/logo.png')}" alt="${escapeDetailHtml(user.name||'ATG Member')}" referrerpolicy="no-referrer">
                <div>
                    <div class="comment-item-head">
                        <strong>${escapeDetailHtml(user.name||'ATG Member')}</strong>
                        <span>${escapeDetailHtml(formatDetailDate(comment.createdAt))}</span>
                    </div>
                    <p>${escapeDetailHtml(comment.content)}</p>
                </div>
            </article>
        `;
    }).join('');
}

function renderTierDetail(){
    renderTierHeader();
    renderRatingSummary();
    renderAdminRating();
    renderTierBoard();
    renderCommentComposer();
    renderComments();
}

function exportCurrentTierDetail(button){
    if(!tierDetailData) return;
    const summary=tierDetailSummary||{};
    exportTierListImage({
        ...tierDetailData,
        averageUserRating:summary.averageUserRating??summary.average??tierDetailData.averageUserRating,
        userRatingCount:summary.userRatingCount??summary.count??tierDetailData.userRatingCount,
        adminRating:summary.adminRating??tierDetailData.adminRating,
        adminRatingDetail:summary.adminRatingDetail??tierDetailData.adminRatingDetail
    },button);
}

async function saveTierUserRating(value){
    if(!getDetailUser()){
        showDetailToast('Vui lòng đăng nhập để đánh giá.','error');
        return;
    }
    const stars=document.getElementById('tier-detail-stars');
    stars.classList.add('is-saving');
    try{
        const response=await fetch(`${TIER_DETAIL_API}/${tierDetailId}/ratings`,{
            method:'POST',
            headers:{'Content-Type':'application/json'},
            body:JSON.stringify({ratingValue:value})
        });
        if(!response.ok) throw new Error(await readDetailApiError(response));
        tierDetailSummary=await response.json();
        renderRatingSummary();
        showDetailToast('Đã lưu đánh giá.');
    }catch(error){
        showDetailToast(`Không lưu được đánh giá: ${error.message}`,'error');
    }finally{
        stars.classList.remove('is-saving');
    }
}

async function saveTierAdminRating(){
    const button=document.getElementById('tier-admin-save-btn');
    const ratingValue=Number(document.getElementById('tier-admin-rating-input').value);
    const note=document.getElementById('tier-admin-note-input').value.trim();
    button.disabled=true;
    try{
        const response=await fetch(`/api/admin/tier-lists/${tierDetailId}/admin-rating`,{
            method:'PUT',
            headers:{'Content-Type':'application/json'},
            body:JSON.stringify({ratingValue,note})
        });
        if(!response.ok) throw new Error(await readDetailApiError(response));
        const payload=await response.json();
        tierDetailSummary={
            ...(tierDetailSummary||{}),
            adminRating:payload.adminRating,
            adminRatingDetail:payload.adminRatingDetail
        };
        tierDetailData={
            ...(tierDetailData||{}),
            adminRating:payload.adminRating,
            adminRatingDetail:payload.adminRatingDetail
        };
        renderAdminRating();
        showDetailToast('Đã lưu đánh giá Admin.');
    }catch(error){
        showDetailToast(`Không lưu được đánh giá Admin: ${error.message}`,'error');
    }finally{
        button.disabled=false;
    }
}

async function submitTierComment(){
    const input=document.getElementById('tier-comment-input');
    const button=document.getElementById('tier-comment-submit');
    const feedback=document.getElementById('tier-comment-feedback');
    const content=input.value.trim();
    if(!content){
        feedback.textContent='Nhập nội dung bình luận.';
        return;
    }
    button.disabled=true;
    feedback.textContent='Đang gửi...';
    try{
        const response=await fetch(`${TIER_DETAIL_API}/${tierDetailId}/comments`,{
            method:'POST',
            headers:{'Content-Type':'application/json'},
            body:JSON.stringify({content})
        });
        if(!response.ok) throw new Error(await readDetailApiError(response));
        input.value='';
        await loadTierComments();
        renderComments();
        feedback.textContent='';
        showDetailToast('Đã gửi bình luận.');
    }catch(error){
        feedback.textContent='';
        showDetailToast(`Không gửi được bình luận: ${error.message}`,'error');
    }finally{
        button.disabled=false;
    }
}

async function refreshAuthSensitiveBlocks(){
    if(!tierDetailId) return;
    try{
        await loadTierSummary();
        renderRatingSummary();
        renderAdminRating();
        renderCommentComposer();
    }catch(error){
        console.error('Cannot refresh tier detail auth blocks:',error);
    }
}

async function initTierDetail(){
    tierDetailId=new URLSearchParams(window.location.search).get('id');
    if(!tierDetailId){
        setDetailState('Thiếu mã Tier List.',true);
        return;
    }
    try{
        setDetailState('Đang tải Tier List...');
        await loadHeroesFromApi();
        await Promise.all([loadTierDetail(),loadTierSummary(),loadTierComments()]);
        setDetailState('');
        renderTierDetail();
        document.getElementById('tier-admin-save-btn').onclick=saveTierAdminRating;
    }catch(error){
        console.error('Cannot load tier detail:',error);
        setDetailState(`Không tải được Tier List: ${error.message}`,true);
    }
}

document.addEventListener('authChanged',refreshAuthSensitiveBlocks);

if(document.readyState==='loading'){
    document.addEventListener('DOMContentLoaded',initTierDetail);
}else{
    initTierDetail();
}
