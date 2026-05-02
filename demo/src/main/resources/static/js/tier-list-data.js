const heroes=[];

const heroImageMap={"Aoi":"Aoi.jpeg","Iggy":"Iggy.jpeg","Arthur":"Athur.jpg","Grakk":"Grakk.png","Alice":"Alice.png","Kahli":"Kahlii.png","Kriknak":"Kriknak.png","Krixi":"Krixi.png","Krizzix":"Krizzix.png","Mina":"Mina.png","Slimz":"Slimz.png","Thane":"Thane.png","Kil'Groth":"Kil'Groth.gif","Lu Bu":"L\u1eef B\u1ed1.jpg","Wukong":"Ng\u1ed9 Kh\u00f4ng.jpg","Zanis":"Tri\u1ec7u V\u00e2n.jpg","Diaochan":"\u0110i\u00eau Thuy\u1ec1n.jpg","Ormarr":"Omarr.jpg","Rourka":"Rourke.jpg","Thorne":"Thorn.jpg","Wonder Woman":"Wonder Women.jpg","Zephys":"Zephis.jpg","Riktor":"Richter.jpg","Flowborn (Marksman)":"Flowborn (ADL).jpg","D'Arcy":"D'Arcy.jpg","Flowborn":"Flowborn (ADL).jpg","Flowborn ADL":"Flowborn (ADL).jpg","Flowborn MID":"Flowborn (MID).jpg","Ngo Khong":"Ng\u1ed9 Kh\u00f4ng.jpg","Trieu Van":"Tri\u1ec7u V\u00e2n.jpg","Dieu Thuyen":"\u0110i\u00eau Thuy\u1ec1n.jpg","Lu Bo":"L\u1eef B\u1ed1.jpg","Roule":"Rouie.jpg","Governa":"Goverra.jpg","Tochi":"Tachi.jpg","Richter":"Richter.jpg","KilGroth":"Kil'Groth.gif"};

const HERO_API='/api/wiki/heroes';
const heroByIdMap=new Map();
const heroByNameMap=new Map();

function rebuildHeroIndexes(){
    heroByIdMap.clear();
    heroByNameMap.clear();
    heroes.forEach(hero=>{
        if(hero.id!==undefined&&hero.id!==null) heroByIdMap.set(String(hero.id),hero);
        if(hero.name) heroByNameMap.set(normalizeHeroName(hero.name),hero);
    });
}

async function loadHeroesFromApi(){
    heroes.length=0;
    heroByIdMap.clear();
    heroByNameMap.clear();
    try{
        const response=await fetch(HERO_API,{headers:{Accept:'application/json'},cache:'no-store'});
        if(!response.ok) throw new Error('HTTP '+response.status);
        const data=await response.json();
        if(!Array.isArray(data)) throw new Error('Invalid hero payload');
        data.forEach(hero=>heroes.push({
            id:hero.id,
            name:hero.name,
            role:Array.isArray(hero.classes)&&hero.classes.length?hero.classes[0]:(hero.heroClass||''),
            classes:Array.isArray(hero.classes)?hero.classes:(hero.heroClass?[hero.heroClass]:[]),
            laneRoles:Array.isArray(hero.laneRoles)?hero.laneRoles:(Array.isArray(hero.roles)?hero.roles:[]),
            attributes:Array.isArray(hero.attributes)?hero.attributes:[],
            avatarUrl:hero.avatarUrl||''
        }));
        rebuildHeroIndexes();
    }catch(error){
        console.error('Cannot load hero catalog:',error);
    }
}

function getHeroFromValue(value){
    if(value&&typeof value==='object'){
        const id=value.heroId??value.id;
        if(id!==undefined&&id!==null&&heroByIdMap.has(String(id))) return heroByIdMap.get(String(id));
        const name=value.name||value.heroName;
        if(name&&heroByNameMap.has(normalizeHeroName(name))) return heroByNameMap.get(normalizeHeroName(name));
    }
    if(typeof value==='number'&&heroByIdMap.has(String(value))) return heroByIdMap.get(String(value));
    if(typeof value==='string'){
        if(/^\d+$/.test(value)&&heroByIdMap.has(value)) return heroByIdMap.get(value);
        const name=normalizeHeroName(value);
        if(heroByNameMap.has(name)) return heroByNameMap.get(name);
    }
    return null;
}

function getHeroNameFromValue(value){
    const hero=getHeroFromValue(value);
    if(hero) return hero.name;
    if(value&&typeof value==='object') return normalizeHeroName(value.name||value.heroName||`Hero #${value.heroId??value.id??''}`.trim());
    return normalizeHeroName(value);
}

function getHeroIdFromValue(value){
    const hero=getHeroFromValue(value);
    if(hero?.id!==undefined&&hero.id!==null) return hero.id;
    if(value&&typeof value==='object') return value.heroId??value.id??null;
    if(typeof value==='number') return value;
    if(typeof value==='string'&&/^\d+$/.test(value)) return Number(value);
    return null;
}

function getHeroRefForStorage(value){
    const hero=getHeroFromValue(value);
    if(hero?.id) return {heroId:hero.id,name:hero.name};
    const name=getHeroNameFromValue(value);
    const id=getHeroIdFromValue(value);
    return id?{heroId:id,name}:{name};
}

function normalizeHeroName(name){
    const aliases={"Flowborn":"Flowborn (Marksman)","Flowborn ADL":"Flowborn (Marksman)","Flowborn MID":"Flowborn (Marksman)","Ngo Khong":"Wukong","Trieu Van":"Zanis","Dieu Thuyen":"Diaochan","Lu Bo":"Lu Bu","Roule":"Rouie","Governa":"Goverra","Tochi":"Tachi","Richter":"Riktor","KilGroth":"Kil'Groth"};
    const trimmed=String(name||'').trim();
    return aliases[trimmed]||trimmed;
}

function getHeroImgUrl(name){
    const hero=getHeroFromValue(name);
    if(hero?.avatarUrl) return hero.avatarUrl;
    const heroName=normalizeHeroName(name);
    const f=heroImageMap[heroName]||`${heroName}.jpg`;
    return `/images/heroes/${encodeURI(f).replace(/'/g,"%27")}?v=3`;
}

const DUMMY_COMMUNITY=[];
