const TIER_DETAIL_API='/api/tier-lists';
const DETAIL_HERO_FALLBACK_IMAGE='/images/ui/default.png';

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
        return payload.error||payload.message||payload.detail||payload.title||response.statusText||'Request failed';
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

window.showDetailToast=showDetailToast;

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

function canCurrentUserDeleteTierList(data=tierDetailData){
    if(!data||data.isOfficial) return false;
    if(data.canDelete===true) return true;
    const user=getDetailUser();
    if(!user) return false;
    if(user.role==='Admin') return true;
    const author=data.author||{};
    return (user.email&&author.email&&String(user.email).toLowerCase()===String(author.email).toLowerCase())
        || (user.id&&author.id&&String(user.id)===String(author.id));
}

function renderDeleteControls(){
    const button=document.getElementById('tier-detail-delete-btn');
    if(!button) return;
    button.hidden=!canCurrentUserDeleteTierList();
}

function isDetailTierListSaved(){
    return tierDetailData?.isSavedByCurrentUser===true||tierDetailData?.saved===true;
}

function renderDetailSaveButton(){
    const button=document.getElementById('tier-detail-save-btn');
    if(!button) return;
    if(!tierDetailData||tierDetailData.isOfficial){
        button.hidden=true;
        return;
    }
    const isSaved=isDetailTierListSaved();
    button.hidden=false;
    button.textContent=isSaved?'Bỏ lưu':'Lưu';
    button.classList.toggle('tier-saved-btn',isSaved);
}

function tierDeleteErrorMessage(status, fallback){
    if(status===401) return 'Vui lòng đăng nhập để xóa Tier List.';
    if(status===403) return 'Bạn không có quyền xóa Tier List này.';
    if(status===404) return 'Tier List không tồn tại hoặc đã bị xóa.';
    return fallback||'Không xóa được Tier List.';
}

function buildTierListDeleteRequest(id){
    const url=`${TIER_DETAIL_API}/${encodeURIComponent(String(id))}`;
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

function resolveTierDeleteErrorMessage(status, fallback){
    if(status===401) return 'Vui lòng đăng nhập để xóa Tier List.';
    if(status===403) return 'Bạn không có quyền xóa Tier List này.';
    if(status===404) return 'Tier List không tồn tại hoặc đã bị xóa.';
    if(status===405) return 'Lỗi kỹ thuật: endpoint xóa Tier List không chấp nhận DELETE.';
    return fallback||'Không xóa được Tier List.';
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

function getDetailAdminRatingDetail(){
    return (tierDetailSummary?.adminRatingDetail||tierDetailData?.adminRatingDetail)||null;
}

function getDetailAdminRatingValue(){
    const detail=getDetailAdminRatingDetail();
    return detail?.ratingValue??tierDetailSummary?.adminRating??tierDetailData?.adminRating??null;
}

function renderRatingSummary(){
    const summary=tierDetailSummary||tierDetailData||{};
    const average=summary.averageUserRating??summary.average??tierDetailData?.averageUserRating??0;
    const count=summary.userRatingCount??summary.count??tierDetailData?.userRatingCount??0;
    const userRating=summary.userRating??tierDetailData?.currentUserRating??0;
    const isAdmin=isDetailAdmin();
    const adminDetail=getDetailAdminRatingDetail();
    const adminRating=getDetailAdminRatingValue();
    const activeRating=isAdmin?adminRating:userRating;
    const summaryLabel=document.querySelector('.rating-summary span');
    const averageEl=document.getElementById('tier-detail-average');
    const countEl=document.getElementById('tier-detail-rating-count');
    const message=document.getElementById('tier-detail-rating-message');

    if(isAdmin){
        if(summaryLabel) summaryLabel.textContent='Điểm Admin';
        averageEl.textContent=adminRating?`${formatDetailRating(adminRating)}/5`:'Chưa có';
        countEl.textContent=`Cộng đồng: ${formatDetailRating(average)}/5 · ${count} đánh giá`;
    }else{
        if(summaryLabel) summaryLabel.textContent='Đánh giá cộng đồng';
        averageEl.textContent=`${formatDetailRating(average)}/5`;
        countEl.textContent=`${count} đánh giá`;
    }

    const stars=document.getElementById('tier-detail-stars');
    stars.innerHTML='';
    for(let i=1;i<=5;i++){
        const button=document.createElement('button');
        button.type='button';
        button.className=i<=Number(activeRating)?'active':'';
        button.textContent='★';
        button.setAttribute('aria-label',isAdmin?`Lưu điểm Admin ${i} sao`:`${i} sao`);
        button.onclick=()=>saveTierRating(i);
        stars.appendChild(button);
    }

    if(!getDetailUser()){
        message.textContent='Đăng nhập để đánh giá.';
        return;
    }

    if(isAdmin){
        const adminText=adminRating?`Điểm Admin hiện tại: ${formatDetailRating(adminRating)}/5`:'Chưa có điểm Admin';
        const noteText=adminDetail?.note?` · Ghi chú: ${adminDetail.note}`:'';
        message.textContent=`${adminText}. Bấm sao để lưu điểm Admin.${noteText}`;
        return;
    }

    message.textContent=`Điểm của bạn: ${userRating?`${userRating}/5`:'chưa đánh giá'}`;
}

function getDetailHeroImage(hero){
    if(typeof resolveHeroImageUrl==='function') return resolveHeroImageUrl(hero);
    if(hero&&typeof hero==='object'&&hero.avatarUrl) return hero.avatarUrl;
    const heroName=getHeroNameFromValue(hero);
    return heroName?getHeroImgUrl(heroName):DETAIL_HERO_FALLBACK_IMAGE;
}

function renderHeroCell(hero){
    const name=getHeroNameFromValue(hero);
    return `
        <div class="hero-avatar-chip detail-hero-chip" title="${escapeDetailHtml(name)}" aria-label="${escapeDetailHtml(name)}">
            <img class="tier-detail-hero-avatar" src="${escapeDetailHtml(getDetailHeroImage(hero))}" alt="${escapeDetailHtml(name)}" title="${escapeDetailHtml(name)}" data-hero-name="${escapeDetailHtml(name)}" loading="lazy" onerror="handleTierHeroImageError(this, this.dataset.heroName, '${DETAIL_HERO_FALLBACK_IMAGE}')">
        </div>
    `;
}

function normalizeTierRows(contentData){
    if(!contentData) return {columns:[],rows:[]};
    const data=typeof normalizeTierRoleColumnOrder==='function'?normalizeTierRoleColumnOrder(contentData):contentData;
    if(Array.isArray(data.rows)){
        return {
            columns:Array.isArray(data.columns)?data.columns:[],
            rows:data.rows
        };
    }
    if(Array.isArray(data.tiers)){
        return {
            columns:[{label:'Tướng'}],
            rows:data.tiers.map(tier=>({
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
    renderTierBoard();
    renderDetailSaveButton();
    renderDeleteControls();
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

function saveTierRating(value){
    return isDetailAdmin()?saveTierAdminRating(value):saveTierUserRating(value);
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

async function saveTierAdminRating(ratingValue){
    if(!isDetailAdmin()){
        showDetailToast('Chỉ Admin mới có thể lưu điểm Admin.','error');
        return;
    }

    const stars=document.getElementById('tier-detail-stars');
    const note=getDetailAdminRatingDetail()?.note||'';
    stars.classList.add('is-saving');
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
            average:payload.average,
            count:payload.count,
            totalRatings:payload.totalRatings,
            averageUserRating:payload.averageUserRating,
            userRatingCount:payload.userRatingCount,
            userOnlyAverageRating:payload.userOnlyAverageRating,
            userOnlyRatingCount:payload.userOnlyRatingCount,
            adminRatingCount:payload.adminRatingCount,
            adminRating:payload.adminRating,
            adminRatingDetail:payload.adminRatingDetail
        };
        tierDetailData={
            ...(tierDetailData||{}),
            communityRating:payload.averageUserRating??payload.average??tierDetailData?.communityRating,
            totalRatings:payload.totalRatings??payload.count??tierDetailData?.totalRatings,
            averageUserRating:payload.averageUserRating??payload.average??tierDetailData?.averageUserRating,
            userRatingCount:payload.userRatingCount??payload.count??tierDetailData?.userRatingCount,
            userOnlyAverageRating:payload.userOnlyAverageRating??tierDetailData?.userOnlyAverageRating,
            userOnlyRatingCount:payload.userOnlyRatingCount??tierDetailData?.userOnlyRatingCount,
            adminRatingCount:payload.adminRatingCount??tierDetailData?.adminRatingCount,
            adminRating:payload.adminRating,
            adminRatingDetail:payload.adminRatingDetail
        };
        renderRatingSummary();
        showDetailToast('Đã lưu điểm Admin.');
    }catch(error){
        showDetailToast(`Không lưu được điểm Admin: ${error.message}`,'error');
    }finally{
        stars.classList.remove('is-saving');
    }
}

async function toggleDetailSavedState(){
    if(!tierDetailData||!tierDetailId||tierDetailData.isOfficial) return;
    if(typeof requireLoginForPersistentAction==='function'&&!requireLoginForPersistentAction('luu Tier List')){
        return;
    }

    const button=document.getElementById('tier-detail-save-btn');
    const isSaved=isDetailTierListSaved();
    const originalText=button?.textContent;
    if(button){
        button.disabled=true;
        button.textContent=isSaved?'Đang bỏ lưu...':'Đang lưu...';
    }

    try{
        const response=await fetch(`${TIER_DETAIL_API}/${tierDetailId}/save`,{
            method:isSaved?'DELETE':'POST',
            headers:{Accept:'application/json'}
        });
        if(!response.ok) throw new Error(await readDetailApiError(response));
        const payload=await response.json();
        if(payload?.item&&typeof payload.item==='object'){
            tierDetailData={...tierDetailData,...payload.item};
        }else{
            tierDetailData={
                ...tierDetailData,
                saved:payload?.saved===true,
                isSavedByCurrentUser:payload?.saved===true,
                savedAt:payload?.savedAt??null
            };
        }
        renderDetailSaveButton();
        showDetailToast(isSaved?'Đã bỏ lưu Tier List.':'Đã lưu Tier List.');
    }catch(error){
        console.error('Cannot toggle saved detail tier list:',error);
        showDetailToast(error.message||'Không cập nhật được trạng thái lưu Tier List.','error');
    }finally{
        if(button){
            button.disabled=false;
            if(button.isConnected){
                renderDetailSaveButton();
            }else{
                button.textContent=originalText;
            }
        }
    }
}

async function deleteCurrentTierList(){
    if(!tierDetailData||!tierDetailId) return;
    if(!canCurrentUserDeleteTierList()){
        showDetailToast('Bạn không có quyền xóa Tier List này.','error');
        return;
    }
    const confirmed=window.confirm('Bạn có chắc muốn xóa Tier List này không? Hành động này không thể hoàn tác.');
    if(!confirmed) return;

    const button=document.getElementById('tier-detail-delete-btn');
    const originalText=button?.textContent;
    if(button){
        button.disabled=true;
        button.textContent='Đang xóa...';
    }
    try{
        const request=buildTierListDeleteRequest(tierDetailId);
        const response=await fetch(request.url,request.options);
        if(!response.ok){
            await logTierListDeleteFailure(request.url,response);
            throw new Error(resolveTierDeleteErrorMessage(response.status,await readDetailApiError(response)));
        }
        window.location.href='/html/tier-list.html';
    }catch(error){
        console.error('Cannot delete tier list:',error);
        showDetailToast(error.message||'Không xóa được Tier List.','error');
    }finally{
        if(button){
            button.disabled=false;
            button.textContent=originalText;
        }
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
        await Promise.all([loadTierDetail(),loadTierSummary()]);
        renderRatingSummary();
        renderDetailSaveButton();
        renderDeleteControls();
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
        document.getElementById('tier-detail-save-btn').onclick=toggleDetailSavedState;
        document.getElementById('tier-detail-delete-btn').onclick=deleteCurrentTierList;
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
